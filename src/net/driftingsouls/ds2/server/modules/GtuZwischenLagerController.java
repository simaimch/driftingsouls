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

import net.driftingsouls.ds2.server.cargo.Cargo;
import net.driftingsouls.ds2.server.cargo.ItemCargoEntry;
import net.driftingsouls.ds2.server.cargo.ResourceList;
import net.driftingsouls.ds2.server.cargo.Resources;
import net.driftingsouls.ds2.server.config.items.Item;
import net.driftingsouls.ds2.server.entities.GtuZwischenlager;
import net.driftingsouls.ds2.server.entities.User;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.pipeline.Module;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.RedirectViewResult;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateController;
import net.driftingsouls.ds2.server.framework.pipeline.generators.UrlParam;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ValidierungException;
import net.driftingsouls.ds2.server.framework.templates.TemplateEngine;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipTypeData;
import org.hibernate.Session;

import java.util.List;

/**
 * Die UI zum GTU-Zwischenlager.
 * <p>Hinweise zur Datenbankstruktur:<br>
 * <ul>
 * <li><b>user1</b> - Die ID des einen Handelspartners</li>
 * <li><b>user2</b> - Die ID des anderen Handelspartner</li>
 * <li><b>cargo1</b> - Die bisher vom zweiten Handelspartner geleistete Zahlung. Dies sind die Waren, die dem ersten Handelspartner zustehen</li>
 * <li><b>cargo1need</b> - Die insgesamt vom zweiten Handelspartner zu leistenden Zahlungen. Diese Warenmenge steht dem ersten Handelspartner insgesamt zu</li>
 * <li><b>cargo2</b> - Die bisher vom ersten Handelspartner geleistete Zahlung. Dies sind die Waren, die dem zweiten Handelspartner zustehen</li>
 * <li><b>cargo2need</b> - Die insgesamt vom ersten Handelspartner zu leistenden Zahlungen. Diese Warenmenge steht dem zweiten Handelspartner insgesamt zu</li>
 * </ul>
 * <p/>
 * Waren koennen erst abgeholt werden, wenn die eigenen Zahlungen geleistet wurden</p>
 *
 * @author Christopher Jung
 */
// TODO: Die ID des Handelspostens sollte per URL spezifiziert werden
@Module(name = "gtuzwischenlager")
public class GtuZwischenLagerController extends TemplateController
{
	/**
	 * Konstruktor.
	 *
	 */
	public GtuZwischenLagerController()
	{
		super();

		setPageTitle("GTU-Lager");
	}

	private void validiereSchiff(Ship ship)
	{
		User user = (User) this.getUser();
		if ((ship == null) || (ship.getId() < 0) || (ship.getOwner() != user))
		{
			throw new ValidierungException("Das angegebene Schiff existiert nicht oder geh&ouml;rt nicht ihnen", Common.buildUrl("default", "module", "schiffe"));
		}
	}

	private Ship ermittleHandelspostenFuerSchiff(Session db, Ship ship)
	{
		Ship handel = (Ship) db.createQuery("from Ship where id>0 and owner.id<0 and locate('tradepost',status)!=0 and " +
				"system=:sys and x=:x and y=:y")
				.setInteger("sys", ship.getSystem())
				.setInteger("x", ship.getX())
				.setInteger("y", ship.getY())
				.setMaxResults(1)
				.uniqueResult();
		if (handel == null)
		{
			throw new ValidierungException("Es existiert kein Handelsposten in diesem Sektor", Common.buildUrl("default", "module", "schiff", "ship", ship.getId()));
		}
		return handel;
	}

	/**
	 * Transferiert nach der Bezahlung (jetzt) eigene Waren aus einem Handelsuebereinkommen
	 * auf das aktuelle Schiff.
	 *  @param ship Die ID des Schiffes, welches auf das GTU-Zwischenlager zugreifen will
	 * @param tradeentry Die ID des Zwischenlager-Eintrags
	 */
	@Action(ActionType.DEFAULT)
	public RedirectViewResult transportOwnAction(Ship ship, @UrlParam(name = "entry") GtuZwischenlager tradeentry)
	{
		org.hibernate.Session db = getDB();
		User user = (User) this.getUser();
		TemplateEngine t = this.getTemplateEngine();

		validiereSchiff(ship);

		t.setVar("global.shipid", ship.getId());

		validiereGtuZwischenlager(tradeentry, ship);

		//  Der Handelspartner
		// Die (zukuenftig) eigenen Waren
		Cargo tradecargo = tradeentry.getCargo1();
		Cargo tradecargoneed = tradeentry.getCargo1Need();
		// Die Bezahlung
		Cargo owncargo = tradeentry.getCargo2();
		Cargo owncargoneed = tradeentry.getCargo2Need();

		if (tradeentry.getUser2().getId() == user.getId())
		{
			tradecargo = tradeentry.getCargo2();
			tradecargoneed = tradeentry.getCargo2Need();
			owncargo = tradeentry.getCargo1();
			owncargoneed = tradeentry.getCargo1Need();
		}

		Cargo tmpowncargoneed = new Cargo(owncargoneed);

		tmpowncargoneed.substractCargo(owncargo);
		if (!tmpowncargoneed.isEmpty())
		{
			throw new ValidierungException("Sie m&uuml;ssen die Waren erst komplett bezahlen", Common.buildUrl("default", "module", "schiff", "ship", ship.getId()));
		}

		ShipTypeData shiptype = ship.getTypeData();

		Cargo shipCargo = new Cargo(ship.getCargo());
		long freecargo = shiptype.getCargo() - shipCargo.getMass();

		Cargo transportcargo;
		if (freecargo <= 0)
		{
			addError("Sie verf&uuml;gen nicht &uuml;ber genug freien Cargo um Waren abholen zu k&ouml;nnen");
			return new RedirectViewResult("viewEntry");
		}
		else if (freecargo < tradecargo.getMass())
		{
			transportcargo = new Cargo(tradecargo).cutCargo(freecargo);
		}
		else
		{
			transportcargo = new Cargo(tradecargo);
		}

		t.setBlock("_GTUZWISCHENLAGER", "transferlist.res.listitem", "transferlist.res.list");

		ResourceList reslist = transportcargo.getResourceList();
		Resources.echoResList(t, reslist, "transferlist.res.list");

		t.setVar("global.transferlist", 1);

		shipCargo.addCargo(transportcargo);
		tradecargoneed.substractCargo(transportcargo);

		ship.setCargo(shipCargo);

		if (tradecargoneed.isEmpty() && owncargo.isEmpty())
		{
			db.delete(tradeentry);

			t.setVar("transferlist.backlink", 1);

			return null;
		}

		if (tradeentry.getUser1() == user)
		{
			tradeentry.setCargo1(tradecargo);
			tradeentry.setCargo1Need(tradecargoneed);
		}
		else
		{
			tradeentry.setCargo2(tradecargo);
			tradeentry.setCargo2Need(tradecargoneed);
		}

		return new RedirectViewResult("viewEntry");
	}

	private void validiereGtuZwischenlager(GtuZwischenlager tradeentry, Ship ship)
	{
		User user = (User) this.getUser();
		org.hibernate.Session db = getDB();

		Ship handel = ermittleHandelspostenFuerSchiff(db, ship);
		if ((tradeentry == null) || (tradeentry.getPosten() != handel) || ((tradeentry.getUser1() != user) && (tradeentry.getUser2() != user)))
		{
			throw new ValidierungException("Es wurde kein passender Handelseintrag gefunden", Common.buildUrl("default", "module", "schiff", "ship", ship.getId()));
		}
	}

	/**
	 * Transferiert fuer einen Eintrag noch fehlende Resourcen.
	 */
	@Action(ActionType.DEFAULT)
	public void transportMissingAction()
	{
		// TODO
	}

	/**
	 * Zeigt einen Handelsuebereinkommen an.
	 *
	 * @param ship Die ID des Schiffes, welches auf das GTU-Zwischenlager zugreifen will
	 * @param tradeentry Die ID des Zwischenlager-Eintrags
	 */
	@Action(ActionType.DEFAULT)
	public void viewEntryAction(Ship ship, @UrlParam(name = "entry") GtuZwischenlager tradeentry)
	{
		User user = (User) this.getUser();
		TemplateEngine t = this.getTemplateEngine();

		validiereSchiff(ship);

		t.setVar("global.shipid", ship.getId());

		validiereGtuZwischenlager(tradeentry, ship);

		t.setVar("global.entry", 1);

		t.setBlock("_GTUZWISCHENLAGER", "res.listitem", "res.list");

		// Der Handelspartner
		User tradepartner = tradeentry.getUser2();
		// Die (zukuenftig) eigenen Waren
		Cargo tradecargo = tradeentry.getCargo1();
		// Die Bezahlung
		Cargo owncargo = tradeentry.getCargo2();
		Cargo owncargoneed = tradeentry.getCargo2Need();

		if (tradepartner.getId() == user.getId())
		{
			tradepartner = tradeentry.getUser1();
			tradecargo = tradeentry.getCargo2();
			owncargo = tradeentry.getCargo1();
			owncargoneed = tradeentry.getCargo1Need();
		}

		t.setVar("tradeentry.id", tradeentry.getId(),
				"tradeentry.partner", Common._title(tradepartner.getName()),
				"tradeentry.missingcargo", "",
				"tradeentry.waren", "");


		// (zukuenftig) eigene Waren anzeigen
		ResourceList reslist = tradecargo.getResourceList();
		Resources.echoResList(t, reslist, "tradeentry.waren", "res.listitem");

		// noch ausstehende Bezahlung anzeigen
		owncargoneed.substractCargo(owncargo);
		if (!owncargoneed.isEmpty())
		{
			reslist = owncargoneed.getResourceList();
			Resources.echoResList(t, reslist, "tradeentry.missingcargo", "res.listitem");
		}
	}

	/**
	 * Zeigt die Liste aller Handelsvereinbarungen auf diesem Handelsposten an, an denen der aktuelle Spieler beteiligt ist.
	 *
	 * @param ship Die ID des Schiffes, welches auf das GTU-Zwischenlager zugreifen will
	 */
	@Action(ActionType.DEFAULT)
	public void defaultAction(Ship ship)
	{
		org.hibernate.Session db = getDB();
		User user = (User) this.getUser();
		TemplateEngine t = this.getTemplateEngine();

		validiereSchiff(ship);

		t.setVar("global.shipid", ship.getId());

		t.setVar("global.tradelist", 1);
		t.setBlock("_GTUZWISCHENLAGER", "tradelist.listitem", "tradelist.list");
		t.setBlock("tradelist.listitem", "res.listitem", "res.list");

		Ship handel = ermittleHandelspostenFuerSchiff(db, ship);

		List<?> tradelist = db.createQuery("from GtuZwischenlager where posten=:posten and (user1= :user or user2= :user)")
				.setEntity("posten", handel)
				.setEntity("user", user)
				.list();
		for (Object aTradelist : tradelist)
		{
			GtuZwischenlager tradeentry = (GtuZwischenlager) aTradelist;

			User tradepartner = tradeentry.getUser2();
			Cargo tradecargo = tradeentry.getCargo1();
			Cargo owncargo = tradeentry.getCargo2();
			Cargo owncargoneed = tradeentry.getCargo2Need();

			if (tradepartner == user)
			{
				tradepartner = tradeentry.getUser1();
				tradecargo = tradeentry.getCargo2();
				owncargo = tradeentry.getCargo1();
				owncargoneed = tradeentry.getCargo1Need();
			}

			t.setVar("list.entryid", tradeentry.getId(),
					"list.user", Common._title(tradepartner.getName()),
					"res.list", "",
					"list.cargoreq.list", "",
					"list.status", "bereit");

			// (zukuenftig) eigene Waren anzeigen
			ResourceList reslist = tradecargo.getResourceList();
			Resources.echoResList(t, reslist, "res.list");

			List<ItemCargoEntry> itemlist = tradecargo.getItems();
			for (ItemCargoEntry item : itemlist)
			{
				Item itemobject = item.getItem();
				if (itemobject.isUnknownItem())
				{
					user.addKnownItem(item.getItemID());
				}
			}

			// noch ausstehende Bezahlung anzeigen
			owncargoneed.substractCargo(owncargo);

			if (!owncargoneed.isEmpty())
			{
				reslist = owncargoneed.getResourceList();
				Resources.echoResList(t, reslist, "list.cargoreq.list", "res.listitem");
			}

			t.parse("tradelist.list", "tradelist.listitem", true);
		}
	}
}
