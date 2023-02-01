package br.gov.es.openpmo.controller.workpack;

import br.gov.es.openpmo.configuration.Authorization;
import br.gov.es.openpmo.dto.EntityDto;
import br.gov.es.openpmo.dto.Response;
import br.gov.es.openpmo.dto.ResponseBase;
import br.gov.es.openpmo.dto.completed.CompleteDeliverableRequest;
import br.gov.es.openpmo.dto.dashboards.v2.SimpleDashboard;
import br.gov.es.openpmo.dto.permission.PermissionDto;
import br.gov.es.openpmo.dto.workpack.EndDeliverableManagementRequest;
import br.gov.es.openpmo.dto.workpack.ResponseBaseWorkpack;
import br.gov.es.openpmo.dto.workpack.ResponseBaseWorkpackDetail;
import br.gov.es.openpmo.dto.workpack.WorkpackDetailDto;
import br.gov.es.openpmo.dto.workpack.WorkpackNameResponse;
import br.gov.es.openpmo.dto.workpack.WorkpackParamDto;
import br.gov.es.openpmo.model.journals.JournalAction;
import br.gov.es.openpmo.model.workpacks.Milestone;
import br.gov.es.openpmo.model.workpacks.Workpack;
import br.gov.es.openpmo.model.workpacks.models.WorkpackModel;
import br.gov.es.openpmo.service.authentication.TokenService;
import br.gov.es.openpmo.service.completed.ICompleteDeliverableService;
import br.gov.es.openpmo.service.completed.IDeliverableEndManagementService;
import br.gov.es.openpmo.service.dashboards.v2.IDashboardService;
import br.gov.es.openpmo.service.journals.JournalCreator;
import br.gov.es.openpmo.service.permissions.canaccess.ICanAccessService;
import br.gov.es.openpmo.service.workpack.GetWorkpackName;
import br.gov.es.openpmo.service.workpack.WorkpackPermissionVerifier;
import br.gov.es.openpmo.service.workpack.WorkpackService;
import br.gov.es.openpmo.utils.ResponseHandler;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static br.gov.es.openpmo.service.workpack.GetPropertyValue.getValueProperty;
import static br.gov.es.openpmo.service.workpack.PropertyComparator.compare;

@Api
@RestController
@CrossOrigin
@RequestMapping("/workpack")
public class WorkpackController {

  private static final String SUCESSO = "Success";
  private final ResponseHandler responseHandler;
  private final WorkpackService workpackService;
  private final TokenService tokenService;
  private final WorkpackPermissionVerifier workpackPermissionVerifier;
  private final JournalCreator journalCreator;
  private final GetWorkpackName getWorkpackName;
  private final ICompleteDeliverableService completeDeliverableService;
  private final IDeliverableEndManagementService deliverableEndManagementService;
  private final IDashboardService dashboardService;
  private final ICanAccessService canAccessService;

  @Autowired
  public WorkpackController(
      final ResponseHandler responseHandler,
      final WorkpackService workpackService,
      final TokenService tokenService,
      final WorkpackPermissionVerifier workpackPermissionVerifier,
      final JournalCreator journalCreator,
      final GetWorkpackName getWorkpackName,
      final ICompleteDeliverableService completeDeliverableService,
      final IDeliverableEndManagementService deliverableEndManagementService,
      final IDashboardService dashboardService,
      final ICanAccessService canAccessService) {
    this.responseHandler = responseHandler;
    this.workpackService = workpackService;
    this.tokenService = tokenService;
    this.workpackPermissionVerifier = workpackPermissionVerifier;
    this.journalCreator = journalCreator;
    this.getWorkpackName = getWorkpackName;
    this.completeDeliverableService = completeDeliverableService;
    this.deliverableEndManagementService = deliverableEndManagementService;
    this.dashboardService = dashboardService;
    this.canAccessService = canAccessService;
  }

  private static <T> boolean isNotNull(final T obj) {
    return Objects.nonNull(obj);
  }

  private static boolean workpackModelHasSortBy(final WorkpackModel workpackModel) {
    return workpackModel != null && workpackModel.getSortBy() != null;
  }

  private static void sortWorkpacks(
      final List<? extends Workpack> workpacks,
      final WorkpackModel workpackModel) {
    workpacks.sort((a, b) -> compare(
        getValueProperty(a, workpackModel.getSortBy()),
        getValueProperty(b, workpackModel.getSortBy())));
  }

  @GetMapping
  public ResponseEntity<ResponseBaseWorkpack> indexBase(
      @RequestParam("id-plan") final Long idPlan,
      @RequestParam(value = "id-plan-model", required = false) final Long idPlanModel,
      @RequestParam(value = "id-workpack-model", required = false) final Long idWorkpackModel,
      @RequestParam(value = "idFilter", required = false) final Long idFilter,
      @Authorization final String authorization) {

    this.canAccessService.ensureCanReadResource(idPlan, authorization);

    final Long idUser = this.tokenService.getUserId(authorization);

    final List<Workpack> workpacks = this.findAllWorkpacks(
        idPlan,
        idPlanModel,
        idWorkpackModel,
        idFilter);

    if (workpacks.isEmpty()) {
      return ResponseEntity.noContent().build();
    }

    if (isNotNull(idWorkpackModel)) {
      final WorkpackModel workpackModel = this.workpackService.getWorkpackModelById(idWorkpackModel);
      if (workpackModelHasSortBy(workpackModel)) {
        sortWorkpacks(workpacks, workpackModel);
      }
    }

    final List<WorkpackDetailDto> workpackDetailDtos = workpacks.parallelStream()
        .map(workpack -> this.mapToWorkpackDetailDto(workpack, idWorkpackModel))
        .collect(Collectors.toList());

    final List<WorkpackDetailDto> verify = this.workpackPermissionVerifier.verify(
        workpackDetailDtos,
        idUser,
        idPlan);

    verify.parallelStream().forEach(workpackDetailDto -> {
      final SimpleDashboard dashboard = this.dashboardService.buildSimple(workpackDetailDto.getId());
      workpackDetailDto.setDashboard(dashboard);
    });

    return verify.isEmpty()
        ? ResponseEntity.noContent().build()
        : ResponseEntity.ok(new ResponseBaseWorkpack().setData(verify).setMessage(SUCESSO).setSuccess(true));
  }

  private List<Workpack> findAllWorkpacks(
      final Long idPlan,
      final Long idPlanModel,
      final Long idWorkPackModel,
      final Long idFilter) {
    return this.workpackService.findAll(
        idPlan,
        idPlanModel,
        idWorkPackModel,
        idFilter);
  }

  private WorkpackDetailDto mapToWorkpackDetailDto(
      final Workpack workpack,
      final Long idWorkpackModel) {
    final WorkpackDetailDto itemDetail = this.workpackService.getWorkpackDetailDto(workpack);
    itemDetail.applyLinkedStatus(workpack, idWorkpackModel);
    return itemDetail;
  }

  @GetMapping("/parent")
  public ResponseEntity<ResponseBaseWorkpack> indexBase(
      @RequestParam("id-plan") final Long idPlan,
      @RequestParam("id-workpack-parent") final Long idWorkpackParent,
      @RequestParam(value = "id-plan-model", required = false) final Long idPlanModel,
      @RequestParam(value = "id-workpack-model", required = false) final Long idWorkpackModel,
      @RequestParam(value = "idFilter", required = false) final Long idFilter,
      @RequestParam(value = "workpackLinked", required = false) final boolean workpackLinked,
      @Authorization final String authorization) {
    this.canAccessService.ensureCanReadResource(idWorkpackParent, authorization);

    final Long idUser = this.tokenService.getUserId(authorization);

    final List<Workpack> workpacks = this.workpackService.findAllUsingParent(
        idPlan,
        idPlanModel,
        idWorkpackModel,
        idWorkpackParent,
        idFilter,
        workpackLinked);

    final List<WorkpackDetailDto> workpackList = workpacks.parallelStream()
        .map(workpack -> this.mapToWorkpackDetailDto(workpack, idWorkpackModel))
        .collect(Collectors.toList());

    if (workpackList.isEmpty()) {
      return ResponseEntity.noContent().build();
    }

    final List<WorkpackDetailDto> verify = this.workpackPermissionVerifier.verify(
        workpackList,
        idUser,
        idPlan);

    verify.parallelStream().filter(workpackDetailDto -> !workpackDetailDto.isCanceled())
        .forEach(workpackDetailDto -> {
          final SimpleDashboard dashboard = this.dashboardService.buildSimple(workpackDetailDto.getId());
          workpackDetailDto.setDashboard(dashboard);
        });

    return verify.isEmpty()
        ? ResponseEntity.noContent().build()
        : ResponseEntity.ok(new ResponseBaseWorkpack().setData(verify).setMessage(SUCESSO).setSuccess(true));
  }

  @GetMapping("/{idWorkpack}")
  public ResponseEntity<ResponseBaseWorkpackDetail> find(
      @PathVariable final Long idWorkpack,
      @RequestParam(value = "id-plan", required = false) final Long idPlan,
      @Authorization final String authorization) {
    this.canAccessService.ensureCanReadResource(idWorkpack, authorization);

    final Long idPerson = this.tokenService.getUserId(authorization);
    final Workpack workpack = this.workpackService.findByIdWithParent(idWorkpack);

    final WorkpackDetailDto workpackDetailDto = this.workpackService.getWorkpackDetailDto(workpack, idPlan);

    final List<PermissionDto> permissions = this.workpackPermissionVerifier.fetchPermissions(
        idPerson,
        idPlan,
        idWorkpack);

    workpackDetailDto.setPermissions(permissions);

    return ResponseEntity.ok(ResponseBaseWorkpackDetail.of(workpackDetailDto));
  }

  @PostMapping
  public ResponseEntity<ResponseBase<EntityDto>> save(
      @RequestBody @Valid final WorkpackParamDto request,
      @Authorization final String authorization) {
    this.canAccessService.ensureCanEditResource(
        Arrays.asList(request.getIdPlan(), request.getIdParent()),
        authorization);
    final Workpack workpack = this.workpackService.getWorkpack(request);
    final EntityDto response = this.workpackService.save(workpack, request.getIdPlan(), request.getIdParent());

    final Long idPerson = this.tokenService.getUserId(authorization);
    this.journalCreator.edition(workpack, JournalAction.CREATED, idPerson);

    if (workpack instanceof Milestone)
      this.workpackService.calculateDashboard(workpack);

    return ResponseEntity.ok(ResponseBase.of(response));
  }

  @PutMapping
  public ResponseEntity<ResponseBase<EntityDto>> update(
      @RequestBody @Valid final WorkpackParamDto request,
      @Authorization final String authorization) {
    this.canAccessService.ensureCanEditResource(request.getId(), authorization);
    final Workpack workpack = this.workpackService.update(this.workpackService.getWorkpack(request));
    final Long idPerson = this.tokenService.getUserId(authorization);
    this.journalCreator.edition(workpack, JournalAction.EDITED, idPerson);

    if (workpack instanceof Milestone)
      this.workpackService.calculateDashboard(workpack);

    return ResponseEntity.ok(ResponseBase.of(EntityDto.of(workpack)));
  }

  @PatchMapping("/{id}/cancel")
  public ResponseEntity<Void> cancel(
      @PathVariable("id") final Long idWorkpack,
      @Authorization final String authorization) {
    this.canAccessService.ensureCanEditResource(idWorkpack, authorization);
    final Workpack workpack = this.workpackService.cancel(idWorkpack);
    final Long idPerson = this.tokenService.getUserId(authorization);
    this.journalCreator.edition(workpack, JournalAction.CANCELLED, idPerson);
    return ResponseEntity.ok().build();
  }

  @PatchMapping("/{id}/restore")
  public ResponseEntity<Void> restore(
      @PathVariable("id") final Long idWorkpack,
      @Authorization final String authorization) {
    this.canAccessService.ensureCanEditResource(idWorkpack, authorization);
    this.workpackService.restore(idWorkpack);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{idWorkpack}")
  public ResponseEntity<Void> delete(
      @PathVariable final Long idWorkpack,
      @Authorization final String authorization) {
    this.canAccessService.ensureCanEditResource(idWorkpack, authorization);
    final Workpack workpack = this.workpackService.findById(idWorkpack);
    this.workpackService.delete(workpack);
    return ResponseEntity.ok().build();
  }

  @Transactional
  @PatchMapping("/complete-deliverable/{id-deliverable}")
  public Response<Void> completeDeliverable(
      @PathVariable("id-deliverable") final Long idDeliverable,
      @RequestBody final CompleteDeliverableRequest request,
      @Authorization final String authorization) {

    this.canAccessService.ensureCanEditResource(idDeliverable, authorization);
    this.completeDeliverableService.apply(idDeliverable, request);
    return this.responseHandler.success();
  }

  @Transactional
  @PatchMapping("/end-deliverable-management/{id-deliverable}")
  public Response<Void> endDeliverableManagement(
      @PathVariable("id-deliverable") final Long idDeliverable,
      @RequestBody final EndDeliverableManagementRequest request,
      @Authorization final String authorization) {
    this.canAccessService.ensureCanEditResource(idDeliverable, authorization);
    this.deliverableEndManagementService.apply(idDeliverable, request);
    return this.responseHandler.success();
  }

  @GetMapping("/{idWorkpack}/name")
  public ResponseEntity<WorkpackNameResponse> getWorkpackName(
      @PathVariable final Long idWorkpack,
      @Authorization final String authorization) {
    this.canAccessService.ensureCanReadResource(idWorkpack, authorization);
    final WorkpackNameResponse response = this.getWorkpackName.execute(idWorkpack);
    return ResponseEntity.ok(response);
  }

}
