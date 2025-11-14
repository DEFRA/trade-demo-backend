package uk.gov.defra.cdp.trade.demo.domain.mdm;

import java.util.HashSet;
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
public class MdmPoe {

  @EqualsAndHashCode.Include
  private UUID pointOfEntryUUID;
  private String code;
  private String name;
  private String status;

  @Builder.Default
  private Set<MdmOrganisation> organisations = new HashSet<>();

}
