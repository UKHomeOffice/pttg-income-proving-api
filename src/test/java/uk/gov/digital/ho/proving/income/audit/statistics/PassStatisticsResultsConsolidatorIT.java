package uk.gov.digital.ho.proving.income.audit.statistics;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.digital.ho.proving.income.audit.AuditResult;
import uk.gov.digital.ho.proving.income.audit.AuditResultComparator;
import uk.gov.digital.ho.proving.income.audit.AuditResultType;
import uk.gov.digital.ho.proving.income.audit.AuditResultTypeComparator;

import java.time.LocalDate;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    PassStatisticsResultsConsolidator.class,
    AuditResultComparator.class,
    AuditResultTypeComparator.class
})
public class PassStatisticsResultsConsolidatorIT {

    private static final int CUTOFF_DAYS = 10;
    private static final LocalDate SOME_DATE = LocalDate.now();
    private static final String SOME_NINO = "AA112233A";

    @Autowired
    public PassStatisticsResultsConsolidator consolidator;

    @Test
    public void consolidateResults_oneNinoWorseResultInRange_returnBetterResult() {
        AuditResult betterResult = new AuditResult("any correlation id", SOME_DATE, SOME_NINO, AuditResultType.PASS);
        AuditResult worseResultWithinRange = new AuditResult("any other correlation id", withinCutoff(SOME_DATE), SOME_NINO, AuditResultType.FAIL);

        AuditResultsGroupedByNino results = new AuditResultsGroupedByNino(betterResult);
        results.add(worseResultWithinRange);
        List<AuditResultsGroupedByNino> someResults = singletonList(results);

        List<AuditResult> consolidatedResult = consolidator.consolidateResults(someResults);
        assertThat(consolidatedResult).containsExactly(betterResult);
    }

    @Test
    public void consolidateResults_oneNinoBetterResultInRange_returnBetterResult() {
        AuditResult worseResult = new AuditResult("any correlation id", SOME_DATE, SOME_NINO, AuditResultType.FAIL);
        AuditResult betterResultInRange = new AuditResult("any other correlation id", withinCutoff(SOME_DATE), SOME_NINO, AuditResultType.PASS);

        AuditResultsGroupedByNino results = new AuditResultsGroupedByNino(worseResult);
        results.add(betterResultInRange);
        List<AuditResultsGroupedByNino> someResults = singletonList(results);

        List<AuditResult> consolidatedResult = consolidator.consolidateResults(someResults);
        assertThat(consolidatedResult).containsExactly(betterResultInRange);
    }

    @Test
    public void consolidateResults_twoNinosResultsAllInCutoff_returnTwoResults() {
        AuditResult worseResult = new AuditResult("any correlation id", SOME_DATE, SOME_NINO, AuditResultType.NOTFOUND);
        AuditResult betterResultInRange = new AuditResult("any correlation id", withinCutoff(SOME_DATE), SOME_NINO, AuditResultType.PASS);

        String someOtherNino = "BB112233A";
        AuditResult betterResult = new AuditResult("any correlation id", SOME_DATE, someOtherNino, AuditResultType.NOTFOUND);
        AuditResult worseResultInRange = new AuditResult("any correlation id", withinCutoff(SOME_DATE), someOtherNino, AuditResultType.ERROR);

        AuditResultsGroupedByNino nino1Results = new AuditResultsGroupedByNino(worseResult);
        nino1Results.add(betterResultInRange);

        AuditResultsGroupedByNino nino2Results = new AuditResultsGroupedByNino(betterResult);
        nino2Results.add(worseResultInRange);
        List<AuditResultsGroupedByNino> someResults = asList(nino1Results, nino2Results);

        List<AuditResult> consolidatedResult = consolidator.consolidateResults(someResults);
        assertThat(consolidatedResult).containsExactlyInAnyOrder(betterResult, betterResultInRange);
    }

    @Test
    public void consolidateResults_oneNinoResultsAfterCutoff_returnTwoResults() {
        LocalDate afterCutoffDate = SOME_DATE.plusDays(CUTOFF_DAYS);

        AuditResult someResult = new AuditResult("any correlation id", SOME_DATE, SOME_NINO, AuditResultType.PASS);
        AuditResult resultAfterCutoffDate = new AuditResult("any correlation id", afterCutoffDate, SOME_NINO, AuditResultType.FAIL);

        AuditResultsGroupedByNino groupedByNino = new AuditResultsGroupedByNino(someResult);
        groupedByNino.add(resultAfterCutoffDate);
        List<AuditResultsGroupedByNino> someResults = singletonList(groupedByNino);

        List<AuditResult> consolidatedResult = consolidator.consolidateResults(someResults);
        assertThat(consolidatedResult).containsExactlyInAnyOrder(someResult, resultAfterCutoffDate);
    }

    @Test
    public void consolidateResults_multipleNinosAndResults_splitWhenAfterCutoff() {
        AuditResultsGroupedByNino shouldBePassAndFail = passAndAFail();
        AuditResultsGroupedByNino shouldBeNotFoundAndError = notFoundAndAnError();
        AuditResultsGroupedByNino shouldBePass = new AuditResultsGroupedByNino(new AuditResult("any correlation id", SOME_DATE, "nino3", AuditResultType.PASS));

        List<AuditResultsGroupedByNino> someResults = asList(shouldBePassAndFail, shouldBeNotFoundAndError, shouldBePass);

        List<AuditResult> expectedResults = asList(shouldBePassAndFail.get(1), shouldBePassAndFail.get(2),
                                                   shouldBeNotFoundAndError.get(0), shouldBeNotFoundAndError.get(1),
                                                   shouldBePass.get(0));


        List<AuditResult> actualResults = consolidator.consolidateResults(someResults);

        assertThat(actualResults).containsExactlyInAnyOrder(expectedResults.toArray(new AuditResult[]{}));
    }

    private AuditResultsGroupedByNino passAndAFail() {
        LocalDate date2 = withinCutoff(SOME_DATE);
        LocalDate date3 = afterCutoff(date2);
        LocalDate date4 = withinCutoff(date3);

        AuditResultsGroupedByNino results = new AuditResultsGroupedByNino(new AuditResult("any correlation id", SOME_DATE, "nino1", AuditResultType.ERROR));
        results.add(new AuditResult("any correlation id", date2, "nino1", AuditResultType.PASS));
        results.add(new AuditResult("any correlation id", date3, "nino1", AuditResultType.FAIL));
        results.add(new AuditResult("any correlation id", date4, "nino1", AuditResultType.NOTFOUND));
        return results;
    }

    private AuditResultsGroupedByNino notFoundAndAnError() {
        LocalDate date2 = afterCutoff(SOME_DATE);
        LocalDate date3 = withinCutoff(date2);

        AuditResultsGroupedByNino results = new AuditResultsGroupedByNino(new AuditResult("any correlation id", SOME_DATE, "nino2", AuditResultType.ERROR));
        results.add(new AuditResult("any correlation id", date2, "nino2", AuditResultType.NOTFOUND));
        results.add(new AuditResult("any correlation id", date3, "nino2", AuditResultType.NOTFOUND));
        return results;
    }

    private LocalDate withinCutoff(LocalDate date) {
        return date.plusDays(CUTOFF_DAYS - 1);
    }

    private LocalDate afterCutoff(LocalDate date) {
        return date.plusDays(CUTOFF_DAYS);
    }
}
