package de.nailsbeauty.whatsappgateway.repository;

import de.nailsbeauty.whatsappgateway.domain.InboundEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring-Data-Repository fuer {@link InboundEvent} (Idempotenz/Audit).
 */
public interface InboundEventRepository extends JpaRepository<InboundEvent, Long> {

    /**
     * Prueft, ob ein Event mit dieser Meta-ID bereits gespeichert wurde.
     *
     * @param metaId eindeutige Event-ID von Meta
     * @return {@code true}, wenn das Event bereits verarbeitet wurde
     */
    boolean existsByMetaId(String metaId);
}
