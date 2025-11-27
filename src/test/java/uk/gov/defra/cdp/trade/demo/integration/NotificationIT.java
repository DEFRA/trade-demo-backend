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
        // Clean up MongoDB before each test
        notificationRepository.deleteAll();
    }

    @Test
    void saveOrUpdate_shouldCreateNewNotification() {
        // Given
        NotificationDto dto = createNotificationDto("CHEDP.GB.2024.1234567", "United Kingdom");

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
        assertThat(notification.getChedReference()).isEqualTo("CHEDP.GB.2024.1234567");
        assertThat(notification.getOriginCountry()).isEqualTo("United Kingdom");
        assertThat(notification.getCreated()).isNotNull();
        assertThat(notification.getUpdated()).isNotNull();
        assertThat(notification.getCommodity()).isNotNull();
        assertThat(notification.getCommodity().getCode()).isEqualTo("0102");
    }

    @Test
    void saveOrUpdate_shouldUpdateExistingNotification() {
        // Given - create initial notification
        NotificationDto initialDto = createNotificationDto("CHEDP.GB.2024.7777777", "Ireland");
        webClient("NoAuth")
            .put()
            .uri(NOTIFICATIONS_ENDPOINT)
            .bodyValue(initialDto)
            .exchange()
            .expectStatus().isOk();

        // When - update with same CHED reference but different data
        NotificationDto updateDto = createNotificationDto("CHEDP.GB.2024.7777777", "France");
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
        assertThat(updated.getChedReference()).isEqualTo("CHEDP.GB.2024.7777777");
        assertThat(updated.getOriginCountry()).isEqualTo("France");
        assertThat(updated.getImportReason()).isEqualTo("re-entry");
        assertThat(updated.getUpdated()).isNotNull();

        // Verify only one notification exists
        assertThat(findAllNotifications()).hasSize(1);
    }

    @Test
    void findAll_shouldReturnAllNotifications() {
        // Given - create multiple notifications
        NotificationDto dto1 = createNotificationDto("CHEDP.GB.2024.1111111", "United Kingdom");
        NotificationDto dto2 = createNotificationDto("CHEDP.GB.2024.2222222", "Ireland");
        NotificationDto dto3 = createNotificationDto("CHEDP.GB.2024.3333333", "France");

        webClient("NoAuth").put().uri(NOTIFICATIONS_ENDPOINT).bodyValue(dto1).exchange();
        webClient("NoAuth").put().uri(NOTIFICATIONS_ENDPOINT).bodyValue(dto2).exchange();
        webClient("NoAuth").put().uri(NOTIFICATIONS_ENDPOINT).bodyValue(dto3).exchange();

        // When
        List<Notification> notifications = findAllNotifications();
        
        // Then
        assertThat(notifications).isNotNull().hasSize(3);
        assertThat(notifications)
            .extracting(Notification::getChedReference)
            .containsExactlyInAnyOrder("CHEDP.GB.2024.1111111", "CHEDP.GB.2024.2222222", "CHEDP.GB.2024.3333333");
    }

    @Test
    void findById_shouldReturnNotification() {
        // Given - create a notification
        NotificationDto dto = createNotificationDto("CHEDP.GB.2024.4444444", "Spain");
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
        assertThat(notification.getChedReference()).isEqualTo("CHEDP.GB.2024.4444444");
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
    void findByChedReference_shouldReturnNotification() {
        // Given - create a notification
        NotificationDto dto = createNotificationDto("CHEDP.GB.2024.5555555", "Germany");
        webClient("NoAuth")
            .put()
            .uri(NOTIFICATIONS_ENDPOINT)
            .bodyValue(dto)
            .exchange();

        // When
        EntityExchangeResult<Notification> result = webClient("NoAuth")
            .get()
            .uri(NOTIFICATIONS_ENDPOINT + "/ched/{chedReference}", "CHEDP.GB.2024.5555555")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Notification.class)
            .returnResult();

        // Then
        Notification notification = result.getResponseBody();
        assertThat(notification).isNotNull();
        assertThat(notification.getChedReference()).isEqualTo("CHEDP.GB.2024.5555555");
        assertThat(notification.getOriginCountry()).isEqualTo("Germany");
    }

    @Test
    void findByChedReference_shouldReturn404_whenNotExists() {
        // When/Then
        webClient("NoAuth")
            .get()
            .uri(NOTIFICATIONS_ENDPOINT + "/ched/{chedReference}", "CHEDP.GB.2024.9999999")
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void delete_shouldRemoveNotification() {
        // Given - create a notification
        NotificationDto dto = createNotificationDto("CHEDP.GB.2024.6666666", "Italy");
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
        NotificationDto createDto = createNotificationDto("CHEDP.GB.2024.8888888", "Netherlands");
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
        assertThat(created.getChedReference()).isEqualTo("CHEDP.GB.2024.8888888");
        assertThat(created.getOriginCountry()).isEqualTo("Netherlands");

        // 2. Find by ID
        webClient("NoAuth")
            .get()
            .uri(NOTIFICATIONS_ENDPOINT + "/{id}", created.getId())
            .exchange()
            .expectStatus().isOk()
            .expectBody(Notification.class)
            .value(notification -> {
                assertThat(notification.getChedReference()).isEqualTo("CHEDP.GB.2024.8888888");
            });

        // 3. Find by CHED reference
        webClient("NoAuth")
            .get()
            .uri(NOTIFICATIONS_ENDPOINT + "/ched/{chedReference}", "CHEDP.GB.2024.8888888")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Notification.class)
            .value(notification -> {
                assertThat(notification.getId()).isEqualTo(created.getId());
            });

        // 4. Update notification (same CHED reference, different data)
        NotificationDto updateDto = createNotificationDto("CHEDP.GB.2024.8888888", "Belgium");
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
        assertThat(updated.getChedReference()).isEqualTo("CHEDP.GB.2024.8888888"); // CHED reference preserved
        assertThat(updated.getOriginCountry()).isEqualTo("Belgium"); // Updated value
        assertThat(updated.getImportReason()).isEqualTo("re-entry"); // Updated value

        // 5. Verify only one notification exists
        assertThat(findAllNotifications()).hasSize(1);

        // 6. Delete notification
        webClient("NoAuth")
            .delete()
            .uri(NOTIFICATIONS_ENDPOINT + "/{id}", created.getId())
            .exchange()
            .expectStatus().isNoContent();

        // 7. Verify deletion
        webClient("NoAuth")
            .get()
            .uri(NOTIFICATIONS_ENDPOINT + "/{id}", created.getId())
            .exchange()
            .expectStatus().isNotFound();

        assertThat(notificationRepository.count()).isEqualTo(0);
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
    private NotificationDto createNotificationDto(String chedReference, String originCountry) {
        Species species = new Species();
        species.setName("Cattle");
        species.setNoOfAnimals(10);
        species.setNoOfPackages(2);

        Commodity commodity = new Commodity();
        commodity.setCode("0102");
        commodity.setDescription("Live bovine animals");
        commodity.setSpecies(Collections.singletonList(species));

        NotificationDto dto = new NotificationDto();
        dto.setChedReference(chedReference);
        dto.setOriginCountry(originCountry);
        dto.setCommodity(commodity);
        dto.setImportReason("internalmarket");
        dto.setInternalMarketPurpose("breeding");

        return dto;
    }
}
