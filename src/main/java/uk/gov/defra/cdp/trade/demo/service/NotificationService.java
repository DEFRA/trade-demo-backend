package uk.gov.defra.cdp.trade.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.defra.cdp.trade.demo.client.IpaffsNotificationClient;
import uk.gov.defra.cdp.trade.demo.domain.Notification;
import uk.gov.defra.cdp.trade.demo.domain.NotificationDto;
import uk.gov.defra.cdp.trade.demo.domain.ipaffs.IpaffsNotification;
import uk.gov.defra.cdp.trade.demo.domain.repository.NotificationRepository;
import uk.gov.defra.cdp.trade.demo.exceptions.NotFoundException;
import uk.gov.defra.cdp.trade.demo.exceptions.NotificationSubmissionException;
import uk.gov.defra.cdp.trade.demo.mapper.IpaffsNotificationMapper;

/**
 * Service layer for Notification CRUD operations.
 * <p>
 * Handles business logic for managing import notifications (CHEDs), including validation and
 * persistence.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final NotificationIdGeneratorService idGenerator;
    private final IpaffsNotificationMapper ipaffsNotificationMapper;
    private final IpaffsNotificationClient ipaffsNotificationClient;
    private final ObjectMapper objectMapper;

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
     * Save or update a notification based on ID. If an ID is provided in the DTO, the existing
     * notification will be updated. Otherwise, a new notification will be created with a generated
     * ID.
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
                    if ("SUBMITTED".equals(existing.getStatus())) {
                        log.error("Notification {} is already submitted", notificationDto.getId());
                        throw new NotificationSubmissionException(
                            "Notification already submitted: " + notificationDto.getId());
                    }
                    updateEntityFromDto(existing, notificationDto);
                    existing.setUpdated(LocalDateTime.now());

                    Notification updated = repository.save(existing);
                    log.info("Updated notification with id: {} and CHED reference: {}",
                        updated.getId(), updated.getChedReference());
                    return updated;
                })
                .orElseThrow(() -> {
                    log.warn("Notification not found with id: {}", notificationDto.getId());
                    return new NotFoundException(
                        "Notification not found with id: " + notificationDto.getId());
                });
        } else {
            // CREATE: No ID provided, generate new ID and create notification
            log.info("Creating new notification with CHED reference: {}",
                notificationDto.getChedReference());

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
     * Submit a notification to IPAFFS.
     * <p>
     * This method: 1. Loads the notification by ID (must exist) 2. Prevents resubmission of already
     * submitted notifications 3. Maps the notification to IPAFFS CHEDA format 4. Submits to IPAFFS
     * and receives CHED reference 5. Updates the notification with CHED reference and SUBMITTED
     * status
     *
     * @param id the notification ID to submit
     * @return the submitted notification with CHED reference
     * @throws NotFoundException               if notification not found
     * @throws NotificationSubmissionException if submission fails or already submitted
     */
    public Notification submitNotification(String id) {
        log.debug("Submitting notification with id: {}", id);

        // Step 1: Load notification by ID
        Notification notification = findById(id);

        // Step 2: Prevent resubmission if already submitted
        if ("SUBMITTED".equals(notification.getStatus())) {
            log.error("Notification {} is already submitted", id);
            throw new NotificationSubmissionException(
                "Notification already submitted: " + id);
        }

        try {
            // Step 3: Map to IPAFFS format
            log.info("Mapping notification {} to IPAFFS CHEDA format", id);
            IpaffsNotification ipaffsNotification = ipaffsNotificationMapper.mapToIpaffsNotification(
                notification);
            
            log.info("IPAFFS notification is: {}", objectMapper.writeValueAsString(ipaffsNotification));

            // Step 4: Submit to IPAFFS
            log.info("Submitting notification {} to IPAFFS", id);
            ResponseEntity<String> submissionResponse = ipaffsNotificationClient
                .submitNotification(ipaffsNotification, notification.getId());
            if (!submissionResponse.getStatusCode().is2xxSuccessful()) {
                throw new NotificationSubmissionException(
                    "Failed to submit notification to IPAFFS. Status: "
                        + submissionResponse.getStatusCode()
                        + ", Body: " + submissionResponse.getBody());
            }
            String chedReference = submissionResponse.getBody();
            log.info("IPAFFS submission successful. CHED reference: {}", chedReference);

            // Step 5: Update notification with CHED reference and SUBMITTED status
            notification.setChedReference(chedReference);
            notification.setStatus("SUBMITTED");
            notification.setUpdated(LocalDateTime.now());

            Notification submittedNotification = repository.save(notification);
            log.info("Notification {} submitted successfully with CHED reference: {}",
                id, chedReference);

            return submittedNotification;
        } catch (Exception e) {
            log.error("Failed to submit notification {} to IPAFFS", id, e);
            throw new NotificationSubmissionException(
                "Failed to submit notification to IPAFFS: " + e.getMessage(), e);
        }
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

        setNotificationDetails(dto, notification);
        return notification;
    }

    /**
     * Update Notification entity from NotificationDto.
     *
     * @param entity the notification entity to update
     * @param dto    the notification DTO with updated data
     */
    private void updateEntityFromDto(Notification entity, NotificationDto dto) {
        setNotificationDetails(dto, entity);
    }

    private void setNotificationDetails(NotificationDto dto, Notification notification) {
        notification.setChedReference(dto.getChedReference());
        notification.setStatus("DRAFT");
        notification.setOriginCountry(dto.getOriginCountry());
        notification.setCommodity(dto.getCommodity());
        notification.setImportReason(dto.getImportReason());
        notification.setInternalMarketPurpose(dto.getInternalMarketPurpose());
        notification.setTransport(dto.getTransport());
    }
}
