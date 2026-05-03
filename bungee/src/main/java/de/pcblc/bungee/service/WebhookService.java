package de.pcblc.bungee.service;

import de.pcblc.bungee.LuckRank;
import de.pcblc.common.config.PluginConfiguration;
import de.pcblc.common.webhook.DiscordWebhookClient;
import de.pcblc.common.webhook.DiscordWebhookPayloadBuilder;
import de.pcblc.common.webhook.WebhookField;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public final class WebhookService {

    private final LuckRank plugin;
    private final PluginConfiguration configuration;
    private final DiscordWebhookClient webhookClient = new DiscordWebhookClient();
    private final DiscordWebhookPayloadBuilder payloadBuilder = new DiscordWebhookPayloadBuilder();

    public WebhookService(LuckRank plugin, PluginConfiguration configuration) {
        this.plugin = plugin;
        this.configuration = configuration;
    }

    public boolean isEnabled() {
        return configuration.isWebhookEnabled() && !configuration.getWebhookUrl().trim().isEmpty();
    }

    public void sendRankSet(String actor, String targetPlayer, String rank, String duration) {
        sendEmbed(
                "rankSet",
                Arrays.asList(
                        new WebhookField(configuration.getWebhookValue("rankSet.titlePlayer", "Player"), actor),
                        new WebhookField(configuration.getWebhookValue("rankSet.titleTargetPlayer", "Target Player"), targetPlayer),
                        new WebhookField(configuration.getWebhookValue("rankSet.titleRank", "Rank"), rank),
                        new WebhookField(configuration.getWebhookValue("rankSet.titleDuration", "Duration"), duration)
                )
        );
    }

    public void sendRankRemove(String actor, String targetPlayer, String rank) {
        sendEmbed(
                "rankRemove",
                Arrays.asList(
                        new WebhookField(configuration.getWebhookValue("rankRemove.titlePlayer", "Player"), actor),
                        new WebhookField(configuration.getWebhookValue("rankRemove.titleTargetPlayer", "Target Player"), targetPlayer),
                        new WebhookField(configuration.getWebhookValue("rankRemove.titleRank", "Rank"), rank)
                )
        );
    }

    public void sendPermissionSet(String actor, String target, String permission, String value) {
        sendEmbed(
                "permissionSet",
                Arrays.asList(
                        new WebhookField(configuration.getWebhookValue("permissionSet.titlePlayer", "Player"), actor),
                        new WebhookField(configuration.getWebhookValue("permissionSet.titleTarget", "Target"), target),
                        new WebhookField(configuration.getWebhookValue("permissionSet.titlePermission", "Permission"), permission),
                        new WebhookField(configuration.getWebhookValue("permissionSet.titleValue", "Value"), value)
                )
        );
    }

    public void sendCreateGroup(String actor, String groupName, int weight, String displayName) {
        sendEmbed(
                "createGroup",
                Arrays.asList(
                        new WebhookField(configuration.getWebhookValue("createGroup.titlePlayer", "Player"), actor),
                        new WebhookField(configuration.getWebhookValue("createGroup.titleGroupName", "Group Name"), groupName),
                        new WebhookField(configuration.getWebhookValue("createGroup.titleWeight", "Weight"), String.valueOf(weight)),
                        new WebhookField(configuration.getWebhookValue("createGroup.titleDisplayName", "Display Name"), displayName)
                )
        );
    }

    private void sendEmbed(String section, List<WebhookField> fields) {
        if (!isEnabled()) {
            return;
        }

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try {
                String payload = payloadBuilder.buildPayload(
                        configuration.getWebhookValue(section + ".title", "LuckRank"),
                        configuration.getWebhookColor(section + ".color", 3447003),
                        configuration.getWebhookValue(section + ".footer", "LuckRank"),
                        configuration.getWebhookValue(section + ".icon_url", ""),
                        fields
                );
                DiscordWebhookClient.WebhookResponse response = webhookClient.post(configuration.getWebhookUrl(), payload);
                if (response.getResponseCode() != 204) {
                    plugin.getLogger().warning("Webhook request failed with response code "
                            + response.getResponseCode()
                            + (response.getResponseBody().isEmpty() ? "." : " and body: " + response.getResponseBody()));
                }
            } catch (Exception exception) {
                plugin.getLogger().log(Level.WARNING, "Failed to send webhook.", exception);
            }
        });
    }
}
