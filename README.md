# RoleBot
Simple role management Discord bot

Feel free to make pull requests or issues if there's anything that you think is incorrect or could be improved on

## Installation
- Requires Java 8, Maven, a [Discord bot token](https://github.com/reactiflux/discord-irc/wiki/Creating-a-discord-bot-&-getting-a-token)
- Copy `config.properties.dummy` into `config.properties` and replace `YOUR_BOT_TOKEN_HERE` with your bot token (no quotes necessary)
	- Alternatively, create an environment variable on your machine with the key `DISCORD_ROLEBOT_TOKEN` and set its value to be your token. This approach can be used for external hosting (e.g. deploying as a Heroku app)
- Compile using an IDE (such as Eclipse) or using the following:
	- `mvn clean install`
	- `java -jar target/discord-role-bot-jar-with-dependencies.jar`

## Usage
**IMPORTANT**: RoleBot must have the permission to manage roles in the server for the commands to work.

The prefix used to activate RoleBot is `@RoleBot` (can be changed in `Global.java`)

List of accepted (case-insenstive) commands and their function:
- `help`: display this help message
- `list`: list your own roles
- `listAll`: list all available roles you can add to yourself
- `addRole ROLE`: add `ROLE` to yourself (where `ROLE` is in `listAll`)
- `addRoles ROLES...`: add `ROLES...` to yourself (where `ROLES...` are in `listAll`)
- `removeRole ROLE`: remove `ROLE` from yourself (where `ROLE` is in `list`)
- `removeRoles ROLES...`: remove `ROLES...` from yourself (where `ROLES...` are in `listAll`)
- `removeAllRoles`: remove all roles from yourself
- `createRole ROLE`: create a role with name `ROLE`
- `createRoles ROLES...`: create multiple roles with names `ROLES...`
- `membersWith ROLE`: list all members to whom ROLE is assigned

All of these commands only apply to the person invoking them. i.e. they cannot be used to modify roles of other members.

The addition/removal of roles will not work if the role of interest is "higher or equal highest role than [the invoker]". This order is based on the the role order in the server (In server settings -> Roles, with the "first" role being the highest and "last" role being the lowest). For example, if the order is ["a", "the\_bots\_role", "b", "c"], then the bot may only manage roles b and c. You can use this to "protect" some roles (such as as admin/mod/elevated roles). 
