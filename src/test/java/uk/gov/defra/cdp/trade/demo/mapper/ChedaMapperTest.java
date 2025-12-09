package uk.gov.defra.cdp.trade.demo.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.defra.cdp.trade.demo.domain.Commodity;
import uk.gov.defra.cdp.trade.demo.domain.Notification;
import uk.gov.defra.cdp.trade.demo.domain.Species;
import uk.gov.defra.cdp.trade.demo.domain.Transport;
import uk.gov.defra.cdp.trade.demo.domain.ipaffs.IpaffsNotification;

/**
 * Unit tests for ChedaMapper.
 */
class ChedaMapperTest {

    private ChedaMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ChedaMapper(new ObjectMapper());
    }

    @Test
    void mapToIpaffsNotification_shouldLoadTemplateAndMapOriginCountry() {
        // Given
        Notification notification = createMinimalNotification();
        notification.setOriginCountry("FR");

        // When
        IpaffsNotification result = mapper.mapToIpaffsNotification(notification);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPartOne()).isNotNull();
        assertThat(result.getPartOne().getCommodities()).isNotNull();
        assertThat(result.getPartOne().getCommodities().getCountryOfOrigin()).isEqualTo("FR");
    }

    @Test
    void mapToIpaffsNotification_shouldMapInternalMarketPurpose() {
        // Given
        Notification notification = createMinimalNotification();
        notification.setInternalMarketPurpose("Breeding");

        // When
        IpaffsNotification result = mapper.mapToIpaffsNotification(notification);

        // Then
        assertThat(result.getPartOne().getPurpose()).isNotNull();
        assertThat(result.getPartOne().getPurpose().getInternalMarketPurpose()).isEqualTo("Breeding");
    }

    @Test
    void mapToIpaffsNotification_shouldMapCommodityDetails() {
        // Given
        Notification notification = createMinimalNotification();

        Species species = new Species();
        species.setName("Cattle");
        species.setNoOfAnimals(10);

        Commodity commodity = new Commodity();
        commodity.setCode("0102");
        commodity.setDescription("Live bovine animals");
        commodity.setSpecies(Collections.singletonList(species));

        notification.setCommodity(commodity);

        // When
        IpaffsNotification result = mapper.mapToIpaffsNotification(notification);

        // Then
        assertThat(result.getPartOne().getCommodities().getCommodityComplement()).isNotEmpty();
        assertAll(
            () -> assertThat(result.getPartOne().getCommodities().getCommodityComplement().get(0).getCommodityID())
                .isEqualTo("0102"),
            () -> assertThat(result.getPartOne().getCommodities().getCommodityComplement().get(0).getCommodityDescription())
                .isEqualTo("Live bovine animals")
        );
    }

    @Test
    void mapToIpaffsNotification_shouldMapTransportDetails() {
        // Given
        Notification notification = createMinimalNotification();

        Transport transport = new Transport();
        transport.setBcpCode("GBAPHA1A");
        transport.setTransportToBcp("Aeroplane");
        transport.setVehicleId("flight-123");

        notification.setTransport(transport);

        // When
        IpaffsNotification result = mapper.mapToIpaffsNotification(notification);

        // Then
        assertAll(
            () -> assertThat(result.getPartOne().getPointOfEntry()).isEqualTo("GBAPHA1A"),
            () -> assertThat(result.getPartOne().getMeansOfTransport()).isNotNull(),
            () -> assertThat(result.getPartOne().getMeansOfTransport().getType()).isEqualTo("Aeroplane"),
            () -> assertThat(result.getPartOne().getMeansOfTransport().getId()).isEqualTo("flight-123")
        );
    }

    @Test
    void mapToIpaffsNotification_shouldSetExternalReferenceToNotificationId() {
        // Given
        Notification notification = createMinimalNotification();
        notification.setId("CDP.2025.12.09.1");

        // When
        IpaffsNotification result = mapper.mapToIpaffsNotification(notification);

        // Then
        assertThat(result.getExternalReferences()).isNotEmpty();
        assertThat(result.getExternalReferences().get(0).getReference()).isEqualTo("CDP.2025.12.09.1");
    }

    @Test
    void mapToIpaffsNotification_shouldPreserveTemplateDefaults() {
        // Given
        Notification notification = createMinimalNotification();

        // When
        IpaffsNotification result = mapper.mapToIpaffsNotification(notification);

        // Then - verify template values are preserved
        assertAll(
            () -> assertThat(result.getType()).isEqualTo("CVEDA"),
            () -> assertThat(result.getStatus()).isEqualTo("DRAFT"),
            () -> assertThat(result.getVersion()).isEqualTo(0),
            () -> assertThat(result.getIsHighRiskEuImport()).isTrue()
        );
    }

    @Test
    void mapToIpaffsNotification_shouldMapAllFieldsTogether() {
        // Given
        Notification notification = createFullNotification();

        // When
        IpaffsNotification result = mapper.mapToIpaffsNotification(notification);

        // Then - verify all mapped fields
        assertAll(
            () -> assertThat(result.getPartOne().getCommodities().getCountryOfOrigin()).isEqualTo("CH"),
            () -> assertThat(result.getPartOne().getPurpose().getInternalMarketPurpose()).isEqualTo("Production"),
            () -> assertThat(result.getPartOne().getCommodities().getCommodityComplement().get(0).getCommodityID())
                .isEqualTo("0102"),
            () -> assertThat(result.getPartOne().getCommodities().getCommodityComplement().get(0).getCommodityDescription())
                .isEqualTo("Live bovine animals"),
            () -> assertThat(result.getPartOne().getPointOfEntry()).isEqualTo("GBAPHA1A"),
            () -> assertThat(result.getPartOne().getMeansOfTransport().getType()).isEqualTo("Aeroplane"),
            () -> assertThat(result.getPartOne().getMeansOfTransport().getId()).isEqualTo("flight-number"),
            () -> assertThat(result.getExternalReferences().get(0).getReference()).isEqualTo("CDP.2025.12.09.5")
        );
    }

    @Test
    void mapToIpaffsNotification_shouldHandleNullFieldsGracefully() {
        // Given
        Notification notification = new Notification();
        notification.setId("CDP.2025.12.09.1");
        // All other fields are null

        // When
        IpaffsNotification result = mapper.mapToIpaffsNotification(notification);

        // Then - should not throw exception, template defaults should be preserved
        assertThat(result).isNotNull();
        assertThat(result.getPartOne()).isNotNull();
    }

    // Helper methods
    private Notification createMinimalNotification() {
        Notification notification = new Notification();
        notification.setId("CDP.2025.12.09.1");
        return notification;
    }

    private Notification createFullNotification() {
        Notification notification = new Notification();
        notification.setId("CDP.2025.12.09.5");
        notification.setOriginCountry("CH");
        notification.setInternalMarketPurpose("Production");

        Species species = new Species();
        species.setName("Cattle");
        species.setNoOfAnimals(12);

        Commodity commodity = new Commodity();
        commodity.setCode("0102");
        commodity.setDescription("Live bovine animals");
        commodity.setSpecies(Collections.singletonList(species));
        notification.setCommodity(commodity);

        Transport transport = new Transport();
        transport.setBcpCode("GBAPHA1A");
        transport.setTransportToBcp("Aeroplane");
        transport.setVehicleId("flight-number");
        notification.setTransport(transport);

        return notification;
    }
}
