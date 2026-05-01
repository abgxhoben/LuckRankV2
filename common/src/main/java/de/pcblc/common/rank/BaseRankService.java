package de.pcblc.common.rank;

import de.pcblc.common.util.TimeParser;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.WeightNode;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public abstract class BaseRankService<P> {

    private final LuckPerms luckPerms;
    private final Map<UUID, String> pendingRemovals = new ConcurrentHashMap<UUID, String>();

    protected BaseRankService(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    public RankActionResult assignRank(P actor, String targetPlayerName, String groupName, TimeParser.Result duration) {
        if (!hasPermission(actor, "luckrank.set." + groupName.toLowerCase(Locale.ROOT))) {
            return RankActionResult.failure("messages.noPermission");
        }

        Group group = luckPerms.getGroupManager().getGroup(groupName);
        if (group == null) {
            return RankActionResult.failure("messages.rankNotFound");
        }

        P targetPlayer = findOnlinePlayer(targetPlayerName);
        if (targetPlayer == null || !isOnline(targetPlayer)) {
            return RankActionResult.failure("messages.targetPlayerNotOnline");
        }

        try {
            User user = luckPerms.getUserManager().loadUser(getUniqueId(targetPlayer)).join();
            if (user == null) {
                return RankActionResult.failure("messages.targetPlayerNotFound");
            }

            user.data().clear(node -> node instanceof InheritanceNode);
            Node rankNode = duration.isPermanent()
                    ? InheritanceNode.builder(groupName).build()
                    : InheritanceNode.builder(groupName).expiry(duration.getSeconds(), TimeUnit.SECONDS).build();
            user.data().add(rankNode);
            user.setPrimaryGroup(groupName);
            luckPerms.getUserManager().saveUser(user).join();
            pushUpdate();
            return RankActionResult.success(getTargetServer(targetPlayer));
        } catch (Exception exception) {
            logWarning("Could not assign rank.", exception);
            return RankActionResult.failure("messages.operationFailed");
        }
    }

    public RankActionResult removeRank(P actor, String targetPlayerName, String groupName) {
        String confirmationKey = targetPlayerName.toLowerCase(Locale.ROOT) + ":" + groupName.toLowerCase(Locale.ROOT);
        UUID actorId = getUniqueId(actor);
        String existingConfirmation = pendingRemovals.get(actorId);

        if (!confirmationKey.equals(existingConfirmation)) {
            pendingRemovals.put(actorId, confirmationKey);
            scheduleConfirmationExpiry(actorId);
            return RankActionResult.confirmationRequired();
        }

        pendingRemovals.remove(actorId);
        if (!hasPermission(actor, "luckrank.remove." + groupName.toLowerCase(Locale.ROOT))) {
            return RankActionResult.failure("messages.noPermission");
        }

        P targetPlayer = findOnlinePlayer(targetPlayerName);
        if (targetPlayer == null || !isOnline(targetPlayer)) {
            return RankActionResult.failure("messages.targetPlayerNotOnline");
        }

        try {
            User user = luckPerms.getUserManager().loadUser(getUniqueId(targetPlayer)).join();
            if (user == null) {
                return RankActionResult.failure("messages.targetPlayerNotFound");
            }

            boolean removed = user.getNodes().stream()
                    .filter(node -> node instanceof InheritanceNode)
                    .filter(node -> node.getKey().equalsIgnoreCase("group." + groupName))
                    .findFirst()
                    .map(user.data()::remove)
                    .map(DataMutateResult::wasSuccessful)
                    .orElse(false);

            if (!removed) {
                return RankActionResult.failure("messages.groupRemoveFailed");
            }

            luckPerms.getUserManager().saveUser(user).join();
            pushUpdate();
            return RankActionResult.success(getTargetServer(targetPlayer));
        } catch (Exception exception) {
            logWarning("Could not remove rank.", exception);
            return RankActionResult.failure("messages.operationFailed");
        }
    }

    public RankPermissionResult setPermission(P actor, String targetName, String permission, boolean value) {
        if (!hasPermission(actor, "luckrank.setperms." + permission.toLowerCase(Locale.ROOT))) {
            return RankPermissionResult.failure("messages.noPermission");
        }

        Group group = luckPerms.getGroupManager().getGroup(targetName);
        if (group != null) {
            try {
                updatePermission(group.data(), permission, value);
                luckPerms.getGroupManager().saveGroup(group).join();
                pushUpdate();
                return RankPermissionResult.groupSuccess();
            } catch (Exception exception) {
                logWarning("Could not update group permission.", exception);
                return RankPermissionResult.failure("messages.operationFailed");
            }
        }

        P targetPlayer = findOnlinePlayer(targetName);
        if (targetPlayer == null || !isOnline(targetPlayer)) {
            return RankPermissionResult.failure("messages.targetPlayerNotOnline");
        }

        try {
            User user = luckPerms.getUserManager().loadUser(getUniqueId(targetPlayer)).join();
            if (user == null) {
                return RankPermissionResult.failure("messages.targetPlayerNotFound");
            }

            updatePermission(user.data(), permission, value);
            luckPerms.getUserManager().saveUser(user).join();
            pushUpdate();
            return RankPermissionResult.playerSuccess(getTargetServer(targetPlayer));
        } catch (Exception exception) {
            logWarning("Could not update player permission.", exception);
            return RankPermissionResult.failure("messages.operationFailed");
        }
    }

    public RankActionResult createGroup(P actor, String groupName, int weight, String displayName) {
        if (!hasPermission(actor, "luckrank.creategroup")) {
            return RankActionResult.failure("messages.noPermission");
        }

        if (luckPerms.getGroupManager().getGroup(groupName) != null) {
            return RankActionResult.failure("messages.groupAlreadyExists");
        }

        try {
            Group group = luckPerms.getGroupManager().createAndLoadGroup(groupName).join();
            if (group == null) {
                return RankActionResult.failure("messages.groupCreateFailed");
            }

            group.data().add(WeightNode.builder(weight).build());
            group.data().add(MetaNode.builder("displayname", displayName).build());
            luckPerms.getGroupManager().saveGroup(group).join();
            pushUpdate();
            return RankActionResult.success(getActorServer(actor));
        } catch (Exception exception) {
            logWarning("Could not create group.", exception);
            return RankActionResult.failure("messages.groupCreateFailed");
        }
    }

    public Collection<Group> getLoadedGroups() {
        return luckPerms.getGroupManager().getLoadedGroups();
    }

    protected abstract boolean hasPermission(P actor, String permission);

    protected abstract UUID getUniqueId(P player);

    protected abstract P findOnlinePlayer(String playerName);

    protected abstract boolean isOnline(P player);

    protected abstract String getTargetServer(P player);

    protected abstract String getActorServer(P actor);

    protected abstract void scheduleConfirmationExpiry(UUID actorId);

    protected abstract void logWarning(String message, Exception exception);

    private void updatePermission(NodeMap nodeMap, String permission, boolean value) {
        nodeMap.clear(node -> node.getKey().equalsIgnoreCase(permission));
        nodeMap.add(Node.builder(permission).value(value).build());
    }

    private void pushUpdate() {
        luckPerms.getMessagingService().ifPresent(messagingService -> messagingService.pushUpdate());
    }

    protected final void clearPendingRemoval(UUID actorId) {
        pendingRemovals.remove(actorId);
    }
}
