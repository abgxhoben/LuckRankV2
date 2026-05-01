package de.pcblc.spigot.command;

import de.pcblc.spigot.service.NotificationService;
import de.pcblc.spigot.service.RankService;
import de.pcblc.spigot.service.UpdateCheckService;
import de.pcblc.spigot.service.WebhookService;
import de.pcblc.spigot.util.MessageService;
import de.pcblc.common.rank.RankActionResult;
import de.pcblc.common.rank.RankPermissionResult;
import de.pcblc.common.util.TimeParser;
import net.luckperms.api.model.group.Group;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class RankCommand implements CommandExecutor, TabCompleter {

    private final Plugin plugin;
    private final RankService rankService;
    private final NotificationService notificationService;
    private final UpdateCheckService updateCheckService;
    private final WebhookService webhookService;
    private final MessageService messageService;

    public RankCommand(
            Plugin plugin,
            RankService rankService,
            NotificationService notificationService,
            UpdateCheckService updateCheckService,
            WebhookService webhookService,
            MessageService messageService
    ) {
        this.plugin = plugin;
        this.rankService = rankService;
        this.notificationService = notificationService;
        this.updateCheckService = updateCheckService;
        this.webhookService = webhookService;
        this.messageService = messageService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messageService.prefixed("messages.playersOnly"));
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "set":
                handleSet(player, args);
                return true;
            case "remove":
                handleRemove(player, args);
                return true;
            case "setperms":
                handleSetPerms(player, args);
                return true;
            case "creategroup":
                handleCreateGroup(player, args);
                return true;
            case "debug":
                handleDebug(player, args);
                return true;
            case "notify":
                handleNotify(player, args);
                return true;
            case "help":
            default:
                sendHelp(player);
                return true;
        }
    }

    private void handleSet(Player actor, String[] args) {
        if (args.length != 4) {
            sendHelp(actor);
            return;
        }

        TimeParser.Result duration = TimeParser.parse(args[3]);
        if (!duration.isValid()) {
            actor.sendMessage(messageService.prefixed("messages.invalidDurationFormat"));
            return;
        }

        RankActionResult result = rankService.assignRank(actor, args[1], args[2], duration);
        if (!result.isSuccess()) {
            actor.sendMessage(messageService.prefixed(result.getMessageKey()));
            return;
        }

        actor.sendMessage(messageService.prefixed(
                "messages.rankSetSuccessfully",
                "targetPlayer", args[1],
                "rank", args[2],
                "duration", duration.toDisplay()
        ));

        notificationService.broadcastToWatchers(messageService.prefixed(
                "messages.globalmessage",
                "player", actor.getName(),
                "targetPlayer", args[1],
                "rank", args[2],
                "server", result.getTargetServer(),
                "duration", duration.toDisplay()
        ));

        webhookService.sendRankSet(actor.getName(), args[1], args[2], duration.toDisplay());
    }

    private void handleRemove(Player actor, String[] args) {
        if (args.length != 3) {
            sendHelp(actor);
            return;
        }

        RankActionResult result = rankService.removeRank(actor, args[1], args[2]);
        if (result.requiresConfirmation()) {
            actor.sendMessage(messageService.prefixed("messages.confirmationWarning"));
            return;
        }
        if (!result.isSuccess()) {
            actor.sendMessage(messageService.prefixed(result.getMessageKey()));
            return;
        }

        actor.sendMessage(messageService.prefixed(
                "messages.rankRemoved",
                "player", args[1],
                "rank", args[2]
        ));

        notificationService.broadcastToWatchers(messageService.prefixed(
                "messages.globalmessageremove",
                "player", actor.getName(),
                "targetPlayer", args[1],
                "rank", args[2],
                "server", result.getTargetServer()
        ));

        webhookService.sendRankRemove(actor.getName(), args[1], args[2]);
    }

    private void handleSetPerms(Player actor, String[] args) {
        if (args.length != 4) {
            sendHelp(actor);
            return;
        }

        Boolean value = parseBoolean(args[3]);
        if (value == null) {
            actor.sendMessage(messageService.prefixed("messages.invalidPermissionValue"));
            return;
        }

        RankPermissionResult result = rankService.setPermission(actor, args[1], args[2], value.booleanValue());
        if (!result.isSuccess()) {
            actor.sendMessage(messageService.prefixed(result.getMessageKey()));
            return;
        }

        String valueText = Boolean.toString(value.booleanValue());
        if (result.isGroupTarget()) {
            actor.sendMessage(messageService.prefixed(
                    "messages.permissionSetgroup",
                    "group", args[1],
                    "permission", args[2],
                    "value", valueText
            ));
            notificationService.broadcastToWatchers(messageService.prefixed(
                    "messages.globalmessagepermissiongroup",
                    "player", actor.getName(),
                    "group", args[1],
                    "permission", args[2],
                    "value", valueText
            ));
        } else {
            actor.sendMessage(messageService.prefixed(
                    "messages.permissionSet",
                    "player", args[1],
                    "permission", args[2],
                    "value", valueText
            ));
            notificationService.broadcastToWatchers(messageService.prefixed(
                    "messages.globalmessagepermission",
                    "player", actor.getName(),
                    "targetPlayer", args[1],
                    "permission", args[2],
                    "server", result.getTargetServer(),
                    "value", valueText
            ));
        }

        webhookService.sendPermissionSet(actor.getName(), args[1], args[2], valueText);
    }

    private void handleCreateGroup(Player actor, String[] args) {
        if (args.length != 4) {
            sendHelp(actor);
            return;
        }

        int weight;
        try {
            weight = Integer.parseInt(args[2]);
        } catch (NumberFormatException exception) {
            actor.sendMessage(messageService.prefixed("messages.invalidWeight"));
            return;
        }

        RankActionResult result = rankService.createGroup(actor, args[1], weight, args[3]);
        if (!result.isSuccess()) {
            actor.sendMessage(messageService.prefixed(result.getMessageKey()));
            return;
        }

        actor.sendMessage(messageService.prefixed(
                "messages.groupCreated",
                "group", args[1],
                "weight", String.valueOf(weight),
                "displayname", args[3]
        ));

        notificationService.broadcastToWatchers(messageService.prefixed(
                "messages.globalMessageCreateGroup",
                "player", actor.getName(),
                "group", args[1],
                "weight", String.valueOf(weight),
                "displayname", args[3]
        ));

        webhookService.sendCreateGroup(actor.getName(), args[1], weight, args[3]);
    }

    private void handleDebug(Player actor, String[] args) {
        if (args.length != 1) {
            sendHelp(actor);
            return;
        }

        actor.sendMessage(messageService.raw(
                "messages.debug",
                "version", plugin.getDescription().getVersion(),
                "developer", String.join(", ", plugin.getDescription().getAuthors()),
                "versionstatus", updateCheckService.isOutdated() ? "&cOUTDATED" : "&aLATEST",
                "status", webhookService.isEnabled() ? "true" : "false"
        ));
    }

    private void handleNotify(Player actor, String[] args) {
        if (args.length != 1) {
            sendHelp(actor);
            return;
        }

        boolean enabled = notificationService.toggle(actor.getUniqueId());
        actor.sendMessage(messageService.prefixed(enabled ? "messages.notifyEnabled" : "messages.notifyDisabled"));
    }

    private void sendHelp(Player player) {
        String groups = rankService.getLoadedGroups().stream()
                .map(Group::getName)
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.joining(", "));

        player.sendMessage(messageService.prefixed(
                "messages.availableRanks",
                "ranks", groups.isEmpty() ? "-" : groups
        ));

        for (String line : messageService.getLines("messages.commandUsage")) {
            player.sendMessage(messageService.prefixedRaw(line));
        }
    }

    private Boolean parseBoolean(String input) {
        if ("true".equalsIgnoreCase(input)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(input)) {
            return Boolean.FALSE;
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterByPrefix(Arrays.asList("set", "remove", "setperms", "creategroup", "debug", "notify", "help"), args[0]);
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && ("set".equals(subCommand) || "remove".equals(subCommand) || "setperms".equals(subCommand))) {
            List<String> values = new ArrayList<String>();
            plugin.getServer().getOnlinePlayers().forEach(player -> values.add(player.getName()));
            if ("setperms".equals(subCommand)) {
                rankService.getLoadedGroups().forEach(group -> values.add(group.getName()));
            }
            return filterByPrefix(values, args[1]);
        }

        if (args.length == 3 && ("set".equals(subCommand) || "remove".equals(subCommand))) {
            return filterByPrefix(groupNames(), args[2]);
        }

        if (args.length == 3 && "setperms".equals(subCommand)) {
            return Collections.singletonList("permission.node");
        }

        if (args.length == 4 && "set".equals(subCommand)) {
            return filterByPrefix(Arrays.asList("30d", "12h", "15min", "-1"), args[3]);
        }

        if (args.length == 4 && "setperms".equals(subCommand)) {
            return filterByPrefix(Arrays.asList("true", "false"), args[3]);
        }

        if (args.length == 2 && "creategroup".equals(subCommand)) {
            return Collections.singletonList("groupname");
        }

        if (args.length == 3 && "creategroup".equals(subCommand)) {
            return Collections.singletonList("100");
        }

        if (args.length == 4 && "creategroup".equals(subCommand)) {
            return Collections.singletonList("DisplayName");
        }

        return Collections.emptyList();
    }

    private List<String> groupNames() {
        return rankService.getLoadedGroups().stream()
                .map(Group::getName)
                .collect(Collectors.toList());
    }

    private List<String> filterByPrefix(List<String> values, String input) {
        String lowerInput = input.toLowerCase(Locale.ROOT);
        return values.stream()
                .distinct()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lowerInput))
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());
    }
}
