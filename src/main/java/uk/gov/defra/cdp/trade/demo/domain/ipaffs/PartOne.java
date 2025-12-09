package uk.gov.defra.cdp.trade.demo.domain.ipaffs;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartOne {
    private PersonResponsible personResponsible;
    private Party consignor;
    private Party consignee;
    private Party importer;
    private Party placeOfDestination;
    private String cphNumber;
    private Commodities commodities;
    private Purpose purpose;
    private String pointOfEntry;
    private String arrivalDate;
    private String arrivalTime;
    private Party transporter;
    private MeansOfTransport meansOfTransport;
    private MeansOfTransport meansOfTransportFromEntryPoint;
    private String departureDate;
    private String departureTime;
    private Integer estimatedJourneyTimeInMinutes;
    private Map<String, Object> veterinaryInformation;
    private String importerLocalReferenceNumber;
    private Boolean complexCommoditySelected;
    private String portOfEntry;
    private Boolean isGVMSRoute;
    private String provideCtcMrn;
    private String storeTransporterContact;
}
