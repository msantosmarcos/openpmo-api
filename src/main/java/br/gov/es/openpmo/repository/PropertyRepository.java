package br.gov.es.openpmo.repository;

import br.gov.es.openpmo.model.properties.Property;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends Neo4jRepository<Property, Long> {

  @Query("MATCH (p:Property)<-[:FEATURES]-(w:Workpack) WHERE ID(w) = $idWorkpack RETURN p ")
  List<Property> findAllByIdWorkpack(@Param("idWorkpack") Long idWorkpack);

  @Query("match (ls:Property) " +
         "where id(ls)=$idLocalitySelection " +
         "return ls, [ " +
         "    [(ls)-[v:VALUES]->(l:Locality) | [v,l]] " +
         "]")
  Optional<Property> findByIdWithProperties(Long idLocalitySelection);

  @Query("match (m:Property)<-[i:IS_SNAPSHOT_OF]-(s:Property)-[c:COMPOSES]->(b:Baseline)  " +
         "where id(m)=$idProperty and id(b)=$idBaseline " +
         "return s, [ " +
         "    [(s)<-[v1:VALUES]->(l:Locality) | [v1,l]],  " +
         "    [(s)<-[v2:VALUES]->(o:Organization) | [v2,o]],  " +
         "    [(s)<-[v3:VALUES]->(u:UnitMeasure) | [v3,u]] " +
         "]")
  Optional<Property> findSnapshotByMasterIdAndBaselineId(
    Long idProperty,
    Long idBaseline
  );

  @Query("match (a:Property)-[:IS_SNAPSHOT_OF]->(m:Property)<-[i:IS_SNAPSHOT_OF]-(s:Property)-[c:COMPOSES]->(b:Baseline)   " +
         "where id(a)=$idProperty and id(b)=$idBaseline  " +
         "return s, [  " +
         "    [(s)<-[v1:VALUES]->(l:Locality) | [v1,l]],   " +
         "    [(s)<-[v2:VALUES]->(o:Organization) | [v2,o]],   " +
         "    [(s)<-[v3:VALUES]->(u:UnitMeasure) | [v3,u]]  " +
         "]")
  Optional<Property> findAnotherSnapshotOfMasterBySnapshotIdAndAnotherBaselineId(
    Long idProperty,
    Long idBaseline
  );

  @Query("match (m:Property)<-[i:IS_SNAPSHOT_OF]-(s:Property) " +
         "where id(s)=$idSnapshot " +
         "return m")
  Optional<Property> findMasterBySnapshotId(Long idSnapshot);

  @Query("match (w:Workpack)<-[f:FEATURES]-(p:Property) " +
         "where id(w)=$workpackId and id(p)=$propertyId " +
         "detach delete f")
  void deleteFeaturesRelationshipByPropertyIdAndWorkpackId(
    Long workpackId,
    Long propertyId
  );

  @Query("match (p:Property), (pm:PropertyModel) " +
         "where id(p)=$propertyId and id(pm)=$propertyModelId " +
         "create (p)-[:IS_DRIVEN_BY]->(pm)")
  void createIsDrivenByRelationship(
    Long propertyId,
    Long propertyModelId
  );

}
