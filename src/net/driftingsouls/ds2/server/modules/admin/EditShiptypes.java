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
package net.driftingsouls.ds2.server.modules.admin;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.driftingsouls.ds2.server.entities.Offizier;
import net.driftingsouls.ds2.server.battles.BattleShip;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.ContextMap;
import net.driftingsouls.ds2.server.framework.db.HibernateUtil;
import net.driftingsouls.ds2.server.framework.pipeline.Request;
import net.driftingsouls.ds2.server.modules.AdminController;
import net.driftingsouls.ds2.server.ships.Ship;
import net.driftingsouls.ds2.server.ships.ShipClasses;
import net.driftingsouls.ds2.server.ships.ShipModules;
import net.driftingsouls.ds2.server.ships.ShipType;
import net.driftingsouls.ds2.server.ships.ShipTypeData;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;

/**
 * Aktualisierungstool fuer die Werte von Schiffstypen.
 *
 * @author Sebastian Gift
 */
@AdminMenuEntry(category = "Schiffe", name = "Typen editieren")
public class EditShiptypes implements AdminPlugin
{
	private static final Log log = LogFactory.getLog(EditShiptypes.class);

	@Override
	public void output(AdminController controller, String page, int action) throws IOException
	{
		Context context = ContextMap.getContext();
		Writer echo = context.getResponse().getWriter();
		org.hibernate.Session db = context.getDB();

		int shiptypeId = context.getRequest().getParameterInt("shiptype");

		// Update values?
		boolean update = context.getRequest().getParameterString("change").equals("Aktualisieren");
		List<?> shiptypes = db.createQuery("from ShipType").list();

		echo.append("<form action=\"./ds\" method=\"post\">");
		echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
		echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
		echo.append("<select size=\"1\" name=\"shiptype\">");
		for (Iterator<?> iter = shiptypes.iterator(); iter.hasNext();)
		{
			ShipType shiptype = (ShipType) iter.next();

			echo.append("<option value=\"" + shiptype.getId() + "\" " + (shiptype.getId() == shiptypeId ? "selected=\"selected\"" : "") + ">" + shiptype.getNickname() + " ("+shiptype.getId()+")</option>");
		}
		echo.append("</select>");
		echo.append("<input type=\"submit\" name=\"choose\" value=\"Ok\" />");
		echo.append("</form>");

		if (update && shiptypeId > 0)
		{
			update(context, echo, db, shiptypeId);
		}

		// Ship choosen - get the values
		if (shiptypeId > 0)
		{
			ShipType ship = (ShipType) db.get(ShipType.class, shiptypeId);

			long shipCount = (Long)db
					.createQuery("select count(*) from Ship s where s.shiptype=:type")
					.setEntity("type", ship)
					.uniqueResult();

			echo.append("<form action=\"./ds\" method=\"post\">");
			echo.append("<input type=\"hidden\" name=\"page\" value=\"" + page + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"act\" value=\"" + action + "\" />\n");
			echo.append("<input type=\"hidden\" name=\"module\" value=\"admin\" />\n");
			echo.append("<input type=\"hidden\" name=\"shiptype\" value=\"" + shiptypeId + "\" />\n");
			echo.append("<div class='gfxbox' style='width:600px'>");
			echo.append("Anzahl vorhandener Schiffe: "+shipCount+"<br />");
			echo.append("<table width=\"100%\">");
			echo.append("<tr><td>Name: </td><td><input type=\"text\" name=\"nickname\" value=\"" + ship.getNickname() + "\"></td></tr>\n");
			echo.append("<tr><td>Bild: </td><td><input type=\"text\" name=\"picture\" value=\"" + ship.getPicture() + "\"></td></tr>\n");
			echo.append("<tr><td>Uranreaktor: </td><td><input type=\"text\" name=\"reactoruran\" value=\"" + ship.getRu() + "\"></td></tr>\n");
			echo.append("<tr><td>Deuteriumreaktor: </td><td><input type=\"text\" name=\"reactordeut\" value=\"" + ship.getRd() + "\"></td></tr>\n");
			echo.append("<tr><td>Antimateriereaktor: </td><td><input type=\"text\" name=\"reactoram\" value=\"" + ship.getRa() + "\"></td></tr>\n");
			echo.append("<tr><td>Reaktor Maximal: </td><td><input type=\"text\" name=\"reactormaximum\" value=\"" + ship.getRm() + "\"></td></tr>\n");
			echo.append("<tr><td>EPS: </td><td><input type=\"text\" name=\"eps\" value=\"" + ship.getEps() + "\"></td></tr>\n");
			echo.append("<tr><td>Flugkosten: </td><td><input type=\"text\" name=\"flycost\" value=\"" + ship.getCost() + "\"></td></tr>\n");
			echo.append("<tr><td>H&uuml;lle: </td><td><input type=\"text\" name=\"hull\" value=\"" + ship.getHull() + "\"></td></tr>\n");
			echo.append("<tr><td>Panzerung: </td><td><input type=\"text\" name=\"armor\" value=\"" + ship.getPanzerung() + "\"></td></tr>\n");
			echo.append("<tr><td>Cargo: </td><td><input type=\"text\" name=\"cargo\" value=\"" + ship.getCargo() + "\"></td></tr>\n");
			echo.append("<tr><td>Nahrungsspeicher: </td><td><input type=\"text\" name=\"nahrungcargo\" value=\"" + ship.getNahrungCargo() + "\"></td></tr>\n");
			echo.append("<tr><td>Hitze: </td><td><input type=\"text\" name=\"heat\" value=\"" + ship.getHeat() + "\"></td></tr>\n");
			echo.append("<tr><td>Crew: </td><td><input type=\"text\" name=\"crew\" value=\"" + ship.getCrew() + "\"></td></tr>\n");
			echo.append("<tr><td>Maximale Gr&ouml;&szlig;e f&ouml;r Einheiten: </td><td><input type=\"text\" name=\"maxunitsize\" value=\"" + ship.getMaxUnitSize() + "\"></td></tr>\n");
			echo.append("<tr><td>Laderaum f&uuml;r Einheiten: </td><td><input type=\"text\" name=\"unitspace\" value=\"" + ship.getUnitSpace() + "\"></td></tr>\n");
			echo.append("<tr><td>Waffen: </td><td><textarea cols=\"50\" rows=\"10\" name=\"weapons\">" + ship.getWeapons() + "</textarea></td></tr>\n");
			echo.append("<tr><td>Maximale Hitze: </td><td><textarea cols=\"50\" rows=\"10\" name=\"maxheat\">" + ship.getMaxHeat() + "</textarea></td></tr>\n");
			echo.append("<tr><td>Torpedoabwehr: </td><td><input type=\"text\" name=\"torpedodef\" value=\"" + ship.getTorpedoDef() + "\"></td></tr>\n");
			echo.append("<tr><td>Schilde: </td><td><input type=\"text\" name=\"shields\" value=\"" + ship.getShields() + "\"></td></tr>\n");
			echo.append("<tr><td>Gr&ouml;&szlig;e: </td><td><input type=\"text\" name=\"size\" value=\"" + ship.getSize() + "\"></td></tr>\n");
			echo.append("<tr><td>J&auml;gerdocks: </td><td><input type=\"text\" name=\"fighterdocks\" value=\"" + ship.getJDocks() + "\"></td></tr>\n");
			echo.append("<tr><td>Aussendocks: </td><td><input type=\"text\" name=\"hulldocks\" value=\"" + ship.getADocks() + "\"></td></tr>\n");
			echo.append("<tr><td>Sensorreichweite: </td><td><input type=\"text\" name=\"sensorrange\" value=\"" + ship.getSensorRange() + "\"></td></tr>\n");
			echo.append("<tr><td>Hydros: </td><td><input type=\"text\" name=\"hydro\" value=\"" + ship.getHydro() + "\"></td></tr>\n");
			echo.append("<tr><td>RE Kosten: </td><td><input type=\"text\" name=\"recosts\" value=\"" + ship.getReCost() + "\"></td></tr>\n");
			echo.append("<tr><td>Beschreibung: </td><td><textarea cols=\"50\" rows=\"10\" name=\"description\">" + ship.getDescrip() + "</textarea></td></tr>\n");
			echo.append("<tr><td>Deuteriumsammeln: </td><td><input type=\"text\" name=\"deutfactor\" value=\"" + ship.getDeutFactor() + "\"></td></tr>\n");
			echo.append("<tr><td>Schiffsklasse: </td><td><input type=\"text\" name=\"class\" value=\"" + ship.getShipClass().ordinal() + "\"></td></tr>\n");
			echo.append("<tr><td>Flags: </td><td><input type=\"text\" name=\"flags\" value=\"" + ship.getFlags() + "\"></td></tr>\n");
			echo.append("<tr><td>Groupwrap: </td><td><input type=\"text\" name=\"groupwrap\" value=\"" + ship.getGroupwrap() + "\"></td></tr>\n");
			echo.append("<tr><td>Werft: </td><td><input type=\"text\" name=\"dockyard\" value=\"" + ship.getWerft() + "\"></td></tr>\n");
			echo.append("<tr><td>Einmalwerft: </td><td><input type=\"text\" name=\"onewaydockyard\" value=\"" + ship.getOneWayWerft() + "\"></td></tr>\n");
			echo.append("<tr><td>Loot-Chance: </td><td><input type=\"text\" name=\"chanceforloot\" value=\"" + ship.getChance4Loot() + "\"></td></tr>\n");
			echo.append("<tr><td>Module: </td><td><input type=\"text\" name=\"modules\" value=\"" + ship.getModules() + "\"></td></tr>\n");
			echo.append("<tr><td>Verstecken: </td><td><input type=\"text\" name=\"hide\" value=\"" + ship.isHide() + "\"></td></tr>\n");
			echo.append("<tr><td>Ablative Panzerung: </td><td><input type=\"text\" name=\"ablativearmor\" value=\"" + ship.getAblativeArmor() + "\"></td></tr>\n");
			echo.append("<tr><td>Besitzt SRS: </td><td><input type=\"text\" name=\"srs\" value=\"" + ship.hasSrs() + "\"></td></tr>\n");
			echo.append("<tr><td>Scankosten: </td><td><input type=\"text\" name=\"scancosts\" value=\"" + ship.getScanCost() + "\"></td></tr>\n");
			echo.append("<tr><td>Picking-Kosten: </td><td><input type=\"text\" name=\"pickingcosts\" value=\"" + ship.getPickingCost() + "\"></td></tr>\n");
			echo.append("<tr><td>Mindest-Crew: </td><td><input type=\"text\" name=\"mincrew\" value=\"" + ship.getMinCrew() + "\"></td></tr>\n");
			echo.append("<tr><td>EMP verfliegen: </td><td><input type=\"text\" name=\"lostinempchance\" value=\"" + ship.getLostInEmpChance() + "\"></td></tr>\n");
			echo.append("<tr><td></td><td><input type=\"submit\" name=\"change\" value=\"Aktualisieren\"></td></tr>\n");
			echo.append("</table>");
			echo.append("</div>");
			echo.append("</form>\n");
		}
	}

	private void update(Context context, Writer echo, Session db, int shiptypeId) throws IOException {
		Request request = context.getRequest();

		int ru = request.getParameterInt("reactoruran");
		int rd = request.getParameterInt("reactordeut");
		int ra = request.getParameterInt("reactoram");
		int rm = request.getParameterInt("reactormaximum");
		int eps = request.getParameterInt("eps");
		int movecost = request.getParameterInt("flycost");
		int hull = request.getParameterInt("hull");
		int armor = request.getParameterInt("armor");
		int cargo = request.getParameterInt("cargo");
		int heat = request.getParameterInt("heat");
		int crew = request.getParameterInt("crew");
		int nahrungcargo = request.getParameterInt("nahrungcargo");
		int shields = request.getParameterInt("shields");
		int ablativeArmor = request.getParameterInt("ablativearmor");
		String nickname = request.getParameterString("nickname");
		String picture = request.getParameterString("picture");
		String weapons = request.getParameterString("weapons");
		String maxHeat = request.getParameterString("maxheat");
		int maxunitsize = request.getParameterInt("maxunitsize");
		int unitspace = request.getParameterInt("unitspace");
		int torpedoDef = request.getParameterInt("torpedodef");
		int size = request.getParameterInt("size");
		int jDocks = request.getParameterInt("fighterdocks");
		int aDocks = request.getParameterInt("hulldocks");
		int sensorRange = request.getParameterInt("sensorrange");
		int hydro = request.getParameterInt("hydro");
		int reCost = request.getParameterInt("recosts");
		String description = request.getParameter("description");
		int deutFactor = request.getParameterInt("deutfactor");
		int shipClass = request.getParameterInt("class");
		String flags = request.getParameterString("flags");
		int groupwrap = request.getParameterInt("groupwrap");
		int werft = request.getParameterInt("dockyard");
		int oneWayWerft = request.getParameterInt("onewaydockyard");
		int chance4Loot = request.getParameterInt("chanceforloot");
		String modules = request.getParameter("modules");
		boolean hide = request.getParameterString("hide").trim().toLowerCase().equals("true");
		boolean srs = request.getParameter("srs").trim().toLowerCase().equals("true");
		int scanCost = request.getParameterInt("scancosts");
		int pickingCost = request.getParameterInt("pickingcosts");
		int minCrew = request.getParameterInt("mincrew");
		double lostInEmpChance = Double.parseDouble(request.getParameter("lostinempchance"));

		ShipType shiptype = (ShipType) db.get(ShipType.class, shiptypeId);
		int oldeps = shiptype.getEps();
		int oldhull = shiptype.getHull();
		int oldcrew = shiptype.getCrew();
		int oldshields = shiptype.getShields();
		int oldablativearmor = shiptype.getAblativeArmor();
		long oldnahrungcargo = shiptype.getNahrungCargo();

		shiptype.setRu(ru);
		shiptype.setRd(rd);
		shiptype.setRa(ra);
		shiptype.setRm(rm);
		shiptype.setEps(eps);
		shiptype.setCost(movecost);
		shiptype.setHull(hull);
		shiptype.setCargo(cargo);
		shiptype.setCrew(crew);
		shiptype.setNahrungCargo(nahrungcargo);
		shiptype.setShields(shields);
		shiptype.setAblativeArmor(ablativeArmor);
		shiptype.setPanzerung(armor);
		shiptype.setHeat(heat);
		shiptype.setNickname(nickname);
		shiptype.setPicture(picture);
		shiptype.setWeapons(weapons);
		shiptype.setMaxHeat(maxHeat);
		shiptype.setMaxUnitSize(maxunitsize);
		shiptype.setUnitSpace(unitspace);
		shiptype.setTorpedoDef(torpedoDef);
		shiptype.setSize(size);
		shiptype.setJDocks(jDocks);
		shiptype.setADocks(aDocks);
		shiptype.setSensorRange(sensorRange);
		shiptype.setHydro(hydro);
		shiptype.setReCost(reCost);
		shiptype.setDescrip(description);
		shiptype.setDeutFactor(deutFactor);
		shiptype.setShipClass(ShipClasses.values()[shipClass]);
		shiptype.setFlags(flags);
		shiptype.setGroupwrap(groupwrap);
		shiptype.setWerft(werft);
		shiptype.setOneWayWerft(oneWayWerft);
		shiptype.setChance4Loot(chance4Loot);
		shiptype.setModules(modules);
		shiptype.setHide(hide);
		shiptype.setSrs(srs);
		shiptype.setScanCost(scanCost);
		shiptype.setPickingCost(pickingCost);
		shiptype.setMinCrew(minCrew);
		shiptype.setLostInEmpChance(lostInEmpChance);

		// Update ships
		int count = 0;

		ScrollableResults ships = db.createQuery("from Ship s left join fetch s.modules where s.shiptype= :type").setEntity("type", shiptype).setCacheMode(CacheMode.IGNORE).scroll(ScrollMode.FORWARD_ONLY);
		while (ships.next())
		{
			Ship ship = (Ship) ships.get(0);
			ShipTypeData type = ship.getTypeData();
			// Weight the difference between the old and the new value
			Map<String, Double> factor = new HashMap<String, Double>();
			if( type.getEps() == eps ) { // Schiff ohne Module
				factor.put("eps", ship.getEnergy() / (double) oldeps);
			}
			else {// Schiff mit Modulen
				factor.put("eps", ship.getEnergy() / (double) type.getEps());
			}
			if( type.getHull() == hull ) {
				factor.put("hull", ship.getHull() / (double) oldhull);
			}
			else {
				factor.put("hull", ship.getHull() / (double) type.getHull());
			}
			if( type.getCrew() == crew ) {
				factor.put("crew", ship.getCrew() / (double) oldcrew);
			}
			else {
				factor.put("crew", ship.getCrew() / (double) type.getCrew());
			}
			if( type.getShields() == shields ) {
				factor.put("shields", ship.getShields() / (double) oldshields);
			}
			else {
				factor.put("shields", ship.getShields() / (double) type.getShields());
			}
			if( type.getAblativeArmor() == ablativeArmor ) {
				factor.put("ablativearmor", ship.getAblativeArmor() / (double) oldablativearmor);
			}
			else {
				factor.put("ablativearmor", ship.getAblativeArmor() / (double) type.getAblativeArmor());
			}
			if( type.getNahrungCargo() == nahrungcargo ) {
				factor.put("nahrungcargo", ship.getNahrungCargo() / (double) oldnahrungcargo);
			}
			else {
				factor.put("nahrungcargo", ship.getNahrungCargo() / (double) type.getNahrungCargo());
			}
			try
			{
				ship.recalculateModules();
				type = ship.getTypeData();

				ship.setEnergy((int)Math.floor(type.getEps() * factor.get("eps")));
				ship.setHull((int)Math.floor(type.getHull() * factor.get("hull")));
				ship.setCrew((int)Math.floor(type.getCrew() * factor.get("crew")));
				ship.setShields((int)Math.floor(type.getShields() * factor.get("shields")));
				ship.setAblativeArmor((int)Math.floor(type.getAblativeArmor() * factor.get("ablativearmor")));
				ship.setNahrungCargo((long)Math.floor(type.getNahrungCargo() * factor.get("nahrungcargo")));

				int fighterDocks = ship.getTypeData().getJDocks();
				if (ship.getLandedCount() > fighterDocks)
				{
					List<Ship> fighters = ship.getLandedShips();
					long toStart = fighters.size() - fighterDocks;
					int fighterCount = 0;

					for (Iterator<Ship> iter2 = fighters.iterator(); iter2.hasNext() && fighterCount < toStart;)
					{
						Ship fighter = iter2.next();

						fighter.setDocked("");
						fighterCount++;
					}
				}

				//Docked
				int outerDocks = ship.getTypeData().getADocks();
				if (ship.getDockedCount() > outerDocks)
				{
					List<Ship> outerDocked = ship.getDockedShips();
					long toStart = outerDocked.size() - outerDocks;
					int dockedCount = 0;

					for (Iterator<?> iter2 = outerDocked.iterator(); iter2.hasNext() && dockedCount < toStart;)
					{
						Ship outer = (Ship) iter2.next();
						outer.setDocked("");

						dockedCount++;
					}
				}

				if(ship.getId() >= 0)
				{
					ship.recalculateShipStatus();
				}

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
		db.flush();
		HibernateUtil.getSessionFactory().getCurrentSession().evict(Ship.class);
		HibernateUtil.getSessionFactory().getCurrentSession().evict(ShipModules.class);
		HibernateUtil.getSessionFactory().getCurrentSession().evict(Offizier.class);

		ScrollableResults battleShips = db.createQuery("from BattleShip where ship.shiptype=:type")
			.setEntity("type", shiptype)
			.setCacheMode(CacheMode.IGNORE)
			.scroll(ScrollMode.FORWARD_ONLY);

		count = 0;
		while (battleShips.next())
		{
			BattleShip battleShip = (BattleShip) battleShips.get(0);

			ShipTypeData type = battleShip.getShip().getTypeData();
			// Weight the difference between the old and the new value
			Map<String, Double> factor = new HashMap<String, Double>();
			if( type.getHull() == hull ) {
				factor.put("hull", battleShip.getHull() / (double) oldhull);
			}
			else {
				factor.put("hull", battleShip.getHull() / (double) type.getHull());
			}
			if( type.getShields() == shields ) {
				factor.put("shields", battleShip.getShields() / (double) oldshields);
			}
			else {
				factor.put("shields", battleShip.getShields() / (double) type.getShields());
			}
			if( type.getAblativeArmor() == ablativeArmor ) {
				factor.put("ablativearmor", battleShip.getAblativeArmor() / (double) oldablativearmor);
			}
			else {
				factor.put("ablativearmor", battleShip.getAblativeArmor() / (double) type.getAblativeArmor());
			}
			try
			{
				battleShip.setShields((int)Math.floor(type.getShields() * factor.get("shields")));
				battleShip.setHull((int)Math.floor(type.getHull() * factor.get("hull")));
				battleShip.setAblativeArmor((int)Math.floor(type.getAblativeArmor() * factor.get("ablativearmor")));
				count++;
				//All unflushed changes are part of the sessioncache, so we need to clean it regularly
				if (count % 20 == 0)
				{
					db.flush();
					HibernateUtil.getSessionFactory().getCurrentSession().evict(Ship.class);
					HibernateUtil.getSessionFactory().getCurrentSession().evict(BattleShip.class);
				}
			}
			catch(Exception e)
			{
				log.error("Der Kampfeintrag zum Schiff mit der ID " + battleShip.getId() + " konnte nicht aktualisiert werden. Fehler: " + e.getMessage());
			}
		}
		db.flush();
		HibernateUtil.getSessionFactory().getCurrentSession().evict(Ship.class);
		HibernateUtil.getSessionFactory().getCurrentSession().evict(BattleShip.class);

		echo.append("<p>Update abgeschlossen.</p>");
	}
}
