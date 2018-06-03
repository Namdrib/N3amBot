package bot.modules;

import java.util.ArrayList;

import bot.Module;
import bot.Bot;
import bot.util.Global;
import bot.util.Helpers;

public class HelpModule extends Module
{
	public HelpModule(Bot bot)
	{
		super(bot, "help");
	}

	public HelpModule(Bot bot, String identifier)
	{
		super(bot, identifier);
	}

	@Override
	protected void setCommandList()
	{
		commandList = new ArrayList<>();
	}

	@Override
	protected void help()
	{
		String helpMessage = " ----- Help message for " + Global.botName
				+ " -----\n" + "Invoke with `" + Global.prefix
				+ " identifier [command [arguments...]]`\n" + "where...\n"
				+ "  `identifier`: a module **identifier** that appears in `"
				+ Global.prefix + " list`\n"
				+ "  `command`: an item that appears in `" + Global.prefix
				+ " identifier help`'s help list\n" + "\n"
				+ "Further help can be found at: `" + Global.prefix
				+ " identifier help` (e.g. `" + Global.prefix
				+ " role help`)\n";

		Helpers.send(channel, helpMessage);
	}
}
