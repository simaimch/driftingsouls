/*
 *	Drifting Souls 2
 *	Copyright (c) 2007 Christopher Jung
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
package net.driftingsouls.ds2.server.map;

import java.util.List;

import net.driftingsouls.ds2.server.Location;
import net.driftingsouls.ds2.server.bases.Base;
import net.driftingsouls.ds2.server.config.StarSystem;
import net.driftingsouls.ds2.server.entities.JumpNode;

import net.driftingsouls.ds2.server.ships.Ship;

/**
 * Die allgemeine Sicht auf eine Sternenkarte ohne nutzerspezifische Anzeigen.
 * @author Christopher Jung
 *
 */
public class PublicStarmap
{

	protected Starmap map;

	/**
	 * Konstruktor.
	 * @param system Die ID des Systems
	 */
	public PublicStarmap(StarSystem system)
	{
		this.map = createMap(system);
	}

	private Starmap createMap(StarSystem system)
	{
		return new Starmap(system.getID());
	}

	/**
	 * Gibt ein evt. abweichendes Basisbild des Sektors aus Sicht des Benutzers im Rahmen
	 * der spezifischen Sternenkarte zurueck. Das Bild enthaelt
	 * keine Flottenmarkierungen. Falls kein abweichendes Basisbild existiert
	 * wird <code>null</code> zurueckgegeben.
	 * @param location Der Sektor
	 * @return Das Bild als String ohne den Pfad zum Data-Verzeichnis oder <code>null</code>.
	 */
	public String getUserSectorBaseImage(Location location)
	{
		return null;
	}

	/**
	 * Gibt das Overlay-Bild des Sektors zurueck. Dieses
	 * enthaelt ausschliesslich spielerspezifische Markierungen
	 * und keinerlei Hintergrundelemente. Der Hintergrund
	 * des Bilds ist transparent.
	 *
	 * Falls keine Overlay-Daten fuer den Sektor angezeigt werden sollen
	 * wird <code>null</code> zurueckgegeben.
	 *
	 * @param location Der Sektor
	 * @return Das Bild als String ohne den Pfad zum Data-Verzeichnis oder <code>null</code>
	 */
	public String getSectorOverlayImage(Location location)
	{
		return null;
	}

	/**
	 * Gibt zurueck, ob der Sektor einen fuer den Spieler theoretisch sichtbaren Inhalt besitzt.
	 * Es spielt dabei einzig der Inhalt des Sektors eine Rolle. Nicht gerpueft wird,
	 * ob sich ein entsprechendes Schiff in scanreichweite befindet bzw ob der Spieler anderweitig
	 * den Inhalt des Sektors scannen kann.
	 * @param position Die Position
	 * @return <code>true</code>, falls der Sektor sichtbaren Inhalt aufweist.
	 */
	public boolean isHasSectorContent(Location position)
	{
		return false;
	}

	/**
	 * Gibt sofern vorhanden ein Schiff zurueck, das den angegebenen
	 * Sektor scannen kann.
	 * @param location Der Sektor, der gescannt werden soll.
	 *
	 * @return Das Schiff, dass diesen Sektor scannen kann oder <code>null</code>
	 */
	public Ship getScanSchiffFuerSektor(Location location)
	{
		return null;
	}

	/**
	 * Gibt an, ob der entsprechende Sektor der Sternenkarte momentan gescannt werden kann.
	 *
	 * @param location Der Sektor.
	 * @return <code>true</code>, wenn der Sektor gescannt werden kann, sonst <code>false</code>
	 */
	public boolean isScannbar(Location location)
	{
		return false;
	}

	/**
	 * Die Beschreibung einer fuer einen Sektor zu verwendenden Grafik.
	 * Die Grafik besteht aus einem Pfad sowie einem fuer die Darstellung
	 * anzuwendenden Offset.
	 */
	public static class SectorBaseImage {
		private final String image;
		private final int x;
		private final int y;

		SectorBaseImage(String image, int x, int y)
		{
			this.image = image;
			this.x = x;
			this.y = y;
		}

		/**
		 * Gibt den Pfad zur Grafik zurueck.
		 * @return Der Pfad
		 */
		public String getImage()
		{
			return image;
		}

		/**
		 * Gibt den x-Offset in Sektoren der Grafik fuer die Darstellung zurueck (vgl. CSS-Sprites).
		 * @return Der x-Offset in Sektoren
		 */
		public int getX()
		{
			return x;
		}

		/**
		 * Gibt den y-Offset in Sektoren der Grafik fuer die Darstellung zurueck (vgl. CSS-Sprites).
		 * @return Der y-Offset in Sektoren
		 */
		public int getY()
		{
			return y;
		}
	}

	/**
	 * Gibt das Basisbild des Sektors zurueck. Das Bild enthaelt
	 * keine Flottenmarkierungen.
	 * @param location Der Sektor
	 * @return Das Bild als String ohne den Pfad zum Data-Verzeichnis.
	 */
	public SectorBaseImage getSectorBaseImage(Location location)
	{
		if(isNebel(location))
		{
			return new SectorBaseImage(map.getNebulaMap().get(location).getImage()+".png", 0, 0);
		}
		List<Base> positionBases = map.getBaseMap().get(location);
		if(positionBases != null && !positionBases.isEmpty())
		{
			Base base = positionBases.get(0);
			int[] offset = base.getBaseImageOffset(location);
			return new SectorBaseImage(base.getBaseImage(location)+".png", offset[0], offset[1]);
		}
		List<JumpNode> positionNodes = map.getNodeMap().get(location);
		if(positionNodes != null && !positionNodes.isEmpty())
		{
			for(JumpNode node: positionNodes)
			{
				if(!node.isHidden())
				{
					return new SectorBaseImage("jumpnode/jumpnode.png", 0, 0);
				}
			}
		}
		return new SectorBaseImage("space/space.png", 0, 0);
	}
	
	private boolean isNebel(Location location)
	{
		return map.isNebula(location);
	}

	/**
	 * Gibt zurueck, ob an der gegebenen Position eine (bekannte/sichtbare)
	 * Schlacht stattfindet.
	 * @param sektor Die Position
	 * @return <code>true</code> falls dem so ist
	 */
	public boolean isSchlachtImSektor(Location sektor)
	{
		return false;
	}

	/**
	 * Gibt zurueck, ob der Sektor als Sektor mit Schiffen auf Alarmstufe
	 * Rot bzw Gelb dargestellt werden soll, d.h. ein Spieler moeglicherweise
	 * beim Einflug in den Sektor angegriffen wird.
	 * @param sektor Der Sektor
	 * @return <code>true</code>, falls dem so ist
	 */
	public boolean isRoterAlarmImSektor(Location sektor)
	{
		return false;
	}
}