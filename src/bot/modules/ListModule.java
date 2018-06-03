package bot.modules;

import java.util.ArrayList;
import java.util.Map.Entry;

import bot.Module;
import bot.Bot;
import bot.util.Global;
import bot.util.Helpers;

public class ListModule extends Module
{
	public ListModule(Bot bot)
	{
		super(bot, "list");
	}

	public ListModule(Bot bot, String identifier)
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
		String helpMessage = "List of all registered modules and their identifiers\n";

		// Find longest name lengths
		int maxModuleName = "module".length();
		int maxIdentifier = "identifier".length();
		for (Entry<String, Module> entry : bot.modules.entrySet())
		{
			maxModuleName = Math.max(maxModuleName,
					entry.getValue().getClass().getSimpleName().length());
			maxIdentifier = Math.max(maxIdentifier, entry.getKey().length());
		}

		// Add header rows
		helpMessage += "```\n";
		helpMessage += String.format(
				"%1$" + maxModuleName + "s %2$" + maxIdentifier + "s\n",
				"module", "identifier");
		helpMessage += String
				.format("%1$" + maxModuleName + "s+%2$" + maxIdentifier + "s\n",
						"", "")
				.replace(' ', '-');

		// Output formatted
		for (Entry<String, Module> entry : bot.modules.entrySet())
		{
			helpMessage += String.format(
					"%1$" + maxModuleName + "s|%2$" + maxIdentifier + "s\n",
					entry.getValue().getClass().getSimpleName(),
					entry.getKey());
		}
		helpMessage += "```\n";
		helpMessage += "Invoke any of these using `" + Global.prefix
				+ " identifier`\n";
		helpMessage += "For further help with individual modules see, `"
				+ Global.prefix + " identifier help`\n";

		Helpers.send(channel, helpMessage);
	}

}
