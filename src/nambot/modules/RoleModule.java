package nambot.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.GuildController;

import nambot.modules.*;
import nambot.util.*;

/**
 * Used to parse a user's commands 
 * 
 * @author Namdrib
 *
 */
public class RoleModule
{
	// Variables
	private StringTokenizer				st;
	private GuildMessageReceivedEvent	event;
	private Message						message;
	private Member						member;
	private MessageChannel				channel;
	private Guild						guild;
	private GuildController				guildController;

	private final static String[] validCommands = {
			"help",
			"list",
			"listAll",
			"addRole",
			"addRoles",
			"removeRole",
			"removeRoles",
			"removeAllRoles",
			"createRole",
			"createRoles",
			"membersWith",
		};

	/**
	 * Set various member variables relating to the received event for later use
	 * Also stores the incoming message in a StringTokeniser
	 * 
	 * @param e the GuildMessageReceivedEvent to which to respond
	 * @throws Exception if the message was a Webhook message or the guild was null
	 */
	public RoleModule(GuildMessageReceivedEvent e) throws Exception
	{
		if (e.isWebhookMessage())
		{
			throw new Exception("Received Webhook message");
		}
		event = e;
		message = e.getMessage();
		member = e.getMember();
		channel = e.getChannel();
		st = new StringTokenizer(message.getContentStripped());
		guild = e.getGuild();

		if (guild == null)
		{
			throw new Exception();
		}

		guildController = guild.getController();
	}

	// Helper functions

	private void help()
	{
		String helpMessage
			= " ----- " + Global.botName + " help -----\n"
			+ "Invoke the bot using `@" + Helpers.getBotName(guild) + "` followed by one of the following commands:\n"
			+ "  `help`: display this help message\n"
			+ "  `list`: list your own roles\n"
			+ "  `listAll`: list all available roles you can add to yourself\n"
			+ "  `addRole ROLE`: add `ROLE` to yourself (where `ROLE` is in `listAll`)\n"
			+ "  `addRoles ROLES...`: add `ROLES...` to yourself (where `ROLES...` are in `listAll`)\n"
			+ "  `removeRole ROLE`: remove `ROLE` from yourself (where `ROLE` is in `list`)\n"
			+ "  `removeRoles ROLES...`: remove `ROLES...` from yourself (where `ROLES...` are in `listAll`)\n"
			+ "  `removeAllRoles`: remove all roles from yourself\n"
			+ "  `createRole ROLE`: create a role with name `ROLE`\n"
			+ "  `createRoles ROLES...`: create multiple roles with names `ROLES...`\n"
			+ "  `membersWith ROLE`: list all members to whom ROLE is assigned\n"
		;

		Helpers.send(channel, helpMessage);
	}

	/**
	 * Bot access determined by role hierarchy - bot cannot access roles with higher position
	 * Place the bot higher in the hierarchy to allow more access, lower to restrict access
	 * 
	 * @return list of roles the bot can access excluding public role
	 */
	private List<Role> getUsableRoles()
	{
		List<Role> botRoles = guild.getSelfMember().getRoles();
		int botPosition = Collections.max(botRoles.stream().map(Role::getPosition).collect(Collectors.toList()));
		List<Role> roles = new ArrayList<>(guild.getRoles()).stream().filter(x -> x.getPosition() < botPosition).collect(Collectors.toList());
		roles.remove(guild.getPublicRole());
		return roles;
	}

	/**
	 * 
	 * Create a role with name `name`
	 * If successful, set mentionable.
	 * Fail if another role with same name exists
	 * 
	 * @param name name of the role to create
	 */
	private void createMentionableRole(String name)
	{
		if (!guild.getRolesByName(name, false).isEmpty())
		{
			Helpers.send(channel, "Role " + name + " already exists");
			return;
		}

		try
		{
			guildController.createRole().setName(name)
					.queue(x -> x.getManager().setMentionable(true).queue());
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	/**
	 * For more user-friendly way to print lists
	 * Meant to integrate with other text
	 * 
	 * @param items any list of items
	 * @return String that is items.toString() without brackets opening '[' and closing ']'
	 */
	private <E> String listWithoutBrackets(List<E> items)
	{
		String s = items.toString();
		return s.substring(1, s.length() - 1);
	}

	// Maybe deprecated by getNamesFrom(List)
	private void outputListOfRoles(List<Role> roles, String prefix)
	{
		String out = prefix;
		Collections.sort(roles, new Comparator<Role>() {
			@Override
			public int compare(Role a, Role b)
			{
				return a.getName().compareTo(b.getName());
			}
		});
		List<String> namesList = roles.stream().map(Role::getName)
				.collect(Collectors.toList());
		out += listWithoutBrackets(namesList);
		Helpers.send(channel, out);
	}

	// Modify commands
	/**
	 * 
	 * Assigns a role to a Member
	 * 
	 * @param argument the name of the role to add
	 * @param member the member to modify
	 * @return the successfully-added role, null if no role was added
	 */
	private Role addRole(String argument, Member member)
	{
		Role out = null;
		try
		{
			List<Role> roles = guild.getRolesByName(argument, true);
			if (roles.isEmpty())
			{
				Helpers.send(channel, "Role " + argument + " does not exist. Maybe try creating it first");
				return null;
			}
			Role r = roles.get(0);
			if (member.getRoles().contains(r))
			{
				Helpers.send(channel, member.getEffectiveName() + " already has role " + r.getName());
				return null;
			}

			Helpers.send(channel, "Adding " + r.getName());
			guildController.addSingleRoleToMember(member, r).queue();
			out = r;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return out;
	}

	/**
	 * 
	 * Assigns multiple roles to a Member
	 * 
	 * @param arguments the names of the roles to add
	 * @param member the member to modify
	 * @return the successfully-added roles, null if no roles were added
	 */
	private List<Role> addRoles(List<String> arguments, Member member)
	{
		return null;
	}

	/**
	 * Removes a role from a member
	 * 
	 * @param argument the name of the role to remove
	 * @param member the member to modify
	 * @return the successfully-removed role, null if no role was removed
	 */
	private Role removeRole(String argument, Member member)
	{
		Role out = null;
		try
		{
			List<Role> guildRoles = guild.getRolesByName(argument, true);
			if (guildRoles.isEmpty())
			{
				Helpers.send(channel, "Role " + argument + " does not exist. Maybe try creating it first");
				return null;
			}
			Role r = guildRoles.get(0);
		
			List<Role> memberRoles = member.getRoles();
			if (!memberRoles.contains(r))
			{
				Helpers.send(channel, argument + " is not assigned to " + member.getEffectiveName());
				return null;
			}

			Helpers.send(channel, "Removing " + r.getName());
			guildController.removeSingleRoleFromMember(member, r).queue();
			out = r;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return out;
	}

	/**
	 * 
	 * Remove multiple roles from a Member
	 * 
	 * @param arguments the names of the roles to remove
	 * @param member the member to modify
	 * @return the successfully-removed roles, null if no roles were removed
	 */
	private List<Role> removeRoles(List<String> arguments)
	{
		return null;
	}

	/**
	 * 
	 * Create a mentionable role in a guild
	 * 
	 * @param arguments the name of the mentionable role to create
	 * @param guild the guild in which to create the role
	 * @return the successfully-created role, null if no roles were created
	 */
	private Role createRole(String argument, Guild guild)
	{
		return null;
	}


	/**
	 * 
	 * Create multiple mentionable roles in a guild
	 * 
	 * @param arguments the names of the mentionable roles to create
	 * @param guild the guild in which to create the roles
	 * @return the successfully-created roles, null if no roles were created
	 */
	private List<Role> createRoles(List<String> arguments, Guild guild)
	{
		return null;
	}

	// Functions
	/**
	 * Parse and carry out specified the user's commands
	 */
	public void execute()
	{
		// Bot wasn't invoked
		if (!(st.hasMoreTokens() && st.nextToken().equals("@" + Helpers.getBotName(guild))))
		{
			return;
		}

		System.out.println(member.getEffectiveName() + " : " + message.getContentDisplay());

		if (!st.hasMoreTokens())
		{
			Helpers.send(channel, "invalid command invoked");
			help();
			return;
		}
		String command = st.nextToken().toLowerCase();
		switch (command)
		{
			case "help":
			{
				Helpers.send(channel, "`help` command invoked");
				help();
				break;
			}

			case "list":
			{
				Helpers.send(channel, "`list` command invoked");

				try
				{
					List<Role> memberRoles = new ArrayList<>(member.getRoles());
					memberRoles.remove(guild.getPublicRole());

					if (memberRoles.isEmpty())
					{
						Helpers.send(channel, member.getEffectiveName() + " has no roles");
						return;
					}

					String listMessage = "List of current roles for " + member.getEffectiveName() + "\n";
					String roleOutputs = listWithoutBrackets(Helpers.getNamesFrom(memberRoles));
					Helpers.send(channel, listMessage + roleOutputs);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
				break;
			}
			case "listall":
			{
				Helpers.send(channel, "`listAll` command invoked");

				List<Role> allRoles = new ArrayList<>(getUsableRoles());
				allRoles.remove(guild.getPublicRole());

				String listAllMessage = "List of all available roles\n";
				String roleOutputs = listWithoutBrackets(Helpers.getNamesFrom(allRoles));
				Helpers.send(channel, listAllMessage + roleOutputs);
				break;
			}

			case "addrole":
			{
				Helpers.send(channel, "`addRole` command invoked");

				if (!st.hasMoreTokens())
				{
					Helpers.send(channel, "Usage: `addRole role`");
					return;
				}
				String argument = st.nextToken();

				Role r = addRole(argument, member);
				break;
			}
			case "addroles":
			{
				Helpers.send(channel, "`addRoles` command invoked");

				// Collect all the aforementioned roles
				String argument;
				List<Role> rolesToAdd = new ArrayList<>();
				final List<Role> allRoles = guild.getRoles();
				while (st.hasMoreTokens())
				{
					argument = st.nextToken();
					for (Role r : allRoles)
					{
						if (argument.equals(r.getName()))
						{
							rolesToAdd.add(r);
							break;
						}
					}
				}

				if (rolesToAdd.isEmpty())
				{
					Helpers.send(channel, "No roles to add");
					return;
				}

				Helpers.send(channel, "Adding " + rolesToAdd);
				List<Role> addedRoles = new ArrayList<>();
				try
				{
					guildController.addRolesToMember(member, rolesToAdd).queue();
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
				break;
			}

			case "removerole":
			{
				Helpers.send(channel, "`removeRole` command invoked");

				if (!st.hasMoreTokens())
				{
					Helpers.send(channel, "Usage: `removeRole role`");
					return;
				}
				String argument = st.nextToken();

				Role r = removeRole(argument, member);
				break;
			}
			case "removeroles":
			{
				Helpers.send(channel, "`removeRoles` command invoked");

				// Collect all the aforementioned roles
				String argument;
				List<Role> rolesToRemove = new ArrayList<>();
				final List<Role> potentialRoles = member.getRoles();
				if (potentialRoles.isEmpty())
				{
					Helpers.send(channel, "No roles to remove");
					return;
				}

				while (st.hasMoreTokens())
				{
					argument = st.nextToken();
					for (Role r : potentialRoles)
					{
						if (argument.equals(r.getName()))
						{
							rolesToRemove.add(r);
							break;
						}
					}
				}

				if (rolesToRemove.isEmpty())
				{
					Helpers.send(channel, "No roles to remove");
					return;
				}

				Helpers.send(channel, "Removing " + rolesToRemove);
				guildController.removeRolesFromMember(member, rolesToRemove).queue();
				break;
			}
			case "removeallroles":
			{
				Helpers.send(channel, "`removeAllRoles` command invoked");

				List<Role> roles = new ArrayList<>(member.getRoles());
				List<Role> removed = new ArrayList<>();
				for (Role r : roles)
				{
					try
					{
						guildController.removeSingleRoleFromMember(member, r).queue();
						removed.add(r);
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
				String out = "Roles removed from " + member.getEffectiveName() + ": ";
				out += listWithoutBrackets(removed);
				Helpers.send(channel, out);
				break;
			}

			case "createrole":
			{
				Helpers.send(channel, "`createRole` command invoked");

				if (!st.hasMoreTokens())
				{
					Helpers.send(channel, "Usage: `createRole role`");
					return;
				}
				String argument = st.nextToken();

				createMentionableRole(argument);
				break;
			}
			case "createroles":
			{
				Helpers.send(channel, "`createRoles` command invoked");

				String argument;
				while (st.hasMoreTokens())
				{
					argument = st.nextToken();
					createMentionableRole(argument);
				}

				break;
			}

			case "memberswith":
			{
				Helpers.send(channel, "`membersWith` command invoked");

				if (!st.hasMoreTokens())
				{
					Helpers.send(channel, "Usage: `membersWith role`");
					return;
				}
				String argument = st.nextToken();

				List<Member> members = guild
						.getMembersWithRoles(guild.getRolesByName(argument, true));
				if (members.isEmpty())
				{
					Helpers.send(channel, "No members with role " + argument);
					return;
				}

				Collections.sort(members, new Comparator<Member>() {
					@Override
					public int compare(Member a, Member b)
					{
						return a.getEffectiveName().compareTo(b.getEffectiveName());
					}
				});
				String out = "Members with role " + argument + ":\n";
				List<String> namesList = members.stream()
						.map(Member::getEffectiveName).collect(Collectors.toList());
				out += listWithoutBrackets(namesList);
				Helpers.send(channel, out);
				break;
			}
			default:
			{
				Helpers.send(channel, "invalid command invoked");
				help();
			}
		}
	}
}
