package nambot.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;

/**
 * Provide static helper functions for functions that are to be used in multiple
 * files
 * 
 * @author Namdrib
 *
 */
public class Helpers
{

	/**
	 * Shortcut for queueing sending a message
	 * 
	 * @param msg
	 *            message to send
	 */
	public static void send(MessageChannel channel, String msg)
	{
		if (channel != null)
		{
			channel.sendMessage(msg).queue();
		}
	}

	/**
	 * Return the bot's effective name in the current guild
	 * 
	 * @return the bot's effective name in the current guild
	 */
	public static String getBotName(Guild guild)
	{
		Member bot = guild.getSelfMember();
		return bot.getEffectiveName();
	}

	/**
	 * 
	 * Extract names from list of objects. Returns early if list is null or any
	 * list elements are null
	 * 
	 * @param list
	 *            list of objects. Ideally related to the JDA entities
	 * @return List<String> containing the names of each item of the list
	 */
	public static <E> List<String> getNamesFrom(List<E> list)
	{
		List<String> out = new ArrayList<>();
		if (list == null)
		{
			return out;
		}
		for (E e : list)
		{
			if (e == null)
			{
				continue;
			}
			if (e instanceof Member)
			{
				out.add(((Member) e).getEffectiveName());
			}
			else if (e instanceof User)
			{
				out.add(((User) e).getName());
			}
			else if (e instanceof Role)
			{
				out.add(((Role) e).getName());
			}
			else
			{
				return out;
			}
		}
		Collections.sort(out);
		return out;
	}

	/**
	 * For more user-friendly way to print lists. Meant to integrate with other
	 * text formatters
	 * 
	 * @param items
	 *            any list of items
	 * @return String that is items.toString() without brackets opening '[' and
	 *         closing ']'
	 */
	public static <E> String listWithoutBrackets(List<E> items)
	{
		String s = items.toString();
		return s.substring(1, s.length() - 1);
	}
}
