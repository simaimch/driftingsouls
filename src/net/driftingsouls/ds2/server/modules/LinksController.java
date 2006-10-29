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

import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.User;
import net.driftingsouls.ds2.server.framework.db.Database;
import net.driftingsouls.ds2.server.framework.pipeline.generators.DSGenerator;

/**
 * Die Menueleiste
 * @author Christopher Jung
 *
 */
public class LinksController extends DSGenerator {
	private static final String SCRIPT_FORUM = "http://forum.drifting-souls.net/phpBB2/";
	public LinksController(Context context) {
		super(context);
		
		setTemplate("links.html");
		setDisableDefaultCSS(true);
		setDisableDebugOutput(true);
	}

	@Override
	protected boolean validateAndPrepare(String action) {
		getTemplateEngine().set_var("SCRIPT_FORUM", SCRIPT_FORUM);
		
		return true;
	}

	public void hasNewPmAjaxAct() {
		User user = this.getUser();
		Database db = getDatabase();
		
		int pmcount = db.first("SELECT count(*) `count` FROM transmissionen WHERE empfaenger='",user.getID(),"' AND gelesen='0'").getInt("count");
		if( pmcount > 0 ) {
			getResponse().getContent().append("1");
		}
		else {
			getResponse().getContent().append("0");
		}
	}
	
	public void defaultAction() {
		User user = getUser();
		
		getTemplateEngine().set_var(	"global.datadir", user.getImagePath(),
										"user.npc"		, user.hasFlag( User.FLAG_ORDER_MENU ),
										"user.admin"	, (user.getAccessLevel() >= 30) );
	}
}
