package br.gov.es.openpmo.dto.person.queries;

import br.gov.es.openpmo.model.actors.File;
import org.springframework.data.neo4j.repository.query.QueryResult;

@QueryResult
public class AllPersonInOfficeQuery {

  private final Long id;
  private final String name;
  private final File avatar;
  private final String email;

  public AllPersonInOfficeQuery(
    final Long id,
    final String name,
    final File avatar,
    final String email
  ) {
    this.id = id;
    this.name = name;
    this.avatar = avatar;
    this.email = email;
  }

  public String getEmail() {
    return this.email;
  }

  public File getAvatar() {
    return this.avatar;
  }

  public Long getId() {
    return this.id;
  }

  public String getName() {
    return this.name;
  }

}
