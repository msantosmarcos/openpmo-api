package br.gov.es.openpmo.repository.custom.filters;

import br.gov.es.openpmo.model.filter.CustomFilter;
import br.gov.es.openpmo.model.filter.Rules;
import br.gov.es.openpmo.repository.PropertyModelRepository;
import br.gov.es.openpmo.repository.WorkpackRepository;
import br.gov.es.openpmo.repository.custom.FindAllUsingCustomFilterBuilder;
import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
public class FindAllWorkpackByParentUsingCustomFilter extends FindAllUsingCustomFilterBuilder
  implements ApplyFilterUsingPropertyModel {

  private final WorkpackRepository repository;

  private final PropertyModelRepository propertyModelRepository;

  @Autowired
  public FindAllWorkpackByParentUsingCustomFilter(
    final WorkpackRepository repository,
    final PropertyModelRepository propertyModelRepository
  ) {
    this.repository = repository;
    this.propertyModelRepository = propertyModelRepository;
  }

  @Override
  public Session getSession() {
    return this.repository.getSession();
  }

  @Override
  public void buildMatchClause(
    final CustomFilter filter,
    final StringBuilder query
  ) {
    query.append(
      "MATCH (node:Workpack{deleted:false})-[rf:BELONGS_TO]->(p:Plan)-[is:IS_STRUCTURED_BY]->(pm:PlanModel), " +
      "(node)-[ii:IS_INSTANCE_BY]->(wm:WorkpackModel)<-[:FEATURES]-(propertyModel:PropertyModel), " +
      "(node)-[wc:IS_IN]->(wp:Workpack), " +
      "(node)<-[:FEATURES]-(property:Property)-[:IS_DRIVEN_BY]->(propertyModel), " +
      "(node)<-[:FEATURES]-(name:Property)-[:IS_DRIVEN_BY]->(:PropertyModel{name: 'name'}), " +
      "(node)<-[:FEATURES]-(fullName:Property)-[:IS_DRIVEN_BY]->(:PropertyModel{name: 'fullName'}) " +
      "OPTIONAL MATCH (propertyModel)-[:GROUPS]->(groupedProperty:PropertyModel) " +
      "OPTIONAL MATCH (node)<-[:FEATURES]-(:Property)-[:VALUES]->(values) " +
      "WITH *, apoc.text.levenshteinSimilarity(apoc.text.clean(name.value), apoc.text.clean($term)) AS nameScore, " +
      "apoc.text.levenshteinSimilarity(apoc.text.clean(fullName.value), apoc.text.clean($term)) AS fullNameScore " +
      "WITH *, CASE WHEN nameScore > fullNameScore THEN nameScore ELSE fullNameScore END AS score, " +
      "collect( property ) + collect( name ) + collect( fullName ) as properties, " +
      "collect( id(values) ) as selectedValues ");
  }

  @Override
  public void buildWhereClause(
    final CustomFilter filter,
    final StringBuilder query
  ) {
    query.append("WHERE (")
      .append("   id(p)=$idPlan ")
      .append("   AND (id(pm)=$idPlanModel OR $idPlanModel IS NULL) ")
      .append("   AND (id(wm)=$idWorkPackModel OR $idWorkPackModel IS NULL) ")
      .append("   AND id(wp)=$idWorkPackParent ")
      .append("   AND ($term is null OR $term = '' OR score > $searchCutOffScore)")
      .append(") ");
  }

  @Override
  public void buildReturnClause(final StringBuilder query) {
    query.append("RETURN node, rf, p, ii, pm, wm, [ " +
                 " [(node)<-[f:FEATURES]-(p:Property)-[d:IS_DRIVEN_BY]->(pm:PropertyModel) | [f, p, d, pm] ], " +
                 " [(node)<-[wi:IS_IN]-(w2:Workpack) | [wi, w2] ], " +
                 " [(node)-[wi2:IS_IN]->(w3:Workpack) | [wi2, w3] ], " +
                 " [(node)<-[wa:APPLIES_TO]-(ca:CostAccount) | [wa, ca] ], " +
                 " [(ca)<-[f1:FEATURES]-(p2:Property)-[d1:IS_DRIVEN_BY]->(pmc:PropertyModel) | [ca, f1, p2, d1, pmc ] ], " +
                 " [(wm)<-[wmi:IS_IN]-(wm2:WorkpackModel) | [wmi,wm2] ], " +
                 " [(wm)-[wmi2:IS_IN]->(wm3:WorkpackModel) | [wmi2,wm3] ], " +
                 " [(wm)<-[f2:FEATURES]-(pm2:PropertyModel) | [f2, pm2] ], " +
                 " [(node)-[sharedWith:IS_SHARED_WITH]->(office:Office) | [sharedWith, office]], " +
                 " [(node)-[isLinkedTo:IS_LINKED_TO]->(workpackModel:WorkpackModel) | [isLinkedTo, workpackModel] ], " +
                 " [(wp)-[parentSharedWith]->(officeParent:Office) | [parentSharedWith, officeParent]] " +
                 "] ");
  }

  @Override
  protected String buildCustomFilterRule(
    final Rules rule,
    final String label
  ) {
    return this.buildFilterRuleForWorkpack(
      rule,
      label
    );
  }

  @Override
  protected boolean hasAppendedBooleanBlock() {
    return true;
  }

  @Override
  protected boolean hasToCloseAppendedBooleanBlock() {
    return true;
  }

  @Override
  protected void buildOrderingAndDirectionClause(
    final CustomFilter filter,
    final Map<String, Object> params,
    final StringBuilder query
  ) {
    final String term = (String) params.get("term");
    if (StringUtils.isEmpty(term)) {
      query.append("\n")
        .append("ORDER BY score DESC");
      return;
    }
    this.buildOrderingAndDirectionClauseForWorkpack(
      filter,
      query
    );
  }

  @Override
  public String[] getDefinedExternalParams() {
    return new String[]{
      "idPlan",
      "idPlanModel",
      "idWorkPackModel",
      "idWorkPackParent",
      "term",
      "searchCutOffScore"
    };
  }

  @Override
  public String getNodeName() {
    return this.nodeName;
  }

  @Override
  public void setNodeName(final String nodeName) {
    this.nodeName = nodeName;
  }

  @Override
  public PropertyModelRepository getPropertyModelRepository() {
    return this.propertyModelRepository;
  }

}
