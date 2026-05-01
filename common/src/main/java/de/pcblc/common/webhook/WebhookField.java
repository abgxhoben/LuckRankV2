package de.pcblc.common.webhook;

public final class WebhookField {

    private final String name;
    private final String value;

    public WebhookField(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
