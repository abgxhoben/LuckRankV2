package de.pcblc.spigot.command;

import de.pcblc.spigot.manager.MySQLManager;
import de.pcblc.spigot.manager.VersionChecker;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.MetaNode;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static de.pcblc.spigot.LuckRank.PREFIX;
public class RankCommand implements CommandExecutor, TabExecutor {

    private final Plugin plugin;
    private final LuckPerms luckPerms = LuckPermsProvider.get();
    private final FileConfiguration config;
    private final VersionChecker versionChecker;
    private final Map<Player, String> confirmationMap = new ConcurrentHashMap<>();
    private final MySQLManager mySQLManager;
    private final FileConfiguration messagesConfig;

    public RankCommand(FileConfiguration config, Plugin plugin, FileConfiguration messagesConfig, VersionChecker versionChecker, MySQLManager mySQLManager) {
        this.config = config;
        this.plugin = plugin;
        this.messagesConfig = messagesConfig;
        this.versionChecker = versionChecker;
        this.mySQLManager = mySQLManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("LuckRank.use")) {
            sender.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', messagesConfig.getString("messages.noPermission")));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set":
                if (args.length != 4) {
                    sendHelp(player);
                    return true;
                }

                handleRankSet(player, args[1], args[2], args[3]);
                break;

            case "remove":
                if (args.length != 3) {
                    sendHelp(player);
                    return true;
                }
                handleRankRemove(player, args[1], args[2]);
                break;

            case "setperms":
                if (args.length != 4) {
                    sendHelp(player);
                    return true;
                }
                handlePermissionSet(player, args[1], args[2], args[3]);
                break;

            case "creategroup":
                if (args.length != 4) {
                    sendHelp(player);
                    return true;
                }

                String groupName = args[1];
                String weightStr = args[2];
                int weight;
                try {
                    weight = Integer.parseInt(weightStr);
                } catch (NumberFormatException e) {
                    String invalidWeightMessage = messagesConfig.getString("messages.invalidWeight");
                    player.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', invalidWeightMessage));
                    return true;
                }
                String displayName = args[3];

                handleCreateGroup(player, groupName, weight, displayName);
                break;

            case "debug":
                if (args.length != 1) {
                    sendHelp(player);
                    return true;
                }
                sendDebug(player);
                break;

            case "notify":
                if (args.length != 1) {
                    sendHelp(player);
                    return true;
                }
                toggleNotify(player);
                break;

            case "help":
                sendHelp(player);
                break;

            default:
                sendHelp(player);
                break;
        }

        return true;
    }
    private void handleRankSet(Player player, String targetPlayerName, String targetGroupName, String time) {
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);

        if (targetPlayer == null || !targetPlayer.isOnline()) {
            player.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', messagesConfig.getString("messages.targetPlayerNotOnline")));
            return;
        }

        if (!hasPermission(player, targetGroupName)) {
            player.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', messagesConfig.getString("messages.noPermission")));
            return;
        }

        long duration = parseDuration(player, time);

        if (duration == -2) {
            return;
        }

        User targetUser = luckPerms.getUserManager().loadUser(targetPlayer.getUniqueId()).join();

        if (targetUser == null) {
            player.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', messagesConfig.getString("messages.targetPlayerNotFound")));
            return;
        }

        targetUser.data().clear(node -> node.getKey().equals("group"));

        if (duration == Long.MIN_VALUE) {
            targetUser.data().add(Node.builder("group." + targetGroupName).build());
        } else {
            targetUser.data().add(Node.builder("group." + targetGroupName)
                    .expiry(duration, TimeUnit.SECONDS)
                    .build());
        }

        luckPerms.getUserManager().saveUser(targetUser);

        String rankUpdatedMessage = messagesConfig.getString("messages.rankUpdated")
                .replace("{rank}", targetGroupName)
                .replace("{duration}", duration > 0 ? formatDuration(duration) : "Lifetime");

        player.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', rankUpdatedMessage));

        String rankSetSuccessfullyMessage = messagesConfig.getString("messages.rankSetSuccessfully")
                .replace("{targetPlayer}", targetPlayerName)
                .replace("{rank}", targetGroupName)
                .replace("{duration}", duration > 0 ? formatDuration(duration) : "Lifetime");

        player.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', rankSetSuccessfullyMessage));

        String rankSetLogMessage = messagesConfig.getString("messages.rankSetLog")
                .replace("{player}", player.getName())
                .replace("{targetPlayer}", targetPlayerName)
                .replace("{rank}", targetGroupName)
                .replace("{duration}", duration > 0 ? formatDuration(duration) : "Lifetime");


        String globalmessage = messagesConfig.getString("messages.globalmessage")
                .replace("{player}", player.getName())
                .replace("{targetPlayer}", targetPlayerName)
                .replace("{rank}", targetGroupName)
                .replace("{duration}", duration > 0 ? formatDuration(duration) : "Lifetime");

        Bukkit.getLogger().info(PREFIX + ChatColor.translateAlternateColorCodes('&', rankSetLogMessage));

        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("luckrank.see"))
                .forEach(playerWithPermission -> {
                    String playerUUID = playerWithPermission.getUniqueId().toString();
                    if (mySQLManager.getNotifyStatus(playerUUID)) {
                        playerWithPermission.sendMessage(ChatColor.translateAlternateColorCodes('&', PREFIX + globalmessage));
                    }
                });

        if (config.getBoolean("webhook.enabled", false)) {
            sendWebhook(player.getName(), targetPlayerName, targetGroupName, duration);
        }
    }
    public void handlePermissionSet(Player player, String targetName, String permission, String value) {
        if (luckPerms.getGroupManager().isLoaded(targetName)) {
            Group targetGroup = luckPerms.getGroupManager().getGroup(targetName);

            if (targetGroup == null) {
                player.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', messagesConfig.getString("messages.targetPlayerNotFound")));
                return;
            }

            if (!hasSetPermsPermission(player, permission)) {
                player.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', messagesConfig.getString("messages.noPermission")));
                return;
            }

            boolean permValue;
            if (value.equalsIgnoreCase("true")) {
                permValue = true;
            } else if (value.equalsIgnoreCase("false")) {
                permValue = false;
            } else {
                String invalidPermissionValue = messagesConfig.getString("messages.invalidPermissionValue");
                player.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', invalidPermissionValue));
                return;
            }

            if (permValue) {
                targetGroup.data().add(Node.builder(permission).build());
            } else {
                targetGroup.data().remove(Node.builder(permission).build());
            }

            luckPerms.getGroupManager().saveGroup(targetGroup);

            String permissionSetMessage = messagesConfig.getString("messages.permissionSetgroup")
                    .replace("{group}", targetName)
                    .replace("{permission}", permission)
                    .replace("{value}", value);

            player.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', permissionSetMessage));

            String permissionSetLogMessage = messagesConfig.getString("messages.permissionSetLoggroup")
                    .replace("{player}", player.getName())
                    .replace("{group}", targetName)
                    .replace("{permission}", permission)
                    .replace("{value}", value);

            String globalmessagepermission = messagesConfig.getString("messages.globalmessagepermissiongroup")
                    .replace("{player}", player.getName())
                    .replace("{group}", targetName)
                    .replace("{permission}", permission)
                    .replace("{value}", value);

            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("luckrank.see"))
                    .forEach(playerWithPermission -> {
                        String playerUUID = playerWithPermission.getUniqueId().toString();
                        if (mySQLManager.getNotifyStatus(playerUUID)) {
                            playerWithPermission.sendMessage(ChatColor.translateAlternateColorCodes('&', PREFIX + globalmessagepermission));
                        }
                    });

            if (config.getBoolean("webhook.enabled", false)) {
                sendPermissionSetWebhook(player.getName(), targetName, permission, value);
            }

            plugin.getLogger().info(permissionSetLogMessage);
        } else {

            Player targetPlayer = plugin.getServer().getPlayer(targetName);

            if (targetPlayer == null || !targetPlayer.isOnline()) {
                player.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', messagesConfig.getString("messages.targetPlayerNotOnline")));
                return;
            }

            User targetUser = luckPerms.getUserManager().loadUser(targetPlayer.getUniqueId()).join();

            if (targetUser == null) {
                player.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', messagesConfig.getString("messages.targetPlayerNotFound")));
                return;
            }

            boolean permValue;
            if (value.equalsIgnoreCase("true")) {
                permValue = true;
            } else if (value.equalsIgnoreCase("false")) {
                permValue = false;
            } else {
                String invalidPermissionValue = messagesConfig.getString("messages.invalidPermissionValue");
                player.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', invalidPermissionValue));
                return;
            }

            if (permValue) {
                targetUser.data().add(Node.builder(permission).build());
            } else {
                targetUser.data().remove(Node.builder(permission).build());
            }

            luckPerms.getUserManager().saveUser(targetUser);

            String permissionSetMessage = messagesConfig.getString("messages.permissionSet")
                    .replace("{player}", targetName)
                    .replace("{permission}", permission)
                    .replace("{value}", value);

            player.sendMessage(PREFIX + net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', permissionSetMessage));

            String permissionSetLogMessage = messagesConfig.getString("messages.permissionSetLog")
                    .replace("{player}", player.getName())
                    .replace("{targetPlayer}", targetName)
                    .replace("{permission}", permission)
                    .replace("{value}", value);


            String globalmessagepermission = messagesConfig.getString("messages.globalmessagepermission")
                    .replace("{player}", player.getName())
                    .replace("{targetPlayer}", targetName)
                    .replace("{permission}", permission)
                    .replace("{value}", value);

            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("luckrank.see"))
                    .forEach(playerWithPermission -> {
                        String playerUUID = playerWithPermission.getUniqueId().toString();
                        if (mySQLManager.getNotifyStatus(playerUUID)) {
                            playerWithPermission.sendMessage(ChatColor.translateAlternateColorCodes('&', PREFIX + globalmessagepermission));
                        }
                    });

            if (config.getBoolean("webhook.enabled", false)) {
                sendPermissionSetWebhook(player.getName(), targetName, permission, value);
            }

            plugin.getLogger().info(permissionSetLogMessage);
        }
    }
    private void handleRankRemove(Player player, String targetPlayerName, String groupName) {
        if (confirmationMap.containsKey(player) && confirmationMap.get(player).equals(targetPlayerName + ":" + groupName)) {
            confirmationMap.remove(player);

            Player targetPlayer = plugin.getServer().getPlayer(targetPlayerName);

        if (!hasRemovePermission(player, groupName)) {
            player.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', messagesConfig.getString("messages.noPermission")));
            return;
        }

        if (targetPlayer == null || !targetPlayer.isOnline()) {
            player.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', messagesConfig.getString("messages.targetPlayerNotOnline")));
            return;
        }

        User targetUser = luckPerms.getUserManager().loadUser(targetPlayer.getUniqueId()).join();

        if (targetUser == null) {
            player.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', messagesConfig.getString("messages.targetPlayerNotFound")));
            return;
        }

        Node groupNodeToRemove = Node.builder("group." + groupName).build();
        DataMutateResult resultPermanent = targetUser.data().remove(groupNodeToRemove);


        Node tempGroupNodeToRemove = Node.builder("group." + groupName).expiry(10, TimeUnit.SECONDS).build();
        DataMutateResult resultTemporary = targetUser.data().remove(tempGroupNodeToRemove);

        if (!resultPermanent.wasSuccessful() && !resultTemporary.wasSuccessful()) {
            String groupRemoveFailedMessage = messagesConfig.getString("messages.groupRemoveFailed");
            player.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', groupRemoveFailedMessage));
            return;
        }

        luckPerms.getUserManager().saveUser(targetUser);

        String rankRemovedMessage = messagesConfig.getString("messages.rankRemoved")
                .replace("{player}", targetPlayerName)
                .replace("{rank}", groupName);

        player.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', rankRemovedMessage));

        String rankRemoveLogMessage = messagesConfig.getString("messages.rankRemoveLog")
                .replace("{player}", player.getName())
                .replace("{targetPlayer}", targetPlayerName)
                .replace("{rank}", groupName);

        String globalmessageremove = messagesConfig.getString("messages.globalmessageremove")
                .replace("{player}", player.getName())
                .replace("{targetPlayer}", targetPlayerName)
                .replace("{rank}", groupName);

            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("luckrank.see"))
                    .forEach(playerWithPermission -> {
                        String playerUUID = playerWithPermission.getUniqueId().toString();
                        if (mySQLManager.getNotifyStatus(playerUUID)) {
                            playerWithPermission.sendMessage(ChatColor.translateAlternateColorCodes('&', PREFIX + globalmessageremove));
                        }
                    });

        if (config.getBoolean("webhook.enabled", false)) {
            sendRankRemoveWebhook(player.getName(), targetPlayerName, groupName);
        }

        plugin.getLogger().info(rankRemoveLogMessage);

        } else {
            player.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', messagesConfig.getString("messages.confirmationWarning")));
            confirmationMap.put(player, targetPlayerName + ":" + groupName);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                confirmationMap.remove(player);
            }, 600L);
        }
    }
    public void handleCreateGroup(Player player, String groupName, int weight, String displayName) {
        if (!player.hasPermission("luckrank.creategroup")) {
            player.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', messagesConfig.getString("messages.noPermission")));
            return;
        }

        CompletableFuture<Group> futureGroup = luckPerms.getGroupManager().createAndLoadGroup(groupName);
        futureGroup.thenAccept(group -> {
            if (group == null) {
                player.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', messagesConfig.getString("messages.groupCreateFailed")));
                return;
            }

            group.data().add(InheritanceNode.builder(groupName).build());
            group.data().add(MetaNode.builder("weight", String.valueOf(weight)).build());
            group.data().add(MetaNode.builder("displayname", displayName).build());

            luckPerms.getGroupManager().saveGroup(group).thenRun(() -> {
                String groupCreatedMessage = messagesConfig.getString("messages.groupCreated")
                        .replace("{group}", groupName)
                        .replace("{weight}", String.valueOf(weight))
                        .replace("{displayname}", displayName);

                player.sendMessage(PREFIX + net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', groupCreatedMessage));

                String globalMessageCreateGroup = messagesConfig.getString("messages.globalMessageCreateGroup")
                        .replace("{player}", player.getName())
                        .replace("{group}", groupName)
                        .replace("{weight}", String.valueOf(weight))
                        .replace("{displayname}", displayName);

                Bukkit.getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission("luckrank.see"))
                        .forEach(playerWithPermission -> {
                            String playerUUID = playerWithPermission.getUniqueId().toString();
                            if (mySQLManager.getNotifyStatus(playerUUID)) {
                                playerWithPermission.sendMessage(ChatColor.translateAlternateColorCodes('&', PREFIX + globalMessageCreateGroup));
                            }
                        });

                if (config.getBoolean("webhook.enabled", false)) {
                    sendCreateGroupWebhook(player.getName(), groupName, weight, displayName);
                }

                plugin.getLogger().info(groupCreatedMessage);
            });
        });
    }
    private boolean hasPermission(Player player, String groupName) {
        return player.hasPermission("luckrank.set." + groupName.toLowerCase());
    }
    private boolean hasRemovePermission(Player player, String groupName) {
        return player.hasPermission("luckrank.remove." + groupName.toLowerCase());
    }
    private boolean hasSetPermsPermission(Player player, String permission) {
        return player.hasPermission("luckrank.setperms." + permission.toLowerCase());
    }
    private long parseDuration(Player player, String time) {
        if (time.equalsIgnoreCase("-1")) {
            return Long.MIN_VALUE;
        }

        long totalSeconds = 0;
        Matcher matcher = Pattern.compile("(\\d+)([dhm])").matcher(time);
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "d":
                    totalSeconds += value * 86400L; // 60 * 60 * 24
                    break;
                case "h":
                    totalSeconds += value * 3600L; // 60 * 60
                    break;
                case "m":
                    totalSeconds += value * 60L;
                    break;
                default:
                    player.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', messagesConfig.getString("messages.invalidDurationFormat")));
                    return -2;
            }
        }

        if (totalSeconds == 0) {
            player.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', messagesConfig.getString("messages.invalidDurationFormat")));
            return -2;
        }

        return totalSeconds;
    }
    private String formatDuration(long duration) {
        long days = TimeUnit.SECONDS.toDays(duration);
        long hours = TimeUnit.SECONDS.toHours(duration) % 24;
        long minutes = TimeUnit.SECONDS.toMinutes(duration) % 60;

        StringBuilder formatted = new StringBuilder();
        if (days > 0) formatted.append(days).append(" days ");
        if (hours > 0) formatted.append(hours).append(" hours ");
        if (minutes > 0) formatted.append(minutes).append(" minutes");

        return formatted.toString().trim();
    }
    private void sendHelp(Player player) {
        List<String> groupNames = luckPerms.getGroupManager().getLoadedGroups().stream()
                .map(Group::getName)
                .collect(Collectors.toList());

        String availableRanks = String.join(" ", groupNames);

        String helpMessage = messagesConfig.getString("messages.availableRanks")
                .replace("{ranks}", availableRanks);

        player.spigot().sendMessage(new TextComponent(PREFIX + ChatColor.translateAlternateColorCodes('&', helpMessage)));

        String commandUsageMessage = messagesConfig.getString("messages.commandUsage");

        String[] commandUsages = commandUsageMessage.split("\\n");

        for (String usage : commandUsages) {
            TextComponent commandComponent = new TextComponent(PREFIX + net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', usage));
            commandComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, extractCommand(usage)));
            player.spigot().sendMessage(commandComponent);
        }
    }
    private void sendDebug(Player player) {
        String pluginVersion = plugin.getDescription().getVersion();
        String developerName = plugin.getDescription().getAuthors().toString();

        boolean webhookEnabled = config.getBoolean("webhook.enabled", false);
        String webhookStatus = webhookEnabled ? "true" : "false";

        boolean isOutdated = versionChecker.isOutdated();
        String versionStatus = isOutdated ? "&cOUTDATED ! PLEASE UPDATE" : "(LATEST)";

        String debugMessage = messagesConfig.getString("messages.debug");

        debugMessage = debugMessage.replace("{version}", pluginVersion);
        debugMessage = debugMessage.replace("{versionstatus}", versionStatus);
        debugMessage = debugMessage.replace("{developer}", developerName);
        debugMessage = debugMessage.replace("{status}", webhookStatus);


        player.sendMessage(ChatColor.translateAlternateColorCodes('&', debugMessage));
    }
    private void toggleNotify(Player player) {
        UUID playerUUID = player.getUniqueId();
        boolean currentStatus = mySQLManager.getNotifyStatus(playerUUID.toString());
        boolean newStatus = !currentStatus;
        mySQLManager.setNotifyStatus(playerUUID.toString(), newStatus);

        String messageKey = newStatus ? "notifyEnabled" : "notifyDisabled";
        String message = messagesConfig.getString("messages." + messageKey);

        player.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', message));
    }

    private String extractCommand(String message) {
        int start = message.indexOf('/');
        if (start == -1) {
            return "";
        }
        return message.substring(start).trim();
    }

    private void sendWebhook(String player, String targetPlayer, String rank, long duration) {
        String webhookUrl = config.getString("webhook.url");
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            plugin.getLogger().warning("Webhook URL is not set in the config file.");
            return;
        }

        String ranksettitle = config.getString("webhook.rankSet.title");
        String ranksetPlayer = config.getString("webhook.rankSet.titlePlayer");
        String ranksetTargetPlayer = config.getString("webhook.rankSet.titleTargetPlayer");
        String ranksetRank = config.getString("webhook.rankSet.titleRank");
        String ranksetDuration = config.getString("webhook.rankSet.titleDuration");
        int ranksetcolor = config.getInt("webhook.rankSet.color");
        String ranksetfooter = config.getString("webhook.rankSet.footer");
        String rankseticon_url = config.getString("webhook.rankSet.icon_url");


        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            String jsonPayload = "{\n" +
                    "  \"embeds\": [\n" +
                    "    {\n" +
                    "      \"title\": \"" + ranksettitle + "\",\n" +
                    "      \"color\":" + ranksetcolor + ",\n" +
                    "      \"fields\": [\n" +
                    "        {\n" +
                    "          \"name\": \"" + ranksetPlayer + "\",\n" +
                    "          \"value\": \"" + player + "\",\n" +
                    "          \"inline\": true\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"name\": \""+ ranksetTargetPlayer + "\",\n" +
                    "          \"value\": \"" + targetPlayer + "\",\n" +
                    "          \"inline\": true\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"name\": \"" + ranksetRank + "\",\n" +
                    "          \"value\": \"" + rank + "\",\n" +
                    "          \"inline\": true\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"name\": \"" + ranksetDuration + "\",\n" +
                    "          \"value\": \"" + (duration > 0 ? formatDuration(duration) : "Lifetime") + "\",\n" +
                    "          \"inline\": true\n" +
                    "        }\n" +
                    "      ],\n" +
                    "      \"footer\": {\n" +
                    "        \"text\": \"" + ranksetfooter + "\",\n" +
                    "        \"icon_url\": \"" + rankseticon_url + "\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonPayload.getBytes());
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 204) {
                plugin.getLogger().warning("Failed to send webhook, response code: " + responseCode);
            } else {
                plugin.getLogger().info("Webhook sent successfully.");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to send webhook: " + e.getMessage(), e);
        }
    }

    private void sendRankRemoveWebhook(String player, String targetPlayer, String rank) {
        String webhookUrl = config.getString("webhook.url");
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            plugin.getLogger().warning("Webhook URL is not set in the config file.");
            return;
        }

        String rankRemoveTitle = config.getString("webhook.rankRemove.title");
        String rankRemovePlayer = config.getString("webhook.rankRemove.titlePlayer");
        String rankRemoveTargetPlayer = config.getString("webhook.rankRemove.titleTargetPlayer");
        String rankRemoveRank = config.getString("webhook.rankRemove.titleRank");
        int rankRemoveColor = config.getInt("webhook.rankRemove.color");
        String rankRemoveFooter = config.getString("webhook.rankRemove.footer");
        String rankRemoveIconUrl = config.getString("webhook.rankRemove.icon_url");

        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            String jsonPayload = "{\n" +
                    "  \"embeds\": [\n" +
                    "    {\n" +
                    "      \"title\": \"" + rankRemoveTitle + "\",\n" +
                    "      \"color\": " + rankRemoveColor + ",\n" +
                    "      \"fields\": [\n" +
                    "        {\n" +
                    "          \"name\": \"" + rankRemovePlayer + "\",\n" +
                    "          \"value\": \"" + player + "\",\n" +
                    "          \"inline\": true\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"name\": \"" + rankRemoveTargetPlayer + "\",\n" +
                    "          \"value\": \"" + targetPlayer + "\",\n" +
                    "          \"inline\": true\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"name\": \"" + rankRemoveRank + "\",\n" +
                    "          \"value\": \"" + rank + "\",\n" +
                    "          \"inline\": true\n" +
                    "        }\n" +
                    "      ],\n" +
                    "      \"footer\": {\n" +
                    "        \"text\": \"" + rankRemoveFooter + "\",\n" +
                    "        \"icon_url\": \"" + rankRemoveIconUrl + "\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonPayload.getBytes());
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 204) {
                plugin.getLogger().warning("Failed to send Rank Remove webhook, response code: " + responseCode);
            } else {
                plugin.getLogger().info("Rank Remove webhook sent successfully.");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to send Rank Remove webhook: " + e.getMessage(), e);
        }
    }

    private void sendPermissionSetWebhook(String player, String targetPlayer, String permission, String value) {
        String webhookUrl = config.getString("webhook.url");
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            plugin.getLogger().warning("Webhook URL is not set in the config file.");
            return;
        }

        String permissionSetTitle = config.getString("webhook.permissionSet.title");
        String permissionSetPlayer = config.getString("webhook.permissionSet.titlePlayer");
        String permissionSetTargetPlayer = config.getString("webhook.permissionSet.titleTargetPlayer");
        String permissionSetPermission = config.getString("webhook.permissionSet.titlePermission");
        String permissionSetValue = config.getString("webhook.permissionSet.titleValue");
        int permissionSetColor = config.getInt("webhook.permissionSet.color");
        String permissionSetFooter = config.getString("webhook.permissionSet.footer");
        String permissionSetIconUrl = config.getString("webhook.permissionSet.icon_url");

        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            String jsonPayload = "{\n" +
                    "  \"embeds\": [\n" +
                    "    {\n" +
                    "      \"title\": \"" + permissionSetTitle + "\",\n" +
                    "      \"color\": " + permissionSetColor + ",\n" +
                    "      \"fields\": [\n" +
                    "        {\n" +
                    "          \"name\": \"" + permissionSetPlayer + "\",\n" +
                    "          \"value\": \"" + player + "\",\n" +
                    "          \"inline\": true\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"name\": \"" + permissionSetTargetPlayer + "\",\n" +
                    "          \"value\": \"" + targetPlayer + "\",\n" +
                    "          \"inline\": true\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"name\": \"" + permissionSetPermission + "\",\n" +
                    "          \"value\": \"" + permission + "\",\n" +
                    "          \"inline\": true\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"name\": \"" + permissionSetValue + "\",\n" +
                    "          \"value\": \"" + value + "\",\n" +
                    "          \"inline\": true\n" +
                    "        }\n" +
                    "      ],\n" +
                    "      \"footer\": {\n" +
                    "        \"text\": \"" + permissionSetFooter + "\",\n" +
                    "        \"icon_url\": \"" + permissionSetIconUrl + "\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonPayload.getBytes());
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 204) {
                plugin.getLogger().warning("Failed to send Permission Set webhook, response code: " + responseCode);
            } else {
                plugin.getLogger().info("Permission Set webhook sent successfully.");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to send Permission Set webhook: " + e.getMessage(), e);
        }
    }

    private void sendCreateGroupWebhook(String player, String groupName, int weight, String displayName) {
        String webhookUrl = config.getString("webhook.url");
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            plugin.getLogger().warning("Webhook URL is not set in the config file.");
            return;
        }

        String groupCreateTitle = config.getString("webhook.createGroup.title");
        String groupCreatePlayer = config.getString("webhook.createGroup.titlePlayer");
        String groupCreateGroupName = config.getString("webhook.createGroup.titleGroupName");
        String groupCreateWeight = config.getString("webhook.createGroup.titleWeight");
        String groupCreateDisplayName = config.getString("webhook.createGroup.titleDisplayName");
        int groupCreateColor = config.getInt("webhook.createGroup.color");
        String groupCreateFooter = config.getString("webhook.createGroup.footer");
        String groupCreateIconUrl = config.getString("webhook.createGroup.icon_url");

        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            String jsonPayload = "{\n" +
                    "  \"embeds\": [\n" +
                    "    {\n" +
                    "      \"title\": \"" + groupCreateTitle + "\",\n" +
                    "      \"color\": " + groupCreateColor + ",\n" +
                    "      \"fields\": [\n" +
                    "        {\n" +
                    "          \"name\": \"" + groupCreatePlayer + "\",\n" +
                    "          \"value\": \"" + player + "\",\n" +
                    "          \"inline\": true\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"name\": \"" + groupCreateGroupName + "\",\n" +
                    "          \"value\": \"" + groupName + "\",\n" +
                    "          \"inline\": true\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"name\": \"" + groupCreateWeight + "\",\n" +
                    "          \"value\": \"" + weight + "\",\n" +
                    "          \"inline\": true\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"name\": \"" + groupCreateDisplayName + "\",\n" +
                    "          \"value\": \"" + displayName + "\",\n" +
                    "          \"inline\": true\n" +
                    "        }\n" +
                    "      ],\n" +
                    "      \"footer\": {\n" +
                    "        \"text\": \"" + groupCreateFooter + "\",\n" +
                    "        \"icon_url\": \"" + groupCreateIconUrl + "\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonPayload.getBytes());
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 204) {
                plugin.getLogger().warning("Failed to send Create Group webhook, response code: " + responseCode);
            } else {
                plugin.getLogger().info("Create Group webhook sent successfully.");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to send Create Group webhook: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("set", "remove", "setperms", "creategroup", "help", "debug", "notify"));
            return completions;
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("setperms"))) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                completions.add(onlinePlayer.getName());
            }
            return completions;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("creategroup")) {
        completions.add("NAME");
        return completions;
    }

        if (args.length == 2 && args[0].equalsIgnoreCase("setperms")) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                completions.add(onlinePlayer.getName());
            }
            completions.addAll(luckPerms.getGroupManager().getLoadedGroups().stream()
                    .map(Group::getName)
                    .collect(Collectors.toList()));
            return completions;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            completions.addAll(luckPerms.getGroupManager().getLoadedGroups().stream()
                    .map(Group::getName)
                    .collect(Collectors.toList()));
            return completions;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("remove")) {
            completions.addAll(luckPerms.getGroupManager().getLoadedGroups().stream()
                    .map(Group::getName)
                    .collect(Collectors.toList()));
            return completions;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("setperms")) {
            completions.add("PERMISSION");
            return completions;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("creategroup")) {
            completions.add("WEIGHT");
            return completions;
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("set")) {
            completions.add("TIME");
            return completions;
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("setperms")) {
            completions.addAll(Arrays.asList("true", "false"));
            return completions;
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("creategroup")) {
            completions.add("DISPLAYNAME");
            return completions;
        }

        return completions;
    }
}