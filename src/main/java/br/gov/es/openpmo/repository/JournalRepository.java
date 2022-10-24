package br.gov.es.openpmo.repository;

import br.gov.es.openpmo.model.actors.File;
import br.gov.es.openpmo.model.actors.Person;
import br.gov.es.openpmo.model.journals.JournalEntry;
import br.gov.es.openpmo.model.journals.JournalType;
//import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalRepository extends Neo4jRepository<JournalEntry, Long> {

  @Query("match (j:JournalEntry)-[:SCOPE_TO]->(w:Workpack) " +
         "where id(w)=$workpackId " +
         "return id(j)")
  List<Long> findAllJournalIdsByWorkpackId(Long workpackId);

  @Query("match (w:Workpack), (j:JournalEntry), (p:Person) " +
         "where ($scope is null or $scope=[] or id(w) in $scope) " +
         " and ( " +
         " (w)<-[:SCOPE_TO]-(j) or (w)<-[:IS_IN*]-(:Workpack)<-[:SCOPE_TO]-(j) or j.type='FAIL' " +
         ") and ( " +
         " (($from is null or (date(datetime($from)) <= date(datetime(j.date)))) " +
         " and " +
         " ($to is null or date(datetime(j.date)) <= date(datetime($to)))) " +
         " and " +
         " (j.type in $journalType or 'ALL' in $journalType) " +
         ") and ( " +
         " (p)<-[:IS_RECORDED_BY]-(j) " +
         ")" +
         "return j " +
         "order by j.date desc")
  List<JournalEntry> findAll(
    LocalDate from,
    LocalDate to,
    List<JournalType> journalType,
    List<Integer> scope
  );

  @Query("match (j:JournalEntry)-[:SCOPE_TO]->(w:Workpack) " +
         "where id(j)=$journalId " +
         "return id(w)")
  Optional<Long> findWorkpackIdByJournalId(Long journalId);

  @Query("match (j:JournalEntry)-[:IS_RECORDED_BY]->(p:Person) " +
         "where id(j)=$journalId " +
         "return p")
  Optional<Person> findPersonByJournalId(Long journalId);

  @Query("match (j:JournalEntry)<-[:IS_EVIDENCE_OF]-(f:File) " +
         "where id(j)=$journalId " +
         "return f")
  List<File> findEvidencesByJournalId(Long journalId);

  @Query("match (:JournalEntry)<-[:IS_EVIDENCE_OF]-(f:File) " +
         "where id(f)=$evidenceId " +
         "detach delete f")
  void deleteEvidenceById(Long evidenceId);

}
