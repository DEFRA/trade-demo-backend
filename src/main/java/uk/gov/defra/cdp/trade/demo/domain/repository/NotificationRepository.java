package uk.gov.defra.cdp.trade.demo.domain.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import uk.gov.defra.cdp.trade.demo.domain.Notification;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for Notification entity.
 *
 * Provides standard CRUD operations plus custom query methods for
 * managing import notifications (CHEDs).
 */
@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    /**
     * Find a notification by CHED reference.
     * Used to enforce a unique CHED reference constraint at the application level.
     *
     * @param chedReference the CHED reference to search for
     * @return Optional containing the notification if found
     */
    Optional<Notification> findByChedReference(String chedReference);
}
