package uk.gov.defra.cdp.trade.demo.domain.ipaffs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommodityComplement {
    private String commodityID;
    private String commodityDescription;
    private Integer complementID;
    private String complementName;
    private String speciesID;
    private String speciesName;
    private String speciesTypeName;
    private String speciesType;
    private String speciesClass;
    private String speciesNomination;
}
