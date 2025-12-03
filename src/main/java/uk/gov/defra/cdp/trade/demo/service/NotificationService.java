package uk.gov.defra.cdp.trade.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.defra.cdp.trade.demo.domain.Notification;
import uk.gov.defra.cdp.trade.demo.domain.NotificationDto;
import uk.gov.defra.cdp.trade.demo.domain.repository.NotificationRepository;
import uk.gov.defra.cdp.trade.demo.exceptions.NotFoundException;

/**
 * Service layer for Notification CRUD operations.
 *
 * Handles business logic for managing import notifications (CHEDs),
 * including validation and persistence.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final NotificationIdGeneratorService idGenerator; 

    /**
     * Get all notifications.
     *
     * @return list of all notifications
     */
    public List<Notification> findAll() {
        log.debug("Fetching all notifications");
        List<Notification> notifications = repository.findAll();
        log.debug("Found {} notifications", notifications.size());
        return notifications;
    }

    /**
     * Get a notification by ID.
     *
     * @param id the notification ID
     * @return the notification
     * @throws NotFoundException if the notification does not exist
     */
    public Notification findById(String id) {
        log.debug("Fetching notification with id: {}", id);
        return repository.findById(id)
            .orElseThrow(() -> {
                log.warn("Notification not found with id: {}", id);
                return new NotFoundException("Notification not found with id: " + id);
            });
    }

    /**
     * Save or update a notification based on ID.
     * If an ID is provided in the DTO, the existing notification will be updated.
     * Otherwise, a new notification will be created with a generated ID.
     *
     * @param notificationDto the notification DTO to save or update
     * @return the saved notification
     * @throws NotFoundException if an ID is provided but the notification does not exist
     */
    public Notification saveOrUpdate(NotificationDto notificationDto) {
        if (notificationDto.getId() != null) {
            // UPDATE: ID provided, find and update existing notification
            log.info("Updating notification with id: {}", notificationDto.getId());

            return repository.findById(notificationDto.getId())
                .map(existing -> {
                    updateEntityFromDto(existing, notificationDto);
                    existing.setUpdated(LocalDateTime.now());

                    Notification updated = repository.save(existing);
                    log.info("Updated notification with id: {} and CHED reference: {}",
                        updated.getId(), updated.getChedReference());
                    return updated;
                })
                .orElseThrow(() -> {
                    log.warn("Notification not found with id: {}", notificationDto.getId());
                    return new NotFoundException("Notification not found with id: " + notificationDto.getId());
                });
        } else {
            // CREATE: No ID provided, generate new ID and create notification
            log.info("Creating new notification with CHED reference: {}", notificationDto.getChedReference());

            Notification notification = toEntity(notificationDto);
            notification.setCreated(LocalDateTime.now());
            notification.setUpdated(LocalDateTime.now());

            Notification saved = repository.save(notification);
            log.info("Created notification with id: {} and CHED reference: {}",
                saved.getId(), saved.getChedReference());
            return saved;
        }
    }

    /**
     * Delete a notification.
     *
     * @param id the notification ID
     * @throws NotFoundException if the notification does not exist
     */
    public void delete(String id) {
        log.info("Deleting notification with id: {}", id);

        // Check if notification exists
        findById(id);

        repository.deleteById(id);
        log.info("Deleted notification with id: {}", id);
    }

    /**
     * Convert NotificationDto to Notification entity.
     *
     * @param dto the notification DTO
     * @return the notification entity
     */
    private Notification toEntity(NotificationDto dto) {
        Notification notification = new Notification();
        String generatedId = idGenerator.generateId();
        notification.setId(generatedId);

        notification.setChedReference(dto.getChedReference());
        notification.setOriginCountry(dto.getOriginCountry());
        notification.setCommodity(dto.getCommodity());
        notification.setImportReason(dto.getImportReason());
        notification.setInternalMarketPurpose(dto.getInternalMarketPurpose());
        notification.setTransport(dto.getTransport());
        return notification;
    }

    /**
     * Update Notification entity from NotificationDto.
     *
     * @param entity the notification entity to update
     * @param dto    the notification DTO with updated data
     */
    private void updateEntityFromDto(Notification entity, NotificationDto dto) {
        entity.setChedReference(dto.getChedReference());
        entity.setOriginCountry(dto.getOriginCountry());
        entity.setCommodity(dto.getCommodity());
        entity.setImportReason(dto.getImportReason());
        entity.setInternalMarketPurpose(dto.getInternalMarketPurpose());
        entity.setTransport(dto.getTransport());
    }
}
