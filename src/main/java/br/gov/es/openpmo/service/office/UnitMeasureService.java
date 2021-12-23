package br.gov.es.openpmo.service.office;

import br.gov.es.openpmo.dto.unitmeasure.UnitMeasureUpdateDto;
import br.gov.es.openpmo.exception.NegocioException;
import br.gov.es.openpmo.model.filter.CustomFilter;
import br.gov.es.openpmo.model.office.Office;
import br.gov.es.openpmo.model.office.UnitMeasure;
import br.gov.es.openpmo.repository.CustomFilterRepository;
import br.gov.es.openpmo.repository.UnitMeasureRepository;
import br.gov.es.openpmo.repository.custom.filters.FindAllUnitMeasureUsingCustomFilter;
import br.gov.es.openpmo.utils.ApplicationMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static br.gov.es.openpmo.utils.ApplicationMessage.CUSTOM_FILTER_NOT_FOUND;

@Service
public class UnitMeasureService {

  private final UnitMeasureRepository repository;
  private final CustomFilterRepository customFilterRepository;
  private final FindAllUnitMeasureUsingCustomFilter findAllUnitMeasure;
  private final OfficeService officeService;


  @Autowired
  public UnitMeasureService(
    final UnitMeasureRepository repository,
    final CustomFilterRepository customFilterRepository,
    final FindAllUnitMeasureUsingCustomFilter findAllUnitMeasure,
    final OfficeService officeService
  ) {
    this.repository = repository;
    this.customFilterRepository = customFilterRepository;
    this.findAllUnitMeasure = findAllUnitMeasure;
    this.officeService = officeService;
  }

  public List<UnitMeasure> findAll(final Long idOffice, final Long idFilter) {

    if(idFilter == null) {
      return this.findAll(idOffice);
    }

    final CustomFilter filter = this.customFilterRepository
      .findById(idFilter)
      .orElseThrow(() -> new NegocioException(CUSTOM_FILTER_NOT_FOUND));

    final Map<String, Object> params = new HashMap<>();
    params.put("idOffice", idOffice);

    return this.findAllUnitMeasure.execute(filter, params);
  }

  public List<UnitMeasure> findAll(final Long idOffice) {
    return this.repository.findByOffice(idOffice);
  }

  public UnitMeasure save(final UnitMeasure unitMeasure) {
    unitMeasure.setOffice(this.getOfficeById(unitMeasure.getOffice().getId()));
    return this.repository.save(unitMeasure);
  }

  private Office getOfficeById(final Long idOffice) {
    return this.officeService.findById(idOffice);
  }

  public void delete(final UnitMeasure unitMeasure) {
    this.repository.delete(unitMeasure);
  }

  public UnitMeasure getUnitMeasure(final UnitMeasureUpdateDto unitMeasureUpdateDto) {
    final UnitMeasure unitMeasure = this.findById(unitMeasureUpdateDto.getId());
    unitMeasure.setName(unitMeasureUpdateDto.getName());
    unitMeasure.setFullName(unitMeasureUpdateDto.getFullName());
    unitMeasure.setPrecision(unitMeasureUpdateDto.getPrecision());
    return unitMeasure;
  }

  public UnitMeasure findById(final Long id) {
    return this.repository.findById(id).orElseThrow(() ->
                                                      new NegocioException(ApplicationMessage.UNITMEASURE_NOT_FOUND));
  }

}
