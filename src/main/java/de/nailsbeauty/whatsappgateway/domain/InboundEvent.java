package de.nailsbeauty.whatsappgateway.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Audit-/Idempotenz-Eintrag fuer ein eingehendes Webhook-Event.
 *
 * <p>Die {@link #metaId} (Message- bzw. Status-ID von Meta) ist eindeutig und dient als
 * Deduplizierungsschluessel: Ein bereits gespeichertes Event wird nicht erneut verarbeitet.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inbound_event")
public class InboundEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Eindeutige ID des Events bei Meta (Message-ID oder Status-ID). */
    @Column(name = "meta_id", nullable = false, unique = true, length = 255)
    private String metaId;

    /** Phone-Number-ID, ueber die das Event eingegangen ist. */
    @Column(name = "phone_number_id", length = 50)
    private String phoneNumberId;

    /** Art des Events (Nachricht oder Status-Update). */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private EventType type;

    /** Roh-Payload des Events als JSON (Audit-Zwecke). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;
}
