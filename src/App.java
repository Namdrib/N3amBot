import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

// https://github.com/reactiflux/discord-irc/wiki/Creating-a-discord-bot-&-getting-a-token
// ^ to add the bot to a server

public class App extends ListenerAdapter
{
	// Try to read the bot token
	// Read from `config.properties` (in project root)
	// If that doesn't exist, try to retrieve from environment vars
	public static String getBotToken()
	{
		String botToken = null;
		final String envVar = "DISCORD_ROLEBOT_TOKEN";

		Properties prop = new Properties();
		final String filename = "config.properties"; // make sure this file exists, store token there

		// Read the bot's token from filename or environment variable
		try (InputStream input = new FileInputStream(filename))
		{
			prop.load(input);
			botToken = prop.getProperty("botToken");
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

	public static void main(String[] args) throws LoginException, IllegalArgumentException, RateLimitedException
	{
		final String botToken = App.getBotToken();
		JDA api = new JDABuilder(AccountType.BOT)
				.setToken(botToken)
				.addEventListener(new App())
				.buildAsync();

		// Set the game to a useful message
		api.getPresence().setGame(Game.playing("Invoke with " + Global.prefix));
	}

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent e)
	{
		// Do not respond to messages from other bots, including ourself
		if (e.getAuthor().isBot()) return;

		try
		{
			Parser parser = new Parser(e);
			if (parser.validate())
			{
				parser.execute();
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
