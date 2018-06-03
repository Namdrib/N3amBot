package bot;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.LoginException;

import bot.modules.*;
import bot.util.*;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.MessageEmbedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageEmbedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

// https://github.com/reactiflux/discord-irc/wiki/Creating-a-discord-bot-&-getting-a-token
// ^ to add the bot to a server

/**
 * The main program. Creates a JDA object with the bot, reads token key from a
 * property or from environment variable. If neither of these exists, cannot
 * start up
 * 
 * @author Namdrib
 *
 */
public class Bot extends ListenerAdapter
{
	public Map<String, Module> modules;

	public Bot()
	{
		modules = new HashMap<>();
	}

	// Try to read the bot token
	// Read from `config.properties` (in project root)
	// If that doesn't exist, try to retrieve from environment vars
	public String getBotToken()
	{
		String botToken = null;
		final String envVar = "DISCORD_BOT_TOKEN";

		Properties prop = new Properties();
		final String filename = "config.properties";

		// Read the bot's token from filename or environment variable
		try (InputStream input = new FileInputStream(filename))
		{
			prop.load(input);
			botToken = (String) prop.getOrDefault("botToken", null);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}

		// Retrieve from environment variables (for Heroku)
		if (botToken == null)
		{
			botToken = System.getenv(envVar);
		}
		return botToken;
	}

	/**
	 * 
	 * Links a Module to the bot for future access. the Module will be invoked
	 * if the first token after tagging the bot is one of the entries in modules
	 * 
	 * @param module
	 *            the Module to link
	 * @param name
	 *            lower-case no-space identified for the module
	 * @return true iff the module was successfully loaded
	 */
	public boolean register(Module module, String identifier)
	{
		String moduleName = module.getClass().getSimpleName();
		String result;
		boolean out;

		if (modules.containsKey(identifier))
		{
			result = "!!!! Already registered " + identifier + " to "
					+ modules.get(identifier).getClass().getName();
			out = false;
		}
		else
		{
			modules.put(identifier, module);
			result = "Successfully registered " + moduleName + " as "
					+ identifier;
			out = true;
		}

		System.out.println(result);
		return out;
	}

	public static void main(String[] args) throws LoginException,
			IllegalArgumentException, RateLimitedException
	{
		Bot bot = new Bot();

		// Start the bot
		final String botToken = bot.getBotToken();
		JDA api = new JDABuilder(AccountType.BOT).setToken(botToken)
				.addEventListener(bot).buildAsync();

		// Set the game to a useful message
		api.getPresence().setGame(Game.playing(Global.prefix + " help"));

		// Load available modules
		new HelpModule(bot, "help");
		new ListModule(bot, "list");
		new RoleModule(bot, "role");
		new OzbModule(bot, "ozb");
	}

	@Override
	public void onMessageEmbed(MessageEmbedEvent event)
	{
		System.out.println("MESSAGE EMBED EVENT");
	}

	@Override
	public void onGuildMessageEmbed(GuildMessageEmbedEvent event)
	{
		System.out.println("GUILD MESSAGE EMBED EVENT");
	}

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent e)
	{
		if (e.getMessage().getEmbeds() != null
				&& !e.getMessage().getEmbeds().isEmpty())
		{
			Module m = modules.get("lyrics");
			if (m != null)
			{
				m.handle(e, new StringTokenizer(
						e.getMessage().getContentDisplay()));
			}
		}

		// Do not respond to messages from other bots, including ourself
		if (e.getAuthor().isBot()) return;

		// Only continue if the bot was actually invoked
		StringTokenizer st = new StringTokenizer(
				e.getMessage().getContentDisplay());
		if (!(st.hasMoreTokens() && st.nextToken()
				.equals("@" + Helpers.getBotName(e.getGuild()))))
		{
			return;
		}

		// Proceed if a valid module was invoked
		if (st.hasMoreTokens())
		{
			String targetModule = st.nextToken();
			if (modules.containsKey(targetModule))
			{
				Module m = modules.get(targetModule);
				m.handle(e, st);
			}
			else
			{
				String msg = "No identifier " + targetModule + " exists. See `"
						+ Global.prefix
						+ " list` for a list of valid identifiers";
				Helpers.send(e.getChannel(), msg);
			}
		}
		else
		{
			String msg = "Invoke with `" + Global.prefix
					+ " identifier [command [arguments...]]`\n" + "See `"
					+ Global.prefix + " list` for a list of valid identifiers";
			Helpers.send(e.getChannel(), msg);
		}
	}
}
