package br.gov.es.openpmo.service.dashboards;

import br.gov.es.openpmo.dto.dashboards.DashboardDataParameters;
import br.gov.es.openpmo.dto.dashboards.tripleconstraint.CostAndScopeData;
import br.gov.es.openpmo.dto.dashboards.tripleconstraint.DateIntervalQuery;
import br.gov.es.openpmo.dto.dashboards.tripleconstraint.ScheduleDataChart;
import br.gov.es.openpmo.dto.dashboards.tripleconstraint.TripleConstraintDataChart;
import br.gov.es.openpmo.exception.NegocioException;
import br.gov.es.openpmo.model.baselines.Baseline;
import br.gov.es.openpmo.model.schedule.Schedule;
import br.gov.es.openpmo.model.schedule.Step;
import br.gov.es.openpmo.model.workpacks.Project;
import br.gov.es.openpmo.model.workpacks.Workpack;
import br.gov.es.openpmo.repository.BaselineRepository;
import br.gov.es.openpmo.repository.ScheduleRepository;
import br.gov.es.openpmo.repository.WorkpackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static br.gov.es.openpmo.utils.ApplicationMessage.BASELINE_NOT_FOUND;
import static br.gov.es.openpmo.utils.ApplicationMessage.INTERVAL_DATE_IN_BASELINE_NOT_FOUND;
import static br.gov.es.openpmo.utils.ApplicationMessage.WORKPACK_NOT_FOUND;

@Component
public class GetTripleConstraintData implements IGetTripleConstraintData {


  private final BaselineRepository repository;
  private final WorkpackRepository workpackRepository;
  private final ScheduleRepository scheduleRepository;
  private final IGetCostAndScope getStepTotalCost;


  @Autowired
  public GetTripleConstraintData(
    final BaselineRepository repository,
    final WorkpackRepository workpackRepository,
    final ScheduleRepository scheduleRepository,
    final IGetCostAndScope getStepTotalCost
  ) {
    this.repository = repository;
    this.getStepTotalCost = getStepTotalCost;
    this.workpackRepository = workpackRepository;
    this.scheduleRepository = scheduleRepository;
  }

  @Override
  public TripleConstraintDataChart get(final DashboardDataParameters parameters) {
    final Long idProject = this.findIdProjectInParentsOf(parameters.getIdWorkpack());

    final Long idBaseline = this.useIdBaselineOrFetchActiveIdBaselineIfNull(
      parameters,
      idProject
    );

    final Set<Workpack> deliverables = this.findMasterDeliverables(idProject);

    final TripleConstraintDataChart tripleConstraint = new TripleConstraintDataChart();

    this.buildTripleConstraintData(
      parameters,
      idBaseline,
      idProject,
      deliverables,
      tripleConstraint
    );

    return tripleConstraint;
  }

  private void buildTripleConstraintData(
    final DashboardDataParameters parameters,
    final Long idBaseline,
    final Long idProject,
    final Iterable<? extends Workpack> deliverables,
    final TripleConstraintDataChart tripleConstraint
  ) {

    this.buildScheduleDataChart(
      idBaseline,
      idProject,
      tripleConstraint,
      parameters.getYearMonth()
    );


    for(final Workpack deliverable : deliverables) {
      final Optional<Schedule> maybeSchedule = this.scheduleRepository.findScheduleByWorkpackId(deliverable.getId());

      if(isEmpty(maybeSchedule)) continue;

      final Schedule schedule = maybeSchedule.get();

      this.sumCostAndWorkOfSteps(
        parameters,
        idBaseline,
        tripleConstraint,
        schedule.getSteps()
      );
    }
  }

  private void buildScheduleDataChart(
    final Long idBaseline,
    final Long idProject,
    final TripleConstraintDataChart tripleConstraint,
    final YearMonth yearMonth
  ) {
    final DateIntervalQuery plannedInterval = this.findIntervalInSnapshots(idBaseline);
    final DateIntervalQuery foreseenInterval = this.findIntervalInProject(idProject);
    tripleConstraint.setSchedule(
      ScheduleDataChart.ofIntervals(
        plannedInterval,
        foreseenInterval,
        yearMonth
      )
    );
  }

  private DateIntervalQuery findIntervalInProject(final Long idProject) {
    return this.workpackRepository.findIntervalInSchedulesChildrenOf(idProject)
      .orElseThrow(() -> new NegocioException(INTERVAL_DATE_IN_BASELINE_NOT_FOUND));
  }

  private DateIntervalQuery findIntervalInSnapshots(final Long idBaseline) {
    return this.repository.findScheduleIntervalInSnapshotsOfBaseline(idBaseline)
      .orElseThrow(() -> new NegocioException(INTERVAL_DATE_IN_BASELINE_NOT_FOUND));
  }

  private void sumCostAndWorkOfSteps(
    final DashboardDataParameters parameters,
    final Long idBaseline,
    final TripleConstraintDataChart tripleConstraint,
    final Collection<? extends Step> steps
  ) {
    final CostAndScopeData costAndScopeData = this.getStepTotalCost.get(
      idBaseline,
      parameters.getYearMonth(),
      steps
    );

    tripleConstraint.sumCostData(costAndScopeData.getCostDataChart());
    tripleConstraint.sumScopeData(costAndScopeData.getScopeDataChart());
  }

  private static boolean isEmpty(final Optional<Schedule> maybeSchedule) {
    return !maybeSchedule.isPresent();
  }

  private Set<Workpack> findMasterDeliverables(final Long idProject) {
    return this.repository.findDeliverableWorkpacksOfProjectMaster(idProject);
  }

  private Long useIdBaselineOrFetchActiveIdBaselineIfNull(final DashboardDataParameters parameters, final Long idProject) {
    return Optional.ofNullable(parameters.getIdBaseline())
      .orElseGet(() -> this.findBaselineActive(idProject).getId());
  }

  private Baseline findBaselineActive(final Long idProject) {
    return this.repository.findActiveBaselineByWorkpackId(idProject)
      .orElseThrow(() -> new NegocioException(BASELINE_NOT_FOUND));
  }

  private Long findIdProjectInParentsOf(final Long idWorkpack) {
    final Workpack workpack = this.workpackRepository.findById(idWorkpack, 0)
      .orElseThrow(() -> new NegocioException(WORKPACK_NOT_FOUND));

    if(workpack instanceof Project) {
      return workpack.getId();
    }

    return this.workpackRepository.findProjectInParentsOf(idWorkpack)
      .map(Workpack::getId)
      .orElseThrow(() -> new NegocioException(WORKPACK_NOT_FOUND));
  }
}
