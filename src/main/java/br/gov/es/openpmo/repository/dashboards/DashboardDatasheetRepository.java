package br.gov.es.openpmo.repository.dashboards;

import br.gov.es.openpmo.dto.dashboards.datasheet.DatasheetStakeholderQueryResult;
import br.gov.es.openpmo.dto.dashboards.datasheet.WorkpackByModelQueryResult;
import br.gov.es.openpmo.model.workpacks.Workpack;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DashboardDatasheetRepository extends Neo4jRepository<Workpack, Long> {

  @Query("MATCH p = (current:Workpack{deleted:false})<-[:IS_IN*]-(child:Workpack{deleted:false}), " +
    "(current)-[:IS_INSTANCE_BY|IS_LINKED_TO]->(n:WorkpackModel), " +
    "(child)-[:IS_INSTANCE_BY|IS_LINKED_TO]->(m:WorkpackModel)-[:IS_IN*]->(n) " +
    "WHERE id(current)=$workpackId and id(n)=$workpackModelId " +
    "return id(m) as idWorkpackModel, count(distinct child) AS quantity, m.modelName AS singularName, m.modelNameInPlural AS pluralName, m.fontIcon AS icon, length(p) as level")
  List<WorkpackByModelQueryResult> workpackByModel(Long workpackId, Long workpackModelId);

  @Query("MATCH (a:Actor)-[s:IS_STAKEHOLDER_IN{active:true}]->(w:Workpack{deleted:false,canceled:false}) " +
         "OPTIONAL MATCH (w)-[:IS_IN*]->(v:Workpack{deleted:false,canceled:false}) " +
         "OPTIONAL MATCH (wm:WorkpackModel)<-[:IS_INSTANCE_BY]-(w) " +
         "OPTIONAL MATCH (a)<-[:IS_A_PORTRAIT_OF]-(file:File) " +
         "WITH * " +
         "ORDER BY " +
         "    (s.role IN wm.organizationRoles), " +
         "    CASE " +
         "      WHEN wm.dashboardShowStakeholders IS NOT NULL " +
         "      THEN [i IN range(0, size(wm.dashboardShowStakeholders)-1) WHERE toLower(wm.dashboardShowStakeholders[i]) = toLower(s.role)][0] " +
         "      ELSE 0 " +
         "    END, " +
         "    a.name " +
         "WHERE " +
         "    ( " +
         "        (a)-[s]->(w) AND id(w)=$workpackId " +
         "        AND any(role IN wm.dashboardShowStakeholders WHERE toLower(role) = toLower(s.role)) " +
         "        AND (s.from IS NULL OR date(s.from) <= date()) " +
         "        AND (s.to IS NULL OR date(s.to) >= date()) " +
         "    ) " +
         "RETURN " +
         "    DISTINCT id(a) AS id, " +
         "    a.name AS name, " +
         "    a.fullName AS fullName, " +
         "    s.role AS role, " +
         "    file, " +
         "    'Organization' IN labels(a) AS organization")
  List<DatasheetStakeholderQueryResult> stakeholders(Long workpackId);

}
