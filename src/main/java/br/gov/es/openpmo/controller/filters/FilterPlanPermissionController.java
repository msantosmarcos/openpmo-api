package br.gov.es.openpmo.controller.filters;

import br.gov.es.openpmo.model.filter.CustomFilterEnum;
import br.gov.es.openpmo.service.filters.CustomFilterService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api
@RestController
@CrossOrigin
@RequestMapping("/filter/plan-permissions")
public class FilterPlanPermissionController extends CreateAndUpdateUsingCommonFilterOperations {

  private final CustomFilterService customFilterService;

  @Autowired
  public FilterPlanPermissionController(final CustomFilterService customFilterService) {
    this.customFilterService = customFilterService;
  }

  @Override
  protected CustomFilterService getCustomFilterService() {
    return this.customFilterService;
  }

  @Override
  protected CustomFilterEnum getFilter() {
    return CustomFilterEnum.PLAN_PERMISSIONS;
  }
}
