package de.pcblc.common.rank;

public final class RankPermissionResult {

    private final boolean success;
    private final boolean groupTarget;
    private final String messageKey;
    private final String targetServer;

    private RankPermissionResult(boolean success, boolean groupTarget, String messageKey, String targetServer) {
        this.success = success;
        this.groupTarget = groupTarget;
        this.messageKey = messageKey;
        this.targetServer = targetServer;
    }

    public static RankPermissionResult groupSuccess() {
        return new RankPermissionResult(true, true, "", "Unknown");
    }

    public static RankPermissionResult playerSuccess(String targetServer) {
        return new RankPermissionResult(true, false, "", targetServer);
    }

    public static RankPermissionResult failure(String messageKey) {
        return new RankPermissionResult(false, false, messageKey, "Unknown");
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isGroupTarget() {
        return groupTarget;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getTargetServer() {
        return targetServer;
    }
}
