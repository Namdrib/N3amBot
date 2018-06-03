package bot.modules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import bot.Module;
import bot.Bot;
import bot.util.Helpers;

public class OzbModule extends Module
{
	/**
	 * Register this module with Bot
	 * 
	 * @param bot
	 *            the Bot object to which this registers
	 */
	public OzbModule(Bot bot)
	{
		super(bot, "role");
	}

	public OzbModule(Bot bot, String identifier)
	{
		super(bot, identifier);
	}

	@Override
	protected void setCommandList()
	{
		commandList = new ArrayList<>(Arrays.asList("help", "info"));
	}

	// Helper functions

	@Override
	protected void help()
	{
		String helpMessage = " ----- Help message for "
				+ getClass().getSimpleName() + " -----\n"
				+ "  `help`: display this help message\n"
				+ "  `info`: print info for the deal\n";

		Helpers.send(channel, helpMessage);
	}

	/**
	 * Parse and carry out specified the user's commands TODO : move majority to
	 * handle()
	 */
	public void execute(String command)
	{
		switch (command)
		{
			case "help":
			{
				Helpers.send(channel, "`help` command invoked");
				help();
				break;
			}
			case "info":
			{
				Helpers.send(channel, "`info` command invoked");

				if (!st.hasMoreTokens())
				{
					Helpers.send(channel, "Usage: `info dealLink`");
					return;
				}
				String argument = st.nextToken();
				if (!argument.contains("www.ozbargain.com.au/node/"))
				{
					Helpers.send(channel,
							"Link must be for a valid OzBargain deal");
				}

				getOzbInfo(argument);
				break;
			}
			default:
			{
				Helpers.send(channel, "invalid command invoked");
				help();
			}
		}
	}

	/**
	 * Prints upvotes, downvotes, net votes, number of clicks, date posted and
	 * date of expiry of a given deal
	 * 
	 * @param dealUrl
	 *            the URL of the deal to get info
	 */
	private void getOzbInfo(String dealUrl)
	{
		try
		{
			Document doc = Jsoup.connect(dealUrl).get();

			// Check it's actually a deal, not a forum post etc.
			if (doc.selectFirst(".node-ozbdeal") == null)
			{
				Helpers.send(channel,
						"Link must be for a valid OzBargain deal");
				return;
			}

			Elements dealVotes = doc.select(".nvb");
			int upvotes = Integer.parseInt(dealVotes.first().text().trim());
			int downvotes = Integer.parseInt(dealVotes.last().text().trim());
			String clicks = doc.selectFirst(".nodeclicks").text().trim();

			// TODO: Better way of getting submitted datetime info
			Element submitted = doc.selectFirst(".submitted");
			// System.out.println("submitted: " + submitted);
			String submitDateTime = submitted.ownText()
					.substring(0, submitted.ownText().indexOf(" Last edited"))
					.trim();
			System.out.println("ownText(): " + submitted.ownText());
			String expiry = doc.selectFirst(".nodeexpiry").text().trim();

			String msg = doc.title() + "\nUp: " + upvotes + ", Down: "
					+ downvotes + ", Net: " + (upvotes - downvotes) + " "
					+ clicks + "\nPosted " + submitDateTime + ", expires "
					+ expiry;
			Helpers.send(channel, msg);
			System.out.println(msg);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
	}
}
