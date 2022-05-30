package br.gov.es.openpmo.apis.edocs.response;

import br.gov.es.openpmo.apis.edocs.ProcessTimeline;
import br.gov.es.openpmo.utils.ApplicationMessage;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class ProcessResponse {

    private final String id;
    private final String processNumber;
    private final String subject;
    private final String status;
    private final boolean priority;
    private final List<ProcessHistoryResponse> history;
    private final List<ProcessTimeline> processTimeline;

    public ProcessResponse(
            final String id,
            final String processNumber,
            final String subject,
            final String status,
            final Boolean priority
    ) {
        this.id = id;
        this.processNumber = processNumber;
        this.subject = subject;
        this.status = status;
        this.priority = priority;
        this.history = new ArrayList<>();
        this.processTimeline = new ArrayList<>();
    }

    public ProcessResponse(final JSONObject json) {
        this.id = json.getString("id");
        this.processNumber = json.getString("protocolo");
        this.subject = json.getString("resumo");
        this.status = json.getString("situacao");
        this.priority = this.isClassifiedAsPriority(json.optString("classificacao"));
        this.history = new ArrayList<>();
        this.processTimeline = new ArrayList<>();
    }

    private boolean isClassifiedAsPriority(final String classificao) {
        return "prioridade".equalsIgnoreCase(classificao);
    }

    public String getCurrentOrganizationAbbreviation() {
        return Optional.ofNullable(this.currentOrganization())
                .map(ProcessHistoryResponse::getAbbreviation)
                .orElse(null);
    }

    private ProcessHistoryResponse currentOrganization() {
        return this.history.stream()
                .max(Comparator.comparing(ProcessHistoryResponse::getDate))
                .orElse(null);
    }

    public String getId() {
        return this.id;
    }

    public String getProcessNumber() {
        return this.processNumber;
    }

    public String getSubject() {
        return this.subject;
    }

    public void addHistory(final List<ProcessHistoryResponse> processHistory) {
        this.history.addAll(Collections.unmodifiableList(processHistory));
    }

    public String getStatus() {
        return this.status;
    }

    public boolean getPriority() {
        return this.priority;
    }

    public List<ProcessTimeline> timeline() {
        if (this.history.isEmpty()) {
            return Collections.emptyList();
        }

        this.history.sort(Comparator.comparing(ProcessHistoryResponse::getDate));

        final Iterator<ProcessHistoryResponse> iterator = this.history.iterator();

        if (this.history.size() == 1) {
            return this.timelineWithOneItem();
        }

        ProcessHistoryResponse current = iterator.next();
        do {
            if (!iterator.hasNext()) {
                throw new IllegalStateException(ApplicationMessage.PROCESS_HISTORY_ITEM_NULL);
            }
            final ProcessHistoryResponse next = iterator.next();
            this.addToTimeline(current, next);
            current = next;
        } while (iterator.hasNext());

        this.addLastOrganizationToTimeline(current);

        return Collections.unmodifiableList(this.processTimeline);
    }

    private void addLastOrganizationToTimeline(final ProcessHistoryResponse current) {
        this.processTimeline.add(this.createTimeline(this.lengthOfStayOn(), current));
    }

    private void addToTimeline(
            final ProcessHistoryResponse current,
            final ProcessHistoryResponse next
    ) {
        final long days = this.calculateDurationInDays(current.getDate(), next.getDate());
        this.processTimeline.add(this.createTimeline(days, current));
    }

    private List<ProcessTimeline> timelineWithOneItem() {
        return this.history.stream()
                .map(processHistory -> this.createTimeline(this.lengthOfStayOn(), processHistory))
                .collect(Collectors.toList());
    }

    public Long lengthOfStayOn() {
        final ProcessHistoryResponse processHistoryResponse = this.currentOrganization();
        if (processHistoryResponse == null) {
            return null;
        }
        return this.calculateDurationInDays(processHistoryResponse.getDate(), LocalDateTime.now());
    }

    private long calculateDurationInDays(final LocalDateTime initialDateTime, final LocalDateTime finalDateTime) {
        LocalDate initialDate = LocalDate.from(initialDateTime);
        LocalDate finalDate = LocalDate.from(finalDateTime);
        return initialDate.until(finalDate, ChronoUnit.DAYS);
    }

    private ProcessTimeline createTimeline(final long daysDuration, final ProcessHistoryResponse detail) {
        return new ProcessTimeline(daysDuration, detail);
    }

    public void addHistory(final ProcessHistoryResponse history) {
        this.history.add(history);
    }
}

