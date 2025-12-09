package uk.gov.defra.cdp.trade.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
import uk.gov.defra.cdp.trade.demo.domain.Transport;
import uk.gov.defra.cdp.trade.demo.domain.repository.NotificationRepository;
import uk.gov.defra.cdp.trade.demo.exceptions.NotFoundException;
import uk.gov.defra.cdp.trade.demo.exceptions.NotificationSubmissionException;
import uk.gov.defra.cdp.trade.demo.mapper.ChedaMapper;
import uk.gov.defra.cdp.trade.demo.client.IpaffsNotificationClient;
import uk.gov.defra.cdp.trade.demo.domain.ipaffs.IpaffsNotification;

/**
 * Unit tests for NotificationService.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private NotificationIdGeneratorService idGeneratorService;

    @Mock
    private ChedaMapper chedaMapper;

    @Mock
    private IpaffsNotificationClient ipaffsNotificationClient;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository, idGeneratorService, chedaMapper, ipaffsNotificationClient);
    }

    @Test
    void findAll_shouldReturnAllNotifications() {
        // Given
        List<Notification> notifications = Arrays.asList(
            createTestNotification("id-001"),
            createTestNotification("id-002")
        );
        when(repository.findAll()).thenReturn(notifications);

        // When
        List<Notification> result = service.findAll();

        // Then
        assertAll(
            () -> assertThat(result).hasSize(2),
            () -> assertThat(result).extracting(Notification::getId)
                .containsExactly("id-001", "id-002")
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
        Notification notification = createTestNotification("test-id-123");
        when(repository.findById("test-id-123")).thenReturn(Optional.of(notification));

        // When
        Notification result = service.findById("test-id-123");

        // Then
        assertAll(
            () -> assertThat(result).isNotNull(),
            () -> assertThat(result.getId()).isEqualTo("test-id-123"),
            () -> assertThat(result.getChedReference()).isEqualTo("CHED-test-id-123")
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
    void delete_shouldDeleteNotificationSuccessfully() {
        // Given
        Notification notification = createTestNotification("test-id-123");
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
    void saveOrUpdate_shouldCreateNewNotification_whenIdIsNull() {
        // Given
        NotificationDto dto = createTestNotificationDto(null); // No ID - create new
        Notification savedNotification = createTestNotification("CDP.2025.12.01.1");
        when(idGeneratorService.generateId()).thenReturn("CDP.2025.12.01.1");
        when(repository.save(any(Notification.class))).thenReturn(savedNotification);

        // When
        Notification result = service.saveOrUpdate(dto);

        // Then
        assertThat(result).isNotNull();
        verify(idGeneratorService).generateId();
        verify(repository).save(notificationCaptor.capture());

        Notification captured = notificationCaptor.getValue();
        assertAll(
            () -> assertThat(captured.getId()).isEqualTo("CDP.2025.12.01.1"),
            () -> assertThat(captured.getChedReference()).isEqualTo("CHED-null"),
            () -> assertThat(captured.getCreated()).isNotNull(),
            () -> assertThat(captured.getUpdated()).isNotNull()
        );
    }

    @Test
    void saveOrUpdate_shouldUpdateExistingNotification_whenIdProvided() {
        // Given
        Notification existing = createTestNotification("test-id-123");
        existing.setCreated(LocalDateTime.now().minusDays(1));
        existing.setUpdated(LocalDateTime.now().minusDays(1));

        NotificationDto dto = createTestNotificationDto("test-id-123"); // ID provided - update existing
        dto.setChedReference("CHED-UPDATED");
        dto.setOriginCountry("France");
        dto.setImportReason("re-entry");

        when(repository.findById("test-id-123")).thenReturn(Optional.of(existing));
        when(repository.save(any(Notification.class))).thenReturn(existing);

        // When
        service.saveOrUpdate(dto);

        // Then
        verify(repository).findById("test-id-123");
        verify(repository).save(notificationCaptor.capture());

        Notification captured = notificationCaptor.getValue();
        assertAll(
            () -> assertThat(captured.getId()).isEqualTo("test-id-123"),
            () -> assertThat(captured.getChedReference()).isEqualTo("CHED-UPDATED"),
            () -> assertThat(captured.getOriginCountry()).isEqualTo("France"),
            () -> assertThat(captured.getImportReason()).isEqualTo("re-entry"),
            () -> assertThat(captured.getTransport()).isEqualTo(createTestTransport()),
            () -> assertThat(captured.getCreated()).isNotNull(),
            () -> assertThat(captured.getUpdated()).isNotNull(),
            () -> assertThat(captured.getUpdated()).isAfter(captured.getCreated())
        );
    }

    @Test
    void saveOrUpdate_shouldPreserveCreatedTimestamp_whenUpdating() {
        // Given
        LocalDateTime originalCreated = LocalDateTime.now().minusDays(5);
        Notification existing = createTestNotification("test-id-123");
        existing.setCreated(originalCreated);

        NotificationDto dto = createTestNotificationDto("test-id-123"); // ID provided - update
        dto.setOriginCountry("Spain");

        when(repository.findById("test-id-123")).thenReturn(Optional.of(existing));
        when(repository.save(any(Notification.class))).thenReturn(existing);

        // When
        service.saveOrUpdate(dto);

        // Then
        verify(repository).save(notificationCaptor.capture());
        Notification captured = notificationCaptor.getValue();

        // Created timestamp should be preserved from existing notification
        assertThat(captured.getCreated()).isEqualTo(originalCreated);
    }

    @Test
    void saveOrUpdate_shouldThrowNotFoundException_whenIdProvidedButNotExists() {
        // Given
        NotificationDto dto = createTestNotificationDto("non-existent-id"); // ID provided but doesn't exist
        when(repository.findById("non-existent-id")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.saveOrUpdate(dto))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("non-existent-id")
            .hasMessageContaining("not found");

        verify(repository).findById("non-existent-id");
        verify(repository, never()).save(any(Notification.class));
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
        notification.setTransport(createTestTransport());
        return notification;
    }

    private Transport createTestTransport() {
        Transport transport = new Transport();
        transport.setBcpCode("GBLHR1");
        transport.setTransportToBcp("road");
        transport.setVehicleId("ABC123");
        return transport;
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

        dto.setTransport(createTestTransport());
        return dto;
    }

    // Tests for submitNotification - simplified signature: submitNotification(String id)

    @Test
    void submitNotification_shouldPreventResubmission_whenNotificationAlreadySubmitted() {
        // Given
        String notificationId = "CDP.2025.12.09.1";

        Notification existingNotification = createTestNotification(notificationId);
        existingNotification.setStatus("SUBMITTED");
        existingNotification.setChedReference("CHEDA.2025.12090100");

        when(repository.findById(notificationId)).thenReturn(Optional.of(existingNotification));

        // When/Then
        assertThatThrownBy(() -> service.submitNotification(notificationId))
            .isInstanceOf(NotificationSubmissionException.class)
            .hasMessageContaining("already submitted");

        verify(ipaffsNotificationClient, never()).submitNotification(any(), anyString());
        verify(repository, never()).save(any(Notification.class));
    }

    @Test
    void submitNotification_shouldThrowException_whenNotificationNotFound() {
        // Given
        String notificationId = "CDP.2025.12.09.999";

        when(repository.findById(notificationId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.submitNotification(notificationId))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("not found");

        verify(ipaffsNotificationClient, never()).submitNotification(any(), anyString());
        verify(repository, never()).save(any(Notification.class));
    }

    @Test
    void submitNotification_shouldMapToIpaffsFormat_andSubmit() {
        // Given
        String notificationId = "CDP.2025.12.09.3";

        Notification existingNotification = createTestNotification(notificationId);
        existingNotification.setStatus("DRAFT");

        when(repository.findById(notificationId)).thenReturn(Optional.of(existingNotification));
        when(chedaMapper.mapToIpaffsNotification(existingNotification))
            .thenReturn(new IpaffsNotification());
        when(ipaffsNotificationClient.submitNotification(any(IpaffsNotification.class), eq(notificationId)))
            .thenReturn("CHEDA.2025.12090300");
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        service.submitNotification(notificationId);

        // Then
        verify(chedaMapper).mapToIpaffsNotification(existingNotification);
        verify(ipaffsNotificationClient).submitNotification(any(IpaffsNotification.class), eq(notificationId));
    }

    @Test
    void submitNotification_shouldUpdateWithChedReference_andSubmittedStatus() {
        // Given
        String notificationId = "CDP.2025.12.09.7";

        Notification existingNotification = createTestNotification(notificationId);
        existingNotification.setStatus("DRAFT");

        when(repository.findById(notificationId)).thenReturn(Optional.of(existingNotification));
        when(chedaMapper.mapToIpaffsNotification(existingNotification))
            .thenReturn(new IpaffsNotification());
        when(ipaffsNotificationClient.submitNotification(any(IpaffsNotification.class), eq(notificationId)))
            .thenReturn("CHEDA.2025.12090700");
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Notification result = service.submitNotification(notificationId);

        // Then
        assertAll(
            () -> assertThat(result.getChedReference()).isEqualTo("CHEDA.2025.12090700"),
            () -> assertThat(result.getStatus()).isEqualTo("SUBMITTED"),
            () -> assertThat(result.getUpdated()).isNotNull()
        );

        verify(repository).save(notificationCaptor.capture());
        Notification saved = notificationCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo("SUBMITTED");
        assertThat(saved.getChedReference()).isEqualTo("CHEDA.2025.12090700");
    }

    @Test
    void submitNotification_shouldThrowException_whenIpaffsSubmissionFails() {
        // Given
        String notificationId = "CDP.2025.12.09.8";

        Notification existingNotification = createTestNotification(notificationId);
        existingNotification.setStatus("DRAFT");

        when(repository.findById(notificationId)).thenReturn(Optional.of(existingNotification));
        when(chedaMapper.mapToIpaffsNotification(existingNotification))
            .thenReturn(new IpaffsNotification());
        when(ipaffsNotificationClient.submitNotification(any(IpaffsNotification.class), eq(notificationId)))
            .thenThrow(new RuntimeException("IPAFFS service unavailable"));

        // When/Then
        assertThatThrownBy(() -> service.submitNotification(notificationId))
            .isInstanceOf(NotificationSubmissionException.class)
            .hasMessageContaining("Failed to submit notification to IPAFFS")
            .hasCauseInstanceOf(RuntimeException.class);

        // Notification should remain in DRAFT state - not saved with SUBMITTED
        verify(repository, never()).save(argThat(n -> "SUBMITTED".equals(n.getStatus())));
    }
}
