package de.nailsbeauty.whatsappgateway.service;

import de.nailsbeauty.whatsappgateway.domain.EventType;
import de.nailsbeauty.whatsappgateway.domain.InboundEvent;
import de.nailsbeauty.whatsappgateway.repository.InboundEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persistiert eingehende Events idempotent und stellt damit die Deduplizierung sicher.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboundEventService {

    private final InboundEventRepository repository;

    /**
     * Speichert ein Event genau dann, wenn dessen Meta-ID noch nicht bekannt ist.
     *
     * <p>Laeuft in einer eigenen Transaktion ({@code REQUIRES_NEW}), damit ein Duplikat-Konflikt
     * einzelne Events isoliert und nicht den gesamten Webhook-Batch zurueckrollt. Ein paralleler
     * Insert derselben ID wird ueber den Unique-Constraint abgefangen.
     *
     * @param metaId        eindeutige Event-ID von Meta
     * @param phoneNumberId Phone-Number-ID des Events
     * @param type          Art des Events
     * @param payload       Roh-Payload als JSON (Audit)
     * @return {@code true}, wenn das Event neu war und gespeichert wurde; {@code false} bei Duplikat
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recordIfNew(String metaId, String phoneNumberId, EventType type, String payload) {
        if (metaId == null || metaId.isBlank()) {
            log.warn("Event ohne Meta-ID ({}) — wird ignoriert", type);
            return false;
        }
        if (repository.existsByMetaId(metaId)) {
            log.debug("Duplikat ignoriert: metaId={}", metaId);
            return false;
        }
        var event = new InboundEvent();
        event.setMetaId(metaId);
        event.setPhoneNumberId(phoneNumberId);
        event.setType(type);
        event.setPayload(payload);
        try {
            repository.save(event);
            return true;
        } catch (DataIntegrityViolationException e) {
            // Race: zwischen existsByMetaId und save hat ein paralleler Thread dieselbe ID gespeichert.
            log.debug("Duplikat (Race) ignoriert: metaId={}", metaId);
            return false;
        }
    }
}
