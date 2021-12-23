package br.gov.es.openpmo.model.relations;

import br.gov.es.openpmo.model.baselines.Baseline;
import br.gov.es.openpmo.model.workpacks.Workpack;
import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

import java.beans.Transient;

@RelationshipEntity(type = "IS_BASELINED_BY")
public class IsBaselinedBy {

  @Id
  @GeneratedValue
  private Long id;

  @StartNode
  private Workpack workpack;

  @EndNode
  private Baseline baseline;

  public IsBaselinedBy() {
  }

  public IsBaselinedBy(final Baseline baseline, final Workpack workpack) {
    this.workpack = workpack;
    this.baseline = baseline;
  }

  public Long getId() {
    return this.id;
  }

  public void setId(final Long id) {
    this.id = id;
  }

  public Workpack getWorkpack() {
    return this.workpack;
  }

  public void setWorkpack(final Workpack workpack) {
    this.workpack = workpack;
  }

  public Baseline getBaseline() {
    return this.baseline;
  }

  public void setBaseline(final Baseline baseline) {
    this.baseline = baseline;
  }

  @Transient
  public Long getIdWorkpack() {
    return this.workpack.getId();
  }

  @Transient
  public Long getIdBaseline() {
    return this.baseline.getId();
  }
}
