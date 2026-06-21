package de.nailsbeauty.whatsappgateway.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Konfiguration eines Studios (Mandant) fuer die WhatsApp Cloud API.
 *
 * <p>Haelt die im Meta Business Manager manuell geteilten IDs sowie das (verschluesselte)
 * Access-Token. Das Routing eingehender Webhook-Events erfolgt ueber {@link #phoneNumberId}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "studio_config")
public class StudioConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Fachliche, eindeutige Studio-Kennung. */
    @Column(name = "studio_id", nullable = false, unique = true, length = 100)
    private String studioId;

    /** WhatsApp Business Account ID (WABA-ID) des Studios. */
    @Column(name = "waba_id", nullable = false, length = 50)
    private String wabaId;

    /** Phone-Number-ID — Schluessel fuer das Webhook-Routing. */
    @Column(name = "phone_number_id", nullable = false, unique = true, length = 50)
    private String phoneNumberId;

    /** Anzeige-Telefonnummer (z. B. {@code +49 ...}). */
    @Column(name = "display_phone_number", length = 30)
    private String displayPhoneNumber;

    /** Access-Token des Studios — at-rest verschluesselt, nie loggen/ausliefern. */
    @ToString.Exclude
    @Convert(converter = AccessTokenConverter.class)
    @Column(name = "access_token", nullable = false, columnDefinition = "text")
    private String accessToken;

    /**
     * Basis-URL der Backend-Instanz dieses Studios (ohne Trailing-Slash), an die
     * eingehende Nachrichten weitergeleitet werden (z. B.
     * {@code https://studio-berlin.example.com}). {@code null}/leer = keine
     * Weiterleitung (nur Audit/Idempotenz im Gateway).
     */
    @Column(name = "backend_base_url", length = 255)
    private String backendBaseUrl;

    /**
     * Geteiltes Geheimnis fuer den Header {@code X-Gateway-Secret} beim Forwarding
     * an das Studio-Backend — identisch zu dessen
     * {@code app.whatsapp.gateway.inbound-secret}. At-rest verschluesselt, nie
     * loggen/ausliefern.
     */
    @ToString.Exclude
    @Convert(converter = AccessTokenConverter.class)
    @Column(name = "forward_secret", columnDefinition = "text")
    private String forwardSecret;

    /** Ob die Konfiguration aktiv ist (inaktive Studios senden/empfangen nicht). */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
