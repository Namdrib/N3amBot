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

public class Parser
{
	// Variables
	private StringTokenizer				st;
	private GuildMessageReceivedEvent	event;
	private Message						message;
	private Member						member;
	private MessageChannel				channel;
	private Guild						guild;
	private GuildController				guildController;

	// Used for `undo`
	private String						recentCommand;
	private Member						recentMember;
	private List<Role>					recentRoles;

	public Parser(GuildMessageReceivedEvent e) throws Exception
	{
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

	private void send(String msg)
	{
		channel.sendMessage(msg).queue();
	}

	private String getBotName()
	{
		Member bot = guild.getSelfMember();
		return bot.getEffectiveName();
	}

	private void help()
	{
		String helpMessage
			= " ----- " + Global.botName + " help -----\n"
			+ "Invoke the bot using `@" + getBotName() + "` followed by one of the following commands:\n"
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
			+ "  `undo`: undo the most recent action (only works for add, remove, and create)\n"
			+ "    only works if the most recent action was performed by the invoking member\n"
			+ "    e.g. if PersonA creates four roles, only PersonA can undo this.\n"
		;

		send(helpMessage);
	}

	private List<Role> getUsableRoles()
	{
		List<Role> botRoles = guild.getSelfMember().getRoles();
		int botPosition = Collections.max(botRoles.stream().map(Role::getPosition).collect(Collectors.toList()));
		List<Role> roles = new ArrayList<>(guild.getRoles()).stream().filter(x -> x.getPosition() < botPosition).collect(Collectors.toList());
		return roles;
	}

	// Create a role with name `name`, and if successful, set mentionable
	// Fail if another role with same name exists
	private void createMentionableRole(String name)
	{
		if (!guild.getRolesByName(name, false).isEmpty())
		{
			send("Role " + name + " already exists");
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

	private <E> String listWithoutBrackets(List<E> items)
	{
		String s = items.toString();
		return s.substring(1, s.length() - 1);
	}

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
		send(out);
	}

	// Call this on modify commands
	private void updateRecent(String recentCommand, Member recentMember, List<Role> recentRoles)
	{
		this.recentCommand = recentCommand;
		this.recentMember = recentMember;
		this.recentRoles = recentRoles;
		System.out.println("Update recent: " + recentCommand + ", " + recentMember.getEffectiveName() + ", " + recentRoles);
	}

	// Functions

	public void execute()
	{
		// Bot wasn't invoked
		if (!(st.hasMoreTokens() && st.nextToken().equals("@" + getBotName())))
		{
			return;
		}

		System.out.println(member.getEffectiveName() + " : " + message.getContentDisplay());

		if (!st.hasMoreTokens())
		{
			send("invalid command invoked");
			help();
			return;
		}
		String command = st.nextToken().toLowerCase();
		switch (command)
		{
		case "help":
		{
			send("`help` command invoked");
			help();
			break;
		}

		case "list":
		{
			send("`list` command invoked");

			try
			{
				List<Role> memberRoles = new ArrayList<>(member.getRoles());
				memberRoles.remove(guild.getPublicRole());

				if (memberRoles.isEmpty())
				{
					send(member.getEffectiveName() + " has no roles");
					return;
				}

				String listMessage = "List of current roles for " + member.getEffectiveName() + "\n";
				outputListOfRoles(memberRoles, listMessage);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
			break;
		}
		case "listall":
		{
			send("`listAll` command invoked");

			List<Role> allRoles = new ArrayList<>(guild.getRoles());
			allRoles.remove(guild.getPublicRole());

			String listAllMessage = "List of all available roles\n";
			outputListOfRoles(allRoles, listAllMessage);
			break;
		}

		case "addrole":
		{
			send("`addRole` command invoked");

			if (!st.hasMoreTokens())
			{
				send("Usage: `addRole role`");
				return;
			}
			String argument = st.nextToken();

			try
			{
				List<Role> roles = guild.getRolesByName(argument, true);
				if (roles.isEmpty())
				{
					send("Role " + argument + " does not exist. Maybe try creating it first");
					return;
				}
				Role r = roles.get(0);
				if (member.getRoles().contains(r))
				{
					send(member.getEffectiveName() + " already has role " + r.getName());
				}
				else
				{
					send("Adding " + r.getName());
					guildController.addSingleRoleToMember(member, r).queue();
					updateRecent("add", member, new ArrayList<Role>(Arrays.asList(r)));
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
			break;
		}
		case "addroles":
		{
			send("`addRoles` command invoked");

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
				send("No roles to add");
				return;
			}

			send("Adding " + rolesToAdd);
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
			send("`removeRole` command invoked");

			if (!st.hasMoreTokens())
			{
				send("Usage: `removeRole role`");
				return;
			}
			String argument = st.nextToken();

			try
			{
				List<Role> roles = guild.getRolesByName(argument, true);
				if (roles.isEmpty())
				{
					send(argument + " is not assigned to " + member.getEffectiveName());
					return;
				}
				Role roleToRemove = roles.get(0);
				send("Removing " + roleToRemove.getName());
				guildController.removeSingleRoleFromMember(member, roleToRemove)
						.queue((r) -> updateRecent("remove", member,
								new ArrayList<Role>(Arrays.asList())));
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
			break;
		}
		case "removeroles":
		{
			send("`removeRoles` command invoked");

			// Collect all the aforementioned roles
			String argument;
			List<Role> rolesToRemove = new ArrayList<>();
			final List<Role> potentialRoles = member.getRoles();
			if (potentialRoles.isEmpty())
			{
				send("No roles to remove");
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
				send("No roles to remove");
				return;
			}

			send("Removing " + rolesToRemove);
			guildController.removeRolesFromMember(member, rolesToRemove).queue();
			updateRecent("remove", member, new ArrayList<Role>(rolesToRemove));
			break;
		}
		case "removeallroles":
		{
			send("`removeAllRoles` command invoked");

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
			updateRecent("remove", member, new ArrayList<Role>(removed));
			String out = "Roles removed from " + member.getEffectiveName() + ": ";
			out += listWithoutBrackets(removed);
			send(out);
			break;
		}

		case "createrole":
		{
			send("`createRole` command invoked");

			if (!st.hasMoreTokens())
			{
				send("Usage: `createRole role`");
				return;
			}
			String argument = st.nextToken();

			createMentionableRole(argument);
			break;
		}
		case "createroles":
		{
			send("`createRoles` command invoked");

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
			send("`membersWith` command invoked");

			if (!st.hasMoreTokens())
			{
				send("Usage: `membersWith role`");
				return;
			}
			String argument = st.nextToken();

			List<Member> members = guild
					.getMembersWithRoles(guild.getRolesByName(argument, true));
			if (members.isEmpty())
			{
				send("No members with role " + argument);
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
			send(out);
			break;
		}
		
		case "undo":
		{
			// TODO : Fill in and fix a lot
			// TODO : "mark" other functions that modify something
			send("`undo` command invoked (work in progress)");

			if (!member.equals(recentMember))
			{
				send("You may only undo your own actions");
			}

			/*
			 * For each command that modifies something (add/remove/create)
			 * when the command is run, store the changes and the invoking member
			 * When using undo, perform the opposite of the changes IFF
			 * the invoking member is the same as the previous invoking member
			 * For example, UserA adds a role R to themself. Store the following:
			 * - user: UserA, action: add, roles: [R]
			 * If undo is called _before_ the next modifying command,
			 * (i.e allow an intervening `list` or `membersWith`, etc.)
			 * perform the following:
			 * removeRoles(UserA, roles)
			 * undo is the only way the bot can delete roles from a server
			 * 
			 * Explicitly, the function of undo based on previous actions are:
			 * Previous | Undo
			 * ---------+-------
			 * Add      | Remove
			 * Remove   | Add
			 * Create   | Delete
			 * Only apply to roles that were actually modified in the previous operation
			 * For example, if adding roles a, b, and c, but only b and c were actually applied,
			 * undo will only affect b and c
			 */
			
			if (recentCommand == null || recentMember == null || recentRoles == null)
			{
				send("No actions to undo");
				return;
			}
			
			switch (recentCommand)
			{
			case "add":
			{
				send("Want to remove " + recentRoles + " from " + recentMember.getEffectiveName());
				break;
			}
			case "remove":
			{
				send("Want to add " + recentRoles + " to " + recentMember.getEffectiveName());
				break;
			}
			case "create":
			{
				send("Want to delete " + recentRoles + " from server");
				break;
			}
			default:
			{
				send("This should never happen");
			}
			}
			break;
		}

		default:
		{
			send("invalid command invoked");
			help();
		}
		}
	}
}
