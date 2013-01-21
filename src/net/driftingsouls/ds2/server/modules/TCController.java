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

import java.util.Iterator;
import java.util.List;

import net.driftingsouls.ds2.server.Offizier;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import net.driftingsouls.ds2.server.ships.ShipTypes;

/**
 * Transferiert Offiziere von und zu Schiffen/Basen.
 *
 * @author Christopher Jung
 *
 * @urlparam Integer ship Die ID des Schiffes, das Ausgangspunkt des Transfers ist.
 * @urlparam Integer target Die ID des Ziels des Transfers
 * @urlparam String conf "ok", falls eine Sicherheitsabfrage bestaetigt werden soll
 * @urlparam Integer off Die ID des zu transferierenden Offiziers, falls mehr als ein Offizier zur Auswahl steht
 *
 */
@Module(name="tc")
public class TCController extends TemplateGenerator {
	private Ship ship = null;

	/**
	 * Konstruktor.
	 * @param context Der zu verwendende Kontext
	 */
	public TCController(Context context) {
		super(context);

		setTemplate("tc.html");

		parameterNumber("ship");
		parameterNumber("target");
		parameterString("conf");
		parameterNumber("off");

		setPageTitle("Offizierstransfer");
	}

	@Override
	protected boolean validateAndPrepare(String action) {
		TemplateEngine t = getTemplateEngine();
		org.hibernate.Session db = getDB();

		int shipId = getInteger("ship");

		t.setVar( "global.shipid", shipId );

		ship = (Ship)db.get(Ship.class, shipId);

		if( (ship == null) || (ship.getOwner() != getUser()) || (ship.getId() < 0) ) {
			addError("Das angegebene Schiff existiert nicht oder geh&ouml;rt ihnen nicht",  Common.buildUrl("default", "module", "schiffe") );

			return false;
		}

		if( ship.getBattle() != null ) {
			addError("Das angegebene Schiff befindet sich in einer Schlacht", Common.buildUrl("default", "module", "schiffe" ) );

			return false;
		}

		return true;
	}

	/**
	 * Offiziersliste eines Objekts ausgeben.
	 *
	 * @param mode	Transfermodus (shipToShip, baseToShip usw)
	 * @param ziel	Der Aufenthaltsort der Offiziere
	 */
	private void echoOffiList( String mode, Object ziel ) {
		TemplateEngine t = getTemplateEngine();

		t.setVar(	"tc.selectoffizier",	1,
					"tc.mode",				mode );

		t.setBlock( "_TC", "tc.offiziere.listitem", "tc.offiziere.list" );

		List<Offizier> offiList = Offizier.getOffiziereByDest(ziel);
		for( Offizier offi : offiList ) {
			if( offi.isTraining() )
			{
				continue;
			}
			t.setVar(	"tc.offizier.picture",		offi.getPicture(),
						"tc.offizier.id",			offi.getID(),
						"tc.offizier.name",			Common._plaintitle(offi.getName()),
						"tc.offizier.ability.ing",	offi.getAbility( Offizier.Ability.ING ),
						"tc.offizier.ability.nav",	offi.getAbility( Offizier.Ability.NAV ),
						"tc.offizier.ability.waf",	offi.getAbility( Offizier.Ability.WAF ),
						"tc.offizier.ability.sec",	offi.getAbility( Offizier.Ability.SEC ),
						"tc.offizier.ability.com",	offi.getAbility( Offizier.Ability.COM ),
						"tc.offizier.special",		offi.getSpecial().getName() );

			t.parse( "tc.offiziere.list", "tc.offiziere.listitem", true );
		}
	}

	/**
	 * Transferiert einen Offizier von einem Schiff zu einem Schiff.
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void shipToShipAction() {
		org.hibernate.Session db = getDB();
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();

		String conf = getString("conf");
		int off = getInteger("off");

		String errorurl = Common.buildUrl("default", "module", "schiff", "ship", ship.getId());

		Ship tarShip = (Ship)db.get(Ship.class, getInteger("target"));
		if( (tarShip == null) || (tarShip.getId() < 0) ) {
			addError("Das angegebene Zielschiff existiert nicht", errorurl);
			setTemplate("");

			return;
		}

		t.setVar( 	"tc.ship",			ship.getId(),
					"tc.target",		tarShip.getId(),
					"tc.target.name",	tarShip.getName(),
					"tc.target.isown",	(tarShip.getOwner() == user),
					"tc.stos",			1,
					"tc.mode",			"shipToShip" );

		if( !ship.getLocation().sameSector(0, tarShip.getLocation(), 0) ) {
			addError( "Die beiden Schiffe befinden sich nicht im selben Sektor", errorurl );
			setTemplate("");

			return;
		}

		if( tarShip.getBattle() != null ) {
			addError("Das Zielschiff befindet sich in einer Schlacht", Common.buildUrl("default","module", "schiffe" ) );

			return;
		}

		long officount = ((Number)db.createQuery("select count(*) from Offizier where stationiertAufSchiff=:dest AND owner=:owner")
				.setEntity("dest", ship)
				.setEntity("owner", user)
				.iterate().next()
			).longValue();

		if( officount == 0 ) {
			addError("Das Schiff hat keinen Offizier an Bord", errorurl );
			setTemplate("");

			return;
		}

		//IFF-Check
		boolean disableIFF = (tarShip.getStatus().indexOf("disable_iff") > -1);
		if( disableIFF ) {
			addError("Sie k&ouml;nnen keinen Offizier zu diesem Schiff transferieren", errorurl);
			setTemplate("");

			return;
		}

		ShipTypeData tarShipType = tarShip.getTypeData();

		// Schiff gross genug?
		if( tarShipType.getSize() <= ShipType.SMALL_SHIP_MAXSIZE ) {
			addError("Das Schiff ist zu klein f&uuml;r einen Offizier", errorurl);
			setTemplate("");

			return;
		}

		// Check ob noch fuer einen weiteren Offi platz ist
		int maxoffis = 1;
		if( tarShipType.hasFlag(ShipTypes.SF_OFFITRANSPORT) ) {
			maxoffis = tarShipType.getCrew();
		}

		long tarOffiCount = ((Number)db.createQuery("select count(*) from Offizier where stationiertAufSchiff=:dest AND owner=:owner")
				.setEntity("dest", tarShip)
				.setEntity("owner", tarShip.getOwner())
				.iterate().next()
			).longValue();
		if( tarOffiCount >= maxoffis ) {
			addError("Das Schiff hat bereits die maximale Anzahl Offiziere an Bord", errorurl );
			setTemplate("");

			return;
		}

		// Offiziersliste bei bedarf ausgeben
		if( (officount > 1) && (off == 0) ) {
			echoOffiList("shipToShip", ship);
			return;
		}

		// Offizier laden
		Offizier offizier;
		if( off != 0 ) {
			offizier = Offizier.getOffizierByID(off);
		}
		else {
			offizier = ship.getOffizier();
		}

		if( (offizier == null) || (offizier.getOwner() != user) ) {
			addError("Der angegebene Offizier existiert nicht oder geh&ouml;rt nicht ihnen", errorurl);
			setTemplate("");

			return;
		}

		if( offizier.isTraining() || offizier.getStationiertAufSchiff() == null || offizier.getStationiertAufSchiff().getId() !=  ship.getId() ) {
			addError("Der angegebene Offizier befindet sich nicht auf dem Schiff", errorurl);
			setTemplate("");

			return;
		}

		t.setVar( "tc.offizier.name", Common._plaintitle(offizier.getName()) );

		// Confirm?
		if( (tarShip.getOwner() != user) && !conf.equals("ok") ) {
			t.setVar("tc.confirm",1);

			return;
		}

		User tarUser = tarShip.getOwner();

		// Transfer!
		offizier.stationierenAuf(tarShip);
		offizier.setOwner( tarUser );

		ship.recalculateShipStatus();
		tarShip.recalculateShipStatus();
	}

	/**
	 * Transferiert einen Offizier von einem Schiff zu einer Basis.
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void shipToBaseAction() {
		org.hibernate.Session db = getDB();
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();

		String conf = getString("conf");
		int off = getInteger("off");

		String errorurl = Common.buildUrl("default", "module", "schiff", "ship", ship.getId());

		Base tarBase = (Base)db.get(Base.class, getInteger("target"));

		t.setVar(	"tc.ship",			ship.getId(),
					"tc.target",		tarBase.getId(),
					"tc.target.name",	tarBase.getName(),
					"tc.target.isown",	(tarBase.getOwner() == user),
					"tc.stob",			1,
					"tc.mode",			"shipToBase" );

		if( !ship.getLocation().sameSector(0, tarBase.getLocation(), tarBase.getSize()) ) {
			addError( "Schiff und Basis befinden sich nicht im selben Sektor", errorurl );
			setTemplate("");

			return;
		}

		long officount = ((Number)db.createQuery("select count(*) from Offizier where stationiertAufSchiff=:dest AND owner=:owner")
				.setEntity("dest", ship)
				.setEntity("owner", user)
				.iterate().next()
			).longValue();
		if( officount == 0 ) {
			addError("Das Schiff hat keinen Offizier an Bord", errorurl);
			setTemplate("");

			return;
		}

		// bei bedarf offiliste ausgeben
		if( (officount > 1) && (off == 0) ) {
			echoOffiList("shipToBase", ship);
			return;
		}

		// Offi laden
		Offizier offizier;
		if( off != 0 ) {
			offizier = Offizier.getOffizierByID(off);
		}
		else {
			offizier = ship.getOffizier();
		}

		if( (offizier == null) || (offizier.getOwner() != user) ) {
			addError("Der angegebene Offizier existiert nicht oder geh&ouml;rt nicht ihnen", errorurl);
			setTemplate("");

			return;
		}

		if( offizier.isTraining() || offizier.getStationiertAufSchiff() == null || offizier.getStationiertAufSchiff().getId() != ship.getId() ) {
			addError("Der angegebene Offizier befindet sich nicht auf dem Schiff", errorurl);
			setTemplate("");

			return;
		}

		t.setVar( "tc.offizier.name", Common._plaintitle(offizier.getName()) );

		// Confirm ?
		if( (tarBase.getOwner() != user) && (!"ok".equals(conf)) ) {
			t.setVar( "tc.confirm", 1 );

			return;
		}

		User tarUser = tarBase.getOwner();

		// Transfer !
		offizier.stationierenAuf(tarBase);
		offizier.setOwner( tarUser );

		ship.recalculateShipStatus();
	}

	/**
	 * Transfieriert Offiziere (sofern genug vorhanden) Offiziere von einer Basis
	 * zu einer Flotte.
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void baseToFleetAction() {
		org.hibernate.Session db = getDB();
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();

		String errorurl = Common.buildUrl("default", "module", "schiff", "ship", ship.getId());

		t.setVar( "tc.ship", ship.getId() );

		Base upBase = (Base)db.get(Base.class, getInteger("target"));
		if( (upBase == null) || (upBase.getOwner() != user) ) {
			addError("Die angegebene Basis existiert nicht oder geh&ouml;rt nicht ihnen", errorurl);
			setTemplate("");

			return;
		}

		if( !ship.getLocation().sameSector(0, upBase.getLocation(), upBase.getSize()) ) {
			addError("Schiff und Basis befinden sich nicht im selben Sektor", errorurl);
			setTemplate("");

			return;
		}

		if( ship.getFleet() == null ) {
			addError("Das Schiff befinden sich in keiner Flotte", errorurl);
			setTemplate("");

			return;
		}

		t.setVar(	"tc.captainzuweisen",	1,
					"tc.ship",				ship.getId(),
					"tc.target",			upBase.getId() );

		List<Offizier> offilist = Offizier.getOffiziereByDest(upBase);
		for( Iterator<Offizier> iter=offilist.iterator(); iter.hasNext(); )
		{
			Offizier offi = iter.next();
			if( offi.isTraining() )
			{
				iter.remove();
			}
		}

		int shipcount = 0;

		List<?> shiplist = db.createQuery("from Ship where fleet=:fleet and owner=:owner and system=:sys and x=:x and y=:y and " +
				"locate('offizier',status)=0")
			.setEntity("fleet", this.ship.getFleet())
			.setEntity("owner", user)
			.setInteger("sys", this.ship.getSystem())
			.setInteger("x", this.ship.getX())
			.setInteger("y", this.ship.getY())
			.setMaxResults(offilist.size())
			.list();
		for (Object aShiplist : shiplist)
		{
			Ship aship = (Ship) aShiplist;
			ShipTypeData shipType = aship.getTypeData();
			if (shipType.getSize() <= ShipType.SMALL_SHIP_MAXSIZE)
			{
				continue;
			}

			Offizier offi = offilist.remove(0);
			offi.stationierenAuf(aship);

			aship.recalculateShipStatus();

			shipcount++;
		}

		t.setVar("tc.message", shipcount+" Offiziere wurden transferiert" );
	}

	/**
	 * Transfieriert Offiziere von einer Basis zu einem Schiff.
	 *
	 */
	@Action(ActionType.DEFAULT)
	public void baseToShipAction() {
		org.hibernate.Session db = getDB();
		TemplateEngine t = getTemplateEngine();
		User user = (User)getUser();

		int off = getInteger("off");

		String errorurl = Common.buildUrl("default", "module", "schiff", "ship", ship.getId());

		t.setVar( "tc.ship", ship.getId() );

		Base upBase = (Base)db.get(Base.class,getInteger("target"));
		if( (upBase == null) || (upBase.getOwner() != user) ) {
			addError("Die angegebene Basis existiert nicht oder geh&ouml;rt nicht ihnen");
			setTemplate("");

			return;
		}

		if( !ship.getLocation().sameSector(0, upBase.getLocation(), upBase.getSize()) ) {
			addError("Schiff und Basis befinden sich nicht im selben Sektor", errorurl);
			setTemplate("");

			return;
		}

		ShipTypeData shipType = ship.getTypeData();
		if( shipType.getSize() <= ShipType.SMALL_SHIP_MAXSIZE ) {
			addError("Das Schiff ist zu klein f&uuml;r einen Offizier", errorurl);
			setTemplate("");

			return;
		}

		t.setVar(	"tc.captainzuweisen",	1,
					"tc.offizier",			off,
					"tc.ship",				ship.getId(),
					"tc.target",			upBase.getId() );

		// Wenn noch kein Offizier ausgewaehlt wurde -> Liste der Offiziere in der Basis anzeigen
		if( off == 0 ) {
			echoOffiList("baseToShip", upBase);

			if( ship.getFleet() != null ) {
				long count = ((Number)db.createQuery("select count(*) from Ship where fleet=:fleet and owner=:owner and system=:sys and x=:x and " +
						"y=:y and locate('offizier',status)=0")
						.setEntity("fleet", ship.getFleet())
						.setEntity("owner", user)
						.setInteger("sys", ship.getSystem())
						.setInteger("x", ship.getX())
						.setInteger("y", ship.getY())
						.iterate().next()
					).longValue();
				if( count > 1 ) {
					t.setVar(	"show.fleetupload",	1,
								"tc.fleetmode",		"baseToFleet");
				}
			}
		}
		//ein Offizier wurde ausgewaehlt -> transferieren
		else {
			Offizier offizier = (Offizier)getDB().get(Offizier.class, off);
			if( offizier == null ) {
				addError("Der angegebene Offizier existiert nicht", errorurl);
				setTemplate("");

				return;
			}

			if( offizier.isTraining() || offizier.getStationiertAufBasis() == null || offizier.getStationiertAufBasis().getId() != upBase.getId() ) {
				addError("Der angegebene Offizier ist nicht in der Basis stationiert", errorurl);
				setTemplate("");

				return;
			}

			// Check ob noch fuer einen weiteren Offi platz ist
			ShipTypeData tarShipType = ship.getTypeData();

			long offi = ((Number)getDB().createQuery("select count(*) from Offizier where stationiertAufSchiff=:dest")
					.setEntity("dest", ship)
					.iterate().next()
				).longValue();

			int maxoffis = 1;
			if( tarShipType.hasFlag(ShipTypes.SF_OFFITRANSPORT) ) {
				maxoffis = tarShipType.getCrew();
			}

			if( offi >= maxoffis ) {
				addError("Das Schiff hat bereits die maximale Anzahl Offiziere an Bord", errorurl );
				setTemplate("");

				return;
			}

			t.setVar("tc.offizier.name",Common._plaintitle(offizier.getName()));

			offizier.stationierenAuf(ship);

			ship.recalculateShipStatus();
		}
	}

	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() {
		// EMPTY
	}
}
