package uk.gov.defra.cdp.trade.demo.mapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.defra.cdp.trade.demo.domain.Notification;
import uk.gov.defra.cdp.trade.demo.domain.Species;
import uk.gov.defra.cdp.trade.demo.domain.ipaffs.Commodities;
import uk.gov.defra.cdp.trade.demo.domain.ipaffs.CommodityComplement;
import uk.gov.defra.cdp.trade.demo.domain.ipaffs.ComplementParameterSet;
import uk.gov.defra.cdp.trade.demo.domain.ipaffs.ExternalReference;
import uk.gov.defra.cdp.trade.demo.domain.ipaffs.IpaffsNotification;
import uk.gov.defra.cdp.trade.demo.domain.ipaffs.KeyDataPair;
import uk.gov.defra.cdp.trade.demo.domain.ipaffs.MeansOfTransport;
import uk.gov.defra.cdp.trade.demo.domain.ipaffs.PartOne;
import uk.gov.defra.cdp.trade.demo.domain.ipaffs.Purpose;
import uk.gov.defra.cdp.trade.demo.domain.ipaffs.User;

/**
 * Mapper for converting CDP Notification to IPAFFS CHEDA format.
 * <p>
 * Builds the IPAFFS notification using object construction with values from the CDP notification.
 */
@Component
@Slf4j
public class IpaffsNotificationMapper {

    /**
     * Map a CDP Notification to an IPAFFS notification format.
     *
     * @param notification the CDP notification to map
     * @return the IPAFFS notification with mapped fields
     */
    public IpaffsNotification mapToIpaffsNotification(Notification notification) {
        log.debug("Mapping notification {} to IPAFFS format", notification.getId());

        IpaffsNotification ipaffsNotification = new IpaffsNotification();

        // Set type and status
        ipaffsNotification.setType("CVEDA");
        ipaffsNotification.setStatus("SUBMITTED");

        // Build external references
        ipaffsNotification.setExternalReferences(buildExternalReferences(notification));

        // Build part one
        ipaffsNotification.setPartOne(buildPartOne(notification));

        ipaffsNotification.setSubmissionDate(LocalDateTime.now());
        ipaffsNotification.setSubmittedBy(
            User.builder().userId("abc123").displayName("Ian from CDP").build());

        log.debug("Successfully mapped notification {} to IPAFFS format", notification.getId());
        return ipaffsNotification;
    }

    private List<ExternalReference> buildExternalReferences(Notification notification) {
        ExternalReference externalReference = new ExternalReference();
        externalReference.setSystem("E-NOTIFICATION");
        externalReference.setReference(notification.getId());
        externalReference.setExactMatch(false);
        externalReference.setVerifiedByImporter(true);
        externalReference.setVerifiedByInspector(true);

        return Collections.singletonList(externalReference);
    }

    private PartOne buildPartOne(Notification notification) {
        PartOne partOne = new PartOne();

        // Build commodities
        partOne.setCommodities(buildCommodities(notification));

        // Build purpose
        partOne.setPurpose(buildPurpose(notification));

        // Map point of entry
        if (notification.getTransport() != null
            && notification.getTransport().getBcpCode() != null) {
            partOne.setPointOfEntry(notification.getTransport().getBcpCode());
        }

        // Build means of transport
        partOne.setMeansOfTransport(buildMeansOfTransport(notification));

        return partOne;
    }

    private Commodities buildCommodities(Notification notification) {
        Commodities commodities = new Commodities();

        // Map country of origin
        if (notification.getOriginCountry() != null) {
            commodities.setCountryOfOrigin(notification.getOriginCountry());
        }

        // Build commodity complements and parameter sets from species
        if (notification.getCommodity() != null && notification.getCommodity().getSpecies() != null
            && !notification.getCommodity().getSpecies().isEmpty()) {

            List<CommodityComplement> commodityComplements = new ArrayList<>();
            List<ComplementParameterSet> parameterSets = new ArrayList<>();
            int totalAnimals = 0;
            int totalPackages = 0;

            int complementId = 1;
            for (Species species : notification.getCommodity().getSpecies()) {
                // Build commodity complement for this species
                CommodityComplement complement = createCommodityComplement(
                    notification, species, complementId);

                commodityComplements.add(complement);

                // Build complement parameter set for this species
                ComplementParameterSet parameterSet = new ComplementParameterSet();
                parameterSet.setUniqueComplementID(UUID.randomUUID().toString());
                parameterSet.setComplementID(complementId);
                parameterSet.setSpeciesID(species.getCode());

                // Build key-data pairs for packages and animals
                List<KeyDataPair> keyDataPairs = new ArrayList<>();

                if (species.getNoOfPackages() != null) {
                    KeyDataPair packagePair = new KeyDataPair();
                    packagePair.setKey("number_package");
                    packagePair.setData(species.getNoOfPackages().toString());
                    keyDataPairs.add(packagePair);
                    totalPackages += species.getNoOfPackages();
                }

                if (species.getNoOfAnimals() != null) {
                    KeyDataPair animalPair = new KeyDataPair();
                    animalPair.setKey("number_animal");
                    animalPair.setData(species.getNoOfAnimals().toString());
                    keyDataPairs.add(animalPair);
                    totalAnimals += species.getNoOfAnimals();
                }

                parameterSet.setKeyDataPair(keyDataPairs);
                parameterSets.add(parameterSet);

                complementId++;
            }

            commodities.setCommodityComplement(commodityComplements);
            commodities.setComplementParameterSet(parameterSets);
            commodities.setNumberOfAnimals(totalAnimals);
            commodities.setNumberOfPackages(totalPackages);
        }

        return commodities;
    }

    private CommodityComplement createCommodityComplement(Notification notification,
        Species species, int complementId) {
        CommodityComplement complement = new CommodityComplement();
        complement.setCommodityID(notification.getCommodity().getCode());
        complement.setCommodityDescription(notification.getCommodity().getDescription());
        complement.setComplementID(complementId);
        complement.setComplementName(species.getName());
        complement.setSpeciesID(species.getCode());
        complement.setSpeciesName(species.getName());
        complement.setSpeciesNomination(species.getName());

        // Map species type from commodity type
        if (notification.getCommodity().getType() != null) {
            complement.setSpeciesTypeName(notification.getCommodity().getType());
        }
        return complement;
    }

    private Purpose buildPurpose(Notification notification) {
        Purpose purpose = new Purpose();

        if (notification.getInternalMarketPurpose() != null) {
            purpose.setInternalMarketPurpose(notification.getInternalMarketPurpose());
        }

        purpose.setPurposeGroup("For Import");

        return purpose;
    }

    private MeansOfTransport buildMeansOfTransport(Notification notification) {
        MeansOfTransport meansOfTransport = new MeansOfTransport();

        if (notification.getTransport() != null) {
            if (notification.getTransport().getVehicleId() != null) {
                meansOfTransport.setId(notification.getTransport().getVehicleId());
            }
            if (notification.getTransport().getTransportToBcp() != null) {
                meansOfTransport.setType(notification.getTransport().getTransportToBcp());
            }
        }

        return meansOfTransport;
    }
}
