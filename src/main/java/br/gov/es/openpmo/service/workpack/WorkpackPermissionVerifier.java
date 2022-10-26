package br.gov.es.openpmo.service.workpack;

import br.gov.es.openpmo.dto.permission.PermissionDto;
import br.gov.es.openpmo.dto.workpack.WorkpackDetailDto;
import br.gov.es.openpmo.exception.NegocioException;
import br.gov.es.openpmo.model.actors.Person;
import br.gov.es.openpmo.model.office.Office;
import br.gov.es.openpmo.model.office.plan.Plan;
import br.gov.es.openpmo.model.relations.CanAccessOffice;
import br.gov.es.openpmo.model.relations.CanAccessPlan;
import br.gov.es.openpmo.model.workpacks.Workpack;
import br.gov.es.openpmo.repository.WorkpackRepository;
import br.gov.es.openpmo.repository.WorkpackSharedRepository;
import br.gov.es.openpmo.service.actors.PersonService;
import br.gov.es.openpmo.service.office.plan.PlanService;
import br.gov.es.openpmo.service.permissions.OfficePermissionService;
import br.gov.es.openpmo.service.permissions.PlanPermissionService;
import br.gov.es.openpmo.utils.ApplicationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static br.gov.es.openpmo.enumerator.PermissionLevelEnum.EDIT;
import static br.gov.es.openpmo.enumerator.PermissionLevelEnum.READ;
import static java.lang.Boolean.TRUE;


@Component
public class WorkpackPermissionVerifier {

  private static final Logger LOGGER = LoggerFactory.getLogger(WorkpackPermissionVerifier.class);
  private final PersonService personService;
  private final PlanService planService;
  private final WorkpackService workpackService;
  private final WorkpackRepository workpackRepository;
  private final PlanPermissionService planPermissionService;
  private final OfficePermissionService officePermissionService;
  private final WorkpackSharedRepository workpackSharedRepository;

  @Autowired
  public WorkpackPermissionVerifier(
    final PersonService personService,
    final PlanService planService,
    final WorkpackService workpackService,
    final WorkpackRepository workpackRepository,
    final PlanPermissionService planPermissionService,
    final OfficePermissionService officePermissionService,
    final WorkpackSharedRepository workpackSharedRepository
  ) {
    this.personService = personService;
    this.planService = planService;
    this.workpackService = workpackService;
    this.workpackRepository = workpackRepository;
    this.planPermissionService = planPermissionService;
    this.officePermissionService = officePermissionService;
    this.workpackSharedRepository = workpackSharedRepository;

  }

  private static boolean hasStakeholderSessionActive(final Workpack workpack) {
    return workpack.hasStakeholderSessionActive();
  }

  private static List<PermissionDto> ifCanAccessWorkpackThenFetchPermissions(
    final Workpack workpack,
    final Long idUser
  ) {
    return Optional.ofNullable(workpack.getCanAccess())
      .map(canAccess -> canAccess.stream()
        .filter(canAccessWorkpack -> canAccessWorkpack.hasSameUser(idUser))
        .map(PermissionDto::of)
        .collect(Collectors.toList()))
      .orElse(null);
  }

  private static boolean hasEditLevel(final PermissionDto permission) {
    return EDIT == permission.getLevel();
  }

  public List<PermissionDto> fetchPermissions(
    final Long idUser,
    final Long idPlan,
    final Long idWorkpack
  ) {
    final Person person = this.personService.findById(idUser);
    final Plan plan = this.findPlanById(idPlan);
    final List<PermissionDto> permissionsOffice = this.fetchOfficePermissions(plan.getOffice(), person);
    final List<PermissionDto> permissionsPlan = this.fetchPlanPermissions(idPlan, idUser);
    final Set<Workpack> workpacks = this.getAllWorkpacksUsingPlan(idPlan);
    final Workpack workpack = this.getWorkpack(workpacks, idWorkpack);

    if(workpack == null) return null;

    if(hasStakeholderSessionActive(workpack)) {
      List<PermissionDto> permissions = ifCanAccessWorkpackThenFetchPermissions(workpack, idUser);
      permissions = this.fetchPermissionsFromWorkpack(workpack, idUser, idPlan);
      final List<PermissionDto> permissionDtos = this.fetchPermissions(
        permissions,
        permissionsPlan,
        permissionsOffice,
        idWorkpack,
        idPlan
      );
      return permissionDtos;
    }
    final List<PermissionDto> permissions = this.fetchPermissionsFromWorkpack(workpack, idUser, idPlan);
    return this.fetchPermissions(permissions, permissionsPlan, permissionsOffice, idWorkpack, idPlan);
  }

  private Set<Workpack> getAllWorkpacksUsingPlan(final Long idPlan) {
    return this.workpackRepository.findAllUsingPlan(idPlan);
  }

  private List<PermissionDto> fetchPermissions(
    final Collection<? extends PermissionDto> permissionsWorkpack,
    final List<PermissionDto> permissionsPlan,
    final List<PermissionDto> permissionsOffice,
    final Long idWorkpack,
    final Long idPlan
  ) {
    if(permissionsWorkpack != null && permissionsWorkpack.stream().anyMatch(WorkpackPermissionVerifier::hasEditLevel)) {
      return permissionsWorkpack.stream().filter(permission -> permission.getIdPlan().equals(idPlan)).collect(Collectors.toList());
    }
    if(permissionsPlan.stream().anyMatch(WorkpackPermissionVerifier::hasEditLevel)) {
      return this.verifyLinkedPermission(permissionsPlan, idWorkpack, idPlan);
    }
    if(permissionsOffice.stream().anyMatch(WorkpackPermissionVerifier::hasEditLevel)) {
      return this.verifyLinkedPermission(permissionsOffice, idWorkpack, idPlan);
    }
    if(permissionsWorkpack != null && !permissionsWorkpack.isEmpty()) {
      return permissionsWorkpack.stream().filter(permission -> permission.getIdPlan().equals(idPlan)).collect(Collectors.toList());
    }
    return (permissionsPlan).isEmpty() ? permissionsOffice : permissionsPlan;
  }

  private List<PermissionDto> verifyLinkedPermission(
    final List<PermissionDto> permissions,
    final Long idWorkpack,
    final Long idPlan
  ) {
    final boolean notLinked = !this.planService.hasLinkWithWorkpack(idWorkpack, idPlan);

    if(notLinked) return permissions;

    final Workpack workpack = this.findSharedWorkpackByIdPlan(idWorkpack, idPlan);

    if(workpack.hasSharedWith()) {
      final PermissionDto sharedPermission = workpack.getSharedWith().stream()
        .filter(shared -> shared.containsPlan(idPlan))
        .map(PermissionDto::of)
        .findFirst()
        .get();
      return sharedPermission.getLevel() == READ ? Collections.singletonList(sharedPermission) : permissions;
    }
    return Collections.singletonList(PermissionDto.of(workpack));
  }

  private Workpack findSharedWorkpackByIdPlan(
    final Long idWorkpack,
    final Long idPlan
  ) {
    return this.workpackSharedRepository.findSharedWorkpackByIdPlan(idWorkpack, idPlan)
      .orElseThrow(() -> new NegocioException(ApplicationMessage.WORKPACK_NOT_FOUND));
  }

  public List<WorkpackDetailDto> verify(
    final List<WorkpackDetailDto> workpackList,
    final Long idUser,
    final Long idPlan
  ) {
    final Person person = this.personService.findById(idUser);
    if(TRUE.equals(person.getAdministrator())) {
      return workpackList;
    }
    final Plan plan = this.findPlanById(idPlan);
    final List<PermissionDto> permissionsOffice = this.fetchOfficePermissions(plan.getOffice(), person);
    final List<PermissionDto> permissionsPlan = this.fetchPlanPermissions(idPlan, idUser);

    final Set<Workpack> workpacks = this.getAllWorkpacksUsingPlan(idPlan);
    for(final Iterator<WorkpackDetailDto> it = workpackList.iterator(); it.hasNext(); ) {
      final WorkpackDetailDto workpackDetailDto = it.next();
      final Workpack workpack = this.getWorkpack(workpacks, workpackDetailDto.getId());
      if(workpack != null) {
        if(hasStakeholderSessionActive(workpack)) {
          List<PermissionDto> permissions = ifCanAccessWorkpackThenFetchPermissions(workpack, idUser);
          permissions = this.fetchPermissions(
            permissions,
            permissionsPlan,
            permissionsOffice,
            workpackDetailDto.getId(),
            idPlan
          );
          if((permissions.isEmpty())) {
            permissions = this.fetchReadOnlyPermissions(workpack.getId(), idUser, idPlan);
            if(permissions.isEmpty()) {
              it.remove();
              continue;
            }
          }
          workpackDetailDto.setPermissions(permissions);
          continue;
        }

        List<PermissionDto> permissions = this.fetchPermissionsFromWorkpack(workpack, idUser, idPlan);

        permissions = this.fetchPermissions(permissions, permissionsPlan, permissionsOffice, workpackDetailDto.getId(), idPlan);

        if(permissions.isEmpty()) {
          permissions = this.fetchReadOnlyPermissions(workpack.getId(), idUser, idPlan);
        }

        if(permissions.isEmpty()) {
          it.remove();
          continue;
        }
        workpackDetailDto.setPermissions(permissions);
      }
    }

    return workpackList;
  }

  private boolean hasPermissionReadWorkpack(
    final Long idWorkpack,
    final Long idUser,
    final Long idPlan
  ) {
    final Long qtdCanAccessWorkpack = this.workpackRepository.countCanAccessWorkpack(idWorkpack, idUser, idPlan);
    return qtdCanAccessWorkpack > 0;
  }

  private List<PermissionDto> fetchReadOnlyPermissions(
    final Long idWorkpack,
    final Long idUser,
    final Long idPlan
  ) {
    final List<PermissionDto> permissions = new ArrayList<>();
    if(this.hasPermissionReadWorkpack(idWorkpack, idUser, idPlan)) {
      final PermissionDto dto = new PermissionDto();
      dto.setId(0L);
      dto.setLevel(READ);
      dto.setRole("user");
      permissions.add(dto);
    }
    return permissions;
  }

  private Workpack getWorkpack(
    final Collection<? extends Workpack> workpacks,
    final Long id
  ) {
    final Optional<? extends Workpack> workpack = workpacks.stream()
      .filter(w -> w.getId().equals(id))
      .findFirst();

    if(workpack.isPresent()) {
      return workpack.get();
    }

    for(final Workpack w : workpacks) {
      if(w.getChildren() != null) {
        final Workpack workpackFetched = this.getWorkpack(w.getChildren(), id);
        if(workpackFetched != null) {
          return workpackFetched;
        }
      }
    }
    return null;
  }

  public List<PermissionDto> fetchOfficePermissions(
    final Office office,
    final Person person
  ) {
    final List<CanAccessOffice> canAccessOffices = this.officePermissionService.findByOfficeAndPerson(
      office.getId(),
      person.getId()
    );
    return canAccessOffices.stream()
      .map(PermissionDto::of)
      .collect(Collectors.toList());
  }

  private List<PermissionDto> fetchPlanPermissions(
    final Long idPlan,
    final Long idUser
  ) {
    final List<CanAccessPlan> canAccess = this.planPermissionService.findByIdPlan(idPlan);
    return canAccess.stream()
      .filter(canAccessPlan -> canAccessPlan.hasSameUser(idUser))
      .map(PermissionDto::of)
      .collect(Collectors.toList());
  }

  private Plan findPlanById(final Long idPlan) {
    return this.planService.findById(idPlan);
  }

  private List<PermissionDto> fetchPermissionsFromWorkpack(
    final Workpack workpack,
    final Long userId,
    final Long planId
  ) {
    if(hasStakeholderSessionActive(workpack) && workpack.sameOriginalPlan(planId)) {
      final List<PermissionDto> permissionDtos = ifCanAccessWorkpackThenFetchPermissions(workpack, userId);
      if(permissionDtos != null) return permissionDtos;
    }
    if(workpack.getParent() != null) {
      return this.fetchPermissionsFromWorkpackParent(workpack.getParent(), userId, planId);
    }
    return Collections.emptyList();
  }

  private List<PermissionDto> fetchPermissionsFromWorkpackParent(
    final Iterable<? extends Workpack> parent,
    final Long userId,
    final Long planId
  ) {
    for(final Workpack workpack : parent) {
      return this.fetchPermissionsFromWorkpack(workpack, userId, planId);
    }
    return Collections.emptyList();
  }

}
