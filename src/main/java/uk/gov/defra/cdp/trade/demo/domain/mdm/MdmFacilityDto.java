package uk.gov.defra.cdp.trade.demo.domain.mdm;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MdmFacilityDto {
  private UUID facilityUUID;
  @EqualsAndHashCode.Include
  private String code;
  private String name;
  private MdmStatus facilityStatus;

  public MdmFacility toEntity() {
    return MdmFacility.builder()
        .facilityUUID(facilityUUID)
        .code(code)
        .name(name)
        .status(facilityStatus != null ? facilityStatus.getCode() : null)
        .build();
  }
}
