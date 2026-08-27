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

import java.util.ArrayList;
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
        if (groupName == null || groupName.trim().isEmpty()) {
            return RankActionResult.failure("messages.rankNotFound");
        }
        if (duration == null || !duration.isValid()) {
            return RankActionResult.failure("messages.invalidDurationFormat");
        }
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

            String canonicalGroupName = group.getName();
            // Replace only the selected global rank. Contextual memberships and
            // other global groups are unrelated and must remain untouched.
            for (Node node : new ArrayList<Node>(user.data().toCollection())) {
                if (isGlobalInheritanceForGroup(node, canonicalGroupName)
                        && !user.data().remove(node).wasSuccessful()) {
                    return RankActionResult.failure("messages.operationFailed");
                }
            }

            Node rankNode = duration.isPermanent()
                    ? InheritanceNode.builder(canonicalGroupName).build()
                    : InheritanceNode.builder(canonicalGroupName).expiry(duration.getSeconds(), TimeUnit.SECONDS).build();
            if (!user.data().add(rankNode).wasSuccessful()
                    || !user.setPrimaryGroup(canonicalGroupName).wasSuccessful()) {
                return RankActionResult.failure("messages.operationFailed");
            }
            luckPerms.getUserManager().saveUser(user).join();
            pushUpdate();
            return RankActionResult.success(getTargetServer(targetPlayer));
        } catch (Exception exception) {
            logWarning("Could not assign rank.", exception);
            return RankActionResult.failure("messages.operationFailed");
        }
    }

    public RankActionResult removeRank(P actor, String targetPlayerName, String groupName) {
        if (targetPlayerName == null || targetPlayerName.trim().isEmpty()
                || groupName == null || groupName.trim().isEmpty()) {
            return RankActionResult.failure("messages.groupRemoveFailed");
        }
        if (!hasPermission(actor, "luckrank.remove." + groupName.toLowerCase(Locale.ROOT))) {
            pendingRemovals.remove(getUniqueId(actor));
            return RankActionResult.failure("messages.noPermission");
        }

        String confirmationKey = targetPlayerName.toLowerCase(Locale.ROOT) + ":" + groupName.toLowerCase(Locale.ROOT);
        UUID actorId = getUniqueId(actor);
        String existingConfirmation = pendingRemovals.get(actorId);

        if (!confirmationKey.equals(existingConfirmation)) {
            pendingRemovals.put(actorId, confirmationKey);
            scheduleConfirmationExpiry(actorId, confirmationKey);
            return RankActionResult.confirmationRequired();
        }

        pendingRemovals.remove(actorId, confirmationKey);
        P targetPlayer = findOnlinePlayer(targetPlayerName);
        if (targetPlayer == null || !isOnline(targetPlayer)) {
            return RankActionResult.failure("messages.targetPlayerNotOnline");
        }

        try {
            User user = luckPerms.getUserManager().loadUser(getUniqueId(targetPlayer)).join();
            if (user == null) {
                return RankActionResult.failure("messages.targetPlayerNotFound");
            }

            String primaryGroupBeforeRemoval = user.getPrimaryGroup();
            boolean removed = false;
            for (Node node : new ArrayList<Node>(user.data().toCollection())) {
                if (isGlobalInheritanceForGroup(node, groupName)) {
                    removed = true;
                    if (!user.data().remove(node).wasSuccessful()) {
                        return RankActionResult.failure("messages.groupRemoveFailed");
                    }
                }
            }

            if (!removed) {
                return RankActionResult.failure("messages.groupRemoveFailed");
            }

            if (groupName.equalsIgnoreCase(primaryGroupBeforeRemoval)) {
                String fallbackGroup = findFallbackPrimaryGroup(user, groupName);
                if (fallbackGroup == null) {
                    return RankActionResult.failure("messages.operationFailed");
                }
                if (!user.setPrimaryGroup(fallbackGroup).wasSuccessful()) {
                    return RankActionResult.failure("messages.operationFailed");
                }
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
        if (permission == null || permission.trim().isEmpty()) {
            return RankPermissionResult.failure("messages.operationFailed");
        }
        if (!hasPermission(actor, "luckrank.setperms." + permission.toLowerCase(Locale.ROOT))) {
            return RankPermissionResult.failure("messages.noPermission");
        }

        permission = permission.trim();
        Group group = luckPerms.getGroupManager().getGroup(targetName);
        if (group != null) {
            try {
                if (!updatePermission(group.data(), permission, value)) {
                    return RankPermissionResult.failure("messages.operationFailed");
                }
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

            if (!updatePermission(user.data(), permission, value)) {
                return RankPermissionResult.failure("messages.operationFailed");
            }
            luckPerms.getUserManager().saveUser(user).join();
            pushUpdate();
            return RankPermissionResult.playerSuccess(getTargetServer(targetPlayer));
        } catch (Exception exception) {
            logWarning("Could not update player permission.", exception);
            return RankPermissionResult.failure("messages.operationFailed");
        }
    }

    public RankActionResult createGroup(P actor, String groupName, int weight, String displayName) {
        if (groupName == null || groupName.trim().isEmpty()
                || displayName == null || displayName.trim().isEmpty()) {
            return RankActionResult.failure("messages.groupCreateFailed");
        }
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

            if (!group.data().add(WeightNode.builder(weight).build()).wasSuccessful()
                    || !group.data().add(MetaNode.builder("displayname", displayName).build()).wasSuccessful()) {
                luckPerms.getGroupManager().deleteGroup(group).join();
                return RankActionResult.failure("messages.groupCreateFailed");
            }
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

    protected abstract void scheduleConfirmationExpiry(UUID actorId, String confirmationKey);

    protected abstract void logWarning(String message, Exception exception);

    private boolean updatePermission(NodeMap nodeMap, String permission, boolean value) {
        for (Node node : new ArrayList<Node>(nodeMap.toCollection())) {
            if (node.getKey().equalsIgnoreCase(permission) && node.getContexts().isEmpty()
                    && !nodeMap.remove(node).wasSuccessful()) {
                return false;
            }
        }
        return nodeMap.add(Node.builder(permission).value(value).build()).wasSuccessful();
    }

    private boolean isGlobalInheritanceForGroup(Node node, String groupName) {
        return isGlobalInheritance(node)
                && ((InheritanceNode) node).getGroupName().equalsIgnoreCase(groupName);
    }

    private boolean isGlobalInheritance(Node node) {
        return node instanceof InheritanceNode && node.getValue() && node.getContexts().isEmpty();
    }

    private String findFallbackPrimaryGroup(User user, String removedGroup) {
        String calculatedPrimaryGroup = user.getPrimaryGroup();
        if (calculatedPrimaryGroup != null
                && !calculatedPrimaryGroup.equalsIgnoreCase(removedGroup)
                && hasGlobalPositiveInheritance(user, calculatedPrimaryGroup)
                && luckPerms.getGroupManager().getGroup(calculatedPrimaryGroup) != null) {
            return calculatedPrimaryGroup;
        }

        for (Node node : user.data().toCollection()) {
            if (isGlobalInheritance(node)) {
                String groupName = ((InheritanceNode) node).getGroupName();
                if (!groupName.equalsIgnoreCase(removedGroup)
                        && luckPerms.getGroupManager().getGroup(groupName) != null) {
                    return groupName;
                }
            }
        }

        Group defaultGroup = luckPerms.getGroupManager().getGroup("default");
        if (defaultGroup == null || defaultGroup.getName().equalsIgnoreCase(removedGroup)) {
            return null;
        }

        if (!hasGlobalPositiveInheritance(user, defaultGroup.getName())
                && !user.data().add(InheritanceNode.builder(defaultGroup.getName()).build()).wasSuccessful()) {
            return null;
        }
        return defaultGroup.getName();
    }

    private boolean hasGlobalPositiveInheritance(User user, String groupName) {
        for (Node node : user.data().toCollection()) {
            if (isGlobalInheritance(node)
                    && ((InheritanceNode) node).getGroupName().equalsIgnoreCase(groupName)) {
                return true;
            }
        }
        return false;
    }

    private void pushUpdate() {
        luckPerms.getMessagingService().ifPresent(messagingService -> messagingService.pushUpdate());
    }

    protected final void clearPendingRemoval(UUID actorId, String confirmationKey) {
        pendingRemovals.remove(actorId, confirmationKey);
    }
}
