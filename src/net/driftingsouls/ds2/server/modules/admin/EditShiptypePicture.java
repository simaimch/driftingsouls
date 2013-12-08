/*
 *	Drifting Souls 2
 *	Copyright (c) 2008 Christopher Jung
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
package net.driftingsouls.ds2.server.modules.admin;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.DynamicContentManager;
import net.driftingsouls.ds2.server.framework.db.HibernateUtil;
import net.driftingsouls.ds2.server.modules.AdminController;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipModules;
import net.driftingsouls.ds2.server.ships.ShipType;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;

/**
 * Aktualisierungstool fuer Schiffstypen-Grafiken.
 *
 * @author Christopher Jung
 */
@AdminMenuEntry(category = "Schiffe", name = "Typengrafik editieren")
public class EditShiptypePicture extends AbstractEditPlugin implements AdminPlugin
{
	private static final Logger log = LogManager.getLogger(EditShiptypePicture.class);

	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();

		int shipid = context.getRequest().getParameterInt("entityId");

		this.beginSelectionBox(echo, page, action);
		List<ShipType> shipTypes = Common.cast(db.createQuery("from ShipType order by id").list());
		for( ShipType st : shipTypes )
		{
			this.addSelectionOption(echo, st.getId(), st.getNickname()+" ("+st.getId()+")");
		}
		this.endSelectionBox(echo);

		if(this.isUpdateExecuted() && shipid != 0)
		{
			ShipType shipType = (ShipType)db.get(ShipType.class, shipid);

			if(shipType != null) {
				String img = this.processDynamicContent("image", shipType.getPicture());
				String oldImg = shipType.getPicture();
				shipType.setPicture("data/dynamicContent/"+img);
				if( oldImg.startsWith("data/dynamicContent/") )
				{
					DynamicContentManager.remove(oldImg);
				}

				echo.append("<p>Update abgeschlossen.</p>");
			}
			else {
				echo.append("<p>Kein Schiffstyp gefunden.</p>");
			}

			recalculateShipModules(db, shipType);
		}

		if(shipid != 0)
		{
			ShipType shipType = (ShipType)db.get(ShipType.class, shipid);

			if(shipType == null)
			{
				return;
			}

			this.beginEditorTable(echo, page, action, shipid);
			this.editLabel(echo, "Name", shipType.getNickname());
			this.editDynamicContentField(echo, "Bild", "image", shipType.getPicture());

			Number count = (Number)db.createQuery("select count(*) from Ship s where s.shiptype=:type and s.modules is not null")
				.setParameter("type", shipType)
				.iterate()
				.next();

			this.editLabel(echo, "Zu aktualisieren", count + " Schiffe mit Modulen");
			this.endEditorTable(echo);
		}
	}

	private void recalculateShipModules(org.hibernate.Session db, ShipType shipType)
	{
		int count = 0;

		ScrollableResults ships = db.createQuery("from Ship s left join fetch s.modules where s.shiptype= :type")
			.setEntity("type", shipType)
			.setCacheMode(CacheMode.IGNORE)
			.scroll(ScrollMode.FORWARD_ONLY);
		while (ships.next())
		{
			Ship ship = (Ship) ships.get(0);
			try
			{
				ship.recalculateModules();

				count++;
				if (count % 20 == 0)
				{
					db.flush();
					HibernateUtil.getSessionFactory().getCurrentSession().evict(Ship.class);
					HibernateUtil.getSessionFactory().getCurrentSession().evict(ShipModules.class);
					HibernateUtil.getSessionFactory().getCurrentSession().evict(Offizier.class);
				}
			}
			catch(Exception e)
			{
				//Riskant, aber, dass nach einem Fehler alle anderen Schiffe nicht aktualisiert werden muss verhindert werden
				log.error("Das Schiff mit der ID " + ship.getId() + " konnte nicht aktualisiert werden. Fehler: " + e.getMessage());
			}
		}
	}
}
