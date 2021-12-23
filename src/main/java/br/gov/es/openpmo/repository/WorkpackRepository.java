package br.gov.es.openpmo.repository;

import br.gov.es.openpmo.dto.person.queries.WorkpackPermissionAndStakeholderQuery;
import br.gov.es.openpmo.dto.workpack.WorkpackName;
import br.gov.es.openpmo.model.workpacks.Workpack;
import br.gov.es.openpmo.model.workpacks.models.WorkpackModel;
import br.gov.es.openpmo.repository.custom.CustomRepository;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface WorkpackRepository extends Neo4jRepository<Workpack, Long>, CustomRepository {

  @Query("MATCH (w:Workpack)-[rf:BELONGS_TO]->(p:Plan), "
         + "(p)-[is:IS_STRUCTURED_BY]->(pm:PlanModel) "
         + "OPTIONAL MATCH (w)-[ii:IS_INSTANCE_BY]->(wm:WorkpackModel) "
         + "OPTIONAL MATCH (w)-[lt:IS_LINKED_TO]->(wm2:WorkpackModel)-[bt:BELONGS_TO]->(pm) "
         + "WITH w, rf, p, is, pm, ii, wm, lt, wm2, bt "
         + "WHERE id(p)=$idPlan "
         + "AND (id(pm)=$idPlanModel OR $idPlanModel IS NULL) "
         + "AND (id(wm)=$idWorkPackModel OR id(wm2)=$idWorkPackModel OR $idWorkPackModel IS NULL) "
         + "RETURN w, rf, p, ii, pm, wm, lt, wm2, bt, [ "
         + " [ (w)<-[f:FEATURES]-(p:Property)-[d:IS_DRIVEN_BY]->(pm:PropertyModel) | [f, p, d, pm] ], "
         + " [ (w)<-[wi:IS_IN]-(w2:Workpack) | [wi, w2] ],"
         + " [ (w)-[wi2:IS_IN]->(w3:Workpack) | [wi2, w3] ],"
         + " [ (w)<-[wa:APPLIES_TO]-(ca:CostAccount) | [wa, ca] ],"
         + " [ (w)<-[wfg:FEATURES]-(wg:Group) | [wfg, wg] ], "
         + " [ (wg)-[wgps:GROUPS]->(wgp:Property)-[gpd:IS_DRIVEN_BY]->(gpm:PropertyModel) | [wgps, wgp, gpd, gpm] ], "
         + " [ (ca)<-[f1:FEATURES]-(p2:Property)-[d1:IS_DRIVEN_BY]->(pmc:PropertyModel) | [ca, f1, p2, d1, pmc ] ],"
         + " [ (wm)<-[wmi:IS_IN]-(wm2:WorkpackModel) | [wmi,wm2] ],"
         + " [ (wm)-[wmi2:IS_IN]->(wm3:WorkpackModel) | [wmi2,wm3] ],"
         + " [ (wm)<-[f2:FEATURES]-(pm2:PropertyModel) | [f2, pm2] ], "
         + " [ (wm)-[featureGroup:FEATURES]->(group:GroupModel) | [featureGroup, group] ], "
         + " [ (group)-[groups:GROUPS]->(groupedProperty:PropertyModel) | [groups, groupedProperty] ], "
         + " [ (w)-[sharedWith:IS_SHARED_WITH]->(office:Office) | [sharedWith, office]], "
         + " [ (w)-[isLinkedTo:IS_LINKED_TO]->(workpackModel:WorkpackModel) | [isLinkedTo, workpackModel] ] "
         + "]")
  List<Workpack> findAll(
    @Param("idPlan") Long idPlan,
    @Param("idPlanModel") Long idPlanModel,
    @Param("idWorkPackModel") Long idWorkPackModel
  );

  @Query("MATCH (wp:Workpack{deleted:false})-[bt:BELONGS_TO]->(p:Plan) " +
         "MATCH (wp)<-[wc:IS_IN*]-(w:Workpack{deleted:false}) " +
         "OPTIONAL MATCH (w)-[ii:IS_INSTANCE_BY]->(wm:WorkpackModel) " +
         "OPTIONAL MATCH (w)-[lt:IS_LINKED_TO]->(wm2:WorkpackModel) " +
         "WITH wp, bt, p, wc, w, ii, wm, lt, wm2 " +
         "WHERE id(wp) = $idWorkpackParent " +
         "AND (id(wm)=$idWorkpackModel OR id(wm2)=$idWorkpackModel OR $idWorkpackModel IS NULL) " +
         "RETURN w, wc, ii, wm, lt, wm2, [ " +
         " [ (w)<-[f:FEATURES]-(p:Property)-[d:IS_DRIVEN_BY]->(pm:PropertyModel) | [f, p, d, pm] ], " +
         " [ (w)<-[wi:IS_IN]-(w2:Workpack{deleted:false}) | [wi, w2] ]," +
         " [ (w)-[wi2:IS_IN]->(w3:Workpack{deleted:false}) | [wi2, w3] ]," +
         " [ (w)<-[wa:APPLIES_TO]-(ca:CostAccount) | [wa, ca] ]," +
         " [ (ca)<-[f1:FEATURES]-(p2:Property)-[d1:IS_DRIVEN_BY]->(pmc:PropertyModel) | [ca, f1, p2, d1, pmc ] ]," +
         " [ (wm)<-[wmi:IS_IN]-(wm2:WorkpackModel) | [wmi,wm2] ]," +
         " [ (wm)-[wmi2:IS_IN]->(wm3:WorkpackModel) | [wmi2,wm3] ]," +
         " [ (wm)<-[f2:FEATURES]-(pm2:PropertyModel) | [f2, pm2] ]," +
         " [ (w)-[sharedWith:IS_SHARED_WITH]->(office:Office) | [sharedWith, office]], " +
         " [ (wp)-[parentSharedWith:IS_SHARED_WITH]->(officeParent:Office) | [parentSharedWith, officeParent]], " +
         " [ (w)-[isLinkedTo:IS_LINKED_TO]->(workpackModel:WorkpackModel) | [isLinkedTo, workpackModel] ] " +
         " ]")
  List<Workpack> findAllUsingParent(
    @Param("idWorkpackModel") Long idWorkpackModel,
    @Param("idWorkpackParent") Long idWorkpackParent
  );

  @Query("OPTIONAL MATCH (w:Workpack{deleted:false})-[ro:BELONGS_TO]->(pl:Plan), (w)-[wp:IS_INSTANCE_BY]->(wm:WorkpackModel) "
         + "WITH w, ro, pl, wp, wm "
         + "WHERE id(w) = $id "
         + "RETURN w, ro, pl, wp, wm, [ "
         + " [(w)<-[f:FEATURES]-(p:Property)-[d:IS_DRIVEN_BY]->(pm:PropertyModel) | [f, p, d, pm] ], "
         + " [(p)-[v1:VALUES]->(o:Organization) | [v1, o] ], "
         + " [(p)-[v2:VALUES]-(l:Locality) | [v2, l] ], "
         + " [(p)-[v3:VALUES]-(u:UnitMeasure) | [v3, u] ], "
         + " [(w)<-[wfg:FEATURES]-(wg:Group) | [wfg, wg] ], "
         + " [(wg)-[wgps:GROUPS]->(wgp:Property)-[gpd:IS_DRIVEN_BY]->(gpm:PropertyModel) | [wgps, wgp, gpd, gpm] ], "
         + " [(w)<-[wi:IS_IN]-(w2:Workpack{deleted:false}) | [wi, w2] ], "
         + " [(w)-[wi2:IS_IN]->(w3:Workpack{deleted:false})-[wp3:IS_INSTANCE_BY]->(wm3:WorkpackModel) | [wi2, w3, wp3, wm3] ],"
         + " [(w)<-[wa:APPLIES_TO]-(ca:CostAccount) | [wa, ca] ],"
         + " [(ca)<-[f1:FEATURES]-(p2:Property)-[d1:IS_DRIVEN_BY]->(pmc:PropertyModel) | [ca, f1, p2, d1, pmc ] ],"
         + " [(wm)<-[wmi:IS_IN]-(wm2:WorkpackModel) | [wmi,wm2] ],"
         + " [(wm)-[wmi2:IS_IN]->(wm3:WorkpackModel) | [wmi2,wm3] ],"
         + " [(wm)<-[f2:FEATURES]-(pm2:PropertyModel) | [f2, pm2] ], "
         + " [(wm)-[featureGroup:FEATURES]->(group:GroupModel) | [featureGroup, group] ], "
         + " [(group)-[groups:GROUPS]->(groupedProperty:PropertyModel) | [groups, groupedProperty] ] "
         + "]")
  Optional<Workpack> findByIdWorkpack(@Param("id") Long id);

  @Query("MATCH (w:Workpack{deleted:false})-[rf:BELONGS_TO]->(p:Plan)-[io:IS_ADOPTED_BY]->(o:Office), "
         + "  (p)-[is:IS_STRUCTURED_BY]->(pm:PlanModel), "
         + "  (w)-[ii:IS_INSTANCE_BY]->(wm:WorkpackModel) "
         + " WHERE id(p) = $idPlan AND NOT (w)-[:IS_IN]->(:Workpack) "
         + " RETURN w, rf, p, io, o, ii, pm, wm, [ "
         + " [(p)<-[cp:CAN_ACCESS_PLAN]-(p2:Person) | [cp, p2] ],"
         + " [(w)<-[ca:CAN_ACCESS_WORKPACK]-(p:Person) | [ca, p] ],"
         + " [(w)<-[wi:IS_IN*]-(w2:Workpack{deleted:false})-[ii_2:IS_INSTANCE_BY]->(wm_2:WorkpackModel) | [wi, w2, ii_2, wm_2] ],"
         + " [(w2)<-[ca2:CAN_ACCESS_WORKPACK]-(p2:Person) | [ca2, p2] ],"
         + " [(w2)-[rf_2:BELONGS_TO]->(p_2:Plan)-[io_2:IS_ADOPTED_BY]->(o_2:Office) | [rf_2, p_2, io_2, o_2] ],"
         + " [(w)-[wi2:IS_IN*]->(w3:Workpack{deleted:false})-[ii_3:IS_INSTANCE_BY]->(wm_3:WorkpackModel) | [wi2, w3, ii_3, wm_3] ],"
         + " [(w3)-[rf_3:BELONGS_TO]->(p_3:Plan)-[io_3:IS_ADOPTED_BY]->(o_3:Office) | [rf_3, p_3, io_3, o_3] ],"
         + " [(wm)<-[wmi:IS_IN*]-(wm2:WorkpackModel) | [wmi,wm2] ]"
         + " ]")
  Set<Workpack> findAllUsingPlan(@Param("idPlan") Long idPlan);

  @Query("MATCH (plan:Plan) " +
         "MATCH (plan)<-[belongsTo:BELONGS_TO]-(w:Workpack{deleted:false}) " +
         "MATCH (plan)-[isStructuredBy:IS_STRUCTURED_BY]->(planModel:PlanModel) " +
         "MATCH (w)-[instanceBy:IS_INSTANCE_BY]->(model:WorkpackModel) " +
         "WHERE id(plan) = $idPlan AND NOT (w)-[:IS_IN]->(:Workpack) " +
         "RETURN w, belongsTo, isStructuredBy, plan, instanceBy, planModel, model, [ " +
         " [(w)-[isLinkedTo:IS_LINKED_TO]-(modelLinked:WorkpackModel) | [isLinkedTo, modelLinked] ], " +
         " [(w)<-[f1:FEATURES]-(p1:Property)-[d1:IS_DRIVEN_BY]->(pm1:PropertyModel) | [f1, p1, d1, pm1] ], " +
         " [(w)<-[wi:IS_IN*]-(w2:Workpack{deleted:false})<-[f2:FEATURES]-(p2:Property)-[d2:IS_DRIVEN_BY]->(pm2:PropertyModel) | [wi,w2,f2, p2, d2, pm2] ], " +
         " [(w2)-[bt:BELONGS_TO]->(p:Plan) | [bt, p]], " +
         " [(w2)-[ib2:IS_INSTANCE_BY]->(wm2:WorkpackModel)<-[f5:FEATURES]-(pm5:PropertyModel) | [ib2, wm2, f5, pm5] ], " +
         " [(model)<-[f4:FEATURES]-(pm4:PropertyModel) | [f4, pm4] ], " +
         " [(model)<-[wmi:IS_IN*]-(wm2:WorkpackModel)<-[f6:FEATURES]-(pm6:PropertyModel) | [wmi,wm2, f6, pm6] ], " +
         " [(model)-[gmf:FEATURES]->(gm:GroupModel) | [gmf, gm] ], " +
         " [(gm)-[gms:GROUPS]->(gpm:PropertyModel) | [gms, gpm] ] " +
         " ]")
  Set<Workpack> findAllByPlanWithProperties(@Param("idPlan") Long idPlan);

  @Query("MATCH (w:Workpack{deleted:false})-[ii:IS_INSTANCE_BY]->(wm:WorkpackModel) "
         + " WHERE id(w) = $id "
         + " RETURN w, ii, wm, [ "
         + " [(w)-[bt:BELONGS_TO]->(pl:Plan) | [bt, pl]], "
         + " [(w)<-[lt:IS_LINKED_TO]-(wml:WorkpackModel) | [lt, wml] ], "
         + " [(wml)<-[mii:IS_IN*]-(wmlc:WorkpackModel) | [mii, wmlc] ], "
         + " [(w)<-[f1:FEATURES]-(p1:Property)-[d1:IS_DRIVEN_BY]->(pm1:PropertyModel) | [f1, p1, d1, pm1] ], "
         + " [(w)-[wi:IS_IN*]->(w2:Workpack)<-[f2:FEATURES]-(p2:Property)-[d2:IS_DRIVEN_BY]->(pm2:PropertyModel) | [wi,w2,f2, p2, d2, pm2] ], "
         + " [(w)<-[f2:FEATURES]-(l:LocalitySelection)-[v1:VALUES]->(l1:Locality) | [f2,l,v1,l1]],  "
         + " [(w)<-[f3:FEATURES]-(o:OrganizationSelection)-[v2:VALUES]->(o1:Organization) | [f3,o,v2,o1]],  "
         + " [(w)<-[f4:FEATURES]-(u:UnitSelection)-[v3:VALUES]->(u1:UnitMeasure) | [f4,u,v3,u1]],  "
         + " [(w2)-[bt2:BELONGS_TO]->(pl2:Plan) | [bt2, pl2] ], "
         + " [(w2)<-[f5:FEATURES]-(l2:LocalitySelection)-[v4:VALUES]->(l2:Locality) | [f5, l2, v4, l2]],  "
         + " [(w2)<-[f6:FEATURES]-(o2:OrganizationSelection)-[v5:VALUES]->(o2:Organization) | [f6,o2,v5,o2]],  "
         + " [(w2)<-[f7:FEATURES]-(u2:UnitSelection)-[v6:VALUES]->(u2:UnitMeasure) | [f7,u2,v6,u2]],  "
         + " [(w)<-[wfg:FEATURES]-(wg:Group) | [wfg, wg] ], "
         + " [(wg)-[wgps:GROUPS]->(wgp:Property)-[gpd:IS_DRIVEN_BY]->(gpm:PropertyModel) | [wgps, wgp, gpd, gpm] ], "
         + " [(w2)-[ib2:IS_INSTANCE_BY]->(wm2:WorkpackModel)<-[f8:FEATURES]-(pm5:PropertyModel) | [ib2, wm2, f8, pm5] ], "
         + " [(wm)<-[f9:FEATURES]-(pm4:PropertyModel) | [f9, pm4] ], "
         + " [(wm)-[featureGroupModel:FEATURES]->(groupModel:GroupModel) | [featureGroupModel, groupModel] ], "
         + " [(groupModel)-[groupModels:GROUPS]->(groupedPropertiesModel:PropertyModel) | [groupModels, groupedPropertiesModel] ] "
         + " ]"
  )
  Optional<Workpack> findByIdWithParent(@Param("id") Long id);

  @Query("MATCH (w:Workpack{deleted:false})-[:IS_IN*]-(child:Workpack{deleted:false})<-[canAccess:CAN_ACCESS_WORKPACK]-(p:Person) " +
         " WHERE id(w) = $idWorkpack " +
         " AND id(p) = $idPerson " +
         " AND canAccess.idPlan = $idPlan" +
         " RETURN count(canAccess)")
  Long countCanAccessWorkpack(
    @Param("idWorkpack") Long idWorkpack,
    @Param("idPerson") Long idPerson,
    @Param("idPlan") Long idPlan
  );

  @Query(
    "MATCH (person:Person) " +
    "WHERE id(person)=$idPerson " +
    "MATCH (plan:Plan)<-[belongsTo:BELONGS_TO]-(workpack:Workpack{deleted:false}) " +
    "OPTIONAL MATCH (workpack)<-[permission:CAN_ACCESS_WORKPACK]-(person) " +
    "WITH plan, belongsTo, workpack, permission, person " +
    "WHERE id(plan)=$idPlan AND ( " +
    " ( (workpack)<-[:CAN_ACCESS_WORKPACK]-(person) ) OR " +
    " ( person.administrator=true ) " +
    ") " +
    "RETURN id(workpack)")
  Set<Long> findAllWorkpacksWithPermissions(
    @Param("idPlan") Long idPlan,
    @Param("idPerson") Long idPerson
  );

  @Query("MATCH (plan:Plan)<-[belongsTo:BELONGS_TO]-(workpack:Workpack{deleted:false})-[instance:IS_INSTANCE_BY]->(model:WorkpackModel) " +
         "OPTIONAL MATCH (workpack)<-[stakeholder:IS_STAKEHOLDER_IN]-(person:Person) " +
         "OPTIONAL MATCH (workpack)<-[canAccessWorkpack:CAN_ACCESS_WORKPACK]-(person) " +
         "WITH workpack, plan, belongsTo, stakeholder, canAccessWorkpack, person, instance, model " +
         "WHERE id(person)=$idPerson AND id(plan)=$idPlan " +
         "RETURN collect(workpack) AS workpacks, " +
         "collect(canAccessWorkpack) AS canAccess, " +
         "collect(stakeholder) AS isStakeholderIn, " +
         "collect(belongsTo), " +
         "collect(instance), " +
         "collect(model) ")
  WorkpackPermissionAndStakeholderQuery findAllByPersonAndPlan(
    Long idPerson,
    Long idPlan
  );

  @Query("MATCH (workpack:Workpack{deleted:false})-[isInstanceBy:IS_INSTANCE_BY]->(workpackModel:WorkpackModel) " +
         "WHERE id(workpack)=$idWorkpack " +
         "RETURN workpack,isInstanceBy,workpackModel, [ " +
         " [(workpack)<-[isIn:IS_IN*]-(children:Workpack{deleted:false}) | [isIn, children] ], " +
         " [(children)-[isInstanceByChildren:IS_INSTANCE_BY]->(workpackModelChildren:WorkpackModel) | [isInstanceByChildren, workpackModelChildren] ] " +
         "]")
  Optional<Workpack> findWithModelAndChildrenById(Long idWorkpack);

  @Query("MATCH (workpack:Workpack{deleted:false}) " +
         "MATCH (workpack)-[belongsTo:BELONGS_TO]->(plan:Plan)-[structuredBy:IS_STRUCTURED_BY]->(planModel:PlanModel) " +
         "MATCH (workpack)-[isLinkedTo:IS_LINKED_TO]->(model:WorkpackModel)-[modelBelongsTo:BELONGS_TO]->(planModel) " +
         "WHERE id(workpack)=$idWorkpack AND id(plan)=$idPlan " +
         "RETURN workpack, belongsTo, plan, structuredBy, planModel, isLinkedTo, model, modelBelongsTo, [" +
         " [ (model)<-[modelIsIn:IS_IN*]-(childrenModel:WorkpackModel) | [modelIsIn, childrenModel]] " +
         "]")
  Optional<WorkpackModel> findWorkpackModeLinkedByWorkpackAndPlan(
    Long idWorkpack,
    Long idPlan
  );

  @Query("MATCH (w:Workpack) " +
         "WHERE id(w)=$idWorkpack " +
         "RETURN w, [ " +
         " [(w)<-[f1:FEATURES]-(p:Property) | [f1,p]], " +
         " [(w)-[a:IS_INSTANCE_BY]->(m:WorkpackModel) | [a,m]], " +
         " [(w)<-[i:IS_IN*]-(v:Workpack{ deleted: false})<-[h:FEATURES]-(q:Property) | [i,v,h,q]], " +
         " [(v)-[ii:IS_INSTANCE_BY]->(n:WorkpackModel) | [ii,n]], " +
         " [(w)<-[f2:FEATURES]-(l:LocalitySelection)-[v1:VALUES]->(l1:Locality) | [f2,l,v1,l1]], " +
         " [(w)<-[f3:FEATURES]-(o:OrganizationSelection)-[v2:VALUES]->(o1:Organization) | [f3,o,v2,o1]], " +
         " [(w)<-[f4:FEATURES]-(u:UnitSelection)-[v3:VALUES]->(u1:UnitMeasure) | [f4,u,v3,u1]], " +
         " [(v)-[f5:FEATURES]-(l:LocalitySelection)-[v1:VALUES]->(l1:Locality) | [f5,l,v1,l1]], " +
         " [(v)-[f6:FEATURES]-(o:OrganizationSelection)-[v2:VALUES]->(o1:Organization) | [f6,o,v2,o1]], " +
         " [(v)-[f7:FEATURES]-(u:UnitSelection)-[v3:VALUES]->(u1:UnitMeasure) | [f7,u,v3,u1]] " +
         "]")
  Optional<Workpack> findWithPropertiesAndModelAndChildrenById(Long idWorkpack);

  @Query("MATCH (workpack:Workpack{deleted:false})-[instanceBy:IS_INSTANCE_BY]->(model:WorkpackModel), " +
         "(model)<-[:FEATURES]-(nameModel:PropertyModel{name:'name', session:'PROPERTIES'})<-[:IS_DRIVEN_BY]-(nameProperty:Property)-[:FEATURES]->(workpack), " +
         "(model)<-[:FEATURES]-(fullNameModel:PropertyModel{name:'fullName', session:'PROPERTIES'})<-[:IS_DRIVEN_BY]-(fullNameProperty:Property)-[:FEATURES]->(workpack) " +
         "WHERE id(workpack)=$idWorkpack " +
         "RETURN id(model) AS idWorkpackModel, " +
         "id(workpack) AS idWorkpack, " +
         "nameProperty.value AS name, " +
         "fullNameProperty.value AS fullName")
  Optional<WorkpackName> findWorkpackNameAndFullname(Long idWorkpack);

  @Query("MATCH (workpack:Workpack) " +
         "OPTIONAL MATCH (workpack)<-[snapshotOf:IS_SNAPSHOT_OF]-(:Workpack) " +
         "WITH workpack, snapshotOf " +
         "WHERE id(workpack)=$idWorkpack " +
         "RETURN COUNT(snapshotOf)>0 ")
  boolean hasSnapshot(Long idWorkpack);

  @Query("MATCH (workpack:Workpack) " +
         "OPTIONAL MATCH (workpack)-[isBaselinedBy:IS_BASELINED_BY]->(baseline:Baseline{active:true}) " +
         "WITH workpack, isBaselinedBy, baseline " +
         "WHERE id(workpack)=$idWorkpack " +
         "RETURN COUNT(baseline)>0 ")
  boolean hasBaselineActive(Long idWorkpack);

  @Query("MATCH (workpack:Workpack) " +
         "OPTIONAL MATCH (workpack)-[isBaselinedBy:IS_BASELINED_BY]->(baseline:Baseline{status:'PROPOSED'}) " +
         "WITH workpack, isBaselinedBy, baseline " +
         "WHERE id(workpack)=$idWorkpack " +
         "RETURN COUNT(baseline)>0 ")
  boolean hasProposedBaseline(Long idWorkpack);

  @Query("MATCH (workpack:Workpack) " +
         "OPTIONAL MATCH (workpack)-[isBaselinedBy:IS_BASELINED_BY]->(baseline:Baseline{status:'PROPOSED', cancelation:true}) " +
         "WITH workpack, isBaselinedBy, baseline " +
         "WHERE id(workpack)=$idWorkpack " +
         "RETURN COUNT(baseline)>0 ")
  boolean hasCancelPropose(Long idWorkpack);

  @Query("match (w:Workpack)-[:IS_INSTANCE_BY]->(m:WorkpackModel) " +
         "where id(w)=$idWorkpack " +
         "return m")
  Optional<WorkpackModel> findWorkpackModelByWorkpackId(Long idWorkpack);

  @Query("match (s:Workpack)-[:IS_SNAPSHOT_OF]->(:Workpack)-[:IS_INSTANCE_BY]->(m:WorkpackModel) " +
         "where id(s)=$idWorkpack " +
         "return m")
  Optional<WorkpackModel> findWorkpackModelBySnapshotId(Long idWorkpack);

  @Query("match (w:Workpack)-[i:IS_IN]->(p:Workpack) " +
         "where id(w)=1126 and id(p)=39 " +
         "return count(i)>0")
  boolean isWorkpackInParent(Long idWorkpack, Long idParent);

  @Query("MATCH (w:Workpack)-[i:IS_IN]->(p:Workpack) " +
         "WHERE id(w)=$workpackId AND id(p)=$parentId " +
         "DETACH DELETE i")
  void deleteIsInRelationshipByWorkpackIdAndParentId(Long workpackId, Long parentId);

  @Query("MATCH (w:Workpack), (p:Workpack) " +
         "WHERE id(w)=$workpackId AND id(p)=$parentId " +
         "CREATE (w)-[:IS_IN]->(p)")
  void createIsInRelationshipByWorkpackIdAndParentId(Long workpackId, Long parentId);

  @Query("MATCH (workpack:Workpack)  " +
         "WHERE id(workpack)=$idWorkpack " +
         "MATCH (workpack)<-[:FEATURES]-(:UnitSelection)-[:VALUES]->(measure:UnitMeasure) " +
         "RETURN measure.name")
  Optional<String> findUnitMeasureNameOfDeliverableWorkpack(Long idWorkpack);

}
