package uk.gov.defra.cdp.trade.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.defra.cdp.trade.demo.domain.Commodity;
import uk.gov.defra.cdp.trade.demo.domain.Notification;
import uk.gov.defra.cdp.trade.demo.domain.NotificationDto;
import uk.gov.defra.cdp.trade.demo.domain.Species;
import uk.gov.defra.cdp.trade.demo.exceptions.NotFoundException;
import uk.gov.defra.cdp.trade.demo.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    private NotificationController controller;

    @BeforeEach
    void setUp() {
        controller = new NotificationController(notificationService);
    }

    @Test
    void findAll_shouldReturnAllNotifications() {
        // Given
        List<Notification> notifications = Arrays.asList(
            createTestNotification("id-001"),
            createTestNotification("id-002"),
            createTestNotification("id-003")
        );
        when(notificationService.findAll()).thenReturn(notifications);

        // When
        List<Notification> result = controller.findAll();

        // Then
        assertAll(
            () -> assertThat(result).hasSize(3),
            () -> assertThat(result).extracting(Notification::getId)
                .containsExactly("id-001", "id-002", "id-003")
        );

        verify(notificationService).findAll();
    }

    @Test
    void findAll_shouldReturnEmptyList_whenNoNotifications() {
        // Given
        when(notificationService.findAll()).thenReturn(Collections.emptyList());

        // When
        List<Notification> result = controller.findAll();

        // Then
        assertThat(result).isEmpty();
        verify(notificationService).findAll();
    }

    @Test
    void findById_shouldReturnNotification_whenExists() {
        // Given
        Notification notification = createTestNotification("test-id-123");
        when(notificationService.findById("test-id-123")).thenReturn(notification);

        // When
        Notification result = controller.findById("test-id-123");

        // Then
        assertAll(
            () -> assertThat(result).isNotNull(),
            () -> assertThat(result.getId()).isEqualTo("test-id-123"),
            () -> assertThat(result.getChedReference()).isEqualTo("CHED-test-id-123")
        );

        verify(notificationService).findById("test-id-123");
    }

    @Test
    void findById_shouldThrowNotFoundException_whenNotExists() {
        // Given
        when(notificationService.findById("non-existent-id"))
            .thenThrow(new NotFoundException("Notification not found with id: non-existent-id"));

        // When/Then
        assertThatThrownBy(() -> controller.findById("non-existent-id"))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("non-existent-id");

        verify(notificationService).findById("non-existent-id");
    }

    @Test
    void delete_shouldCallServiceDelete() {
        // Given
        doNothing().when(notificationService).delete("test-id-123");

        // When
        controller.delete("test-id-123");

        // Then
        verify(notificationService).delete("test-id-123");
    }

    @Test
    void delete_shouldThrowNotFoundException_whenNotificationDoesNotExist() {
        // Given
        doThrow(new NotFoundException("Notification not found with id: non-existent-id"))
            .when(notificationService).delete("non-existent-id");

        // When/Then
        assertThatThrownBy(() -> controller.delete("non-existent-id"))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("non-existent-id");

        verify(notificationService).delete("non-existent-id");
    }
    
    @Test
    void saveOrUpdate_shouldCreateNotification_whenNotExists() {
        // Given
        NotificationDto dto = createTestNotificationDto(null); // No ID - create new
        Notification savedNotification = createTestNotification("test-id-789");
        savedNotification.setCreated(LocalDateTime.now());
        savedNotification.setUpdated(LocalDateTime.now());

        when(notificationService.saveOrUpdate(dto)).thenReturn(savedNotification);

        // When
        Notification result = controller.saveOrUpdate(dto);

        // Then
        assertAll(
            () -> assertThat(result).isNotNull(),
            () -> assertThat(result.getId()).isEqualTo("test-id-789"),
            () -> assertThat(result.getChedReference()).isEqualTo("CHED-test-id-789"),
            () -> assertThat(result.getCreated()).isNotNull(),
            () -> assertThat(result.getUpdated()).isNotNull()
        );

        verify(notificationService).saveOrUpdate(dto);
    }

    @Test
    void saveOrUpdate_shouldUpdateNotification_whenExists() {
        // Given
        NotificationDto dto = createTestNotificationDto("test-id-123");
        dto.setOriginCountry("Germany");

        Notification updatedNotification = createTestNotification("test-id-123");
        updatedNotification.setOriginCountry("Germany");
        updatedNotification.setCreated(LocalDateTime.now().minusDays(2));
        updatedNotification.setUpdated(LocalDateTime.now());

        when(notificationService.saveOrUpdate(dto)).thenReturn(updatedNotification);

        // When
        Notification result = controller.saveOrUpdate(dto);

        // Then
        assertAll(
            () -> assertThat(result).isNotNull(),
            () -> assertThat(result.getId()).isEqualTo("test-id-123"),
            () -> assertThat(result.getOriginCountry()).isEqualTo("Germany"),
            () -> assertThat(result.getUpdated()).isNotNull()
        );

        verify(notificationService).saveOrUpdate(dto);
    }

    @Test
    void saveOrUpdate_shouldHandleCompleteNotification() {
        // Given
        NotificationDto dto = createCompleteNotificationDto();
        Notification savedNotification = createCompleteNotification();
        savedNotification.setId("test-id-999");
        savedNotification.setCreated(LocalDateTime.now());
        savedNotification.setUpdated(LocalDateTime.now());

        when(notificationService.saveOrUpdate(dto)).thenReturn(savedNotification);

        // When
        Notification result = controller.saveOrUpdate(dto);

        // Then
        assertAll(
            () -> assertThat(result).isNotNull(),
            () -> assertThat(result.getId()).isEqualTo("test-id-999"),
            () -> assertThat(result.getCommodity()).isNotNull(),
            () -> assertThat(result.getCommodity().getSpecies()).hasSize(2)
        );

        verify(notificationService).saveOrUpdate(dto);
    }

    // Helper methods
    private Notification createTestNotification(String id) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setChedReference("CHED-" + id);
        notification.setOriginCountry("United Kingdom");
        notification.setImportReason("internalmarket");
        notification.setInternalMarketPurpose("breeding");

        Species species = new Species();
        species.setName("Cattle");
        species.setNoOfAnimals(10);
        species.setNoOfPackages(2);

        Commodity commodity = new Commodity();
        commodity.setCode("0102");
        commodity.setDescription("Live bovine animals");
        commodity.setSpecies(Collections.singletonList(species));

        notification.setCommodity(commodity);
        return notification;
    }

    private NotificationDto createTestNotificationDto(String id) {
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
        dto.setChedReference("CHED-" + id);
        dto.setOriginCountry("United Kingdom");
        dto.setCommodity(commodity);
        dto.setImportReason("internalmarket");
        dto.setInternalMarketPurpose("breeding");

        return dto;
    }

    private Notification createCompleteNotification() {
        Notification notification = new Notification();
        notification.setChedReference("CHED-FULL");
        notification.setOriginCountry("Ireland");
        notification.setImportReason("internalmarket");
        notification.setInternalMarketPurpose("breeding");

        Species cattle = new Species();
        cattle.setName("Cattle");
        cattle.setNoOfAnimals(15);
        cattle.setNoOfPackages(3);

        Species sheep = new Species();
        sheep.setName("Sheep");
        sheep.setNoOfAnimals(25);
        sheep.setNoOfPackages(5);

        Commodity commodity = new Commodity();
        commodity.setCode("0102");
        commodity.setDescription("Live animals");
        commodity.setSpecies(Arrays.asList(cattle, sheep));

        notification.setCommodity(commodity);
        return notification;
    }

    private NotificationDto createCompleteNotificationDto() {
        Species cattle = new Species();
        cattle.setName("Cattle");
        cattle.setNoOfAnimals(15);
        cattle.setNoOfPackages(3);

        Species sheep = new Species();
        sheep.setName("Sheep");
        sheep.setNoOfAnimals(25);
        sheep.setNoOfPackages(5);

        Commodity commodity = new Commodity();
        commodity.setCode("0102");
        commodity.setDescription("Live animals");
        commodity.setSpecies(Arrays.asList(cattle, sheep));

        NotificationDto dto = new NotificationDto();
        dto.setChedReference("CHED-FULL");
        dto.setOriginCountry("Ireland");
        dto.setCommodity(commodity);
        dto.setImportReason("internalmarket");
        dto.setInternalMarketPurpose("breeding");

        return dto;
    }
}
