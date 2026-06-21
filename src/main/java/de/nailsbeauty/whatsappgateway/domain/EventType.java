package de.nailsbeauty.whatsappgateway.domain;

/**
 * Art eines eingehenden Webhook-Events.
 */
public enum EventType {

    /** Eingehende Nachricht ({@code value.messages[]}). */
    MESSAGE,

    /** Status-Update einer gesendeten Nachricht ({@code value.statuses[]}). */
    STATUS
}
