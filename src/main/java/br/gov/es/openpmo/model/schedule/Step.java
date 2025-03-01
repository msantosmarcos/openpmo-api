package br.gov.es.openpmo.model.schedule;

import br.gov.es.openpmo.enumerator.CategoryEnum;
import br.gov.es.openpmo.model.Entity;
import br.gov.es.openpmo.model.baselines.Baseline;
import br.gov.es.openpmo.model.baselines.Snapshotable;
import br.gov.es.openpmo.model.relations.Consumes;
import br.gov.es.openpmo.model.relations.IsStepSnapshotOf;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.springframework.data.annotation.Transient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.neo4j.ogm.annotation.Relationship.INCOMING;

@NodeEntity
public class Step extends Entity implements Snapshotable<Step> {

  private BigDecimal actualWork;

  private BigDecimal plannedWork;

  private Long periodFromStart;

  @Relationship(type = "COMPOSES")
  private Baseline baseline;

  @Relationship(type = "COMPOSES")
  private Schedule schedule;

  @Relationship(type = "IS_SNAPSHOT_OF")
  private IsStepSnapshotOf master;

  @Relationship(type = "IS_SNAPSHOT_OF", direction = INCOMING)
  private Set<IsStepSnapshotOf> snapshots;

  @Relationship("CONSUMES")
  private Set<Consumes> consumes;

  private CategoryEnum category;

  public int getYear() {
    return this.getPeriodFromStartDate().getYear();
  }

  public LocalDate getPeriodFromStartDate() {
    return this.schedule.getStart().plusMonths(this.periodFromStart);
  }

  public YearMonth getYearMonth() {
    return YearMonth.from(this.getPeriodFromStartDate());
  }

  public Schedule getSchedule() {
    return this.schedule;
  }

  public void setSchedule(final Schedule schedule) {
    this.schedule = schedule;
  }

  public Set<Consumes> getConsumes() {
    if (this.consumes == null) {
      this.consumes = new HashSet<>();
    }
    return this.consumes;
  }

  public void setConsumes(final Set<Consumes> consumes) {
    this.consumes = consumes;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), this.actualWork, this.plannedWork, this.periodFromStart);
  }

  @Override
  public boolean equals(final Object o) {
    if(this == o) {
      return true;
    }
    if(o == null || this.getClass() != o.getClass()) {
      return false;
    }
    if(!super.equals(o)) {
      return false;
    }
    final Step step = (Step) o;
    return Objects.equals(this.actualWork, step.actualWork)
           && Objects.equals(this.plannedWork, step.plannedWork)
           && Objects.equals(this.periodFromStart, step.periodFromStart);
  }

  @Override
  public Step snapshot() {
    final Step snapshot = new Step();
    snapshot.setActualWork(this.getActualWork());
    snapshot.setPlannedWork(this.getPlannedWork());
    snapshot.setPeriodFromStart(this.getPeriodFromStart());
    return snapshot;
  }

  @Override
  public Baseline getBaseline() {
    return this.baseline;
  }

  @Override
  public void setBaseline(final Baseline baseline) {
    this.baseline = baseline;
  }

  @Override
  public CategoryEnum getCategory() {
    return this.category;
  }

  @Override
  public void setCategory(final CategoryEnum category) {
    this.category = category;
  }

  @Override
  public boolean hasChanges(final Step other) {
    final boolean hasActualWorkChanges =
      (this.actualWork != null
       || other.actualWork != null)
      && (this.actualWork != null &&
          other.actualWork == null
          || this.actualWork == null
          || !this.actualWork.equals(other.actualWork));

    final boolean hasPlannedWorkChanges =
      (this.plannedWork != null
       || other.plannedWork != null)
      && (this.plannedWork != null
          && other.plannedWork == null
          || this.plannedWork == null
          || !this.plannedWork.equals(other.plannedWork));

    final boolean hasPeriodFromStartChanges =
      (this.periodFromStart != null
       || other.periodFromStart != null)
      && (this.periodFromStart != null
          && other.periodFromStart == null
          || this.periodFromStart == null
          || !this.periodFromStart.equals(other.periodFromStart));

    return hasActualWorkChanges
           || hasPlannedWorkChanges
           || hasPeriodFromStartChanges;
  }

  public BigDecimal getActualWork() {
    return this.actualWork;
  }

  public void setActualWork(final BigDecimal actualWork) {
    this.actualWork = actualWork;
  }

  public BigDecimal getPlannedWork() {
    return this.plannedWork;
  }

  public void setPlannedWork(final BigDecimal plannedWork) {
    this.plannedWork = plannedWork;
  }

  public Long getPeriodFromStart() {
    return this.periodFromStart;
  }

  public void setPeriodFromStart(final Long periodFromStart) {
    this.periodFromStart = periodFromStart;
  }

  public IsStepSnapshotOf getMaster() {
    return this.master;
  }

  public void setMaster(final IsStepSnapshotOf master) {
    this.master = master;
  }

  public Set<IsStepSnapshotOf> getSnapshots() {
    return this.snapshots;
  }

  public void setSnapshots(final Set<IsStepSnapshotOf> snapshots) {
    this.snapshots = snapshots;
  }

  @Transient
  public Long getScheduleId() {
    return this.schedule.getId();
  }


  @Transient
  public LocalDate getScheduleStart() {
    return this.schedule.getStart();
  }

  @Transient
  public LocalDate getScheduleEnd() {
    return this.schedule.getEnd();
  }

  private LocalDate relativeToSchedule() {
    return this.getScheduleStart().plusMonths(this.periodFromStart)
      .with(TemporalAdjusters.lastDayOfMonth());
  }

  @Transient
  public boolean equivalent(final Step another) {
    final LocalDate anotherRelativeToSchedule = another.relativeToSchedule();
    final LocalDate relativeToSchedule = this.relativeToSchedule();
    return relativeToSchedule.compareTo(anotherRelativeToSchedule) == 0;
  }

}
