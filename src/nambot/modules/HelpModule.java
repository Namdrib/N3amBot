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

	public HelpModule(NamBot nambot, String command)
	{
		super(nambot, command);
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
				+ "Invoke " + Global.botName + " with `@" + Global.botName + " identifier [command [arguments...]]`\n"
				+ "where...\n"
				+ "  `identifier` is a module **identifier** that appears in `@" + Global.botName + " list`\n"
				+ "  `command` is an item that appears in `@" + Global.botName + " identifier help`'s help list\n"
				+ "\n"
				+ "Further help can be foudn at: `@" + Global.botName + " identifier help`\n"
		;

		Helpers.send(channel, helpMessage);
	}
}
