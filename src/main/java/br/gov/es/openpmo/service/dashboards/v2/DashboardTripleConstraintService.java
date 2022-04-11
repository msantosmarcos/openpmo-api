package br.gov.es.openpmo.service.dashboards.v2;

import br.gov.es.openpmo.dto.dashboards.DashboardParameters;
import br.gov.es.openpmo.dto.dashboards.tripleconstraint.CostAndScopeData;
import br.gov.es.openpmo.dto.dashboards.tripleconstraint.DateIntervalQuery;
import br.gov.es.openpmo.dto.dashboards.tripleconstraint.ScheduleDataChart;
import br.gov.es.openpmo.dto.dashboards.tripleconstraint.TripleConstraintDataChart;
import br.gov.es.openpmo.exception.NegocioException;
import br.gov.es.openpmo.model.baselines.Baseline;
import br.gov.es.openpmo.model.schedule.Step;
import br.gov.es.openpmo.repository.BaselineRepository;
import br.gov.es.openpmo.repository.ScheduleRepository;
import br.gov.es.openpmo.repository.WorkpackRepository;
import br.gov.es.openpmo.repository.dashboards.DashboardRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

import static br.gov.es.openpmo.utils.ApplicationMessage.BASELINE_NOT_FOUND;
import static br.gov.es.openpmo.utils.ApplicationMessage.INTERVAL_DATE_IN_BASELINE_NOT_FOUND;

@Service
public class DashboardTripleConstraintService implements IDashboardTripleConstraintService {

    private final WorkpackRepository workpackRepository;
    private final BaselineRepository baselineRepository;
    private final ScheduleRepository scheduleRepository;
    private final DashboardRepository dashboardRepository;
    private final IDashboardCostScopeService costScopeService;

    public DashboardTripleConstraintService(
            WorkpackRepository workpackRepository,
            BaselineRepository baselineRepository,
            ScheduleRepository scheduleRepository,
            DashboardRepository dashboardRepository,
            IDashboardCostScopeService costScopeService
    ) {
        this.workpackRepository = workpackRepository;
        this.baselineRepository = baselineRepository;
        this.scheduleRepository = scheduleRepository;
        this.dashboardRepository = dashboardRepository;
        this.costScopeService = costScopeService;
    }

    @Override
    public TripleConstraintDataChart build(DashboardParameters parameters) {
        final YearMonth yearMonth = parameters.getYearMonth();

        if (yearMonth == null) {
            return null;
        }

        final Long workpackId = parameters.getWorkpackId();
        final Long baselineId = parameters.getBaselineId();

        final Set<Long> deliverablesId = getDeliverablesId(workpackId);
        return calculateForMonth(workpackId, baselineId, yearMonth, deliverablesId);
    }

    private Set<Long> getDeliverablesId(Long workpackId) {
        final Set<Long> deliverablesId = this.workpackRepository.getDeliverablesId(workpackId);

        if (this.workpackRepository.isDeliverable(workpackId)) {
            deliverablesId.add(workpackId);
        }

        return deliverablesId;
    }

    private TripleConstraintDataChart calculateForMonth(
            Long workpackId,
            Long baselineId,
            YearMonth yearMonth,
            Set<Long> deliverablesId
    ) {
        final TripleConstraintDataChart tripleConstraint = new TripleConstraintDataChart();

        tripleConstraint.setIdBaseline(baselineId);
        tripleConstraint.setMesAno(yearMonth.atEndOfMonth());

        this.buildScheduleDataChart(baselineId, workpackId, tripleConstraint, yearMonth);

        for (Long deliverableId : deliverablesId) {
            calculateForDeliverable(baselineId, tripleConstraint, deliverableId, yearMonth);
        }

        return tripleConstraint;
    }

    @Override
    @NonNull
    public Optional<List<TripleConstraintDataChart>> calculate(@NonNull Long workpackId) {
        final List<Long> baselineIds = getActiveBaselineIds(workpackId);

        return this.dashboardRepository.fetchIntervalOfSchedules(workpackId, baselineIds)
                .filter(DateIntervalQuery::isValid)
                .map(DateIntervalQuery::toYearMonths)
                .map(months -> this.calculateForAllMonths(workpackId, months));
    }

    private List<TripleConstraintDataChart> calculateForAllMonths(
            @NonNull Long workpackId,
            @NonNull List<YearMonth> months
    ) {
        final ArrayList<TripleConstraintDataChart> charts = new ArrayList<>();
        final Set<Long> deliverablesId = getDeliverablesId(workpackId);

        for (Baseline baseline : getBaselines(workpackId)) {
            calculateForBaseline(workpackId, baseline.getId(), months, deliverablesId, charts);
        }

        return charts;
    }

    private List<Baseline> getBaselines(Long workpackId) {
        final List<Baseline> baselines =
                this.baselineRepository.findApprovedOrProposedBaselinesByAnyWorkpackId(workpackId);

        if (this.workpackRepository.isProject(workpackId)) {
            return baselines;
        }

        for (Baseline baseline : baselines) {
            if (baseline.isActive()) {
                return Collections.singletonList(baseline);
            }
        }

        return baselines.stream()
                .max(Comparator.comparing(Baseline::getProposalDate))
                .map(Collections::singletonList)
                .orElse(null);
    }

    private void calculateForBaseline(
            Long workpackId,
            Long baselineId,
            List<YearMonth> months,
            Set<Long> deliverablesId,
            List<TripleConstraintDataChart> charts
    ) {
        for (YearMonth month : months) {
            charts.add(calculateForMonth(workpackId, baselineId, month, deliverablesId));
        }
    }

    private void buildScheduleDataChart(
            final Long baselineId,
            final Long workpackId,
            final TripleConstraintDataChart tripleConstraint,
            final YearMonth yearMonth
    ) {
        final List<Long> baselineIds = Optional.ofNullable(baselineId)
                .map(Collections::singletonList)
                .orElse(getActiveBaselineIds(workpackId));

        final DateIntervalQuery plannedInterval = this.findIntervalInSnapshots(workpackId, baselineIds);
        final DateIntervalQuery foreseenInterval = this.findIntervalInWorkpack(workpackId);
        final ScheduleDataChart schedule = ScheduleDataChart.ofIntervals(plannedInterval, foreseenInterval, yearMonth);
        tripleConstraint.setSchedule(schedule);
    }

    private void calculateForDeliverable(
            Long baselineId,
            TripleConstraintDataChart tripleConstraint,
            Long deliverableId,
            YearMonth yearMonth
    ) {
        this.scheduleRepository.findScheduleByWorkpackId(deliverableId).ifPresent(schedule ->
                this.sumCostAndWorkOfSteps(deliverableId, baselineId, tripleConstraint, schedule.getSteps(), yearMonth));
    }

    private List<Long> getActiveBaselineIds(Long workpackId) {
        List<Baseline> baselines = hasActiveBaseline(workpackId)
                ? findActiveBaseline(workpackId)
                : findAllActiveBaselines(workpackId);

        return baselines.stream()
                .map(Baseline::getId)
                .collect(Collectors.toList());
    }

    private DateIntervalQuery findIntervalInSnapshots(final Long workpackId, final List<Long> baselineIds) {
        return this.baselineRepository.findScheduleIntervalInSnapshotsOfBaselines(workpackId, baselineIds)
                .orElseThrow(() -> new NegocioException(INTERVAL_DATE_IN_BASELINE_NOT_FOUND));
    }

    private void sumCostAndWorkOfSteps(
            final Long deliverableId,
            final Long baselineId,
            final TripleConstraintDataChart tripleConstraint,
            final Set<? extends Step> steps,
            final YearMonth yearMonth
    ) {
        final CostAndScopeData costAndScopeData = this.costScopeService.build(deliverableId, baselineId, yearMonth, steps);
        tripleConstraint.sumCostData(costAndScopeData.getCostDataChart());
        tripleConstraint.sumScopeData(costAndScopeData.getScopeDataChart());
    }

    private DateIntervalQuery findIntervalInWorkpack(final Long workpackId) {
        return this.workpackRepository.findIntervalInSchedulesChildrenOf(workpackId)
                .orElseThrow(() -> new NegocioException(INTERVAL_DATE_IN_BASELINE_NOT_FOUND));
    }

    private boolean hasActiveBaseline(Long workpackId) {
        return this.workpackRepository.hasActiveBaseline(workpackId);
    }

    private List<Baseline> findActiveBaseline(Long workpackId) {
        return this.baselineRepository.findActiveBaseline(workpackId)
                .map(Collections::singletonList)
                .orElseThrow(() -> new NegocioException(BASELINE_NOT_FOUND));
    }

    private List<Baseline> findAllActiveBaselines(Long workpackId) {
        return this.baselineRepository.findAllActiveBaselines(workpackId);
    }

}
