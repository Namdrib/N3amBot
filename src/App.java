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
	public final static String botToken = "YOUR_BOT_TOKEN_HERE";
	public static void main(String[] args) throws LoginException, IllegalArgumentException, RateLimitedException
	{
		JDA api = new JDABuilder(AccountType.BOT)
				.setToken(App.botToken)
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
			;
		}
	}
}
