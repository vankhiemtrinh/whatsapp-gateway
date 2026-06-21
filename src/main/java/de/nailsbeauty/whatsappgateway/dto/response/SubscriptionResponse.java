package de.nailsbeauty.whatsappgateway.dto.response;

/**
 * Response-DTO nach dem App-Abo auf eine WABA.
 *
 * @param wabaId  betroffene WABA-ID
 * @param success ob Meta die Subscription bestaetigt hat
 */
public record SubscriptionResponse(
        String wabaId,
        boolean success
) {
}
