package uk.gov.defra.cdp.trade.demo.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.Arrays;
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
class IpaffsNotificationMapperTest {

    private IpaffsNotificationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new IpaffsNotificationMapper();
    }

    @Test
    void mapToIpaffsNotification_shouldMapOriginCountry() {
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
    void mapToIpaffsNotification_shouldSetDefaultValues() {
        // Given
        Notification notification = createMinimalNotification();

        // When
        IpaffsNotification result = mapper.mapToIpaffsNotification(notification);

        // Then - verify default values are set
        assertAll(
            () -> assertThat(result.getType()).isEqualTo("CVEDA"),
            () -> assertThat(result.getStatus()).isEqualTo("SUBMITTED"),
            () -> assertThat(result.getPartOne().getPurpose().getPurposeGroup()).isEqualTo("For Import")
        );
    }

    @Test
    void mapToIpaffsNotification_shouldMapStatusFromNotification() {
        // Given
        Notification notification = createMinimalNotification();
        notification.setStatus("SUBMITTED");

        // When
        IpaffsNotification result = mapper.mapToIpaffsNotification(notification);

        // Then
        assertThat(result.getStatus()).isEqualTo("SUBMITTED");
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

        // Then - should not throw exception, default values should be set
        assertThat(result).isNotNull();
        assertThat(result.getPartOne()).isNotNull();
    }

    @Test
    void mapToIpaffsNotification_shouldMapMultipleSpeciesToCommodityComplements() {
        // Given
        Notification notification = createMinimalNotification();

        Species species1 = new Species();
        species1.setName("Bibos spp.");
        species1.setCode("587923");
        species1.setNoOfAnimals(1);
        species1.setNoOfPackages(2);

        Species species2 = new Species();
        species2.setName("Bison bison");
        species2.setCode("41481");
        species2.setNoOfAnimals(3);
        species2.setNoOfPackages(4);

        Commodity commodity = new Commodity();
        commodity.setCode("0102");
        commodity.setDescription("Live bovine animals");
        commodity.setType("Domestic");
        commodity.setSpecies(Arrays.asList(species1, species2));

        notification.setCommodity(commodity);

        // When
        IpaffsNotification result = mapper.mapToIpaffsNotification(notification);

        // Then - verify commodity complements
        assertThat(result.getPartOne().getCommodities().getCommodityComplement()).hasSize(2);

        var complement1 = result.getPartOne().getCommodities().getCommodityComplement().get(0);
        assertAll(
            () -> assertThat(complement1.getCommodityID()).isEqualTo("0102"),
            () -> assertThat(complement1.getCommodityDescription()).isEqualTo("Live bovine animals"),
            () -> assertThat(complement1.getComplementID()).isEqualTo(1),
            () -> assertThat(complement1.getSpeciesID()).isEqualTo("587923"),
            () -> assertThat(complement1.getSpeciesName()).isEqualTo("Bibos spp."),
            () -> assertThat(complement1.getComplementName()).isEqualTo("Bibos spp."),
            () -> assertThat(complement1.getSpeciesNomination()).isEqualTo("Bibos spp."),
            () -> assertThat(complement1.getSpeciesTypeName()).isEqualTo("Domestic")
        );

        var complement2 = result.getPartOne().getCommodities().getCommodityComplement().get(1);
        assertAll(
            () -> assertThat(complement2.getCommodityID()).isEqualTo("0102"),
            () -> assertThat(complement2.getCommodityDescription()).isEqualTo("Live bovine animals"),
            () -> assertThat(complement2.getComplementID()).isEqualTo(2),
            () -> assertThat(complement2.getSpeciesID()).isEqualTo("41481"),
            () -> assertThat(complement2.getSpeciesName()).isEqualTo("Bison bison"),
            () -> assertThat(complement2.getComplementName()).isEqualTo("Bison bison"),
            () -> assertThat(complement2.getSpeciesNomination()).isEqualTo("Bison bison"),
            () -> assertThat(complement2.getSpeciesTypeName()).isEqualTo("Domestic")
        );

        // Verify complement parameter sets
        assertThat(result.getPartOne().getCommodities().getComplementParameterSet()).hasSize(2);

        var paramSet1 = result.getPartOne().getCommodities().getComplementParameterSet().get(0);
        assertAll(
            () -> assertThat(paramSet1.getComplementID()).isEqualTo(1),
            () -> assertThat(paramSet1.getSpeciesID()).isEqualTo("587923"),
            () -> assertThat(paramSet1.getUniqueComplementID()).isNotNull(),
            () -> assertThat(paramSet1.getKeyDataPair()).hasSize(2),
            () -> assertThat(paramSet1.getKeyDataPair().get(0).getKey()).isEqualTo("number_package"),
            () -> assertThat(paramSet1.getKeyDataPair().get(0).getData()).isEqualTo("2"),
            () -> assertThat(paramSet1.getKeyDataPair().get(1).getKey()).isEqualTo("number_animal"),
            () -> assertThat(paramSet1.getKeyDataPair().get(1).getData()).isEqualTo("1")
        );

        var paramSet2 = result.getPartOne().getCommodities().getComplementParameterSet().get(1);
        assertAll(
            () -> assertThat(paramSet2.getComplementID()).isEqualTo(2),
            () -> assertThat(paramSet2.getSpeciesID()).isEqualTo("41481"),
            () -> assertThat(paramSet2.getUniqueComplementID()).isNotNull(),
            () -> assertThat(paramSet2.getKeyDataPair()).hasSize(2),
            () -> assertThat(paramSet2.getKeyDataPair().get(0).getKey()).isEqualTo("number_package"),
            () -> assertThat(paramSet2.getKeyDataPair().get(0).getData()).isEqualTo("4"),
            () -> assertThat(paramSet2.getKeyDataPair().get(1).getKey()).isEqualTo("number_animal"),
            () -> assertThat(paramSet2.getKeyDataPair().get(1).getData()).isEqualTo("3")
        );

        // Verify totals
        assertAll(
            () -> assertThat(result.getPartOne().getCommodities().getNumberOfAnimals()).isEqualTo(4),
            () -> assertThat(result.getPartOne().getCommodities().getNumberOfPackages()).isEqualTo(6)
        );
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
