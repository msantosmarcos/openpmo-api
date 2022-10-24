package br.gov.es.openpmo.model.workpacks.models;

import br.gov.es.openpmo.model.workpacks.Program;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import static org.springframework.data.neo4j.core.schema.Relationship.Direction.INCOMING;

import java.util.Set;

@Node
public class ProgramModel extends WorkpackModel {

  @Relationship(value = "IS_INSTANCE_BY", direction = INCOMING)
  private Set<Program> instances;

  public Set<Program> getInstances() {
    return this.instances;
  }

  public void setInstances(final Set<Program> instances) {
    this.instances = instances;
  }

}
