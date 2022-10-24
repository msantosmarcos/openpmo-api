package br.gov.es.openpmo.model.relations;

import br.gov.es.openpmo.enumerator.PermissionLevelEnum;
import br.gov.es.openpmo.model.office.Office;
import br.gov.es.openpmo.model.workpacks.Workpack;
import br.gov.es.openpmo.model.workpacks.models.WorkpackModel;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.data.annotation.Transient;

@RelationshipProperties
public class IsSharedWith {

  @RelationshipId
  private Long id;

  private PermissionLevelEnum permissionLevel;

  private Workpack workpack;

  @TargetNode
  private Office office;

  public IsSharedWith() {
  }

  public Long getId() {
    return this.id;
  }

  public void setId(final Long id) {
    this.id = id;
  }

  public PermissionLevelEnum getPermissionLevel() {
    return this.permissionLevel;
  }

  public void setPermissionLevel(final PermissionLevelEnum permissionLevel) {
    this.permissionLevel = permissionLevel;
  }

  public String comboName() {
    return this.workpack.getName() + this.workpack.getOriginalOffice().map(a -> " (" + a.getName() + ") ").orElse("");
  }

  public Workpack getWorkpack() {
    return this.workpack;
  }

  public void setWorkpack(final Workpack workpack) {
    this.workpack = workpack;
  }

  public Office getOffice() {
    return this.office;
  }

  public void setOffice(final Office office) {
    this.office = office;
  }

  @Transient
  public Long workpackId() {
    return this.workpack.getId();
  }

  @Transient
  public WorkpackModel workpackInstance() {
    return this.workpack.getWorkpackModelInstance();
  }

  @Transient
  public Long getOfficeId() {
    return this.office.getId();
  }

  public boolean containsPlan(final Long idPlan) {
    return this.office.getPlans().stream().anyMatch(plan -> plan.getId().equals(idPlan));
  }

}
