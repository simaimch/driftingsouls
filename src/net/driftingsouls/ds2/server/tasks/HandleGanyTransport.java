/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.driftingsouls.ds2.server.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.entities.FactionShopEntry;
import net.driftingsouls.ds2.server.entities.FactionShopOrder;
import net.driftingsouls.ds2.server.entities.JumpNode;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.db.SQLResultRow;
import net.driftingsouls.ds2.server.ships.JumpNodeRouter;
import net.driftingsouls.ds2.server.ships.Ship;

import org.apache.commons.lang.StringUtils;

/**
 * TASK_GANY_TRANSPORT
 * 		Ein Gany-Transportauftrag.
 *
 * 	- data1 -> die Order-ID des Auftrags
 *  - data2 -> Schiffs-ID [Wird von der Task selbst gesetzt!]
 *  - data3 -> Status [autom. gesetzt: Nichts = Warte auf Schiff od. flug zur Ganymede; 1 = Gany-Transport; 2 = Rueckweg]
 *
 *  @author Christopher Jung
 */
class HandleGanyTransport implements TaskHandler
{
	@Override
	public void handleEvent(Task task, String event)
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();
		Taskmanager tm = Taskmanager.getInstance();

		int orderid = Integer.parseInt(task.getData1());
		FactionShopOrder order = (FactionShopOrder)db.get(FactionShopOrder.class, orderid);

		if( event.equals("tick_timeout") )
		{
			doTickTimeout(tm, task, order);
		}
		else if( event.equals("spa_start") )
		{
			tm.modifyTask( task.getTaskID(), task.getData1(), task.getData2(), "1" );
		}
		else if( event.equals("spa_completed") )
		{
			order.setStatus(4);

			tm.modifyTask( task.getTaskID(), task.getData1(), task.getData2(), "2" );
		}
		else if( event.equals("spa_error") )
		{
			order.setStatus(3);

			tm.modifyTask( task.getTaskID(), task.getData1(), task.getData2(), "2" );
		}
		else if( event.equals("spa_end") )
		{
			tm.removeTask( task.getTaskID() );
		}
	}

	private void doTickTimeout(Taskmanager tm, Task task, FactionShopOrder order)
	{
		Context context = ContextMap.getContext();
		org.hibernate.Session db = context.getDB();

		FactionShopEntry entry = order.getShopEntry();
		if( entry.getType() != 2 ) {
			String[] tmp = StringUtils.split(order.getAddData(), '@');
			int ganyid = Integer.parseInt(tmp[0]);
			tmp = StringUtils.split(tmp[1], "->");
			Location source = Location.fromString(tmp[0]);
			Location target = Location.fromString(tmp[1]);

			Ship ship = findBestTransporter(entry, source, target);

			if( ship != null )
			{
				Map<Integer,List<JumpNode>> jumpnodes = new HashMap<Integer,List<JumpNode>>();
				Map<Integer,JumpNode> jumpnodeindex = new HashMap<Integer,JumpNode>();

				List<?> jnList = db.createQuery("from JumpNode where hidden=0")
					.list();
				for( Object obj : jnList )
				{
					JumpNode jn = (JumpNode)obj;
					if( !jumpnodes.containsKey(jn.getSystem()) ) {
						jumpnodes.put(jn.getSystem(), new ArrayList<JumpNode>());
					}
					jumpnodes.get(jn.getSystem()).add(jn);
					jumpnodeindex.put(jn.getId(), jn);
				}

				JumpNodeRouter.Result shortestpath = new JumpNodeRouter(jumpnodes)
					.locateShortestJNPath(source.getSystem(),source.getX(),source.getY(),
							target.getSystem(),target.getX(),target.getY());
				if( shortestpath == null ) {
					User sourceUser = (User)db.get(User.class, -1);

					String msg = "[color=orange]WARNUNG[/color]\nDer Taskmanager kann keinen Weg f&uuml;r die Gany-Transport-Order mit der ID "+order.getId()+" finden.";
					PM.sendToAdmins(sourceUser, "Taskmanager-Warnung", msg, 0);

					tm.incTimeout( task.getTaskID() );
					return;
				}

				JumpNodeRouter.Result pathtogany = new JumpNodeRouter(jumpnodes)
					.locateShortestJNPath(ship.getSystem(),ship.getX(),ship.getY(),
							source.getSystem(),source.getX(),source.getY());
				if( pathtogany == null ) {
					User sourceUser = (User)db.get(User.class, -1);

					// Eigenartig....es gibt kein Weg zur Gany. Pausieren wir besser mal
					String msg = "[color=orange]WARNUNG[/color]\nDer Taskmanager kann keinen Weg zur Ganymede f&uuml;r die Gany-Transport-Order mit der ID "+order.getId()+" finden.";

					PM.sendToAdmins(sourceUser, "Taskmanager-Warnung", msg, 0);

					tm.incTimeout( task.getTaskID() );
					return;
				}

				JumpNodeRouter.Result pathtohome = new JumpNodeRouter(jumpnodes)
					.locateShortestJNPath(target.getSystem(),target.getX(),target.getY(),
							ship.getSystem(),ship.getX(),ship.getY());
				JumpNodeRouter.Result pathback = new JumpNodeRouter(jumpnodes)
					.locateShortestJNPath(source.getSystem(),source.getX(),source.getY(),
							ship.getSystem(),ship.getX(),ship.getX());

				StringBuilder script = new StringBuilder(300);
				for( int i=0; i < pathtogany.path.size(); i++ ) {
					JumpNode jn = pathtogany.path.get(i);
					script.append("!SHIPMOVE "+jn.getSystem()+":"+jn.getX()+"/"+jn.getY()+"\n");
					script.append("!NJUMP "+jn.getId()+"\n");
				}
				script.append("!SHIPMOVE "+source.getSystem()+":"+source.getX()+"/"+source.getY()+"\n");

				script.append("!GETSHIPOWNER "+ganyid+"\n");
				script.append("!COMPARE #A "+order.getUser().getId()+"\n");
				script.append("!JNE error\n");

				script.append("!DOCK "+ganyid+"\n");
				script.append("!EXECUTETASK "+task.getTaskID()+" start\n");

				for( int i=0; i < shortestpath.path.size(); i++ ) {
					JumpNode jn = shortestpath.path.get(i);
					script.append("!SHIPMOVE "+jn.getSystem()+":"+jn.getX()+"/"+jn.getY()+"\n");
					script.append("!NJUMP "+jn.getId()+"\n");
				}
				script.append("!SHIPMOVE "+target.getSystem()+":"+target.getX()+"/"+target.getY()+"\n");

				script.append("!UNDOCK "+ganyid+"\n");
				script.append("!EXECUTETASK "+task.getTaskID()+" completed\n");
				script.append("!JUMP ende\n");

				script.append(":error\n");
				script.append("#title = \"Shop-Fehler: Order "+order.getId()+"\"\n");
				script.append("#msg = \"Der Besitzer der Gany "+ganyid+" konnte nicht korrekt &uuml;berpr&uuml;ft werden. Erwartet wurde ID "+order.getUser().getId()+"\"\n");
				script.append("!MSG "+entry.getFaction()+" #title #msg\n");
				script.append("!EXECUTETASK "+task.getTaskID()+" error\n");
				for( int i=0; i < pathback.path.size(); i++ ) {
					JumpNode jn = pathback.path.get(i);
					script.append("!SHIPMOVE "+jn.getSystem()+":"+jn.getX()+"/"+jn.getY()+"\n");
					script.append("!NJUMP "+jn.getId()+"\n");
				}

				script.append("!SHIPMOVE "+ship.getSystem()+":"+ship.getX()+"/"+ship.getY()+"\n");
				script.append("!WAIT\n");
				script.append("!WAIT\n");
				script.append("!WAIT\n");
				script.append("!WAIT\n");
				script.append("!EXECUTETASK "+task.getTaskID()+" end\n");
				script.append("!RESETSCRIPT\n");

				script.append(":ende\n");
				for( int i=0; i < pathtohome.path.size(); i++ ) {
					JumpNode jn = pathtohome.path.get(i);
					script.append("!SHIPMOVE "+jn.getSystem()+":"+jn.getX()+"/"+jn.getY()+"\n");
					script.append("!NJUMP "+jn.getId()+"\n");
				}
				script.append("!SHIPMOVE "+ship.getSystem()+":"+ship.getX()+"/"+ship.getY()+"\n");
				script.append("!WAIT\n");
				script.append("!WAIT\n");
				script.append("!WAIT\n");
				script.append("!WAIT\n");
				script.append("!EXECUTETASK "+task.getTaskID()+" end\n");
				script.append("!RESETSCRIPT\n");

				ship.setScript(script.toString());
				tm.modifyTask( task.getTaskID(), task.getData1(), Integer.toString(ship.getId()), task.getData3() );

				order.setStatus(2);
			}
			else {
				order.setStatus(1);
				tm.incTimeout( task.getTaskID() );
			}
		}
		else {
			User sourceUser = (User)db.get(User.class, -1);

			String msg = "[color=orange]WARNUNG[/color]\nDer Taskmanager kann die Gany-Transport-Order mit der ID "+order.getId()+" nicht finden.";
			PM.sendToAdmins(sourceUser, "Taskmanager-Warnung", msg, 0);
			tm.removeTask( task.getTaskID() );
		}
	}

	private Ship findBestTransporter(FactionShopEntry entryowner, Location source, Location target)
	{
		Context context = ContextMap.getContext();
		Database database = context.getDatabase();
		org.hibernate.Session db = context.getDB();

		SQLResultRow shiptrans = database.first("SELECT * FROM ships " +
				"WHERE owner=",entryowner.getFaction()," AND system=",source.getSystem()," AND " +
						"scriptData_id AND LOCATE('#!/tm gany_transport',destcom)");
		if( shiptrans.isEmpty() ) {
			shiptrans = database.first("SELECT * FROM ships WHERE owner=",entryowner.getFaction()," AND system=",target.getSystem()," AND scriptData_id AND LOCATE('#!/tm gany_transport',destcom)");
		}
		if( shiptrans.isEmpty() ) {
			shiptrans = database.first("SELECT * FROM ships WHERE owner=",entryowner.getFaction()," AND scriptData_id AND LOCATE('#!/tm gany_transport',destcom)");
		}
		if( !shiptrans.isEmpty() )
		{
			return (Ship)db.get(Ship.class, shiptrans.getInt("id"));
		}
		return null;
	}

}
