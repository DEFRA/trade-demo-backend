package uk.gov.defra.cdp.trade.demo.controller;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.defra.cdp.trade.demo.domain.Notification;
import uk.gov.defra.cdp.trade.demo.domain.NotificationDto;
import uk.gov.defra.cdp.trade.demo.service.NotificationService;

/**
 * REST API for Notification (CHED) operations.
 * <p>
 * Provides endpoints for managing import notifications submitted through the trade-demo-frontend
 * application.
 */
@RestController
@RequestMapping("/notifications")
@Tag(name = "Notification API", description = "Manage import notifications (CHEDs)")
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Save or update a notification. If the DTO contains an ID, the existing notification will be
     * updated. Otherwise, a new notification will be created with a generated ID.
     *
     * @param notificationDto the notification data to save or update
     * @return the saved notification
     */
    @PutMapping
    @Operation(summary = "Save or update notification", description = "Creates a new notification or updates an existing one based on ID")
    @Timed("PutNotificationUpsert")
    public Notification saveOrUpdate(@Valid @RequestBody NotificationDto notificationDto) {
        log.info(
            "PUT /notifications - Saving or updating notification (ID: {}, CHED reference: {})",
            notificationDto.getId(), notificationDto.getChedReference());
        return notificationService.saveOrUpdate(notificationDto);
    }

    /**
     * Get all notifications.
     *
     * @return list of all notifications
     */
    @GetMapping
    @Operation(summary = "List notifications", description = "Returns all import notifications")
    @Timed("GetAllNotifications")
    public List<Notification> findAll() {
        log.debug("GET /notifications - Fetching all notifications");
        return notificationService.findAll();
    }

    /**
     * Get a notification by ID.
     *
     * @param id the notification ID
     * @return the notification
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get notification by ID", description = "Returns a single notification by ID")
    @Timed("GetNotification")
    public Notification findById(@PathVariable String id) {
        log.debug("GET /notifications/{} - Fetching notification", id);
        return notificationService.findById(id);
    }

    /**
     * Delete a notification.
     *
     * @param id the notification ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete notification", description = "Deletes a notification by ID")
    @Timed("DeleteNotification")
    public void delete(@PathVariable String id) {
        log.info("DELETE /notifications/{} - Deleting notification", id);
        notificationService.delete(id);
    }

    /**
     * Submit a notification to IPAFFS. First saves/updates the notification, then submits to
     * IPAFFS. If the notification doesn't have an ID, one will be generated during save. The
     * notification will be mapped to CHEDA format and submitted to IPAFFS. On successful
     * submission, the CHED reference is stored and status is set to SUBMITTED.
     *
     * @param notificationDto the notification data to submit
     * @return the submitted notification with CHED reference
     */
    @PostMapping("/submit")
    @Operation(summary = "Submit notification to IPAFFS",
        description = "Submits notification to IPAFFS and returns CHED reference")
    @Timed("SubmitNotification")
    public Notification submit(@Valid @RequestBody NotificationDto notificationDto) {
        log.info("POST /notifications/submit - Submitting notification (ID: {})",
            notificationDto.getId());

        // Save/update notification first (handles ID generation if needed)
        Notification savedNotification = notificationService.saveOrUpdate(notificationDto);

        // Submit to IPAFFS using the ID
        return notificationService.submitNotification(savedNotification.getId());
    }
}
