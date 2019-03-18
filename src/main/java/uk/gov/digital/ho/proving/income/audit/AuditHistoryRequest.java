package uk.gov.digital.ho.proving.income.audit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@Getter
@Accessors(fluent = true)
class AuditHistoryRequest {
    private LocalDate toDate;
    private List<AuditEventType> eventTypes;
}
