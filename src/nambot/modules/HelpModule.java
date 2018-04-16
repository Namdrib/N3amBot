package nambot.modules;

import java.util.ArrayList;

import nambot.Module;
import nambot.NamBot;
import nambot.util.Global;
import nambot.util.Helpers;

public class HelpModule extends Module
{
	public HelpModule(NamBot nambot)
	{
		super(nambot, "help");
	}

	public HelpModule(NamBot nambot, String identifier)
	{
		super(nambot, identifier);
	}

	@Override
	protected void setCommandList()
	{
		commandList = new ArrayList<>();
	}

	@Override
	protected void help()
	{
		String helpMessage = " ----- Help message for " + Global.botName + " -----\n"
				+ "Invoke with `" + Global.prefix + " identifier [command [arguments...]]`\n"
				+ "where...\n"
				+ "  `identifier`: a module **identifier** that appears in `" + Global.prefix + " list`\n"
				+ "  `command`: an item that appears in `" + Global.prefix + " identifier help`'s help list\n"
				+ "\n"
				+ "Further help can be found at: `" + Global.prefix + " identifier help` (e.g. `@NamBot role help`)\n"
		;

		Helpers.send(channel, helpMessage);
	}
}
