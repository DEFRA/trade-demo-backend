package uk.gov.defra.cdp.trade.demo.domain.mdm;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MdmOrganisationDto {

  private UUID organisationUUID;
  private String name;
  @EqualsAndHashCode.Include
  private String code;
  private List<String> categoryCodes;
  private String buildingName;
  private String street;
  private String townCity;
  private String county;
  private String postCode;
  private String phone1;
  private String phone2;
  private String email1;
  private String email2;
  private String contact;
  private String countryCode;
  private MdmStatus status;
  private MdmType organisationType;
  private List<String> certificateCodes;

  @Builder.Default
  private List<MdmPoeDto> pointsOfEntry = new ArrayList<>();

  @Builder.Default
  private List<MdmFacilityDto> facilities = new ArrayList<>();

  public MdmOrganisation toEntity() {

    var mdmBcp = MdmOrganisation.builder()
        .organisationUUID(organisationUUID)
        .name(name)
        .code(code)
        .categoryCodes(categoryCodes)
        .buildingName(buildingName)
        .street(street)
        .townCity(townCity)
        .county(county)
        .postcode(postCode)
        .phone1(phone1)
        .email1(email1)
        .countryCode(countryCode)
        .status(status != null ? status.getCode() : null)
        .certificateCodes(certificateCodes)
        .organisationType(organisationType != null ? organisationType.getCode() : null)
        .build();

    if (facilities != null && !facilities.isEmpty()) {
      facilities.stream()
          .filter(f -> StringUtils.isNotBlank(f.getCode()))
          .collect(Collectors.toSet())
          .forEach(f -> mdmBcp.addFacility(f.toEntity()));
    }
    if (pointsOfEntry != null && !pointsOfEntry.isEmpty()) {
      pointsOfEntry.stream()
          .filter(poe -> StringUtils.isNotBlank(poe.getCode()))
          .collect(Collectors.toSet())
          .forEach(poe -> mdmBcp.addPointOfEntry(poe.toEntity()));
    }

    return mdmBcp;
  }
}
