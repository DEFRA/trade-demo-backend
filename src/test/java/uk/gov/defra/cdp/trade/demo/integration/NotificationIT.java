package uk.gov.defra.cdp.trade.demo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import uk.gov.defra.cdp.trade.demo.domain.Commodity;
import uk.gov.defra.cdp.trade.demo.domain.Notification;
import uk.gov.defra.cdp.trade.demo.domain.NotificationDto;
import uk.gov.defra.cdp.trade.demo.domain.Species;
import uk.gov.defra.cdp.trade.demo.domain.repository.NotificationRepository;

@Slf4j
class NotificationIT extends IntegrationBase {

    private static final String NOTIFICATIONS_ENDPOINT = "/notifications";

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void saveOrUpdate_shouldCreateNewNotification() {
        // Given
        NotificationDto dto = createNotificationDto(null, "United Kingdom"); // No ID - create new

        // When
        EntityExchangeResult<Notification> result = webClient("NoAuth")
            .put()
            .uri(NOTIFICATIONS_ENDPOINT)
            .bodyValue(dto)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Notification.class)
            .returnResult();

        // Then
        Notification notification = result.getResponseBody();
        assertThat(notification).isNotNull();
        assertThat(notification.getId()).isNotNull();
        assertThat(notification.getChedReference()).isEqualTo("CHED-NEW");
        assertThat(notification.getOriginCountry()).isEqualTo("United Kingdom");
        assertThat(notification.getCreated()).isNotNull();
        assertThat(notification.getUpdated()).isNotNull();
        assertThat(notification.getCommodity()).isNotNull();
        assertThat(notification.getCommodity().getCode()).isEqualTo("0102");
    }

    @Test
    void saveOrUpdate_shouldUpdateExistingNotification() {
        // Given - create initial notification
        NotificationDto initialDto = createNotificationDto(null, "Ireland"); // Create new
        EntityExchangeResult<Notification> createResult = webClient("NoAuth")
            .put()
            .uri(NOTIFICATIONS_ENDPOINT)
            .bodyValue(initialDto)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Notification.class)
            .returnResult();

        String createdId = createResult.getResponseBody().getId();

        // When - update using the ID
        NotificationDto updateDto = createNotificationDto(createdId, "France"); // Provide ID for update
        updateDto.setChedReference("CHED-UPDATED");
        updateDto.setImportReason("re-entry");

        EntityExchangeResult<Notification> result = webClient("NoAuth")
            .put()
            .uri(NOTIFICATIONS_ENDPOINT)
            .bodyValue(updateDto)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Notification.class)
            .returnResult();

        // Then
        Notification updated = result.getResponseBody();
        assertThat(updated).isNotNull();
        assertThat(updated.getId()).isEqualTo(createdId);
        assertThat(updated.getChedReference()).isEqualTo("CHED-UPDATED");
        assertThat(updated.getOriginCountry()).isEqualTo("France");
        assertThat(updated.getImportReason()).isEqualTo("re-entry");
        assertThat(updated.getUpdated()).isNotNull();

        // Verify only one notification exists
        assertThat(findAllNotifications()).hasSize(1);
    }

    @Test
    void findAll_shouldReturnAllNotifications() {
        // Given - create multiple notifications
        NotificationDto dto1 = createNotificationDto(null, "United Kingdom", "CHED-UK-001");
        NotificationDto dto2 = createNotificationDto(null, "Ireland", "CHED-IE-002");
        NotificationDto dto3 = createNotificationDto(null, "France", "CHED-FR-003");

        webClient("NoAuth").put().uri(NOTIFICATIONS_ENDPOINT).bodyValue(dto1).exchange();
        webClient("NoAuth").put().uri(NOTIFICATIONS_ENDPOINT).bodyValue(dto2).exchange();
        webClient("NoAuth").put().uri(NOTIFICATIONS_ENDPOINT).bodyValue(dto3).exchange();

        // When
        List<Notification> notifications = findAllNotifications();

        // Then
        assertThat(notifications).isNotNull().hasSize(3);
        assertThat(notifications)
            .extracting(Notification::getId)
            .allMatch(id -> id != null && !id.isEmpty());
    }

    @Test
    void findById_shouldReturnNotification() {
        // Given - create a notification
        NotificationDto dto = createNotificationDto(null, "Spain");
        EntityExchangeResult<Notification> createResult = webClient("NoAuth")
            .put()
            .uri(NOTIFICATIONS_ENDPOINT)
            .bodyValue(dto)
            .exchange()
            .expectBody(Notification.class)
            .returnResult();

        String id = createResult.getResponseBody().getId();

        // When
        EntityExchangeResult<Notification> result = webClient("NoAuth")
            .get()
            .uri(NOTIFICATIONS_ENDPOINT + "/{id}", id)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Notification.class)
            .returnResult();

        // Then
        Notification notification = result.getResponseBody();
        assertThat(notification).isNotNull();
        assertThat(notification.getId()).isEqualTo(id);
        assertThat(notification.getOriginCountry()).isEqualTo("Spain");
    }

    @Test
    void findById_shouldReturn404_whenNotExists() {
        // When/Then
        webClient("NoAuth")
            .get()
            .uri(NOTIFICATIONS_ENDPOINT + "/{id}", "non-existent-id")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void delete_shouldRemoveNotification() {
        // Given - create a notification
        NotificationDto dto = createNotificationDto(null, "Italy");
        EntityExchangeResult<Notification> createResult = webClient("NoAuth")
            .put()
            .uri(NOTIFICATIONS_ENDPOINT)
            .bodyValue(dto)
            .exchange()
            .expectBody(Notification.class)
            .returnResult();

        String id = createResult.getResponseBody().getId();

        // When
        webClient("NoAuth")
            .delete()
            .uri(NOTIFICATIONS_ENDPOINT + "/{id}", id)
            .exchange()
            .expectStatus().isNoContent();

        // Then - verify it's deleted
        webClient("NoAuth")
            .get()
            .uri(NOTIFICATIONS_ENDPOINT + "/{id}", id)
            .exchange()
            .expectStatus().isNotFound();

        assertThat(findAllNotifications()).isEmpty();
    }

    @Test
    void delete_shouldReturn404_whenNotExists() {
        // When/Then
        webClient("NoAuth")
            .delete()
            .uri(NOTIFICATIONS_ENDPOINT + "/{id}", "non-existent-id")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void fullCrudFlow_shouldWorkEndToEnd() {
        // 1. Create notification
        NotificationDto createDto = createNotificationDto(null, "Netherlands");
        EntityExchangeResult<Notification> createResult = webClient("NoAuth")
            .put()
            .uri(NOTIFICATIONS_ENDPOINT)
            .bodyValue(createDto)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Notification.class)
            .returnResult();

        Notification created = createResult.getResponseBody();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getChedReference()).isEqualTo("CHED-NEW");
        assertThat(created.getOriginCountry()).isEqualTo("Netherlands");

        // 2. Find by ID
        webClient("NoAuth")
            .get()
            .uri(NOTIFICATIONS_ENDPOINT + "/{id}", created.getId())
            .exchange()
            .expectStatus().isOk()
            .expectBody(Notification.class)
            .value(notification -> {
                assertThat(notification.getId()).isEqualTo(created.getId());
                assertThat(notification.getOriginCountry()).isEqualTo("Netherlands");
            });

        // 3. Update notification using ID
        NotificationDto updateDto = createNotificationDto(created.getId(), "Belgium"); // Provide ID for update
        updateDto.setChedReference("CHED-UPDATED");
        updateDto.setImportReason("re-entry");
        updateDto.setInternalMarketPurpose("slaughter");

        EntityExchangeResult<Notification> updateResult = webClient("NoAuth")
            .put()
            .uri(NOTIFICATIONS_ENDPOINT)
            .bodyValue(updateDto)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Notification.class)
            .returnResult();

        Notification updated = updateResult.getResponseBody();
        assertThat(updated.getId()).isEqualTo(created.getId()); // Same ID
        assertThat(updated.getChedReference()).isEqualTo("CHED-UPDATED"); // CHED reference updated
        assertThat(updated.getOriginCountry()).isEqualTo("Belgium"); // Updated value
        assertThat(updated.getImportReason()).isEqualTo("re-entry"); // Updated value

        // 4. Verify only one notification exists
        assertThat(findAllNotifications()).hasSize(1);

        // 5. Delete notification
        webClient("NoAuth")
            .delete()
            .uri(NOTIFICATIONS_ENDPOINT + "/{id}", created.getId())
            .exchange()
            .expectStatus().isNoContent();

        // 6. Verify deletion
        webClient("NoAuth")
            .get()
            .uri(NOTIFICATIONS_ENDPOINT + "/{id}", created.getId())
            .exchange()
            .expectStatus().isNotFound();

        assertThat(notificationRepository.count()).isEqualTo(0);
    }

    @Test
    void saveOrUpdate_shouldAllowMultipleNotificationsWithNullChedReference() {
        // Given - create multiple notifications with null chedReference
        NotificationDto dto1 = createNotificationDto(null, "United Kingdom", null);
        NotificationDto dto2 = createNotificationDto(null, "Ireland", null);
        NotificationDto dto3 = createNotificationDto(null, "France", null);

        // When - save all notifications with null chedReference
        webClient("NoAuth")
            .put()
            .uri(NOTIFICATIONS_ENDPOINT)
            .bodyValue(dto1)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Notification.class)
            .returnResult();

        webClient("NoAuth")
            .put()
            .uri(NOTIFICATIONS_ENDPOINT)
            .bodyValue(dto2)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Notification.class)
            .returnResult();

        webClient("NoAuth")
            .put()
            .uri(NOTIFICATIONS_ENDPOINT)
            .bodyValue(dto3)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Notification.class)
            .returnResult();

        // Then - verify all notifications were created with null chedReference
        List<Notification> allNotifications = findAllNotifications();
        assertThat(allNotifications).hasSize(3);
        assertThat(allNotifications)
            .extracting(Notification::getChedReference)
            .containsOnly((String) null);
        assertThat(allNotifications)
            .extracting(Notification::getOriginCountry)
            .containsExactlyInAnyOrder("United Kingdom", "Ireland", "France");
    }

    @Test
    void saveOrUpdate_shouldRejectDuplicateChedReference() {
        // Given - create first notification with a specific chedReference
        String duplicateChedRef = "CHED-DUPLICATE-001";
        NotificationDto dto1 = createNotificationDto(null, "United Kingdom", duplicateChedRef);

        webClient("NoAuth")
            .put()
            .uri(NOTIFICATIONS_ENDPOINT)
            .bodyValue(dto1)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Notification.class)
            .returnResult();

        // When - attempt to create second notification with same chedReference
        NotificationDto dto2 = createNotificationDto(null, "Ireland", duplicateChedRef);

        // Then - expect failure due to duplicate chedReference
        webClient("NoAuth")
            .put()
            .uri(NOTIFICATIONS_ENDPOINT)
            .bodyValue(dto2)
            .exchange()
            .expectStatus().is5xxServerError(); // MongoDB will throw duplicate key error

        // Verify only the first notification was saved
        List<Notification> allNotifications = findAllNotifications();
        assertThat(allNotifications).hasSize(1);
        assertThat(allNotifications.get(0).getChedReference()).isEqualTo(duplicateChedRef);
        assertThat(allNotifications.get(0).getOriginCountry()).isEqualTo("United Kingdom");
    }
    
    private List<Notification> findAllNotifications() {
        return webClient("NoAuth")
            .get()
            .uri(NOTIFICATIONS_ENDPOINT)
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(Notification.class)
            .returnResult().getResponseBody();
    }

    // Helper methods
    private NotificationDto createNotificationDto(String id, String originCountry) {
        return createNotificationDto(id, originCountry, id != null ? "CHED-" + id : "CHED-NEW");
    }

    private NotificationDto createNotificationDto(String id, String originCountry, String chedReference) {
        Species species = new Species();
        species.setName("Cattle");
        species.setNoOfAnimals(10);
        species.setNoOfPackages(2);

        Commodity commodity = new Commodity();
        commodity.setCode("0102");
        commodity.setDescription("Live bovine animals");
        commodity.setSpecies(Collections.singletonList(species));

        NotificationDto dto = new NotificationDto();
        dto.setId(id);
        dto.setChedReference(chedReference);
        dto.setOriginCountry(originCountry);
        dto.setCommodity(commodity);
        dto.setImportReason("internalmarket");
        dto.setInternalMarketPurpose("breeding");

        return dto;
    }
}
