package uk.gov.defra.cdp.trade.demo.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.defra.cdp.trade.demo.domain.Notification;
import uk.gov.defra.cdp.trade.demo.domain.ipaffs.IpaffsNotification;

/**
 * Mapper for converting CDP Notification to IPAFFS CHEDA format.
 *
 * Loads the draft-cheda-plane.json template and overwrites fields
 * with values from the CDP notification.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ChedaMapper {

    private static final String TEMPLATE_PATH = "/draft-cheda-plane.json";

    private final ObjectMapper objectMapper;

    /**
     * Map a CDP Notification to an IPAFFS notification format.
     *
     * @param notification the CDP notification to map
     * @return the IPAFFS notification with mapped fields
     */
    public IpaffsNotification mapToIpaffsNotification(Notification notification) {
        log.debug("Mapping notification {} to IPAFFS format", notification.getId());

        IpaffsNotification ipaffsNotification = loadTemplate();

        // Map origin country
        if (notification.getOriginCountry() != null && ipaffsNotification.getPartOne() != null
                && ipaffsNotification.getPartOne().getCommodities() != null) {
            ipaffsNotification.getPartOne().getCommodities()
                .setCountryOfOrigin(notification.getOriginCountry());
        }

        // Map internal market purpose
        if (notification.getInternalMarketPurpose() != null && ipaffsNotification.getPartOne() != null
                && ipaffsNotification.getPartOne().getPurpose() != null) {
            ipaffsNotification.getPartOne().getPurpose()
                .setInternalMarketPurpose(notification.getInternalMarketPurpose());
        }

        // Map commodity details
        if (notification.getCommodity() != null && ipaffsNotification.getPartOne() != null
                && ipaffsNotification.getPartOne().getCommodities() != null
                && ipaffsNotification.getPartOne().getCommodities().getCommodityComplement() != null
                && !ipaffsNotification.getPartOne().getCommodities().getCommodityComplement().isEmpty()) {

            var commodityComplement = ipaffsNotification.getPartOne().getCommodities()
                .getCommodityComplement().get(0);

            if (notification.getCommodity().getCode() != null) {
                commodityComplement.setCommodityID(notification.getCommodity().getCode());
            }
            if (notification.getCommodity().getDescription() != null) {
                commodityComplement.setCommodityDescription(notification.getCommodity().getDescription());
            }
        }

        // Map transport details
        if (notification.getTransport() != null && ipaffsNotification.getPartOne() != null) {
            // Map BCP code to point of entry
            if (notification.getTransport().getBcpCode() != null) {
                ipaffsNotification.getPartOne().setPointOfEntry(notification.getTransport().getBcpCode());
            }

            // Map transport type and vehicle ID to means of transport
            if (ipaffsNotification.getPartOne().getMeansOfTransport() != null) {
                if (notification.getTransport().getTransportToBcp() != null) {
                    ipaffsNotification.getPartOne().getMeansOfTransport()
                        .setType(notification.getTransport().getTransportToBcp());
                }
                if (notification.getTransport().getVehicleId() != null) {
                    ipaffsNotification.getPartOne().getMeansOfTransport()
                        .setId(notification.getTransport().getVehicleId());
                }
            }
        }

        // Set external reference to notification ID
        if (ipaffsNotification.getExternalReferences() != null
                && !ipaffsNotification.getExternalReferences().isEmpty()) {
            ipaffsNotification.getExternalReferences().get(0)
                .setReference(notification.getId());
        }

        log.debug("Successfully mapped notification {} to IPAFFS format", notification.getId());
        return ipaffsNotification;
    }

    /**
     * Load the CHEDA template from resources.
     *
     * @return the template as an IpaffsNotification object
     */
    private IpaffsNotification loadTemplate() {
        try (InputStream inputStream = getClass().getResourceAsStream(TEMPLATE_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException("Template file not found: " + TEMPLATE_PATH);
            }

            IpaffsNotification template = objectMapper.readValue(inputStream, IpaffsNotification.class);
            log.debug("Loaded CHEDA template from {}", TEMPLATE_PATH);
            return template;

        } catch (IOException e) {
            log.error("Failed to load CHEDA template from {}", TEMPLATE_PATH, e);
            throw new IllegalStateException("Failed to load CHEDA template", e);
        }
    }
}
