package uk.gov.defra.cdp.trade.demo.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.defra.cdp.trade.demo.domain.Commodity;
import uk.gov.defra.cdp.trade.demo.domain.NotificationDto;

/**
 * Request object for creating a notification in IPAFFS via the Imports Proxy.
 *
 * <p>This class represents the API request structure, separating the concerns
 * of the API contract from the domain model (NotificationDto).
 *
 * <p>The request contains only the fields required to create a new notification.
 * Server-generated fields like 'id' are not included.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationRequest {

    /**
     * CHED reference number for the notification.
     * Example: CHED-2024-001
     */
    private String chedReference;

    /**
     * ISO country code of the origin country.
     * Example: GB, FR, DE
     */
    private String originCountry;

    /**
     * Commodity information for the notification.
     */
    private Commodity commodity;

    /**
     * Reason for importing the commodity.
     * Example: Commercial import, Personal use
     */
    private String importReason;

    /**
     * Internal market purpose for the import.
     * Example: Direct sale, Further processing
     */
    private String internalMarketPurpose;

    /**
     * Converts this request to a NotificationDto for internal processing.
     *
     * @return NotificationDto representation of this request
     */
    public NotificationDto toDto() {
        NotificationDto dto = new NotificationDto();
        dto.setChedReference(this.chedReference);
        dto.setOriginCountry(this.originCountry);
        dto.setCommodity(this.commodity);
        dto.setImportReason(this.importReason);
        dto.setInternalMarketPurpose(this.internalMarketPurpose);
        return dto;
    }

    /**
     * Creates a request from a NotificationDto.
     * Note: Server-generated fields like 'id' are excluded.
     *
     * @param dto The NotificationDto to convert
     * @return CreateNotificationRequest
     */
    public static CreateNotificationRequest fromDto(NotificationDto dto) {
        return CreateNotificationRequest.builder()
                .chedReference(dto.getChedReference())
                .originCountry(dto.getOriginCountry())
                .commodity(dto.getCommodity())
                .importReason(dto.getImportReason())
                .internalMarketPurpose(dto.getInternalMarketPurpose())
                .build();
    }
}
