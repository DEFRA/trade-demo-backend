package uk.gov.defra.cdp.trade.demo.domain.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import uk.gov.defra.cdp.trade.demo.domain.Notification;

/**
 * Spring Data MongoDB repository for Notification entity.
 *
 * Provides standard CRUD operations for managing import notifications.
 */
@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
}
