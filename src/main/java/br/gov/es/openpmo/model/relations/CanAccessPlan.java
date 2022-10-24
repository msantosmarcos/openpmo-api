package br.gov.es.openpmo.model.relations;

import br.gov.es.openpmo.enumerator.PermissionLevelEnum;
import br.gov.es.openpmo.model.actors.Person;
import br.gov.es.openpmo.model.office.plan.Plan;
import br.gov.es.openpmo.scheduler.updateroles.HasRole;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.annotation.Transient;


@RelationshipProperties
public class CanAccessPlan implements HasRole {

  @RelationshipId
  private Long id;
  private String organization;
  @Property("permitedRole")
  private String role;
  private PermissionLevelEnum permissionLevel;

  private Person person;

  @TargetNode
  private Plan plan;

  public CanAccessPlan(
    final Long id,
    final String organization,
    final String role,
    final PermissionLevelEnum permissionLevel,
    final Person person,
    final Plan plan
  ) {
    this.id = id;
    this.organization = organization;
    this.role = role;
    this.permissionLevel = permissionLevel;
    this.person = person;
    this.plan = plan;
  }

  @Transient
  public String getPlanName() {
    if(this.plan == null) {
      return null;
    }
    return this.plan.getName();
  }

  @Transient
  public Long getIdPlan() {
    if(this.plan == null) {
      return null;
    }
    return this.plan.getId();
  }

  public Long getId() {
    return this.id;
  }

  public void setId(final Long id) {
    this.id = id;
  }

  public String getOrganization() {
    return this.organization;
  }

  public void setOrganization(final String organization) {
    this.organization = organization;
  }

  public String getRole() {
    return this.role;
  }

  public void setRole(final String role) {
    this.role = role;
  }

  public PermissionLevelEnum getPermissionLevel() {
    return this.permissionLevel;
  }

  public void setPermissionLevel(final PermissionLevelEnum permissionLevel) {
    this.permissionLevel = permissionLevel;
  }

  public Person getPerson() {
    return this.person;
  }

  public void setPerson(final Person person) {
    this.person = person;
  }

  public Plan getPlan() {
    return this.plan;
  }

  public void setPlan(final Plan plan) {
    this.plan = plan;
  }

  @Transient
  public boolean hasSameUser(final Long idUser) {
    if(this.person == null) return false;
    return this.person.getId().equals(idUser);
  }

}
