package uk.gov.defra.cdp.trade.demo.controller;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import uk.gov.defra.cdp.trade.demo.domain.Notification;
import uk.gov.defra.cdp.trade.demo.domain.NotificationDto;
import uk.gov.defra.cdp.trade.demo.service.NotificationService;

import java.util.List;

/**
 * REST API for Notification (CHED) operations.
 *
 * Provides endpoints for managing import notifications submitted
 * through the trade-demo-frontend application.
 */
@RestController
@RequestMapping("/notifications")
@Tag(name = "Notification API", description = "Manage import notifications (CHEDs)")
@Slf4j
@AllArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Save or update a notification.
     * If the DTO contains an ID, the existing notification will be updated.
     * Otherwise, a new notification will be created with a generated ID.
     *
     * @param notificationDto the notification data to save or update
     * @return the saved notification
     */
    @PutMapping
    @Operation(summary = "Save or update notification", description = "Creates a new notification or updates an existing one based on ID")
    @Timed("PutNotificationUpsert")
    public Notification saveOrUpdate(@Valid @RequestBody NotificationDto notificationDto) {
        log.info("PUT /notifications - Saving or updating notification (ID: {}, CHED reference: {})",
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
}
