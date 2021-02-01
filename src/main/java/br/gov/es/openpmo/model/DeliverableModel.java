package br.gov.es.openpmo.model;

import java.util.Set;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class DeliverableModel extends WorkpackModel {

    @Relationship(value = "IS_INSTANCE_BY", direction = Relationship.INCOMING)
    private Set<Deliverable> instances;

    public Set<Deliverable> getInstances() {
        return instances;
    }

    public void setInstances(Set<Deliverable> instances) {
        this.instances = instances;
    }
}
