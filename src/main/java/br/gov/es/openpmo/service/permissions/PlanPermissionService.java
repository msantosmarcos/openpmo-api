package br.gov.es.openpmo.service.permissions;

import br.gov.es.openpmo.dto.permission.PermissionDto;
import br.gov.es.openpmo.dto.person.PersonDto;
import br.gov.es.openpmo.dto.person.RoleResource;
import br.gov.es.openpmo.dto.planpermission.PlanPermissionDto;
import br.gov.es.openpmo.dto.planpermission.PlanPermissionParamDto;
import br.gov.es.openpmo.exception.NegocioException;
import br.gov.es.openpmo.exception.RegistroNaoEncontradoException;
import br.gov.es.openpmo.model.actors.Person;
import br.gov.es.openpmo.model.office.Office;
import br.gov.es.openpmo.model.office.plan.Plan;
import br.gov.es.openpmo.model.relations.CanAccessPlan;
import br.gov.es.openpmo.model.relations.IsAuthenticatedBy;
import br.gov.es.openpmo.repository.PlanPermissionRepository;
import br.gov.es.openpmo.service.actors.IsAuthenticatedByService;
import br.gov.es.openpmo.service.actors.PersonService;
import br.gov.es.openpmo.service.office.OfficeService;
import br.gov.es.openpmo.service.office.plan.PlanService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static br.gov.es.openpmo.utils.ApplicationMessage.OFFICE_NOT_FOUND;
import static br.gov.es.openpmo.utils.ApplicationMessage.PLAN_PERMISSION_NOT_FOUND;

@Service
public class PlanPermissionService {

  private final PlanPermissionRepository repository;
  private final PlanService planService;
  private final PersonService personService;
  private final OfficeService officeService;
  private final IsAuthenticatedByService isAuthenticatedByService;
  private final IRemoteRolesFetcher remoteRolesFetcher;
  private final ModelMapper modelMapper;

  @Autowired
  public PlanPermissionService(
    final PlanPermissionRepository repository,
    final PlanService planService,
    final PersonService personService,
    final OfficeService officeService,
    final IsAuthenticatedByService isAuthenticatedByService,
    final IRemoteRolesFetcher remoteRolesFetcher,
    final ModelMapper modelMapper
  ) {
    this.repository = repository;
    this.planService = planService;
    this.personService = personService;
    this.officeService = officeService;
    this.isAuthenticatedByService = isAuthenticatedByService;
    this.remoteRolesFetcher = remoteRolesFetcher;
    this.modelMapper = modelMapper;
  }

  public void delete(final Long idPlan, final String email) {
    final Optional<Person> personOptional = this.personService.findByEmail(email);
    if(!personOptional.isPresent()) {
      throw new RegistroNaoEncontradoException(PLAN_PERMISSION_NOT_FOUND);
    }
    final List<CanAccessPlan> list = this.repository.findByIdPlanAndIdPerson(idPlan, personOptional.get().getId());
    this.repository.deleteAll(list);
  }

  public List<PlanPermissionDto> findAllDto(final Long idPlan, final String email) {
    final List<PlanPermissionDto> plansPermissionDto = new ArrayList<>();
    final Plan plan = this.planService.findById(idPlan);
    final List<CanAccessPlan> listPlansPermission = this.listPlansPermissions(plan, email);
    final Map<Person, List<PermissionDto>> mapPermission = new HashMap<>();

    listPlansPermission.forEach(p -> {
      final PermissionDto dto = new PermissionDto();
      dto.setId(p.getId());
      dto.setLevel(p.getPermissionLevel());
      dto.setRole(p.getRole());
      mapPermission.computeIfAbsent(p.getPerson(), k -> new ArrayList<>());
      mapPermission.get(p.getPerson()).add(dto);
    });

    mapPermission.keySet().forEach(person -> {
      final PlanPermissionDto planPermissionDto = new PlanPermissionDto();
      planPermissionDto.setIdPlan(idPlan);
      planPermissionDto.setPermissions(mapPermission.get(person));

      final Optional<IsAuthenticatedBy> maybeAuthenticatedBy = this.isAuthenticatedByService
        .findAuthenticatedBy(person.getId());

      planPermissionDto.setPerson(PersonDto.from(
        person,
        Optional.empty(),
        maybeAuthenticatedBy
      ));
      plansPermissionDto.add(planPermissionDto);

      final List<RoleResource> roles = this.remoteRolesFetcher.fetch(person.getId());

      planPermissionDto.getPerson().addAllRoles(roles);

    });

    return plansPermissionDto;
  }

  private List<CanAccessPlan> listPlansPermissions(final Plan plan, final String email) {
    if(email == null || email.isEmpty()) {
      return this.findByIdPlan(plan.getId());
    }

    final Person person = this.personService.findPersonByEmail(email);

    return this.findByPlanAndPerson(plan, person);
  }

  public List<CanAccessPlan> findByIdPlan(final Long idPlan) {
    return this.repository.findByIdPlan(idPlan);
  }

  public List<CanAccessPlan> findByPlanAndPerson(final Plan plan, final Person person) {
    return this.repository.findByIdPlanAndIdPerson(plan.getId(), person.getId());
  }

  public CanAccessPlan findById(final Long id) {
    return this.repository.findById(id)
      .orElseThrow(() -> new NegocioException(PLAN_PERMISSION_NOT_FOUND));
  }

  public void deleteAll(final Set<CanAccessPlan> permissions) {
    this.repository.deleteAll(permissions);
  }

  public void update(final PlanPermissionParamDto request) {

    final Office office = this.officeService.findOfficeByPlan(request.getIdPlan())
      .orElseThrow(() -> new NegocioException(OFFICE_NOT_FOUND));

    final Person person = this.returnPersonOrCreateIfNotExists(
      request.getEmail(),
      request.getPerson(),
      office.getId()
    );

    final Plan plan = this.planService.findById(request.getIdPlan());
    final List<CanAccessPlan> plansPermissionsDataBase = this.findByPlanAndPerson(plan, person);

    plansPermissionsDataBase.forEach(permissionDatabase -> {
      final boolean find = request.getPermissions() != null && request.getPermissions().stream()
        .anyMatch(filtro -> permissionDatabase.getId().equals(filtro.getId()));
      if(find) {
        return;
      }
      this.delete(permissionDatabase);
    });

    if(request.getPermissions() != null) {
      request.getPermissions().forEach(permission -> {
        if(permission.getId() == null) {
          this.save(this.buildCanAccessPlan(person, plan, permission, null));
          return;
        }

        final Optional<CanAccessPlan> optionalCanAccessPlan = this.repository.findById(permission.getId());
        if(!optionalCanAccessPlan.isPresent()) {
          throw new RegistroNaoEncontradoException(PLAN_PERMISSION_NOT_FOUND);
        }
        this.save(this.buildCanAccessPlan(person, plan, permission, optionalCanAccessPlan.get().getId()));
      });
    }

  }

  private Person returnPersonOrCreateIfNotExists(final String email, final PersonDto person, final Long idOffice) {
    final Optional<Person> maybePerson = this.personService.findByEmail(email);
    return maybePerson.orElseGet(() -> this.storePerson(email, person, idOffice));

  }

  private Person storePerson(final String email, final PersonDto person, final Long idOffice) {
    return this.personService.savePerson(email, person, idOffice);
  }

  public void delete(final CanAccessPlan plan) {
    this.repository.delete(plan);
  }

  public CanAccessPlan save(final CanAccessPlan planPermission) {
    return this.repository.save(planPermission);
  }

  private CanAccessPlan buildCanAccessPlan(final Person person, final Plan plan, final PermissionDto request, final Long id) {
    return new CanAccessPlan(
      id,
      "",
      request.getRole(),
      request.getLevel(),
      person,
      plan
    );
  }

  public void store(final PlanPermissionParamDto request) {

    final Office office = this.officeService.findOfficeByPlan(request.getIdPlan())
      .orElseThrow(() -> new NegocioException(OFFICE_NOT_FOUND));

    final Person person = this.returnPersonOrCreateIfNotExists(
      request.getEmail(),
      request.getPerson(),
      office.getId()
    );

    final Plan plan = this.planService.findById(request.getIdPlan());

    request.getPermissions().forEach(permission -> this.save(this.buildCanAccessPlan(
      person,
      plan,
      permission,
      null
    )));
  }

  public Set<CanAccessPlan> findInheritedPermission(final Long workpackId, final Long personId) {
    return this.repository.findInheritedPermission(workpackId, personId);
  }
}
