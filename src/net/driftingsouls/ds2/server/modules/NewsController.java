package net.driftingsouls.ds2.server.modules;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.driftingsouls.ds2.server.entities.NewsEntry;
import net.driftingsouls.ds2.server.framework.Common;
import net.driftingsouls.ds2.server.framework.Context;
import net.driftingsouls.ds2.server.framework.pipeline.generators.Action;
import net.driftingsouls.ds2.server.framework.pipeline.generators.ActionType;
import net.driftingsouls.ds2.server.framework.pipeline.generators.TemplateGenerator;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

/**
 * Zeigt die News der letzten Zeit als RSS Feed an.
 * 
 * @author Sebastian Gift
 */
public class NewsController extends TemplateGenerator 
{	
	/**
	 * Legt den RSS Feed an.
	 * 
	 * @param context Der Kontext.
	 */
	public NewsController(Context context)
	{
		super(context);
		requireValidSession(false);
	}

	@Override
	protected void printHeader(String action) throws IOException 
	{}
	
	@Override
	protected void printFooter(String action) throws IOException
	{}

	@Override
	protected boolean validateAndPrepare(String action) 
	{
		return true;
	}

	/**
	 * Gibt den News RSS Feed aus.
	 */
	@Override
	@Action(ActionType.DEFAULT)
	public void defaultAction() throws IOException
	{
		SyndFeed feed = new SyndFeedImpl();
		feed.setFeedType("rss_2.0");
		feed.setTitle("Drifting-Souls News");
		feed.setLink("http://ds.drifting-souls.net");
		feed.setDescription("Drifting-Souls Newsfeed");
		
		Session db = getDB();
		List<SyndEntry> entries = new ArrayList<SyndEntry>();
		List<NewsEntry> allNews = Common.cast(db.createQuery("from NewsEntry").list());
		for(NewsEntry news: allNews)
		{
	     
	     SyndEntry entry;
	     SyndContent description;

	     entry = new SyndEntryImpl();
	     entry.setTitle(news.getTitle());
	     entry.setPublishedDate(new Date(news.getDate()));
	     
	     description = new SyndContentImpl();
	     description.setType("text/plain");
	     description.setValue(news.getDescription());
	     entry.setDescription(description);
	     entries.add(entry);
		}
		feed.setEntries(entries);
		
		SyndFeedOutput result = new SyndFeedOutput();
		Writer writer = getContext().getResponse().getWriter();

		try
		{
			result.output(feed, writer);
		}
		catch( FeedException e )
		{
			log.error("Could not write out rss feed due to errors", e);
		}
	}
	
	Logger log = Logger.getLogger(NewsController.class);
}