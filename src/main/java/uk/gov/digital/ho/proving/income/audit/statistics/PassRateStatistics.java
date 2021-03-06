package uk.gov.digital.ho.proving.income.audit.statistics;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
// Accessors not fluent because SuperCsv relies on setters following the "getX" format.
public class PassRateStatistics {
    private final LocalDate fromDate;
    private final LocalDate toDate;

    private final long totalRequests;
    private final long passes;
    private final long failures;
    private final long notFound;
    private final long errors;
}
