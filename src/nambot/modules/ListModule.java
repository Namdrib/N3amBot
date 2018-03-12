package nambot.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import nambot.Module;
import nambot.NamBot;
import nambot.util.Global;
import nambot.util.Helpers;
import net.dv8tion.jda.core.entities.Role;

public class ListModule extends Module
{
	public ListModule(NamBot nambot)
	{
		super(nambot, "list");
	}

	public ListModule(NamBot nambot, String command)
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
		String helpMessage = "List of all registered modules and their identifiers\n";

		// Find longest name lengths
		int maxModuleName = "module".length();
		int maxIdentifier = "identifier".length();
		for (Entry<String, Module> entry : nambot.modules.entrySet())
		{
			maxModuleName = Math.max(maxModuleName, entry.getValue().getClass().getSimpleName().length());
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
		for (Entry<String, Module> entry : nambot.modules.entrySet())
		{
			helpMessage += String.format(
					"%1$" + maxModuleName + "s|%2$" + maxIdentifier + "s\n",
					entry.getValue().getClass().getSimpleName(),
					entry.getKey());
		}
		helpMessage += "```\n";
		helpMessage += "Invoke any of these using `@" + Global.botName + " identifier`\n";
		helpMessage += "For further help with individual modules see, `@" + Global.botName + " identifier help`\n";

		Helpers.send(channel, helpMessage);
	}

}
