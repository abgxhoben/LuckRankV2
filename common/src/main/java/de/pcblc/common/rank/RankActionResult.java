package de.pcblc.common.rank;

public final class RankActionResult {

    private final boolean success;
    private final boolean requiresConfirmation;
    private final String messageKey;
    private final String targetServer;

    private RankActionResult(boolean success, boolean requiresConfirmation, String messageKey, String targetServer) {
        this.success = success;
        this.requiresConfirmation = requiresConfirmation;
        this.messageKey = messageKey;
        this.targetServer = targetServer;
    }

    public static RankActionResult success(String targetServer) {
        return new RankActionResult(true, false, "", targetServer);
    }

    public static RankActionResult failure(String messageKey) {
        return new RankActionResult(false, false, messageKey, "Unknown");
    }

    public static RankActionResult confirmationRequired() {
        return new RankActionResult(false, true, "", "Unknown");
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean requiresConfirmation() {
        return requiresConfirmation;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getTargetServer() {
        return targetServer;
    }
}
