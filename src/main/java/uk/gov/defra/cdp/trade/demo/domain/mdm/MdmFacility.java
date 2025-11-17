package uk.gov.defra.cdp.trade.demo.domain.mdm;

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
public class MdmFacility {

  @EqualsAndHashCode.Include
  private UUID facilityUUID;
  private String code;
  private String name;
  private String status;

  private MdmOrganisation organisation;

}
