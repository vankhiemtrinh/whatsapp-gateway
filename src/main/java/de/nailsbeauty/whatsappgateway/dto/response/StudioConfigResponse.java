package de.nailsbeauty.whatsappgateway.dto.response;

import java.time.Instant;

/**
 * Response-DTO einer Studio-Konfiguration.
 *
 * <p>Enthaelt bewusst <strong>kein</strong> Access-Token — Secrets werden nie nach aussen gegeben.
 *
 * @param id                 technische ID
 * @param studioId           fachliche Studio-Kennung
 * @param wabaId             WhatsApp Business Account ID
 * @param phoneNumberId      Phone-Number-ID
 * @param displayPhoneNumber Anzeige-Telefonnummer
 * @param active             Aktiv-Status
 * @param createdAt          Anlagezeitpunkt
 * @param updatedAt          letzter Aenderungszeitpunkt
 */
public record StudioConfigResponse(
        Long id,
        String studioId,
        String wabaId,
        String phoneNumberId,
        String displayPhoneNumber,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
