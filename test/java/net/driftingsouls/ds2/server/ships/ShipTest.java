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
package net.driftingsouls.ds2.server.ships;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import net.driftingsouls.ds2.server.DriftingSoulsDBTestCase;

import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.junit.Before;
import org.junit.Test;

/**
 * Testet die Schiffe
 * 
 * @author Christopher Jung
 * 
 */
public class ShipTest extends DriftingSoulsDBTestCase
{
	private Ship tanker;
	private Ship container1;
	private Ship container2;
	private Ship jaeger1;
	private Ship jaeger2;

	public IDataSet getDataSet() throws Exception
	{
		return new FlatXmlDataSet(ShipTest.class.getResourceAsStream("ShipTest.xml"));
	}

	/**
	 * Laedt einige Schiffe
	 */
	@Before
	public void loadShips()
	{
		this.tanker = (Ship)context.getDB().get(Ship.class, 1);
		this.container1 = (Ship)context.getDB().get(Ship.class, 2);
		this.container2 = (Ship)context.getDB().get(Ship.class, 3);
		this.jaeger1 = (Ship)context.getDB().get(Ship.class, 4);
		this.jaeger2 = (Ship)context.getDB().get(Ship.class, 5);
	}

	/**
	 * Testet das Andocken von Containern an ein Schiff
	 */
	@Test
	public void testDock()
	{
		assertThat(this.container1.getDocked(), is(""));
		assertThat(this.container2.getDocked(), is(""));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.container1.getTypeData().getCargo(), is(1500L));
		assertThat(this.container2.getTypeData().getCargo(), is(1500L));

		assertThat(this.tanker.dock(this.container1), is(false));
		assertThat(this.container1.getDocked(), is("" + this.tanker.getId()));
		assertThat(this.container2.getDocked(), is(""));
		assertThat(this.tanker.getTypeData().getCargo(), is(3000L));
		assertThat(this.container1.getTypeData().getCargo(), is(0L));
		assertThat(this.container2.getTypeData().getCargo(), is(1500L));

		assertThat(this.tanker.dock(this.container2), is(false));
		assertThat(this.container1.getDocked(), is("" + this.tanker.getId()));
		assertThat(this.container2.getDocked(), is("" + this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(4500L));
		assertThat(this.container1.getTypeData().getCargo(), is(0L));
		assertThat(this.container2.getTypeData().getCargo(), is(0L));

		// Wiederholtes docken sollte nicht funktionieren
		assertThat(this.tanker.dock(this.container2), is(true));
	}

	/**
	 * Testet das Andocken von mehreren Containern auf einmal an ein Schiff
	 */
	@Test
	public void testDockGroup()
	{
		assertThat(this.container1.getDocked(), is(""));
		assertThat(this.container2.getDocked(), is(""));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.container1.getTypeData().getCargo(), is(1500L));
		assertThat(this.container2.getTypeData().getCargo(), is(1500L));

		assertThat(this.tanker.dock(this.container1, this.container2), is(false));
		assertThat(this.container1.getDocked(), is("" + this.tanker.getId()));
		assertThat(this.container2.getDocked(), is("" + this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(4500L));
		assertThat(this.container1.getTypeData().getCargo(), is(0L));
		assertThat(this.container2.getTypeData().getCargo(), is(0L));
	}

	/**
	 * Testet das Abdocken von Containern an ein Schiff
	 */
	@Test
	public void testUndock()
	{
		assertThat(this.tanker.dock(this.container1, this.container2), is(false));
		assertThat(this.container1.getDocked(), is("" + this.tanker.getId()));
		assertThat(this.container2.getDocked(), is("" + this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(4500L));
		assertThat(this.container1.getTypeData().getCargo(), is(0L));
		assertThat(this.container2.getTypeData().getCargo(), is(0L));

		this.context.commit();

		this.tanker.undock(this.container1);
		assertThat(this.container1.getDocked(), is(""));
		assertThat(this.container2.getDocked(), is("" + this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(3000L));
		assertThat(this.container1.getTypeData().getCargo(), is(1500L));
		assertThat(this.container2.getTypeData().getCargo(), is(0L));
	}
	
	/**
	 * Testet das Abdocken aller Container an ein Schiff auf einmal
	 */
	@Test
	public void testUndockAll()
	{
		assertThat(this.tanker.dock(this.container1, this.container2), is(false));
		assertThat(this.container1.getDocked(), is("" + this.tanker.getId()));
		assertThat(this.container2.getDocked(), is("" + this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(4500L));
		assertThat(this.container1.getTypeData().getCargo(), is(0L));
		assertThat(this.container2.getTypeData().getCargo(), is(0L));

		this.context.commit();

		this.tanker.undock();
		assertThat(this.container1.getDocked(), is(""));
		assertThat(this.container2.getDocked(), is(""));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.container1.getTypeData().getCargo(), is(1500L));
		assertThat(this.container2.getTypeData().getCargo(), is(1500L));
	}
	
	/**
	 * Testet das Abdocken einer Liste von Container von ein Schiff
	 */
	@Test
	public void testUndockList()
	{
		assertThat(this.tanker.dock(this.container1, this.container2), is(false));
		assertThat(this.container1.getDocked(), is("" + this.tanker.getId()));
		assertThat(this.container2.getDocked(), is("" + this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(4500L));
		assertThat(this.container1.getTypeData().getCargo(), is(0L));
		assertThat(this.container2.getTypeData().getCargo(), is(0L));

		this.context.commit();

		this.tanker.undock(this.container1, this.container2);
		assertThat(this.container1.getDocked(), is(""));
		assertThat(this.container2.getDocked(), is(""));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.container1.getTypeData().getCargo(), is(1500L));
		assertThat(this.container2.getTypeData().getCargo(), is(1500L));
	}

	/**
	 * Testet das Starten von Jaegern von einem Schiff
	 */
	@Test
	public void testStart()
	{
		assertThat(this.tanker.land(this.jaeger1, this.jaeger2), is(false));
		assertThat(this.jaeger1.getDocked(), is("l " + this.tanker.getId()));
		assertThat(this.jaeger2.getDocked(), is("l " + this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));

		this.context.commit();

		this.tanker.start(this.jaeger1);
		assertThat(this.jaeger1.getDocked(), is(""));
		assertThat(this.jaeger2.getDocked(), is("l " + this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));

		this.tanker.start(this.jaeger2);
		assertThat(this.jaeger1.getDocked(), is(""));
		assertThat(this.jaeger2.getDocked(), is(""));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));
	}
	
	/**
	 * Testet das Starten aller Jaegern von einem Schiff
	 */
	@Test
	public void testStartAll()
	{
		assertThat(this.tanker.land(this.jaeger1, this.jaeger2), is(false));
		assertThat(this.jaeger1.getDocked(), is("l " + this.tanker.getId()));
		assertThat(this.jaeger2.getDocked(), is("l " + this.tanker.getId()));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));

		this.context.commit();

		this.tanker.start();
		
		// TODO: Refresh ist momentan notwendig, da die Objekte innerhalb 
		// der Session nicht aktualisiert werden
		context.getDB().refresh(this.jaeger1);
		context.getDB().refresh(this.jaeger2);
		
		assertThat(this.jaeger1.getDocked(), is(""));
		assertThat(this.jaeger2.getDocked(), is(""));
		assertThat(this.tanker.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger1.getTypeData().getCargo(), is(1500L));
		assertThat(this.jaeger2.getTypeData().getCargo(), is(1500L));
	}
	
	/**
	 * Testet {@link Ship#getDockedShips()}
	 */
	@Test
	public void testGetDockedList()
	{
		// Ausgangslage: Liste ist leer - keine Schiffe gedockt
		assertThat(this.tanker.getDockedShips().size(), is(0));
		
		// Zwei Container andocken
		assertThat(this.tanker.dock(this.container1, this.container2), is(false));
		assertThat(this.container1.getDocked(), is("" + this.tanker.getId()));
		assertThat(this.container2.getDocked(), is("" + this.tanker.getId()));

		this.context.commit();

		// Liste muss beide Container beinhalten
		assertThat(this.tanker.getDockedShips().size(), is(2));
		assertThat(this.tanker.getDockedShips().contains(this.container1), is(true));
		assertThat(this.tanker.getDockedShips().contains(this.container2), is(true));
		
		this.tanker.undock(this.container1);
		this.context.commit();
		
		// Die Liste muss nun nur noch den zweiten Container enthalten
		assertThat(this.tanker.getDockedShips().size(), is(1));
		assertThat(this.tanker.getDockedShips().contains(this.container1), is(false));
		assertThat(this.tanker.getDockedShips().contains(this.container2), is(true));
	}
}