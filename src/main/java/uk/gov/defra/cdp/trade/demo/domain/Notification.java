package uk.gov.defra.cdp.trade.demo.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Notification (CHED) entity for storing import notification submissions.
 *
 * Represents the complete notification/CHED data collected through the import journey
 * in trade-demo-frontend, including origin country, commodity information, and purpose.
 */
@Document(collection = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    private String id;

    @Indexed(unique = true, sparse = true)
    private String chedReference;
    
    private String status;

    private String originCountry;

    private Commodity commodity;

    private String importReason;

    private String internalMarketPurpose;
    
    private Transport transport;

    private LocalDateTime created;

    private LocalDateTime updated;
}
