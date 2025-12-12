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
import uk.gov.defra.cdp.trade.demo.domain.Transport;
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
        NotificationDto updateDto = createNotificationDto(createdId,
            "France"); // Provide ID for update
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
        NotificationDto updateDto = createNotificationDto(created.getId(),
            "Belgium"); // Provide ID for update
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

    // ========================================
    // Notification Submission Tests
    // ========================================

    @Test
    void submit_shouldSubmitNewNotificationSuccessfully() {
        // Given - new notification without ID
        NotificationDto dto = createNotificationDto(null, "United Kingdom", null);
        
        // When - submit notification
        EntityExchangeResult<Notification> result = webClient("NoAuth")
            .post()
            .uri(NOTIFICATIONS_ENDPOINT + "/submit")
            .bodyValue(dto)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Notification.class)
            .returnResult();

        // Then
        Notification submitted = result.getResponseBody();
        assertThat(submitted).isNotNull();
        assertThat(submitted.getId()).isNotNull();
        assertThat(submitted.getStatus()).isEqualTo("SUBMITTED");
        assertThat(submitted.getChedReference()).isNotNull();
        assertThat(submitted.getChedReference()).startsWith("CHEDA.");
        assertThat(submitted.getOriginCountry()).isEqualTo("United Kingdom");

        // Verify notification is persisted in database
        Notification persisted = notificationRepository.findById(submitted.getId()).orElse(null);
        assertThat(persisted).isNotNull();
        assertThat(persisted.getStatus()).isEqualTo("SUBMITTED");
        assertThat(persisted.getChedReference()).isEqualTo(submitted.getChedReference());
    }

    @Test
    void submit_shouldSubmitExistingNotificationSuccessfully() {
        // Given - create a notification first
        NotificationDto createDto = createNotificationDto(null, "Ireland", null);
        EntityExchangeResult<Notification> createResult = webClient("NoAuth")
            .put()
            .uri(NOTIFICATIONS_ENDPOINT)
            .bodyValue(createDto)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Notification.class)
            .returnResult();

        String notificationId = createResult.getResponseBody().getId();

        // When - submit the existing notification
        NotificationDto submitDto = createNotificationDto(notificationId, "Ireland", null);

        EntityExchangeResult<Notification> result = webClient("NoAuth")
            .post()
            .uri(NOTIFICATIONS_ENDPOINT + "/submit")
            .bodyValue(submitDto)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Notification.class)
            .returnResult();

        // Then
        Notification submitted = result.getResponseBody();
        assertThat(submitted).isNotNull();
        assertThat(submitted.getId()).isEqualTo(notificationId);
        assertThat(submitted.getStatus()).isEqualTo("SUBMITTED");
        assertThat(submitted.getChedReference()).isNotNull();
        assertThat(submitted.getChedReference()).startsWith("CHEDA.");

        // Verify only one notification exists
        assertThat(findAllNotifications()).hasSize(1);
    }

    @Test
    void submit_shouldGenerateChedReferenceFromNotificationId() {
        // Given - new notification
        NotificationDto dto = createNotificationDto(null, "France", null);

        // When - submit notification
        EntityExchangeResult<Notification> result = webClient("NoAuth")
            .post()
            .uri(NOTIFICATIONS_ENDPOINT + "/submit")
            .bodyValue(dto)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Notification.class)
            .returnResult();

        // Then - verify CHED reference format
        Notification submitted = result.getResponseBody();
        assertThat(submitted).isNotNull();
        assertThat(submitted.getId()).isNotNull();
        assertThat(submitted.getChedReference()).isNotNull();

        // CHED reference should follow pattern: CHEDA.YYYY.MMDDSSSS
        // Example: CDP.2025.12.09.1 -> CHEDA.2025.12090100
        assertThat(submitted.getChedReference()).matches("CHEDA\\.\\d{4}\\.\\d{8,9}");

        log.info("Notification ID: {}", submitted.getId());
        log.info("Generated CHED reference: {}", submitted.getChedReference());
    }

    @Test
    void submit_shouldPreventResubmission_whenAlreadySubmitted() {
        // Given - create and submit a notification
        NotificationDto dto = createNotificationDto(null, "Spain", null);

        EntityExchangeResult<Notification> firstSubmit = webClient("NoAuth")
            .post()
            .uri(NOTIFICATIONS_ENDPOINT + "/submit")
            .bodyValue(dto)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Notification.class)
            .returnResult();

        String notificationId = firstSubmit.getResponseBody().getId();
        String originalChedRef = firstSubmit.getResponseBody().getChedReference();

        // When - attempt to resubmit the same notification
        NotificationDto resubmitDto = createNotificationDto(notificationId, "Spain",
            originalChedRef);

        // Then - expect error
        webClient("NoAuth")
            .post()
            .uri(NOTIFICATIONS_ENDPOINT + "/submit")
            .bodyValue(resubmitDto)
            .exchange()
            .expectStatus().is5xxServerError();

        // Verify the notification still has original CHED reference and status
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        assertThat(notification).isNotNull();
        assertThat(notification.getStatus()).isEqualTo("SUBMITTED");
        assertThat(notification.getChedReference()).isEqualTo(originalChedRef);
    }

    @Test
    void submit_shouldUpdateNotificationWithChedReferenceAndStatus() {
        // Given - new notification
        NotificationDto dto = createNotificationDto(null, "Germany", null);

        // When - submit notification
        EntityExchangeResult<Notification> result = webClient("NoAuth")
            .post()
            .uri(NOTIFICATIONS_ENDPOINT + "/submit")
            .bodyValue(dto)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Notification.class)
            .returnResult();

        // Then - verify notification was saved with CHED reference and status
        Notification submitted = result.getResponseBody();
        assertThat(submitted).isNotNull();
        assertThat(submitted.getChedReference()).isNotNull();
        assertThat(submitted.getStatus()).isEqualTo("SUBMITTED");
        assertThat(submitted.getUpdated()).isNotNull();

        // Verify in database
        Notification persisted = notificationRepository.findById(submitted.getId()).orElse(null);
        assertThat(persisted).isNotNull();
        assertThat(persisted.getChedReference()).isEqualTo(submitted.getChedReference());
        assertThat(persisted.getStatus()).isEqualTo("SUBMITTED");
    }

    @Test
    void submit_shouldWorkInFullSubmissionFlow() {
        // 1. Create notification as draft
        NotificationDto draftDto = createNotificationDto(null, "Belgium", null);

        EntityExchangeResult<Notification> draftResult = webClient("NoAuth")
            .put()
            .uri(NOTIFICATIONS_ENDPOINT)
            .bodyValue(draftDto)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Notification.class)
            .returnResult();

        Notification draft = draftResult.getResponseBody();
        assertThat(draft.getId()).isNotNull();
        assertThat(draft.getChedReference()).isNull(); // No CHED reference yet

        // 2. Update the draft notification
        NotificationDto updateDto = createNotificationDto(draft.getId(), "Belgium", null);
        updateDto.setImportReason("re-entry");

        webClient("NoAuth")
            .put()
            .uri(NOTIFICATIONS_ENDPOINT)
            .bodyValue(updateDto)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Notification.class)
            .value(updated -> {
                assertThat(updated.getId()).isEqualTo(draft.getId());
                assertThat(updated.getImportReason()).isEqualTo("re-entry");
            });

        // 3. Submit the notification
        NotificationDto submitDto = createNotificationDto(draft.getId(), "Belgium", null);
        submitDto.setImportReason("re-entry");

        EntityExchangeResult<Notification> submitResult = webClient("NoAuth")
            .post()
            .uri(NOTIFICATIONS_ENDPOINT + "/submit")
            .bodyValue(submitDto)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Notification.class)
            .returnResult();

        Notification submitted = submitResult.getResponseBody();
        assertThat(submitted.getId()).isEqualTo(draft.getId()); // Same ID
        assertThat(submitted.getStatus()).isEqualTo("SUBMITTED");
        assertThat(submitted.getChedReference()).isNotNull();
        assertThat(submitted.getChedReference()).startsWith("CHEDA.");

        // 4. Verify final state in database
        Notification finalNotification = notificationRepository.findById(draft.getId())
            .orElse(null);
        assertThat(finalNotification).isNotNull();
        assertThat(finalNotification.getStatus()).isEqualTo("SUBMITTED");
        assertThat(finalNotification.getChedReference()).isEqualTo(submitted.getChedReference());
        assertThat(finalNotification.getImportReason()).isEqualTo("re-entry");

        // 5. Verify only one notification exists
        assertThat(findAllNotifications()).hasSize(1);
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

    private NotificationDto createNotificationDto(String id, String originCountry,
        String chedReference) {
        Species species = new Species();
        species.setName("Cattle");
        species.setNoOfAnimals(10);
        species.setNoOfPackages(2);

        Commodity commodity = new Commodity();
        commodity.setCode("0102");
        commodity.setDescription("Live bovine animals");
        commodity.setSpecies(Collections.singletonList(species));

        Transport transport = new Transport();
        transport.setBcpCode("GBLHR1");
        transport.setTransportToBcp("road");
        transport.setVehicleId("ABC123");

        NotificationDto dto = new NotificationDto();
        dto.setId(id);
        dto.setChedReference(chedReference);
        dto.setOriginCountry(originCountry);
        dto.setCommodity(commodity);
        dto.setImportReason("internalmarket");
        dto.setInternalMarketPurpose("breeding");
        dto.setTransport(transport);

        return dto;
    }
}
