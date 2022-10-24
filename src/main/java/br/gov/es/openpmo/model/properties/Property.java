package br.gov.es.openpmo.model.properties;

import br.gov.es.openpmo.model.Entity;
import br.gov.es.openpmo.model.baselines.Snapshotable;
import br.gov.es.openpmo.model.properties.models.PropertyModel;
import br.gov.es.openpmo.model.relations.IsPropertySnapshotOf;
import br.gov.es.openpmo.model.workpacks.Workpack;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import io.swagger.annotations.ApiModel;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import static org.springframework.data.neo4j.core.schema.Relationship.Direction.INCOMING;

import java.beans.Transient;
import java.util.Optional;
import java.util.Set;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = Integer.class, name = "Integer"),
  @JsonSubTypes.Type(value = Text.class, name = "Text"),
  @JsonSubTypes.Type(value = Date.class, name = "Date"),
  @JsonSubTypes.Type(value = Toggle.class, name = "Toggle"),
  @JsonSubTypes.Type(value = UnitSelection.class, name = "UnitSelection"),
  @JsonSubTypes.Type(value = Selection.class, name = "Selection"),
  @JsonSubTypes.Type(value = TextArea.class, name = "TextArea"),
  @JsonSubTypes.Type(value = Number.class, name = "Number"),
  @JsonSubTypes.Type(value = Currency.class, name = "Currency"),
  @JsonSubTypes.Type(value = LocalitySelection.class, name = "LocalitySelection"),
  @JsonSubTypes.Type(value = Group.class, name = "Group"),
  @JsonSubTypes.Type(value = OrganizationSelection.class, name = "OrganizationSelection")
})
@ApiModel(subTypes = {
  Integer.class,
  Text.class,
  Date.class,
  Toggle.class,
  UnitSelection.class,
  Selection.class,
  TextArea.class,
  Number.class,
  Currency.class,
  LocalitySelection.class,
  Group.class,
  OrganizationSelection.class
}, discriminator = "type", description = "Supertype of all Property.")
@Node
public abstract class Property<T, V> extends Entity implements HasValue<V>, Snapshotable<T> {

  @Relationship(type = "IS_SNAPSHOT_OF")
  private IsPropertySnapshotOf master;

  @Relationship(type = "IS_SNAPSHOT_OF", direction = INCOMING)
  private Set<IsPropertySnapshotOf> snapshots;

  protected Property() {
  }

  public abstract Workpack getWorkpack();

  public abstract void setWorkpack(Workpack workpack);

  public abstract PropertyModel getPropertyModel();

  @Transient
  public Long getPropertyModelId() {
    return Optional.ofNullable(this.getPropertyModel()).map(PropertyModel::getId).orElse(null);
  }

  public IsPropertySnapshotOf getMaster() {
    return this.master;
  }

  public void setMaster(final IsPropertySnapshotOf master) {
    this.master = master;
  }

  public Set<IsPropertySnapshotOf> getSnapshots() {
    return this.snapshots;
  }

  public void setSnapshots(final Set<IsPropertySnapshotOf> snapshots) {
    this.snapshots = snapshots;
  }

}
