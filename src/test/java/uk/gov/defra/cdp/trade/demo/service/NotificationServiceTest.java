package uk.gov.defra.cdp.trade.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.defra.cdp.trade.demo.domain.Commodity;
import uk.gov.defra.cdp.trade.demo.domain.Notification;
import uk.gov.defra.cdp.trade.demo.domain.NotificationDto;
import uk.gov.defra.cdp.trade.demo.domain.Species;
import uk.gov.defra.cdp.trade.demo.domain.repository.NotificationRepository;
import uk.gov.defra.cdp.trade.demo.exceptions.NotFoundException;

/**
 * Unit tests for NotificationService.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository);
    }

    @Test
    void findAll_shouldReturnAllNotifications() {
        // Given
        List<Notification> notifications = Arrays.asList(
            createTestNotification("CHEDP.GB.2024.1111111"),
            createTestNotification("CHEDP.GB.2024.2222222")
        );
        when(repository.findAll()).thenReturn(notifications);

        // When
        List<Notification> result = service.findAll();

        // Then
        assertAll(
            () -> assertThat(result).hasSize(2),
            () -> assertThat(result).extracting(Notification::getChedReference)
                .containsExactly("CHEDP.GB.2024.1111111", "CHEDP.GB.2024.2222222")
        );

        verify(repository).findAll();
    }

    @Test
    void findAll_shouldReturnEmptyList_whenNoNotifications() {
        // Given
        when(repository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<Notification> result = service.findAll();

        // Then
        assertThat(result).isEmpty();
        verify(repository).findAll();
    }

    @Test
    void findById_shouldReturnNotification_whenExists() {
        // Given
        Notification notification = createTestNotification("CHEDP.GB.2024.1234567");
        notification.setId("test-id-123");
        when(repository.findById("test-id-123")).thenReturn(Optional.of(notification));

        // When
        Notification result = service.findById("test-id-123");

        // Then
        assertAll(
            () -> assertThat(result).isNotNull(),
            () -> assertThat(result.getId()).isEqualTo("test-id-123"),
            () -> assertThat(result.getChedReference()).isEqualTo("CHEDP.GB.2024.1234567")
        );

        verify(repository).findById("test-id-123");
    }

    @Test
    void findById_shouldThrowNotFoundException_whenNotExists() {
        // Given
        when(repository.findById("non-existent-id")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.findById("non-existent-id"))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("non-existent-id")
            .hasMessageContaining("not found");

        verify(repository).findById("non-existent-id");
    }

    @Test
    void findByChedReference_shouldReturnNotification_whenExists() {
        // Given
        Notification notification = createTestNotification("CHEDP.GB.2024.1234567");
        when(repository.findByChedReference("CHEDP.GB.2024.1234567")).thenReturn(Optional.of(notification));

        // When
        Notification result = service.findByChedReference("CHEDP.GB.2024.1234567");

        // Then
        assertAll(
            () -> assertThat(result).isNotNull(),
            () -> assertThat(result.getChedReference()).isEqualTo("CHEDP.GB.2024.1234567")
        );

        verify(repository).findByChedReference("CHEDP.GB.2024.1234567");
    }

    @Test
    void findByChedReference_shouldThrowNotFoundException_whenNotExists() {
        // Given
        when(repository.findByChedReference("CHEDP.GB.2024.9999999")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.findByChedReference("CHEDP.GB.2024.9999999"))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("CHEDP.GB.2024.9999999")
            .hasMessageContaining("not found");

        verify(repository).findByChedReference("CHEDP.GB.2024.9999999");
    }

    @Test
    void delete_shouldDeleteNotificationSuccessfully() {
        // Given
        Notification notification = createTestNotification("CHEDP.GB.2024.1234567");
        notification.setId("test-id-123");
        when(repository.findById("test-id-123")).thenReturn(Optional.of(notification));

        // When
        service.delete("test-id-123");

        // Then
        verify(repository).findById("test-id-123");
        verify(repository).deleteById("test-id-123");
    }

    @Test
    void delete_shouldThrowNotFoundException_whenNotificationDoesNotExist() {
        // Given
        when(repository.findById("non-existent-id")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.delete("non-existent-id"))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("non-existent-id");

        verify(repository).findById("non-existent-id");
        verify(repository, never()).deleteById(anyString());
    }

    @Test
    void saveOrUpdate_shouldCreateNewNotification_whenNotExists() {
        // Given
        NotificationDto dto = createTestNotificationDto("CHEDP.GB.2024.1234567");
        Notification savedNotification = createTestNotification("CHEDP.GB.2024.1234567");
        when(repository.findByChedReference("CHEDP.GB.2024.1234567")).thenReturn(Optional.empty());
        when(repository.save(any(Notification.class))).thenReturn(savedNotification);

        // When
        Notification result = service.saveOrUpdate(dto);

        // Then
        assertThat(result).isNotNull();
        verify(repository).findByChedReference("CHEDP.GB.2024.1234567");
        verify(repository).save(notificationCaptor.capture());

        Notification captured = notificationCaptor.getValue();
        assertAll(
            () -> assertThat(captured.getChedReference()).isEqualTo("CHEDP.GB.2024.1234567"),
            () -> assertThat(captured.getCreated()).isNotNull(),
            () -> assertThat(captured.getUpdated()).isNotNull()
        );
    }

    @Test
    void saveOrUpdate_shouldUpdateExistingNotification_whenExists() {
        // Given
        Notification existing = createTestNotification("CHEDP.GB.2024.1234567");
        existing.setId("test-id-123");
        existing.setCreated(LocalDateTime.now().minusDays(1));
        existing.setUpdated(LocalDateTime.now().minusDays(1));

        NotificationDto dto = createTestNotificationDto("CHEDP.GB.2024.1234567");
        dto.setOriginCountry("France");
        dto.setImportReason("re-entry");

        when(repository.findByChedReference("CHEDP.GB.2024.1234567")).thenReturn(Optional.of(existing));
        when(repository.save(any(Notification.class))).thenReturn(existing);

        // When
        service.saveOrUpdate(dto);

        // Then
        verify(repository).findByChedReference("CHEDP.GB.2024.1234567");
        verify(repository).save(notificationCaptor.capture());

        Notification captured = notificationCaptor.getValue();
        assertAll(
            () -> assertThat(captured.getId()).isEqualTo("test-id-123"),
            () -> assertThat(captured.getChedReference()).isEqualTo("CHEDP.GB.2024.1234567"),
            () -> assertThat(captured.getOriginCountry()).isEqualTo("France"),
            () -> assertThat(captured.getImportReason()).isEqualTo("re-entry"),
            () -> assertThat(captured.getCreated()).isNotNull(),
            () -> assertThat(captured.getUpdated()).isNotNull(),
            () -> assertThat(captured.getUpdated()).isAfter(captured.getCreated())
        );
    }

    @Test
    void saveOrUpdate_shouldPreserveCreatedTimestamp_whenUpdating() {
        // Given
        LocalDateTime originalCreated = LocalDateTime.now().minusDays(5);
        Notification existing = createTestNotification("CHEDP.GB.2024.1234567");
        existing.setId("test-id-123");
        existing.setCreated(originalCreated);

        NotificationDto dto = createTestNotificationDto("CHEDP.GB.2024.1234567");
        dto.setOriginCountry("Spain");

        when(repository.findByChedReference("CHEDP.GB.2024.1234567")).thenReturn(Optional.of(existing));
        when(repository.save(any(Notification.class))).thenReturn(existing);

        // When
        service.saveOrUpdate(dto);

        // Then
        verify(repository).save(notificationCaptor.capture());
        Notification captured = notificationCaptor.getValue();

        // Created timestamp should be preserved from existing notification
        assertThat(captured.getCreated()).isEqualTo(originalCreated);
    }

    // Helper methods
    private Notification createTestNotification(String chedReference) {
        Notification notification = new Notification();
        notification.setChedReference(chedReference);
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

    private NotificationDto createTestNotificationDto(String chedReference) {
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
        dto.setOriginCountry("United Kingdom");
        dto.setCommodity(commodity);
        dto.setImportReason("internalmarket");
        dto.setInternalMarketPurpose("breeding");

        return dto;
    }
}
