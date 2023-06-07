package br.gov.es.openpmo.service.reports.models;

import br.gov.es.openpmo.exception.NegocioException;
import br.gov.es.openpmo.model.actors.File;
import br.gov.es.openpmo.repository.FileRepository;
import br.gov.es.openpmo.utils.ApplicationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
public class CreateReportModelTemplateSource {

  private static final Logger log = LoggerFactory.getLogger(CreateReportModelTemplateSource.class);

  private final FileRepository fileRepository;

  private final String fileReportPath;

  public CreateReportModelTemplateSource(
    final FileRepository fileRepository,
    @Value("${app.reportPath}") final String fileReportPath
  ) {
    this.fileRepository = fileRepository;
    this.fileReportPath = fileReportPath;
  }

  public Set<File> execute(
    final Collection<? extends CreateTemplateSourceRequest> files
  ) {

    final List<File> newFiles = files.stream()
      .map(file -> {
        final File newFile = new File();
        newFile.setId(null);
        newFile.setPerson(null);
        newFile.setJournalEntry(null);
        newFile.setMimeType(file.getMimeType());
        newFile.setUniqueNameKey(file.getUniqueNameKey());
        newFile.setUserGivenName(file.getUserGivenName());
        newFile.setMain(Boolean.TRUE.equals(file.getMain()));
        return newFile;
      })
      .collect(Collectors.toList());

    this.ensureAllFilesAlreadyStoredOnDisc(newFiles);
    this.ensureHasOnlyOneMainReportTemplate(newFiles);

    this.fileRepository.saveAll(newFiles);

    return new HashSet<>(newFiles);
  }


  private void ensureHasOnlyOneMainReportTemplate(final Collection<? extends File> reportTemplateFiles) {
    log.debug("Validando arquivo principal do template de relatório");
    final List<File> mainFiles = reportTemplateFiles.stream()
      .filter(File::getMain)
      .collect(Collectors.toList());
    log.debug("Foram informados {} arquivo(s) como principal", mainFiles.size());
    if (mainFiles.size() != 1) {
      log.error("O relatório pode ter apenas 1 arquivo principal");
      throw new NegocioException(ApplicationMessage.REPORT_DESIGN_MAIN_FILE_TEMPLATE_QUANTITY_DIFFERENT_THAN_ONE);
    }
    log.debug("Validação terminada com sucesso");
  }

  private void ensureAllFilesAlreadyStoredOnDisc(final Collection<? extends File> reportTemplateFiles) {
    log.debug("Verificando se existem arquivos informados que não armazenados no disco");
    final List<Path> invalidFiles = reportTemplateFiles.stream()
      .map(file -> Paths.get(this.fileReportPath.concat(file.getUniqueNameKey())))
      .filter(Files::notExists)
      .collect(Collectors.toList());
    if (!invalidFiles.isEmpty()) {
      log.error("Foram encontrado(s) {} arquivos que não estão armazenados no disco {}", invalidFiles.size(),
                invalidFiles.stream()
                  .map(Path::toAbsolutePath)
                  .collect(Collectors.toList())
      );
      throw new NegocioException(ApplicationMessage.FILE_NOT_FOUND);
    }
    log.debug("Validação terminada com sucesso");
  }

  public interface CreateTemplateSourceRequest {

    String getMimeType();

    String getUniqueNameKey();

    String getUserGivenName();

    Boolean getMain();

  }

}
