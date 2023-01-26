package br.gov.es.openpmo.controller.schedule;

import br.gov.es.openpmo.dto.EntityDto;
import br.gov.es.openpmo.dto.ResponseBase;
import br.gov.es.openpmo.dto.schedule.ScheduleDto;
import br.gov.es.openpmo.dto.schedule.ScheduleParamDto;
import br.gov.es.openpmo.dto.schedule.StepUpdateDto;
import br.gov.es.openpmo.model.schedule.Schedule;
import br.gov.es.openpmo.model.schedule.Step;
import br.gov.es.openpmo.service.permissions.canaccess.ICanAccessService;
import br.gov.es.openpmo.service.schedule.ScheduleService;
import br.gov.es.openpmo.service.schedule.StepService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Api
@RestController
@CrossOrigin
@RequestMapping("/schedules")
public class ScheduleController {

  private final ScheduleService scheduleService;
  private final StepService stepService;
  private final ICanAccessService canAccessService;

  @Autowired
  public ScheduleController(
      final ScheduleService scheduleService,
      final StepService stepService,
      final ICanAccessService canAccessService) {
    this.scheduleService = scheduleService;
    this.stepService = stepService;
    this.canAccessService = canAccessService;
  }

  @GetMapping
  public ResponseEntity<ResponseBase<List<ScheduleDto>>> findAll(@RequestParam("id-workpack") final Long idWorkpack) {
    final List<ScheduleDto> schedules = this.scheduleService.findAll(idWorkpack);
    if (schedules.isEmpty()) {
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.ok(ResponseBase.of(schedules));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ResponseBase<ScheduleDto>> findById(@PathVariable final Long id) {
    final Schedule schedule = this.scheduleService.findById(id);
    final ScheduleDto scheduleDto = this.scheduleService.mapsToScheduleDto(schedule);
    return ResponseEntity.ok(ResponseBase.of(scheduleDto));
  }

  @PostMapping
  public ResponseEntity<ResponseBase<EntityDto>> save(@Valid @RequestBody final ScheduleParamDto scheduleParamDto,
      @RequestHeader(name = "Authorization") final String authorization) {

    this.canAccessService.ensureCanEditResource(scheduleParamDto.getId(), authorization);
    final Schedule schedule = this.scheduleService.save(scheduleParamDto);
    return ResponseEntity.ok(ResponseBase.of(new EntityDto(schedule.getId())));
  }

  @PutMapping
  public ResponseEntity<ResponseBase<EntityDto>> update(@RequestBody @Valid final StepUpdateDto stepUpdateDto,
      @RequestHeader(name = "Authorization") final String authorization) {

    this.canAccessService.ensureCanEditResource(stepUpdateDto.getId(), authorization);
    final Step step = this.stepService.update(stepUpdateDto);
    return ResponseEntity.ok(ResponseBase.of(new EntityDto(step.getId())));
  }

  @DeleteMapping("{id}")
  public ResponseEntity<Void> delete(@PathVariable final Long id,
      @RequestHeader(name = "Authorization") final String authorization) {

    this.canAccessService.ensureCanEditResource(id, authorization);
    this.scheduleService.delete(id);
    return ResponseEntity.ok().build();
  }

}
