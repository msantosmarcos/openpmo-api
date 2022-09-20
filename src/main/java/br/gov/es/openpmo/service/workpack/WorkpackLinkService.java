package br.gov.es.openpmo.service.workpack;

import br.gov.es.openpmo.dto.permission.PermissionDto;
import br.gov.es.openpmo.dto.workpack.WorkpackDetailDto;
import br.gov.es.openpmo.dto.workpackLink.WorkpackModelLinkedDetailDto;
import br.gov.es.openpmo.dto.workpackLink.WorkpackModelLinkedDto;
import br.gov.es.openpmo.exception.NegocioException;
import br.gov.es.openpmo.model.office.plan.Plan;
import br.gov.es.openpmo.model.properties.models.PropertyModel;
import br.gov.es.openpmo.model.relations.BelongsTo;
import br.gov.es.openpmo.model.relations.IsLinkedTo;
import br.gov.es.openpmo.model.relations.IsSharedWith;
import br.gov.es.openpmo.model.workpacks.Workpack;
import br.gov.es.openpmo.model.workpacks.models.WorkpackModel;
import br.gov.es.openpmo.repository.BelongsToRepository;
import br.gov.es.openpmo.repository.WorkpackLinkRepository;
import br.gov.es.openpmo.repository.WorkpackRepository;
import br.gov.es.openpmo.repository.WorkpackSharedRepository;
import br.gov.es.openpmo.service.dashboards.v2.IAsyncDashboardService;
import br.gov.es.openpmo.service.office.plan.PlanService;
import br.gov.es.openpmo.service.ui.BreadcrumbWorkpackLinkedHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static br.gov.es.openpmo.dto.permission.PermissionDto.of;
import static br.gov.es.openpmo.dto.permission.PermissionDto.read;
import static br.gov.es.openpmo.utils.ApplicationMessage.WORKPACKMODEL_NOT_FOUND;
import static br.gov.es.openpmo.utils.ApplicationMessage.WORKPACK_MODEL_TYPE_MISMATCH;
import static java.lang.Boolean.TRUE;

@Service
public class WorkpackLinkService implements BreadcrumbWorkpackLinkedHelper {

  private final WorkpackLinkRepository repository;
  private final WorkpackService workpackService;
  private final WorkpackModelService workpackModelService;
  private final BelongsToRepository belongsToRepository;
  private final WorkpackSharedRepository workpackSharedRepository;
  private final PlanService planService;
  private final WorkpackRepository workpackRepository;
  private final IAsyncDashboardService dashboardService;

  @Autowired
  public WorkpackLinkService(
    final WorkpackLinkRepository repository,
    final WorkpackService workpackService,
    final WorkpackModelService workpackModelService,
    final BelongsToRepository belongsToRepository,
    final WorkpackSharedRepository workpackSharedRepository,
    final PlanService planService,
    final WorkpackRepository workpackRepository,
    final IAsyncDashboardService dashboardService
  ) {
    this.repository = repository;
    this.workpackService = workpackService;
    this.workpackModelService = workpackModelService;
    this.belongsToRepository = belongsToRepository;
    this.workpackSharedRepository = workpackSharedRepository;
    this.planService = planService;
    this.workpackRepository = workpackRepository;
    this.dashboardService = dashboardService;
  }

  private static void ifNotSameModelTypeThrowException(
    final Workpack workpack,
    final WorkpackModel workpackModel
  ) {
    if(!workpack.hasSameModelType(workpackModel)) {
      throw new NegocioException(WORKPACK_MODEL_TYPE_MISMATCH);
    }
  }

  private static List<WorkpackModelLinkedDetailDto> returnBuildedChildren(
    final WorkpackModel modelLinked,
    final Collection<WorkpackModel> childrenOriginalModel
  ) {
    final Set<WorkpackModel> linkedChildren = Optional.ofNullable(modelLinked.getChildren()).orElse(Collections.emptySet());
    final Iterable<WorkpackModel> modelChildrenLinked = new ArrayList<>(linkedChildren);
    final List<WorkpackModelLinkedDetailDto> childrenDetail = new ArrayList<>();
    for(final WorkpackModel children : modelChildrenLinked) {
      final Optional<WorkpackModel> maybeOriginalEquivalent = findOriginalModelEquivalence(children, childrenOriginalModel);
      if(maybeOriginalEquivalent.isPresent()) {
        final WorkpackModelLinkedDetailDto dto = new WorkpackModelLinkedDetailDto();
        dto.setIdWorkpackModelOriginal(maybeOriginalEquivalent.get().getId());
        dto.setIdWorkpackModelLinked(children.getId());
        dto.setNameWorkpackModelLinked(children.getModelName());
        dto.setNameInPluralWorkpackModelLinked(children.getModelNameInPlural());
        childrenDetail.add(dto);
      }
    }
    return childrenDetail;
  }

  private void makeWorkpackLinkedBelongTo(
    final Long idPlan,
    final Workpack workpack
  ) {
    final Plan plan = this.planService.findById(idPlan);
    final BelongsTo belongsTo = new BelongsTo();
    belongsTo.setWorkpack(workpack);
    belongsTo.setPlan(plan);
    belongsTo.setLinked(true);
    this.belongsToRepository.save(belongsTo);
  }

  public void linkWorkpackToWorkpackModel(
    final Long idWorkpack,
    final Long idworkpackModel,
    final Long idPlan,
    final Long idParent
  ) {
    final Workpack workpack = this.workpackService.findById(idWorkpack);
    final WorkpackModel workpackModel = this.workpackModelService.findById(idworkpackModel);
    ifNotSameModelTypeThrowException(workpack, workpackModel);
    this.makeWorkpackLinkedBelongTo(idPlan, workpack);
    this.ifHasParentCreateRelationshipAsChildren(idParent, workpack);
    this.createLinkBetween(workpack, workpackModel);
    this.workpackRepository.findAllInHierarchy(idWorkpack).forEach(this.dashboardService::calculate);
  }

  private void createLinkBetween(
    final Workpack workpack,
    final WorkpackModel workpackModel
  ) {
    final IsLinkedTo isLinkedTo = new IsLinkedTo();
    isLinkedTo.setWorkpack(workpack);
    isLinkedTo.setWorkpackModel(workpackModel);
    this.repository.save(isLinkedTo);
  }

  private void ifHasParentCreateRelationshipAsChildren(
    final Long idParent,
    final Workpack workpack
  ) {
    if(Objects.nonNull(idParent)) {
      final Workpack parent = this.workpackService.findById(idParent);
      parent.addChildren(workpack);
      this.workpackService.saveDefault(workpack);
    }
  }

  public WorkpackDetailDto getByIdWorkpack(
    final Long idWorkpack,
    final Long idWorkpackModelLinked
  ) {
    final Workpack workpack = this.workpackService.findById(idWorkpack);
    final WorkpackModel workpackModelLinked = this.workpackModelService.findById(idWorkpackModelLinked);

    final WorkpackModel workpackModelOriginal = this.workpackModelService.findById(workpack.getIdWorkpackModel());

    this.filterPropertiesWithMismatchType(workpackModelOriginal, workpackModelLinked.getProperties());

    final WorkpackDetailDto workpackDetailDto = this.workpackService.getWorkpackDetailDto(workpack);

    workpackDetailDto.setModelLinked(this.returnBuildedWorkpackModelLinkedDto(
      workpackModelLinked,
      workpackModelOriginal.getId()
    ));

    workpackDetailDto.setPermissions(this.buildPermissions(
      workpack,
      idWorkpackModelLinked
    ));

    return workpackDetailDto;
  }

  private void filterPropertiesWithMismatchType(
    final WorkpackModel workpackModel,
    final Iterable<? extends PropertyModel> modelLinkedProperties
  ) {
    final Set<PropertyModel> filteredProperties = workpackModel.getProperties().stream()
      .filter(property -> this.compareProperties(property, modelLinkedProperties))
      .collect(Collectors.toSet());
    workpackModel.setProperties(filteredProperties);
  }

  public void delete(
    final Long idWorkpack,
    final Long idworkpackModel
  ) {
    final Optional<IsLinkedTo> maybeLinkedTo = this.repository.findByIdWorkpackAndIdWorkpackModel(
      idWorkpack,
      idworkpackModel
    );
    if(!maybeLinkedTo.isPresent()) {
      throw new NegocioException("Link not found.");
    }

    this.repository.delete(maybeLinkedTo.get());
  }

  private boolean compareProperties(
    final PropertyModel property,
    final Iterable<? extends PropertyModel> linkedPropertiesModel
  ) {
    if(linkedPropertiesModel == null) return false;
    for(final PropertyModel linkedPropertyModel : linkedPropertiesModel) {
      if(property.hasSameType(linkedPropertyModel) &&
         linkedPropertyModel.getName().equalsIgnoreCase(property.getName())) {
        return true;
      }
    }
    return false;
  }

  private WorkpackModelLinkedDto returnBuildedWorkpackModelLinkedDto(
    final WorkpackModel workpackModelLinked,
    final Long idWorkpackModelOriginal
  ) {
    final WorkpackModelLinkedDto dto = new WorkpackModelLinkedDto();
    dto.setId(workpackModelLinked.getId());
    dto.setName(workpackModelLinked.getModelName());
    dto.setNameInPlural(workpackModelLinked.getModelNameInPlural());
    final WorkpackModel workpackModelOriginal = this.workpackModelService.findById(idWorkpackModelOriginal);
    dto.setChildren(returnBuildedChildren(workpackModelLinked, workpackModelOriginal.getChildren()));
    return dto;
  }

  private List<PermissionDto> buildPermissions(
    final Workpack workpack,
    final Long idWorkpackModelLinked
  ) {
    if(TRUE.equals(workpack.getPublicShared())) {
      return Collections.singletonList(of(workpack));
    }
    final Optional<IsSharedWith> sharedPermissions =
      this.workpackSharedRepository.commonSharedWithBetweenLinkedWorkpackModelAndWorkpack(
      workpack.getId(),
      idWorkpackModelLinked
    );
    return sharedPermissions.map(isSharedWith -> Collections.singletonList(of(isSharedWith)))
      .orElse(Collections.singletonList(read()));
  }

  private static Optional<WorkpackModel> findOriginalModelEquivalence(
    final WorkpackModel children,
    final Collection<WorkpackModel> childrenOriginalModel
  ) {
    return childrenOriginalModel.stream()
      .filter(model -> model.hasSameType(children) && model.getModelName().equals(children.getModelName()))
      .findFirst();
  }

  @Override
  public WorkpackModel findWorkpackModelLinkedByWorkpackAndPlan(
    final Long idWorkpack,
    final Long idPlan
  ) {
    return this.repository.findWorkpackModelLinkedByWorkpackAndPlan(idWorkpack, idPlan)
      .orElseThrow(() -> new NegocioException(WORKPACKMODEL_NOT_FOUND));
  }

  @Override
  public Optional<IsLinkedTo> findWorkpackParentLinked(
    final Long idWorkpack,
    final Long idPlan
  ) {
    return this.repository.findWorkpackParentLinked(idWorkpack, idPlan);
  }


  @Transactional
  public void unlink(
    final Long idWorkpack,
    final Long idWorkpackModel,
    final Long idPlan
  ) {
    this.repository.unlinkPermissions(idPlan, idWorkpackModel, idWorkpack);
    this.repository.unlinkParentRelation(idPlan, idWorkpackModel, idWorkpack);
    this.repository.unlinkWorkpackModelAndPlan(idPlan, idWorkpackModel, idWorkpack);
    this.workpackRepository.findAllInHierarchy(idWorkpack).forEach(this.dashboardService::calculate);
  }

}
