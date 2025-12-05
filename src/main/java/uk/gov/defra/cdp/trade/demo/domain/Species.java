package uk.gov.defra.cdp.trade.demo.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Species details for the notification.
 * Represents individual species within the commodity with quantity information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Species {
    private String name;
    private String code;
    private Integer noOfAnimals;
    private Integer noOfPackages;
}
