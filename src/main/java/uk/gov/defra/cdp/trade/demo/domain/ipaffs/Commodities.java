package uk.gov.defra.cdp.trade.demo.domain.ipaffs;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Commodities {
    private Integer numberOfPackages;
    private Integer numberOfAnimals;
    private List<CommodityComplement> commodityComplement;
    private List<ComplementParameterSet> complementParameterSet;
    private Boolean includeNonAblactedAnimals;
    private String countryOfOrigin;
    private String animalsCertifiedAs;
}
