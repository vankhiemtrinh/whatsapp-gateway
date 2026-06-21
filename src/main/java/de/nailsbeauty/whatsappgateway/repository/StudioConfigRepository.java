package de.nailsbeauty.whatsappgateway.repository;

import de.nailsbeauty.whatsappgateway.domain.StudioConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring-Data-Repository fuer {@link StudioConfig}.
 */
public interface StudioConfigRepository extends JpaRepository<StudioConfig, Long> {

    /**
     * Sucht eine Studio-Konfiguration anhand der fachlichen Studio-Kennung.
     *
     * @param studioId fachliche Studio-Kennung
     * @return die Konfiguration, falls vorhanden
     */
    Optional<StudioConfig> findByStudioId(String studioId);

    /**
     * Sucht eine Studio-Konfiguration anhand der Phone-Number-ID (Webhook-Routing).
     *
     * @param phoneNumberId Phone-Number-ID aus dem eingehenden Event
     * @return die Konfiguration, falls vorhanden
     */
    Optional<StudioConfig> findByPhoneNumberId(String phoneNumberId);

    /**
     * Prueft, ob bereits eine Konfiguration mit dieser Studio-Kennung existiert.
     *
     * @param studioId fachliche Studio-Kennung
     * @return {@code true}, wenn vorhanden
     */
    boolean existsByStudioId(String studioId);

    /**
     * Prueft, ob bereits eine Konfiguration mit dieser Phone-Number-ID existiert.
     *
     * @param phoneNumberId Phone-Number-ID
     * @return {@code true}, wenn vorhanden
     */
    boolean existsByPhoneNumberId(String phoneNumberId);
}
