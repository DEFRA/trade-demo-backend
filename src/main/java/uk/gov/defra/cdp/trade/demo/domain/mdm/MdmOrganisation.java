package uk.gov.defra.cdp.trade.demo.domain.mdm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MdmOrganisation {

  @EqualsAndHashCode.Include
  private UUID organisationUUID;
  private String name;
  private String code;
  private String buildingName;
  private String street;
  private String townCity;
  private String county;
  private String postcode;
  private String phone1;
  private String email1;
  private String countryCode;
  private String status;
  private String organisationType;

  private List<String> categoryCodes;

  private List<String> certificateCodes;

  @Builder.Default
  private List<MdmFacility> facilities = new ArrayList<>();

  @Builder.Default
  private Set<MdmPoe> pointsOfEntry = new HashSet<>();

  public void addFacility(MdmFacility facility) {
    facilities.add(facility);
    facility.setOrganisation(this);
  }

  public void addPointOfEntry(MdmPoe poe) {
    pointsOfEntry.add(poe);
    poe.getOrganisations().add(this);
  }

}
