package br.gov.es.openpmo.service.journals;

import br.gov.es.openpmo.model.baselines.Status;
import br.gov.es.openpmo.model.journals.JournalAction;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.stream.Stream;

class BaselineStatusProvider implements ArgumentsProvider {

  @Override
  public Stream<? extends Arguments> provideArguments(final ExtensionContext context) {
    return Stream.of(
        Arguments.of(Status.DRAFT, JournalAction.DRAFT),
        Arguments.of(Status.PROPOSED, JournalAction.PROPOSED),
        Arguments.of(Status.APPROVED, JournalAction.APPROVED),
        Arguments.of(Status.REJECTED, JournalAction.REJECTED)
    );
  }

}
