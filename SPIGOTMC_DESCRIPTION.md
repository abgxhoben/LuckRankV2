[CENTER][SIZE=6][B]LuckRank[/B][/SIZE]
[SIZE=4]A clean rank management plugin for LuckPerms with timed ranks, permission tools, staff notifications and Discord webhook support[/SIZE][/CENTER]

[HR][/HR]

[SIZE=5][B]What is LuckRank?[/B][/SIZE]

LuckRank is a lightweight and configurable rank management plugin for Spigot/Paper and Bungee-based environments.
It was built for servers that want fast staff workflows without unnecessary complexity.

[B]Core focus:[/B]
[LIST]
[*]Rank management directly in-game
[*]Temporary and permanent ranks
[*]Permission management for players and groups
[*]Group creation through commands
[*]Staff notifications
[*]Discord webhook logging
[*]Update notifications
[/LIST]

[HR][/HR]

[SIZE=5][B]Features[/B][/SIZE]

[LIST]
[*]Set ranks for online players
[*]Remove ranks with confirmation protection
[*]Support timed and permanent ranks
[*]Edit permissions for players
[*]Edit permissions for groups
[*]Create new LuckPerms groups in-game
[*]Set group weight and display name
[*]Toggle staff notifications
[*]Send action logs to Discord webhooks
[*]Show update notifications for new releases
[/LIST]

[HR][/HR]

[SIZE=5][B]Commands[/B][/SIZE]

[CODE]
/rank set <player> <rank> <duration>
/rank remove <player> <rank>
/rank setperms <player|group> <permission> <true|false>
/rank creategroup <name> <weight> <displayname>
/rank notify
/rank debug
[/CODE]

[HR][/HR]

[SIZE=5][B]Permissions[/B][/SIZE]

[LIST]
[*][B]luckrank.use[/B] - Access to the main rank command
[*][B]luckrank.see[/B] - Receive staff notifications and update messages
[*][B]luckrank.creategroup[/B] - Create new LuckPerms groups
[*][B]luckrank.set.<group>[/B] - Set a specific rank
[*][B]luckrank.remove.<group>[/B] - Remove a specific rank
[*][B]luckrank.setperms.<permission>[/B] - Edit a specific permission node
[/LIST]

[HR][/HR]

[SIZE=5][B]Configuration[/B][/SIZE]

LuckRank is designed to stay simple while still giving you control over the important parts.

[B]Configurable parts:[/B]
[LIST]
[*]Prefix
[*]Database type and credentials
[*]Discord webhook settings
[*]Update notification behavior
[*]Chat and system messages
[/LIST]

[B]Files:[/B]
[LIST]
[*][B]config.yml[/B] - General settings and webhook/database options
[*][B]messages.yml[/B] - All plugin messages and update texts
[/LIST]

[HR][/HR]

[SIZE=5][B]Why this plugin?[/B][/SIZE]

[LIST]
[*]Clean and practical command workflow
[*]No bloated menu system
[*]Easy to understand configuration
[*]Useful for staff teams and live administration
[*]Good base for server-specific rank systems
[*]Works well alongside LuckPerms setups
[/LIST]

[HR][/HR]

[SIZE=5][B]Perfect for[/B][/SIZE]

[LIST]
[*]Survival servers
[*]Citybuild servers
[*]PvP servers
[*]Minigame networks
[*]Staff teams working with LuckPerms
[/LIST]

[HR][/HR]

[SIZE=5][B]Requirements[/B][/SIZE]

[LIST]
[*]LuckPerms
[*]Java 8 or higher
[/LIST]

[HR][/HR]

[SIZE=5][B]Notes[/B][/SIZE]

LuckRank was built to be clean, maintainable and easy to extend.

If you want a focused rank tool for real moderation and administration workflows, this plugin is made for that.

[HR][/HR]

[SIZE=5][B]Feedback[/B][/SIZE]

If you like the plugin, feel free to leave feedback or suggestions.
