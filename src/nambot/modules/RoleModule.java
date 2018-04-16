package nambot.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import nambot.Module;
import nambot.NamBot;
import nambot.util.*;

/**
 * Module responsible for handling roles Includes listing, adding/removing from
 * a user, and creating
 * 
 * @author Namdrib
 *
 */
public class RoleModule extends Module
{
	/**
	 * Register this module with NamBot
	 * 
	 * @param nambot
	 *            the NamBot object to which this registers
	 */
	public RoleModule(NamBot nambot)
	{
		super(nambot, "role");
	}

	public RoleModule(NamBot nambot, String identifier)
	{
		super(nambot, identifier);
	}

	@Override
	protected void setCommandList()
	{
		commandList = new ArrayList<>(Arrays.asList("help", "list", "listall",
				"add", "addn", "remove", "removen", "removeall", "create",
				"createn", "memberswith"));
	}

	// Helper functions

	@Override
	protected void help()
	{
		String helpMessage = " ----- Help message for "
				+ getClass().getSimpleName() + " -----\n"
				+ "  `help`: display this help message\n"
				+ "  `list`: list your own roles\n"
				+ "  `listAll`: list all available roles you can add to yourself\n"
				+ "  `add ROLE`: add `ROLE` to yourself (where `ROLE` is in `listAll`)\n"
				+ "  `addN ROLES...`: add `ROLES...` to yourself (where `ROLES...` are in `listAll`)\n"
				+ "  `remove ROLE`: remove `ROLE` from yourself (where `ROLE` is in `list`)\n"
				+ "  `removeN ROLES...`: remove `ROLES...` from yourself (where `ROLES...` are in `listAll`)\n"
				+ "  `removeAll`: remove all roles from yourself\n"
				+ "  `create ROLE`: create a role with name `ROLE`\n"
				+ "  `createN ROLES...`: create multiple roles with names `ROLES...`\n"
				+ "  `membersWith ROLE`: list all members to whom ROLE is assigned\n";

		Helpers.send(channel, helpMessage);
	}

	/**
	 * Bot access determined by role hierarchy - bot cannot access roles with
	 * higher position. Place the bot higher in the hierarchy to allow more
	 * access, lower to restrict access. Calls getUsableRoles(List<Role>) with
	 * all available guild roles
	 * 
	 * @return list of roles the bot can access excluding public role
	 */
	private List<Role> getUsableRoles()
	{
		return getUsableRoles(guild.getRoles());
	}

	/**
	 * Bot access determined by role hierarchy - bot cannot access roles with
	 * higher position. Place the bot higher in the hierarchy to allow more
	 * access, lower to restrict access
	 * 
	 * @param input
	 *            input roles from which to filter
	 * @return list of roles the bot can access excluding public role
	 */
	private List<Role> getUsableRoles(List<Role> input)
	{
		// The highest position of all the roles the boy has
		int botPosition = Collections.max(guild.getSelfMember().getRoles()
				.stream().map(Role::getPosition).collect(Collectors.toList()));

		List<Role> roles = new ArrayList<>(input).stream()
				.filter(x -> x.getPosition() < botPosition)
				.collect(Collectors.toList());
		roles.remove(guild.getPublicRole());
		return roles;
	}

	/**
	 * 
	 * Create a role with name `name` If successful, set mentionable. Fail if
	 * another role with same name exists
	 * 
	 * @param name
	 *            name of the role to create
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
					.queue(x -> x.getManager().setMentionable(true).queue(a -> {
						if (guild.getRolesByName(name, false).get(0)
								.getName() == name)
						{
							Helpers.send(channel,
									"Created mentionable role " + name);
						}
					}));
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	// Commands for modifying role presence
	/**
	 * 
	 * Assigns a role to a Member
	 * 
	 * @param argument
	 *            the name of the role to add
	 * @param member
	 *            the member to modify
	 * @return the successfully-added role, null if no role was added
	 */
	private void addRole(String argument, Member member)
	{
		try
		{
			List<Role> roles = guild.getRolesByName(argument, false);
			if (roles == null || roles.isEmpty())
			{
				Helpers.send(channel, "Role " + argument
						+ " does not exist. Maybe try creating it first");
				return;
			}
			Role r = roles.get(0);
			if (member.getRoles().contains(r))
			{
				Helpers.send(channel, member.getEffectiveName()
						+ " already has role " + r.getName());
				return;
			}

			guildController.addSingleRoleToMember(member, r).queue(a -> {
				Helpers.send(channel, "Added role " + r.getName() + " to "
						+ member.getEffectiveName());
			}, b -> {
				Helpers.send(channel, "Failed to add role " + r.getName()
						+ " to " + member.getEffectiveName());
			});
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	/**
	 * 
	 * Assigns multiple roles to a Member
	 * 
	 * @param arguments
	 *            the names of the roles to add
	 * @param member
	 *            the member to modify
	 * @return the successfully-added roles, null if no roles were added
	 */
	private void addRoles(List<String> arguments, Member member)
	{
		if (arguments.isEmpty())
		{
			Helpers.send(channel, "No roles to add");
			return;
		}
		if (member == null)
		{
			Helpers.send(channel, "No target member");
			return;
		}

		List<Role> rolesToAdd = new ArrayList<>();
		for (String s : arguments)
		{
			rolesToAdd.add(guild.getRolesByName(s, false).get(0));
		}

		try
		{
			// TODO : Check old roles a bit closer to adding.
			// Sometimes says did not add any roles when it did something
			List<Role> oldRoles = member.getRoles();
			guildController.addRolesToMember(member, rolesToAdd).queue(a -> {
				List<Role> newRoles = new ArrayList<>(member.getRoles());
				newRoles.removeAll(oldRoles);

				String msg = new String();
				if (newRoles == null || newRoles.isEmpty())
				{
					msg = "Did not add any roles";
				}
				else
				{
					msg = "Successfully added roles to "
							+ member.getEffectiveName() + ":\n";
					msg += Helpers.listWithoutBrackets(
							Helpers.getNamesFrom(newRoles));
				}
				Helpers.send(channel, msg);
			});
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	/**
	 * Removes a role from a member
	 * 
	 * @param argument
	 *            the name of the role to remove
	 * @param member
	 *            the member to modify
	 */
	private void removeRole(String argument, Member member)
	{
		try
		{
			List<Role> guildRoles = guild.getRolesByName(argument, false);
			if (guildRoles.isEmpty())
			{
				Helpers.send(channel, "Role " + argument
						+ " does not exist. Maybe try creating it first");
				return;
			}
			Role r = guildRoles.get(0);

			List<Role> oldRoles = member.getRoles();
			if (!oldRoles.contains(r))
			{
				Helpers.send(channel, argument + " is not assigned to "
						+ member.getEffectiveName());
				return;
			}

			guildController.removeSingleRoleFromMember(member, r).queue(a -> {
				Helpers.send(channel, "Removed role " + r.getName() + " from "
						+ member.getEffectiveName());
			}, b -> {
				Helpers.send(channel, "Failed to remove role " + r.getName()
						+ " from " + member.getEffectiveName());
			});
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	/**
	 * 
	 * Remove multiple roles from a Member After removal, announce which roles
	 * were removed. If no roles were removed, sends "Did not remove any roles"
	 * 
	 * @param arguments
	 *            the names of the roles to remove
	 * @param member
	 *            the member to modify
	 */
	private void removeRoles(List<String> arguments, Member member)
	{
		if (arguments.isEmpty())
		{
			Helpers.send(channel, "No roles to remove");
			return;
		}
		if (member == null)
		{
			Helpers.send(channel, "No target member");
			return;
		}

		List<Role> rolesToRemove = new ArrayList<>();
		for (String s : arguments)
		{
			rolesToRemove.add(guild.getRolesByName(s, false).get(0));
		}

		try
		{
			List<Role> oldRoles = new ArrayList<>(member.getRoles());
			guildController.removeRolesFromMember(member, rolesToRemove)
					.queue(a -> {
						List<Role> newRoles = member.getRoles();
						oldRoles.removeAll(newRoles);

						String msg = new String();
						if (oldRoles == null || oldRoles.isEmpty())
						{
							msg = "Did not remove any roles";
						}
						else
						{
							msg = "Successfully removed roles from "
									+ member.getEffectiveName() + ":\n";
							msg += Helpers.listWithoutBrackets(
									Helpers.getNamesFrom(oldRoles));
						}
						Helpers.send(channel, msg);
					});
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	/**
	 * 
	 * Create a mentionable role in a guild. Do not allow multiple roles of the
	 * same name to exist
	 * 
	 * @param arguments
	 *            the name of the mentionable role to create
	 * @param guild
	 *            the guild in which to create the role
	 * @return the successfully-created role, null if no roles were created
	 */
	private void createRole(String argument, Guild guild)
	{
		List<Role> existingRoles = guild.getRolesByName(argument, false);
		if (!existingRoles.isEmpty())
		{
			Helpers.send(channel, "Role " + argument + " already exists.");
		}
		else
		{
			createMentionableRole(argument);
		}
	}

	/**
	 * 
	 * Create multiple mentionable roles in a guild
	 * 
	 * @param arguments
	 *            the names of the mentionable roles to create
	 * @param guild
	 *            the guild in which to create the roles
	 * @return the successfully-created roles, null if no roles were created
	 */
	private void createRoles(List<String> arguments, Guild guild)
	{
		List<String> noAdd = new ArrayList<>();
		for (String s : arguments)
		{
			if (!guild.getRolesByName(s, false).isEmpty())
			{
				noAdd.add(s);
			}
			else
			{
				createMentionableRole(s);
			}
		}

		arguments.removeAll(noAdd);
		String msg;
		if (arguments.isEmpty())
		{
			msg = "Did not create any roles";
		}
		else
		{
			msg = "Successfully created roles:\n";
			msg += Helpers.listWithoutBrackets(arguments);
		}
		Helpers.send(channel, msg);
	}

	// Functions
	/**
	 * Parse and carry out specified the user's commands
	 * TODO : move majority to handle()
	 */
	public void execute(String command)
	{
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
						Helpers.send(channel,
								member.getEffectiveName() + " has no roles");
						return;
					}

					String listMessage = "List of current roles for "
							+ member.getEffectiveName() + "\n";
					String roleOutputs = Helpers.listWithoutBrackets(
							Helpers.getNamesFrom(memberRoles));
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
				String roleOutputs = Helpers
						.listWithoutBrackets(Helpers.getNamesFrom(allRoles));
				Helpers.send(channel, listAllMessage + roleOutputs);
				break;
			}

			case "add":
			{
				Helpers.send(channel, "`add` command invoked");

				if (!st.hasMoreTokens())
				{
					Helpers.send(channel, "Usage: `addN role`");
					return;
				}
				String argument = st.nextToken();

				addRole(argument, member);
				break;
			}
			case "addn":
			{
				Helpers.send(channel, "`addN` command invoked");

				// Collect all the aforementioned roles
				String argument;
				List<String> rolesToAdd = new ArrayList<>();
				final List<Role> allRoles = guild.getRoles();
				while (st.hasMoreTokens())
				{
					argument = st.nextToken();
					for (Role r : allRoles)
					{
						if (argument.equals(r.getName()))
						{
							rolesToAdd.add(r.getName());
							break;
						}
					}
				}

				addRoles(rolesToAdd, member);
				break;
			}

			case "remove":
			{
				Helpers.send(channel, "`remove` command invoked");

				if (!st.hasMoreTokens())
				{
					Helpers.send(channel, "Usage: `remove role`");
					return;
				}
				String argument = st.nextToken();

				removeRole(argument, member);
				break;
			}
			case "removen":
			{
				Helpers.send(channel, "`removeN` command invoked");

				// Collect all the aforementioned roles
				String argument;
				List<String> rolesToRemove = new ArrayList<>();
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
							rolesToRemove.add(r.getName());
							break;
						}
					}
				}

				if (rolesToRemove.isEmpty())
				{
					Helpers.send(channel, "No roles to remove");
					return;
				}

				removeRoles(rolesToRemove, member);
				break;
			}
			case "removeall":
			{
				Helpers.send(channel, "`removeAll` command invoked");

				List<Role> roles = new ArrayList<>(member.getRoles());
				List<Role> removed = new ArrayList<>();
				for (Role r : roles)
				{
					try
					{
						guildController.removeSingleRoleFromMember(member, r)
								.queue();
						removed.add(r);
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
				String out = "Roles removed from " + member.getEffectiveName()
						+ ": ";
				out += Helpers.listWithoutBrackets(removed);
				Helpers.send(channel, out);
				break;
			}

			case "create":
			{
				Helpers.send(channel, "`create` command invoked");

				if (!st.hasMoreTokens())
				{
					Helpers.send(channel, "Usage: `create role`");
					return;
				}
				String argument = st.nextToken();

				createRole(argument, guild);
				break;
			}
			case "createn":
			{
				Helpers.send(channel, "`createN` command invoked");

				List<String> rolesToCreate = new ArrayList<>();
				String argument;
				while (st.hasMoreTokens())
				{
					argument = st.nextToken();
					rolesToCreate.add(argument);
				}

				createRoles(rolesToCreate, guild);
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

				List<Member> members = guild.getMembersWithRoles(
						guild.getRolesByName(argument, false));
				if (members.isEmpty())
				{
					Helpers.send(channel, "No members with role " + argument);
					return;
				}

				String out = members.size() + " members with role " + argument
						+ ":\n";
				List<String> namesList = Helpers.getNamesFrom(members);
				out += Helpers.listWithoutBrackets(namesList);
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
