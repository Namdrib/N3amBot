import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.Role;
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

	private String tagMember()
	{
		return member.getAsMention();
	}

	private List<Role> getUsableRoles(Guild g)
	{
		List<Role> roles = g.getRoles();
		roles.remove(g.getPublicRole());
		roles.removeAll(g.getRolesByName(Global.botName, false));
		roles.removeAll(guild.getRolesByName("Adminh", true));
		roles.removeAll(guild.getRolesByName("Mod", true));
		roles.removeAll(guild.getRolesByName("Tutor", true));
		roles.removeAll(guild.getRolesByName("Server Overlords", true));

		return roles;
	}

	private void createMentionableRole(String name)
	{
		if (!guild.getRolesByName(name, false).isEmpty())
		{
			send("Role " + name + " already exists");
			return;
		}

		System.out.println("Creating role with name " + name);
		try
		{
			guildController.createRole().setName(name).queue();
			guild.getRolesByName(name, true).get(0).getManager().setMentionable(true).queueAfter(500, TimeUnit.MILLISECONDS);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		System.out.println("Done");
	}

	private void outputListOfRoles(List<Role> roles, String prefix)
	{
		String out = prefix;
		List<String> roleNames = new ArrayList<>();
		for (Role r : roles)
		{
			roleNames.add(r.getName());
		}
		Collections.sort(roleNames);
		String s = roleNames.toString();
		out += s.substring(1, s.length() - 1);
		send(out);
	}

	private void help()
	{
		String helpMessage
				= " ----- " + Global.botName + " help -----\n"
				+ "Invoke the bot using `" + Global.prefix + "` followed by one of the following commands:\n"
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
				;

		send(helpMessage);
	}

	// Functions

	public boolean validate()
	{
		return st.nextToken().equals(Global.prefix);
	}

	public void execute()
	{
		System.out.println(member.getEffectiveName() + " : " + message.getContentDisplay());

		if (!st.hasMoreTokens())
		{
			send("invalid command invoked");
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
						send(tagMember() + " has no roles");
						return;
					}

					String listMessage = "List of current roles for " + tagMember() + "\n";
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

				String argument = st.nextToken();
				if (argument == null)
				{
					send("Usage: `addRole role`");
					return;
				}
				
				try
				{
					Role r = guild.getRolesByName(argument, true).get(0);
					send("Adding " + r.getName());
					guildController.addSingleRoleToMember(member, r).queue();
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

				String argument = st.nextToken();
				if (argument == null)
				{
					send("Usage: `removeRole role`");
					return;
				}

				try
				{
					Role roleToRemove = guild.getRolesByName(argument, true).get(0);
					send("Removing " + roleToRemove.getName());
					guildController.removeSingleRoleFromMember(member, roleToRemove).queue();
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
				break;
			}
			case "removeallroles":
			{
				send("`removeAllRoles` command invoked");

				List<Role> roles = new ArrayList<>(member.getRoles());
				for (Role r : roles)
				{
					try
					{
						guildController.removeSingleRoleFromMember(member, r).queue();
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
				break;
			}

			case "createrole":
			{
				send("`createRole` command invoked");

				String argument = st.nextToken();
				if (argument == null)
				{
					send("Usage: `createRole role`");
					return;
				}
				
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
			
			default:
			{
				send("invalid command invoked");
				help();
			}
		}
	}
}
