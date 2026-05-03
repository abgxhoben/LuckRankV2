package de.pcblc.bungee.command;

import de.pcblc.bungee.service.NotificationService;
import de.pcblc.bungee.service.RankService;
import de.pcblc.bungee.service.UpdateCheckService;
import de.pcblc.bungee.service.WebhookService;
import de.pcblc.bungee.util.MessageService;
import de.pcblc.common.rank.RankActionResult;
import de.pcblc.common.rank.RankPermissionResult;
import de.pcblc.common.util.TimeParser;
import net.luckperms.api.model.group.Group;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class RankCommand extends Command implements TabExecutor {

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
        super("rank", "luckrank.use", "r");
        this.plugin = plugin;
        this.rankService = rankService;
        this.notificationService = notificationService;
        this.updateCheckService = updateCheckService;
        this.webhookService = webhookService;
        this.messageService = messageService;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(messageService.toComponents(messageService.prefixed("messages.playersOnly")));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;
        if (args.length == 0) {
            sendHelp(player);
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "set":
                handleSet(player, args);
                return;
            case "remove":
                handleRemove(player, args);
                return;
            case "setperms":
                handleSetPerms(player, args);
                return;
            case "creategroup":
                handleCreateGroup(player, args);
                return;
            case "debug":
                handleDebug(player, args);
                return;
            case "notify":
                handleNotify(player, args);
                return;
            case "reload":
                handleReload(player, args);
                return;
            case "help":
            default:
                sendHelp(player);
        }
    }

    private void handleSet(ProxiedPlayer actor, String[] args) {
        if (args.length != 4) {
            sendMissingArgument(actor, args, "player", "rank", "duration");
            return;
        }

        TimeParser.Result duration = TimeParser.parse(args[3]);
        if (!duration.isValid()) {
            actor.sendMessage(messageService.toComponents(messageService.prefixed("messages.invalidDurationFormat")));
            return;
        }

        RankActionResult result = rankService.assignRank(actor, args[1], args[2], duration);
        if (!result.isSuccess()) {
            actor.sendMessage(messageService.toComponents(messageService.prefixed(result.getMessageKey())));
            return;
        }

        actor.sendMessage(messageService.toComponents(messageService.prefixed(
                "messages.rankSetSuccessfully",
                "targetPlayer", args[1],
                "rank", args[2],
                "duration", duration.toDisplay()
        )));

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

    private void handleRemove(ProxiedPlayer actor, String[] args) {
        if (args.length != 3) {
            sendMissingArgument(actor, args, "player", "rank");
            return;
        }

        RankActionResult result = rankService.removeRank(actor, args[1], args[2]);
        if (result.requiresConfirmation()) {
            actor.sendMessage(messageService.toComponents(messageService.prefixed("messages.confirmationWarning")));
            return;
        }
        if (!result.isSuccess()) {
            actor.sendMessage(messageService.toComponents(messageService.prefixed(result.getMessageKey())));
            return;
        }

        actor.sendMessage(messageService.toComponents(messageService.prefixed(
                "messages.rankRemoved",
                "player", args[1],
                "rank", args[2]
        )));

        notificationService.broadcastToWatchers(messageService.prefixed(
                "messages.globalmessageremove",
                "player", actor.getName(),
                "targetPlayer", args[1],
                "rank", args[2],
                "server", result.getTargetServer()
        ));

        webhookService.sendRankRemove(actor.getName(), args[1], args[2]);
    }

    private void handleSetPerms(ProxiedPlayer actor, String[] args) {
        if (args.length != 4) {
            sendMissingArgument(actor, args, "rank", "permission", "true/false");
            return;
        }

        Boolean value = parseBoolean(args[3]);
        if (value == null) {
            actor.sendMessage(messageService.toComponents(messageService.prefixed("messages.invalidPermissionValue")));
            return;
        }

        RankPermissionResult result = rankService.setPermission(actor, args[1], args[2], value.booleanValue());
        if (!result.isSuccess()) {
            actor.sendMessage(messageService.toComponents(messageService.prefixed(result.getMessageKey())));
            return;
        }

        String valueText = Boolean.toString(value.booleanValue());
        if (result.isGroupTarget()) {
            actor.sendMessage(messageService.toComponents(messageService.prefixed(
                    "messages.permissionSetgroup",
                    "group", args[1],
                    "permission", args[2],
                    "value", valueText
            )));
            notificationService.broadcastToWatchers(messageService.prefixed(
                    "messages.globalmessagepermissiongroup",
                    "player", actor.getName(),
                    "group", args[1],
                    "permission", args[2],
                    "value", valueText
            ));
        } else {
            actor.sendMessage(messageService.toComponents(messageService.prefixed(
                    "messages.permissionSet",
                    "player", args[1],
                    "permission", args[2],
                    "value", valueText
            )));
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

    private void handleCreateGroup(ProxiedPlayer actor, String[] args) {
        if (args.length != 4) {
            sendMissingArgument(actor, args, "name", "weight", "displayname");
            return;
        }

        int weight;
        try {
            weight = Integer.parseInt(args[2]);
        } catch (NumberFormatException exception) {
            actor.sendMessage(messageService.toComponents(messageService.prefixed("messages.invalidWeight")));
            return;
        }

        RankActionResult result = rankService.createGroup(actor, args[1], weight, args[3]);
        if (!result.isSuccess()) {
            actor.sendMessage(messageService.toComponents(messageService.prefixed(result.getMessageKey())));
            return;
        }

        actor.sendMessage(messageService.toComponents(messageService.prefixed(
                "messages.groupCreated",
                "group", args[1],
                "weight", String.valueOf(weight),
                "displayname", args[3]
        )));

        notificationService.broadcastToWatchers(messageService.prefixed(
                "messages.globalMessageCreateGroup",
                "player", actor.getName(),
                "group", args[1],
                "weight", String.valueOf(weight),
                "displayname", args[3]
        ));

        webhookService.sendCreateGroup(actor.getName(), args[1], weight, args[3]);
    }

    private void handleDebug(ProxiedPlayer actor, String[] args) {
        if (args.length != 1) {
            actor.sendMessage(messageService.toComponents(messageService.prefixed("messages.noArgumentsNeeded")));
            return;
        }

        actor.sendMessage(messageService.toComponents(messageService.raw(
                "messages.debug",
                "version", plugin.getDescription().getVersion(),
                "developer", plugin.getDescription().getAuthor(),
                "versionstatus", updateCheckService.isOutdated() ? "&cOUTDATED" : "&aLATEST",
                "status", webhookService.isEnabled() ? "true" : "false"
        )));
    }

    private void handleNotify(ProxiedPlayer actor, String[] args) {
        if (args.length != 1) {
            actor.sendMessage(messageService.toComponents(messageService.prefixed("messages.noArgumentsNeeded")));
            return;
        }

        boolean enabled = notificationService.toggle(actor.getUniqueId());
        String key = enabled ? "messages.notifyEnabled" : "messages.notifyDisabled";
        actor.sendMessage(messageService.toComponents(messageService.prefixed(key)));
    }

    private void handleReload(ProxiedPlayer actor, String[] args) {
        if (args.length != 1) {
            actor.sendMessage(messageService.toComponents(messageService.prefixed("messages.noArgumentsNeeded")));
            return;
        }
        if (!actor.hasPermission("luckrank.reload")) {
            actor.sendMessage(messageService.toComponents(messageService.prefixed("messages.noPermission")));
            return;
        }

        de.pcblc.bungee.LuckRank bungeePlugin = (de.pcblc.bungee.LuckRank) plugin;
        boolean reloaded = bungeePlugin.reloadPluginState();
        actor.sendMessage(bungeePlugin.getMessageService().toComponents(
                bungeePlugin.getMessageService().prefixed(reloaded ? "messages.reloadSuccess" : "messages.reloadFailed")
        ));
    }

    private void sendHelp(ProxiedPlayer player) {
        String groups = rankService.getLoadedGroups().stream()
                .map(Group::getName)
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.joining(", "));

        player.sendMessage(messageService.toComponents(messageService.prefixed(
                "messages.availableRanks",
                "ranks", groups.isEmpty() ? "-" : groups
        )));

        for (String line : messageService.getLines("messages.commandUsage")) {
            String text = messageService.prefixedRaw(line);
            TextComponent component = new TextComponent(TextComponent.fromLegacyText(text));
            String command = extractCommand(line);
            if (!command.isEmpty()) {
                component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
            }
            player.sendMessage(component);
        }
    }

    private String extractCommand(String line) {
        int index = line.indexOf('/');
        return index >= 0 ? line.substring(index).trim() : "";
    }

    private void sendMissingArgument(ProxiedPlayer player, String[] args, String... requiredArguments) {
        int providedArguments = Math.max(0, args.length - 1);
        if (providedArguments >= requiredArguments.length) {
            player.sendMessage(messageService.toComponents(messageService.prefixed("messages.tooManyArguments")));
            return;
        }

        player.sendMessage(messageService.toComponents(messageService.prefixed(
                "messages.missingArgument",
                "argument", "<" + requiredArguments[providedArguments] + ">"
        )));
    }

    private Boolean parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        return null;
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterByPrefix(Arrays.asList("set", "remove", "setperms", "creategroup", "debug", "notify", "reload", "help"), args[0]);
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && ("set".equals(subCommand) || "remove".equals(subCommand) || "setperms".equals(subCommand))) {
            List<String> values = new ArrayList<String>();
            plugin.getProxy().getPlayers().forEach(player -> values.add(player.getName()));
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
