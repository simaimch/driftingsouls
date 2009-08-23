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
package net.driftingsouls.ds2.server.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ResourceEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.comm.PM;
import net.driftingsouls.ds2.server.config.items.Items;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipFleet;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypes;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableLong;

/**
 * Transfer von Waren zwischen Basen und Schiffen.
 * @author Christopher Jung
 *
 */
public class TransportController extends TemplateGenerator {
	private static class MultiTarget {
		private String name;
		private String targetlist;
		
		MultiTarget( String name, String targetlist ) {
			this.name = name;
			this.targetlist = targetlist;
		}
		
		/**
		 * Gibt den Namen des MultiTargets zurueck.
		 * @return Der Name
		 */
		String getName() {
			return name;
		}
		
		/**
		 * Gibt eine |-separierte Liste mit Zielen zurueck.
		 * @return Liste der Ziele
		 */
		String getTargetList() {
			return targetlist;
		}
	}
	
	private abstract static class TransportFactory {
		TransportFactory() {
			//EMPTY
		}
		
		abstract List<TransportTarget> createTargets(int role, String target) throws Exception;
	}
	
	private static class BaseTransportFactory extends TransportFactory {
		BaseTransportFactory() {
			// EMPTY
		}
		
		@Override
		List<TransportTarget> createTargets(int role, String target) throws Exception {
			List<TransportTarget> list = new ArrayList<TransportTarget>();
			
			int[] fromlist = Common.explodeToInt("|", target);
			for( int i=0; i < fromlist.length; i++ ) {
				TransportTarget handler = new BaseTransportTarget();
				handler.create( role, fromlist[i] );
				if( list.size() > 0 ) {
					Location loc = list.get(0).getLocation();
					Location thisLoc = handler.getLocation();
					if( !loc.sameSector(list.get(0).getSize(), thisLoc, handler.getSize()) ) {
						continue;
					}
				}
				list.add(handler);
			}
			
			return list;
		}
	}
	
	private static class ShipTransportFactory extends TransportFactory {
		ShipTransportFactory() {
			// EMPTY
		}
		
		@Override
		List<TransportTarget> createTargets(int role, String target) throws Exception {
			List<TransportTarget> list = new ArrayList<TransportTarget>();
					
			String[] fromlist = StringUtils.split(target, '|');
			for( int i=0; i < fromlist.length; i++ ) {				
				if( fromlist[i].equals("fleet") ) {
					if( list.size() == 0 ) {
						throw new Exception("Es wurde kein Schiff angegeben, zu dem die Flotte ausgewaehlt werden soll");
					}
					ShipTransportTarget handler  = (ShipTransportTarget)list.remove(list.size()-1);
					if( handler.getFleet() == null ) {
						throw new Exception("Das angegebene Schiff befindet sich in keiner Flotte");
					}
					
					org.hibernate.Session db = ContextMap.getContext().getDB();
					
					Location loc = handler.getLocation();
					
					List<?> fleetlist = db.createQuery("from Ship " +
							"where id>0 and fleet=? and x=? and y=? and system=?")
							.setEntity(0, handler.getFleet())
							.setInteger(1, loc.getX())
							.setInteger(2, loc.getY())
							.setInteger(3, loc.getSystem())
							.list();
					for( Iterator<?> iter=fleetlist.iterator(); iter.hasNext(); ) {
						ShipTransportTarget shiphandler = new ShipTransportTarget();
						shiphandler.create(role, (Ship)iter.next());
						list.add(shiphandler);
					}
				}
				else {
					ShipTransportTarget handler = new ShipTransportTarget();
					handler.create( role, Integer.parseInt(fromlist[i]) );
					if( list.size() > 0 ) {
						Location loc = list.get(0).getLocation();
						Location thisLoc = handler.getLocation();
						if( !loc.sameSector(list.get(0).getSize(), thisLoc, handler.getSize()) ) {
							continue;
						}
					}
					list.add(handler);
				}
			}
			
			return list;
		}
	}
	
	private abstract static class TransportTarget {
		protected int role;
		protected int id;
		protected int owner;
		protected Cargo cargo;
		protected long maxCargo;
		
		static final int ROLE_SOURCE = 1;
		static final int ROLE_TARGET = 1;
		
		/**
		 * Konstruktor.
		 *
		 */
		public TransportTarget() {
			// EMPTY
		}
		
		/**
		 * Erstellt ein neues TransportTarget.
		 * @param role Die Rolle (Source oder Target)
		 * @param id Die ID
		 * @throws Exception
		 */
		void create(int role, int id) throws Exception {
			this.role = role;
			this.id = id;
		}
		
		/**
		 * Gibt die ID des Objekts zurueck.
		 * @return Die ID
		 */
		int getId() {
			return this.id;
		}
		
		/**
		 * Gibt den Radius des Objekts zurueck.
		 * @return Der Radius
		 */
		abstract int getSize();
		
		/**
		 * Gibt die ID des Besitzers zurueck.
		 * @return Die ID des Besitzers
		 */
		int getOwner() {
			return owner;
		}
		
		/**
		 * Setzt den Besitzer auf den angegebenen Wert.
		 * @param owner Der neue Besitzer
		 */
		void setOwner(int owner) {
			this.owner = owner;
		}
			
		/**
		 * Gibt den maximalen Cargo zurueck.
		 * @return Der maximale Cargo
		 */
		long getMaxCargo() {
			return maxCargo;
		}
		
		/**
		 * Setzt den maximalen Cargo.
		 * @param maxcargo der neue maximale Cargo
		 */
		void setMaxCargo(long maxcargo) {
			this.maxCargo = maxcargo;
		}
		
		/**
		 * Gibt den Cargo zurueck.
		 * @return Der Cargo
		 */
		Cargo getCargo() {
			return cargo;
		}
		
		/**
		 * Setzt den Cargo auf den angegebenen Wert.
		 * @param cargo der neue Cargo
		 */
		void setCargo(Cargo cargo) {
			this.cargo = cargo;
		}
		
		/**
		 * Gibt die Position des Objekts zurueck.
		 * @return Die Position
		 */
		abstract Location getLocation();
		
		/**
		 * Schreibt die Daten in die Datenbank.
		 */
		abstract void write();
		/**
		 * Gibt die MultiTarget-Variante zurueck.
		 * Wenn keine MultiTarget-Variante verfuegbar ist, so wird <code>null</code> zurueckgegeben
		 * @return Die MultiTarget-Variante oder <code>null</code>
		 */
		abstract MultiTarget getMultiTarget();
		
		/**
		 * Gibt den Namen des Target-Typen zurueck.
		 * @return Der Name
		 */
		abstract String getTargetName();
		
		/**
		 * Gibt den Namen des konkreten Objekts zurueck (z.B. der Name des Schiffes/der Basis).
		 * @return Der Name
		 */
		abstract String getObjectName();
	}
	
	private static class ShipTransportTarget extends TransportTarget {
		private Ship ship;
		
		/**
		 * Konstruktor.
		 */
		public ShipTransportTarget() {
			// EMPTY
		}
		
		void create(int role, Ship ship) throws Exception {
			if( (ship == null) || (ship.getId() < 0) ) {
				throw new Exception("Eines der angegebenen Schiffe existiert nicht");
			}
			
			super.create(role, ship.getId());
			
			if( ship.getBattle() != null ) {
				throw new Exception("Das Schiff (id:"+ship.getId()+") ist in einen Kampf verwickelt");
			}

			if( role == ROLE_TARGET ) {
				User user = (User)ContextMap.getContext().getActiveUser();
				if( (ship.getStatus().indexOf("disable_iff") > -1) && (ship.getOwner() != user) ) {
					throw new Exception("Zu dem angegebenen Schiff (id:"+ship.getId()+") k&ouml;nnen sie keine Waren transportieren");
				}
			}
			
			ShipTypeData tmptype = ship.getTypeData();
			
			if( tmptype.hasFlag(ShipTypes.SF_KEIN_TRANSFER) ) {
				throw new Exception("Sie k&ouml;nnen keine Waren zu oder von diesem Schiff (id:"+ship.getId()+") transferieren");
			}
			
			setOwner(ship.getOwner().getId());
			setMaxCargo(tmptype.getCargo());
			this.ship = ship;
			setCargo(ship.getCargo());	
		}
		
		@Override
		void create(int role, int shipid) throws Exception {
			org.hibernate.Session db = ContextMap.getContext().getDB();
			
			Ship ship = (Ship)db.get(Ship.class, shipid);
			
			create(role, ship);
		}
		
		@Override
		MultiTarget getMultiTarget() {
			ShipFleet fleet = ship.getFleet();
			if( fleet == null ) {
				return null;
			}
			
			return new MultiTarget("Flotte", ship.getId()+"|fleet");
		}

		@Override
		String getTargetName() {
			return "Schiff";
		}

		@Override
		void write() {
			this.ship.setCargo(getCargo());
			this.ship.recalculateShipStatus();
		}
		
		@Override
		Location getLocation() {
			return this.ship.getLocation();
		}
		
		@Override
		String getObjectName() {
			return this.ship.getName();
		}
		
		@Override
		int getSize() {
			return 0;
		}
		
		/**
		 * Gibt die Flotte zurueck, zu der das Schiff gehoert.
		 * @return Die Flotte
		 */
		ShipFleet getFleet() {
			return this.ship.getFleet();
		}
	}
	
	private static class BaseTransportTarget extends TransportTarget {
		private Base base;
		
		/**
		 * Konstruktor.
		 */
		public BaseTransportTarget() {
			// EMPTY
		}
		
		@Override
		void create(int role, int baseid) throws Exception {
			super.create(role, baseid);
			org.hibernate.Session db = ContextMap.getContext().getDB();
			
			Base base = (Base)db.get(Base.class, baseid);

			if( base == null ) {
				throw new Exception("Die angegebene Basis (id:"+baseid+") existiert nicht");
			}

			setOwner(base.getOwner().getId());
			setMaxCargo(base.getMaxCargo());

			setCargo(base.getCargo());
			this.base = base;
		}

		@Override
		MultiTarget getMultiTarget() {
			return null;
		}

		@Override
		String getTargetName() {
			return "Basis";
		}

		@Override
		void write() {
			base.setCargo(getCargo());
		}
		
		@Override
		Location getLocation() {
			return base.getLocation();
		}
		
		@Override
		String getObjectName() {
			return base.getName();
		}
		
		@Override
		int getSize() {
			return base.getSize();
		}
	}
	private String[] way;
	
	private List<TransportTarget> from;
	private List<TransportTarget> to;
	
	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public TransportController(Context context) {
		super(context);
		
		from = new ArrayList<TransportTarget>();
		to = new ArrayList<TransportTarget>();
		
		setTemplate("transport.html");
		
		parameterString("from");
		parameterString("to");
		parameterString("way");
		
		setPageTitle("Warentransfer");
	}
	
	@Override
	protected boolean validateAndPrepare(String action) {
		String to = getString("to");
		String from = getString("from");
		String rawway = getString("way");
				
		String[] way = StringUtils.split(rawway, "to");

		Map<String,TransportFactory> wayhandler = new HashMap<String,TransportFactory>();
		wayhandler.put("s", new ShipTransportFactory());
		wayhandler.put("b", new BaseTransportFactory());
		
		/*
			"From" bearbeiten
		*/
	
		if( wayhandler.containsKey(way[0]) ) {
			try {
				this.from.addAll(wayhandler.get(way[0]).createTargets(TransportTarget.ROLE_SOURCE, from));
			}
			catch( Exception e ) {
				e.printStackTrace();
				addError(e.toString());
				return false;
			}
		}
		else {
			addError("Ung&uuml;ltige Transportquelle", "./ds?module=ueber" );

			return false;
		}

		/*
			"To" bearbeiten
		*/
		if( wayhandler.containsKey(way[1]) ) {
			try {
				this.to.addAll(wayhandler.get(way[1]).createTargets(TransportTarget.ROLE_TARGET, to));
			}
			catch( Exception e ) {
				e.printStackTrace();
				addError(e.toString());
				return false;
			}
		}
		else {
			addError( "Ung&uuml;ltiges Transportziel", "./ds?module=ueber" );
			
			return false;
		}
		
		if( (this.from.size() == 0) || (this.to.size() == 0) ) {
			addError("Sie muessen mindestens ein Quell- und ein Zielobjekt angeben");
			
			return false;
		}
		
		/*
			Check ob das selbe Objekt in Quelle in Ziel vorkommt
		*/
		if( way[0].equals(way[1]) ) {
			for( int i=0; i < this.from.size(); i++ ) {
				TransportTarget afrom = this.from.get(i);
				for( int j=0; j < this.to.size(); j++ ) {
					if( this.to.get(j).getId() == afrom.getId() ) {
						addError("Sie k&ouml;nnen keine Waren zu sich selbst transportieren",(way[0]=="b"?"./ds?module=base&":"./ds?module=schiff&")+"&"+(way[0]=="b"?"col":"ship")+"="+afrom);
						return false;
					}
				}
			}
		}
		
		/*
			Sind die beiden Objekte auch im selben Sektor?
		*/
		Location fromLoc = this.from.get(0).getLocation();
		Location toLoc = this.to.get(0).getLocation();
		if( !fromLoc.sameSector( this.from.get(0).getSize(), toLoc, this.to.get(0).getSize()) ) {
			addError("Die angegebenen Objekte befinden sich nicht im selben Sektor" );
			
			return false;
		}
		
		for( int i=1; i < this.from.size(); i++ ) {
			if( !fromLoc.sameSector( this.from.get(0).getSize(), this.from.get(i).getLocation(), this.from.get(i).getSize()) ) {
				addError("Die angegebenen Objekte befinden sich nicht im selben Sektor" );
				
				return false;
			}
		}
		
		for( int i=1; i < this.to.size(); i++ ) {
			if( !toLoc.sameSector( this.to.get(0).getSize(), this.to.get(i).getLocation(), this.to.get(i).getSize()) ) {
				addError("Die angegebenen Objekte befinden sich nicht im selben Sektor" );
				
				return false;
			}
		}

		for( TransportTarget afrom : this.from ) {
			if( afrom.getOwner() != getUser().getId() ) {
				addError("Das Schiff geh&ouml;rt ihnen nicht", Common.buildUrl("default", "module", "ueber") );
				
				return false;
			}
		}
		
		this.way = way;

		return true;
	}
	
	private long transferSingleResource(TransportTarget fromItem, TransportTarget toItem, ResourceEntry res, long count, Cargo newfromc, Cargo newtoc, MutableLong cargofrom, MutableLong cargoto, StringBuilder msg, char mode) {
		TemplateEngine t = getTemplateEngine();
		
		t.setVar(
				"transfer.notenoughcargo", 0,
				"transfer.notenoughspace", 0 );
		
		if( count > newfromc.getResourceCount( res.getId() ) ) {
			t.setVar(	"transfer.notenoughcargo",	1,
						"transfer.from.cargo",		Common.ln(newfromc.getResourceCount(res.getId()) ) );
						
			count = newfromc.getResourceCount( res.getId() );
			if( count < 0 ) {
				count = 0;	
			}
		}
					
		if( cargoto.longValue() - Cargo.getResourceMass( res.getId(), count ) < 0 ) {
			count = cargoto.longValue() / Cargo.getResourceMass( res.getId(), 1 );
			
			if( count < 0 ) {
				Common.writeLog("transport.error.log", Common.date("d.m.y H:i:s")+": "+getUser().getId()+" -> "+toItem.getOwner()+" | "+getString("from")+" -> "+getString("to")+" ["+getString("way")+"] : "+mode+res.getId()+"@"+count+" ; "+msg+"\n---------\n");
				count = 0;
			}
						
			t.setVar(	"transfer.notenoughspace",	1,
						"transfer.count.new",		Common.ln(count) );
		}
		
		newtoc.addResource( res.getId(), count );
		newfromc.substractResource( res.getId(), count );
		
		if( count > 0 ) {
			msg.append("[resource="+res.getId()+"]"+count+"[/resource] umgeladen\n");
		}
		
		if( mode == 't' ) {
			cargofrom.setValue(fromItem.getMaxCargo() - newfromc.getMass());
			cargoto.setValue(toItem.getMaxCargo() - newtoc.getMass());
		}
		else {
			cargofrom.setValue(toItem.getMaxCargo() - newfromc.getMass());
			
			cargoto.setValue(fromItem.getMaxCargo() - newtoc.getMass());
		}
			
		if( (fromItem.getOwner() == toItem.getOwner()) || (toItem.getOwner() == 0) ) {
			t.setVar(	"transfer.reportnew",		1,
						"transfer.count.complete",	Common.ln(newtoc.getResourceCount(res.getId()) ) );
		}	
		
		return count;
	}
	
	/**
	 * Transferiert die Waren.
	 * @urlparam Integer $resid+"to" Die Menge von $resid, welche zum Zielschiff transferiert werden soll
	 * @urlparam Integer $resid+"from" Die Menge von $resid, welche von Zielschiff runter zum Quellschiff transferiert werden soll
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void transferAction() {
		TemplateEngine t = getTemplateEngine();
		t.setBlock( "_TRANSPORT", "transfer.listitem", "transfer.list" );

		boolean transfer = false;
		List<TransportTarget> tolist = this.to;

		if( this.to.size() == 1 ) {
			t.setVar( "transfer.multitarget", 0 );		
		}			
			
		List<Cargo> newtoclist = new ArrayList<Cargo>();
		List<Long> cargotolist = new ArrayList<Long>();
		Cargo totaltocargo = new Cargo();
		
		List<Cargo> newfromclist = new ArrayList<Cargo>();
		List<Long> cargofromlist = new ArrayList<Long>();
		Cargo totalfromcargo = new Cargo();
		
		// TODO: rewrite
		for( int k=0; k < tolist.size(); k++ ) {
			newtoclist.add(k, (Cargo)tolist.get(k).getCargo().clone());
			totaltocargo.addCargo( tolist.get(k).getCargo() );
			cargotolist.add(k, tolist.get(k).getMaxCargo() - tolist.get(k).getCargo().getMass());
		}
		
		for( int k=0; k < from.size(); k++ ) {
			newfromclist.add(k, (Cargo)from.get(k).getCargo().clone());
			totalfromcargo.addCargo( from.get(k).getCargo() );
			cargofromlist.add(k, from.get(k).getMaxCargo() - from.get(k).getCargo().getMass());
		}

		Map<Integer,StringBuilder> msg = new HashMap<Integer,StringBuilder>();
		
		if( (tolist.size() > 1) || (from.size() > 1) ) {
			t.setBlock("_TRANSPORT", "transfer.multitarget.listitem", "transfer.multitarget.list" );
		}

		ResourceList reslist = totalfromcargo.compare( totaltocargo, true );
		for( ResourceEntry res : reslist ) {
			parameterNumber(res.getId()+"to");
			int transt = getInteger(res.getId()+"to");
			
			parameterNumber(res.getId()+"from");
			int transf = getInteger(res.getId()+"from");
			
			t.setVar("transfer.multitarget.list", "");
	
			if( transt > 0 ) {
				t.setVar(	"transfer.count",		Common.ln(transt),
							"transfer.mode.to",		1,
							"transfer.res.image",	res.getImage() );
				
				for( int k=0; k < from.size(); k++ ) {
					TransportTarget from = this.from.get(k);
					t.setVar("transfer.source.name", Common._plaintitle(from.getObjectName()));
					
					for( int j=0; j < tolist.size(); j++ ) {
						TransportTarget to = tolist.get(j);
						if( (tolist.size() > 1) || (this.from.size() > 1) ) {
							t.start_record();
						}
						
						t.setVar("transfer.target.name", Common._plaintitle(to.getObjectName()) );
				
						if( !msg.containsKey(to.getOwner()) ) {
							msg.put(to.getOwner(), new StringBuilder());
						}
						
						MutableLong mCargoFrom = new MutableLong(cargofromlist.get(k));
						MutableLong mCargoTo = new MutableLong(cargotolist.get(j));
						if( transferSingleResource( from, to, res, transt, newfromclist.get(k), newtoclist.get(j), mCargoFrom, mCargoTo, msg.get(to.getOwner()), 't') != 0 ) {
							transfer = true;
							
							// Evt unbekannte Items bekannt machen
							if( res.getId().isItem() && (getUser().getId() != to.getOwner()) ) {
								if( Items.get().item(res.getId().getItemID()).isUnknownItem() ) {
									User auser = (User)getDB().get(User.class, to.getOwner());
									auser.addKnownItem(res.getId().getItemID());
								}
							}
						}
						cargofromlist.set(k, mCargoFrom.longValue());
						cargotolist.set(j, mCargoTo.longValue());
					
						if( (tolist.size() > 1) || (this.from.size() > 1) ) {
							t.parse("transfer.multitarget.list", "transfer.multitarget.listitem", true);
						
							t.stop_record();
							t.clear_record();
						}
					}
				}
				t.parse("transfer.list", "transfer.listitem", true);
			}
			else if( transf > 0 ) {		
				t.setVar(	"transfer.count",		Common.ln(transf),
							"transfer.res.image",	res.getImage(),
							"transfer.mode.to",		0 );
				for( int k=0; k < from.size(); k++ ) {
					TransportTarget from = this.from.get(k);				
					t.setVar("transfer.source.name", Common._plaintitle(from.getObjectName()));
					
					for( int j=0; j < tolist.size(); j++ ) {
						TransportTarget to = tolist.get(j);
						if( (tolist.size() > 1) || (this.from.size() > 1) ) {
							t.start_record();
						}
						
						if( (to.getOwner() != getUser().getId()) && (to.getOwner() != 0) ) {
							addError("Das geh&ouml;rt dir nicht!");
							
							redirect();
							return;
						} 
						
						t.setVar("transfer.target.name", Common._plaintitle(to.getObjectName()) );
				
						if( !msg.containsKey(to.getOwner()) ) {
							msg.put(to.getOwner(), new StringBuilder());
						}
						MutableLong mCargoFrom = new MutableLong(cargofromlist.get(k));
						MutableLong mCargoTo = new MutableLong(cargotolist.get(j));
						if( transferSingleResource( from, to, res, transf, newtoclist.get(j), newfromclist.get(k), mCargoTo, mCargoFrom, msg.get(to.getOwner()), 'f') != 0 ) {
							transfer = true;
						}
						cargofromlist.set(k, mCargoFrom.longValue());
						cargotolist.set(j, mCargoTo.longValue());
						
						if( (tolist.size() > 1) || (this.from.size() > 1) ) {
							t.parse("transfer.multitarget.list", "transfer.multitarget.listitem", true);
						
							t.stop_record();
							t.clear_record();
						}
					}
				}
				t.parse("transfer.list", "transfer.listitem", true);
			}
		}
		
		Map<Integer, String> ownerpmlist = new HashMap<Integer,String>();
		
		List<String> sourceshiplist = new ArrayList<String>();;
		for( int i=0; i < this.from.size(); i++ ) {
			sourceshiplist.add(from.get(i).getObjectName()+" ("+from.get(i).getId()+")");	
		}
		
		for( int j=0; j < tolist.size(); j++  ) {
			TransportTarget to = tolist.get(j);
			if( getUser().getId() != to.getOwner() ) {
				if( msg.containsKey(to.getOwner()) && (msg.get(to.getOwner()).length() > 0) && !ownerpmlist.containsKey(to.getOwner()) ) {
					Common.writeLog("transport.log", Common.date("d.m.y H:i:s")+": "+getUser().getId()+" -> "+to.getOwner()+" | "+getString("from")+" -> "+getString("to")+" ["+getString("way")+"] : "+"\n"+msg+"---------\n");
				
					t.setVar( "transfer.pm", 1 );

					List<String> shiplist = new ArrayList<String>();
					
					for( int k=j; k < tolist.size(); k++ ) {
						if( this.to.get(j).getOwner() == tolist.get(k).getOwner() ) {
							shiplist.add(tolist.get(k).getObjectName()+" ("+tolist.get(k).getId()+")");	
						}
					}
					
					String tmpmsg = Common.implode(",",sourceshiplist)+" l&auml;dt Waren auf "+Common.implode(",",shiplist)+"\n"+msg.get(to.getOwner());
					PM.send((User)getUser(), to.getOwner(), "Waren transferiert", tmpmsg);
					
					ownerpmlist.put(to.getOwner(), msg.get(to.getOwner()).toString());
				}
			}
		}
		
		if( !transfer ) {
			redirect();
			
			return;
		}

		/*
			"from" bearbeiten
		*/
		for( int k=0; k < newfromclist.size(); k++ ) {
			Cargo newfromc = newfromclist.get(k);
			if( newfromc.save().equals(from.get(k).getCargo().save(true)) ) {
				continue;	
			}
			from.get(k).setCargo(newfromc);
			from.get(k).write();
		}
	
		/*
			"to" bearbeiten 
		*/
		for( int k=0; k < newtoclist.size(); k++ ) {
			Cargo newtoc = newtoclist.get(k);
			if( newtoc.save().equals(to.get(k).getCargo().save(true)) ) {
				continue;	
			}
			to.get(k).setCargo(newtoc);
			to.get(k).write();
		}
		
		redirect();	
	}

	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		TemplateEngine t = getTemplateEngine();
		t.setBlock("_TRANSPORT", "target.targets.listitem", "target.targets.list" );
		t.setBlock("_TRANSPORT", "source.sources.listitem", "source.sources.list" );
		
		t.setVar(	"global.rawway",	getString("way"),
					"source.isbase",	way[0].equals("b"),
					"target.isbase",	way[1].equals("b") );

		// Die Quelle(n) ausgeben
		if( from.size() == 1 ) {
			TransportTarget first = from.get(0);
			
			t.setVar(	"sourceobj.name",	first.getObjectName(),
						"sourceobj.id",		first.getId(),
						"source.cargo",		Common.ln(first.getMaxCargo() - first.getCargo().getMass()) );
			
			t.setVar("source.id", first.getId());
		}
		else if( from.size() < 10 ){			
			long cargo = 0;
			for( TransportTarget afromd : from ) {
				cargo = Math.max(afromd.getMaxCargo() - afromd.getCargo().getMass(), cargo);
				t.setVar(	"sourceobj.name",	afromd.getObjectName(),
							"sourceobj.id",		afromd.getId() );
				
				t.parse( "source.sources.list", "source.sources.listitem", true );
			}
			
			t.setVar(	"source.id",	getString("from"),
						"sourceobj.id",	from.get(0).getId(),
						"source.cargo",	"max "+Common.ln(cargo) );
		}
		else {
			long cargo = 0;
			for( TransportTarget afromd : from ) {
				cargo = Math.max(afromd.getMaxCargo() - afromd.getCargo().getMass(), cargo);
			}
			TransportTarget first = from.get(0);
			
			t.setVar(	"sourceobj.name",		first.getObjectName(),
						"sourceobj.id",			first.getId(),
						"sourceobj.addinfo",	"und "+(from.size()-1)+" weiteren Schiffen",
						"source.cargo",			"max "+Common.ln(cargo) );
								
			t.setVar("source.id", getString("from"));
		}
		
		// Das Ziel / die Ziele ausgeben
		if( to.size() == 1 ) {
			t.setVar(	"targetobj.name",	to.get(0).getObjectName(),
						"targetobj.id",		to.get(0).getId(),
						"target.cargo",		Common.ln(to.get(0).getMaxCargo() - to.get(0).getCargo().getMass()) );
			
			t.setVar("target.id", to.get(0).getId());
		} 
		else if( to.size() < 10 ){		
			long cargo = 0;
			for( TransportTarget atod : to ) {
				cargo = Math.max(atod.getMaxCargo() - atod.getCargo().getMass(), cargo);
				t.setVar(	"targetobj.name",	atod.getObjectName(),
							"targetobj.id",		atod.getId() );
				
				t.parse( "target.targets.list", "target.targets.listitem", true );
			}
			
			t.setVar(	"target.id",	getString("to"),
						"targetobj.id",	to.get(0).getId(),
						"target.cargo",	"max "+Common.ln(cargo) );
		}
		else {
			long cargo = 0;
			for( TransportTarget atod : to ) {
				cargo = Math.max(atod.getMaxCargo() - atod.getCargo().getMass(), cargo);
			}
			TransportTarget first = to.get(0);
			
			t.setVar(	"targetobj.name",	first.getObjectName(),
						"targetobj.id",		first.getId(),
						"targetobj.addinfo",	"und "+(to.size()-1)+" weiteren Schiffen",
						"target.cargo",			"max "+Common.ln(cargo) );
								
			t.setVar("target.id", getString("to"));
		}
		
		// Transfermodi ausgeben
		t.setBlock( "_TRANSPORT","transfermode.listitem", "transfermode.list" );
		if( (to.size() > 1) || (from.size() > 1) || (to.get(0).getMultiTarget() != null) ||
			(from.get(0).getMultiTarget() != null) ) {
			TransportTarget first = to.get(0);
			TransportTarget second = from.get(0);
			
			MultiTarget multiTo = null;
			if( to.size() > 1 ) {
				multiTo = first.getMultiTarget();
				if( (multiTo == null) || !multiTo.getTargetList().equals(getString("to")) ) {
					multiTo = new MultiTarget("Gruppe", getString("to"));
				}
			}
			else {
				multiTo = first.getMultiTarget();
			}
			
			MultiTarget multiFrom = null;
			if( from.size() > 1 ) {
				multiFrom = second.getMultiTarget();
				if( (multiFrom == null) || !multiFrom.getTargetList().equals(getString("from")) ) {
					multiFrom = new MultiTarget("Gruppe", getString("from"));
				}
			}
			else {
				multiFrom = second.getMultiTarget();
			}
			
			// Single to Single
			t.setVar(	"transfermode.from.name",	second.getTargetName(),
						"transfermode.from",		second.getId(),
						"transfermode.to.name",		first.getTargetName(),
						"transfermode.to",			first.getId(),
						"transfermode.selected",	to.size() == 1 && (from.size() <= 1) );
			t.parse("transfermode.list", "transfermode.listitem", true);
								
			
			// Single to Multi
			if( multiTo != null ) {
				t.setVar(	"transfermode.from.name",	second.getTargetName(),
							"transfermode.from",		second.getId(),
							"transfermode.to.name",		multiTo.getName(),
							"transfermode.to",			multiTo.getTargetList(),
							"transfermode.selected",	to.size() > 1 && (from.size() <= 1) );
				t.parse("transfermode.list", "transfermode.listitem", true);
			}
			
			// Multi to Single
			if( multiFrom != null ) {
				t.setVar(	"transfermode.to.name",		first.getTargetName(),
							"transfermode.to",			first.getId(),
							"transfermode.from.name",	multiFrom.getName(),
							"transfermode.from",		multiFrom.getTargetList(),
							"transfermode.selected",	(from.size() > 1) && to.size() == 1 );
				t.parse("transfermode.list", "transfermode.listitem", true);
			}
			
			// Multi to Multi
			if( (multiFrom != null) && (multiTo != null) && 
				!multiFrom.getTargetList().equals(multiTo.getTargetList()) ) {
				t.setVar(	"transfermode.to.name",		multiTo.getName(),
							"transfermode.to",			multiTo.getTargetList(),
							"transfermode.from.name",	multiFrom.getName(),
							"transfermode.from",		multiFrom.getTargetList(),
							"transfermode.selected",	(from.size() > 1) && to.size() > 1 );
				t.parse("transfermode.list", "transfermode.listitem", true);
			}
		}
		
		t.setBlock( "_TRANSPORT","res.listitem", "res.list" );

		// Soll der Zielcargo gezeigt werden?
		boolean showtarget = false;
		Cargo tocargo = new Cargo();
		
		for( TransportTarget to : this.to ) {
			if( getUser().getId() != to.getOwner() ) {
				continue;
			}
			showtarget = true;
			
			ResourceList reslist = to.getCargo().getResourceList();
			for( ResourceEntry res : reslist ) {
				if( res.getCount1() > tocargo.getResourceCount(res.getId()) ) {
					tocargo.setResource(res.getId(), res.getCount1());
				}
			}
		}
		
		t.setVar("target.show", showtarget);
		
		Cargo fromcargo = new Cargo();
		for( TransportTarget afrom : from ) {
			ResourceList reslist = afrom.getCargo().getResourceList();
			for( ResourceEntry res : reslist ) {
				if( res.getCount1() > fromcargo.getResourceCount(res.getId()) ) {
					fromcargo.setResource(res.getId(), res.getCount1());
				}
			}
		}

		// Muss verglichen werden oder reicht unsere eigene Resliste?
		ResourceList reslist = null;
		if( !showtarget ) {
			reslist = fromcargo.getResourceList();
		}
		else {
			reslist = fromcargo.compare( tocargo, true );
		}
		
		for( ResourceEntry res : reslist ) {
			t.setVar(	"res.name",		res.getName(),
						"res.image",	res.getImage(),
						"res.id",		res.getId(),
						"res.cargo.source",	(from.size() > 1 ? "max " : "")+res.getCargo1(),
						"res.cargo.target",	showtarget ? (to.size() > 1 ? "max " : "" )+res.getCargo2() : 0 );
								
			t.parse( "res.list", "res.listitem", true );
		}
	}
}
