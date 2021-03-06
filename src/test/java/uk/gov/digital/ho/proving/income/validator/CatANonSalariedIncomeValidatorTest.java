package uk.gov.digital.ho.proving.income.validator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.digital.ho.proving.income.api.IncomeThresholdCalculator;
import uk.gov.digital.ho.proving.income.api.domain.Applicant;
import uk.gov.digital.ho.proving.income.hmrc.domain.AnnualSelfAssessmentTaxReturn;
import uk.gov.digital.ho.proving.income.hmrc.domain.HmrcIndividual;
import uk.gov.digital.ho.proving.income.hmrc.domain.Income;
import uk.gov.digital.ho.proving.income.hmrc.domain.IncomeRecord;
import uk.gov.digital.ho.proving.income.validator.domain.ApplicantIncome;
import uk.gov.digital.ho.proving.income.validator.domain.IncomeValidationRequest;
import uk.gov.digital.ho.proving.income.validator.domain.IncomeValidationResult;
import uk.gov.digital.ho.proving.income.validator.domain.IncomeValidationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static uk.gov.digital.ho.proving.income.validator.CatASalariedTestData.amount;
import static uk.gov.digital.ho.proving.income.validator.domain.IncomeValidationStatus.*;

@RunWith(MockitoJUnitRunner.class)
public class CatANonSalariedIncomeValidatorTest {

    private static final LocalDate APPLICATION_RAISED_DATE = LocalDate.now();
    private static final LocalDate ANY_DOB = APPLICATION_RAISED_DATE.minusYears(20);
    private static final Applicant ANY_APPLICANT = new Applicant("any forename", "any surname", ANY_DOB, "any nino");
    private static final Applicant ANY_PARTNER = new Applicant("any other forename", "any other surname", ANY_DOB, "any other nino");;
    private static final HmrcIndividual ANY_HMRC_INDIVIDUAL = new HmrcIndividual("any forename", "any surname", "any nino", ANY_DOB);
    private static final HmrcIndividual ANY_HMRC_INDIVIDUAL_PARTNER = new HmrcIndividual("any other forename", "any other surname", "any other nino", ANY_DOB);
    private static final List<ApplicantIncome> ANY_APPLICANT_INCOME = singletonList(new ApplicantIncome(ANY_APPLICANT, new IncomeRecord(emptyList(), emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL)));

    @Mock
    private IncomeThresholdCalculator incomeThresholdCalculator;

    private CatANonSalariedIncomeValidator validator;

    @Before
    public void setUp() {
        validator = new CatANonSalariedIncomeValidator(incomeThresholdCalculator);
        when(incomeThresholdCalculator.yearlyThreshold(0)).thenReturn(BigDecimal.valueOf(18600));
    }

    @Test
    public void shouldReturnNotEnoughRecordsWhenNoIncomeRecords() {
        IncomeValidationResult result = validator.validate(new IncomeValidationRequest(ANY_APPLICANT_INCOME, APPLICATION_RAISED_DATE, 0));

        assertThat(result.status()).isEqualTo(NOT_ENOUGH_RECORDS);
    }

    @Test
    public void shouldReturnNotEnoughRecordsWhenNoIncomeRecordsJoint() {
        List<ApplicantIncome> noJointIncome = asList(
            new ApplicantIncome(ANY_APPLICANT, new IncomeRecord(emptyList(), emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL)),
            new ApplicantIncome(ANY_PARTNER, new IncomeRecord(emptyList(), emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL_PARTNER))
        );
        IncomeValidationResult result = validator.validate(new IncomeValidationRequest(noJointIncome, APPLICATION_RAISED_DATE, 0));

        assertThat(result.status()).isEqualTo(NOT_ENOUGH_RECORDS);
    }

    @Test
    public void checkedIndividualShouldHaveSameNinoAsInRequest() {
        String givenNino = "a given nino";
        Applicant applicant = new Applicant("any forename", "any surname", ANY_DOB, givenNino);
        HmrcIndividual hmrcIndividual = new HmrcIndividual("any forename", "any surname", givenNino, ANY_DOB);

        List<ApplicantIncome> incomes = singletonList(new ApplicantIncome(applicant, new IncomeRecord(emptyList(), emptyList(), emptyList(), hmrcIndividual)));

        IncomeValidationResult result = validator.validate(new IncomeValidationRequest(incomes, APPLICATION_RAISED_DATE, 0));
        assertThat(result.individuals()).hasSize(1);
        assertThat(result.individuals().get(0).nino()).isEqualTo(givenNino);
    }

    @Test
    public void resultShouldBeCategoryA() {
        IncomeValidationResult result = validator.validate(new IncomeValidationRequest(ANY_APPLICANT_INCOME, APPLICATION_RAISED_DATE, 0));

        assertThat(result.category()).isEqualTo("A");
    }

    @Test
    public void calculationTypeShouldBeCategoryANonSalaried() {
        String expectedCalculationType = "Category A Non Salaried";

        IncomeValidationResult result = validator.validate(new IncomeValidationRequest(ANY_APPLICANT_INCOME, APPLICATION_RAISED_DATE, 0));

        assertThat(result.calculationType()).isEqualTo(expectedCalculationType);
    }

    @Test
    public void assessmentStartDateShouldBe6MonthsBeforeApplicationDate() {
        LocalDate applicationDate = LocalDate.of(2018, Month.AUGUST, 23);
        LocalDate expectedAssessmentStartDate = LocalDate.of(2018, Month.FEBRUARY, 23);

        IncomeValidationResult result = validator.validate(new IncomeValidationRequest(ANY_APPLICANT_INCOME, applicationDate, 0));

        assertThat(result.assessmentStartDate()).isEqualTo(expectedAssessmentStartDate);
    }

    @Test
    public void annualThresholdForDependantsShouldBeCorrect() {
        Map<Integer, Integer> dependantsAndExpectedThreshold = new HashMap<>();
        dependantsAndExpectedThreshold.put(0, 18_600);
        dependantsAndExpectedThreshold.put(1, 22_400);
        dependantsAndExpectedThreshold.put(2, 24_800);
        dependantsAndExpectedThreshold.put(3, 27_200);
        dependantsAndExpectedThreshold.put(4, 29_600);
        dependantsAndExpectedThreshold.put(5, 32_000);

        for (int dependants = 0; dependants <= 5; dependants++) {
            expectDependants(dependants);
            IncomeValidationResult result = validator.validate(new IncomeValidationRequest(ANY_APPLICANT_INCOME, APPLICATION_RAISED_DATE, dependants));

            Integer expectedThreshold = dependantsAndExpectedThreshold.get(dependants);
            assertThat(result.threshold()).isEqualTo(BigDecimal.valueOf(expectedThreshold));
            reset(incomeThresholdCalculator);
        }
    }

    @Test
    public void shouldPassWhenOverThresholdSingleMonth() {
        List<Income> incomes = singletonList(new Income(BigDecimal.valueOf(18_600 / 2), APPLICATION_RAISED_DATE.minusDays(1), null, null, "any employer ref"));

        IncomeRecord incomeRecord = new IncomeRecord(incomes, emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL);
        List<ApplicantIncome> applicantIncomes = singletonList(new ApplicantIncome(ANY_APPLICANT, incomeRecord));

        assertExpectedResult(new IncomeValidationRequest(applicantIncomes, APPLICATION_RAISED_DATE, 0), CATA_NON_SALARIED_PASSED);
    }

    @Test
    public void shouldFailWhenBelowThresholdSingleMonth() {
        List<Income> incomes = singletonList(new Income(BigDecimal.valueOf(18_600 / 2).subtract(BigDecimal.ONE), APPLICATION_RAISED_DATE.minusDays(1), null, null, "any employer ref"));

        IncomeRecord incomeRecord = new IncomeRecord(incomes, emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL);
        List<ApplicantIncome> applicantIncomes = singletonList(new ApplicantIncome(ANY_APPLICANT, incomeRecord));

        assertExpectedResult(new IncomeValidationRequest(applicantIncomes, APPLICATION_RAISED_DATE, 0), CATA_NON_SALARIED_BELOW_THRESHOLD);
    }

    @Test
    public void shouldFailWhenOnlyIncomeNotPaye() {
        List<AnnualSelfAssessmentTaxReturn> selfAssessmentIncome = singletonList(new AnnualSelfAssessmentTaxReturn(String.valueOf(APPLICATION_RAISED_DATE.getYear()), BigDecimal.valueOf(33_000)));

        IncomeRecord incomeRecord = new IncomeRecord(emptyList(), selfAssessmentIncome, emptyList(), ANY_HMRC_INDIVIDUAL);
        List<ApplicantIncome> applicantIncomes = singletonList(new ApplicantIncome(ANY_APPLICANT, incomeRecord));

        assertExpectedResult(new IncomeValidationRequest(applicantIncomes, APPLICATION_RAISED_DATE, 0), NOT_ENOUGH_RECORDS);
    }

    @Test
    public void shouldPassWhenOverThreshold2MonthsSummed() {
        List<Income> incomes = asList(
            new Income(BigDecimal.valueOf(18_600 / 4), APPLICATION_RAISED_DATE.minusDays(1), null, null, "any employer ref"),
            new Income(BigDecimal.valueOf(18_600 / 4), APPLICATION_RAISED_DATE.minusMonths(5), null, null, "any employer ref")
        );

        IncomeRecord incomeRecord = new IncomeRecord(incomes, emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL);
        List<ApplicantIncome> applicantIncomes = singletonList(new ApplicantIncome(ANY_APPLICANT, incomeRecord));

        assertExpectedResult(new IncomeValidationRequest(applicantIncomes, APPLICATION_RAISED_DATE, 0), CATA_NON_SALARIED_PASSED);
    }

    @Test
    public void shouldFailWhenOverThresholdMonthOutOfRange() {
        List<Income> incomes = singletonList(new Income(BigDecimal.valueOf(18_600 / 2), APPLICATION_RAISED_DATE.minusMonths(7), null, null, "any employer ref"));

        IncomeRecord incomeRecord = new IncomeRecord(incomes, emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL);
        List<ApplicantIncome> applicantIncomes = singletonList(new ApplicantIncome(ANY_APPLICANT, incomeRecord));

        assertExpectedResult(new IncomeValidationRequest(applicantIncomes, APPLICATION_RAISED_DATE, 0), NOT_ENOUGH_RECORDS);
    }

    @Test
    public void shouldFailWhenOverThreshold2MonthsSummed1MonthOutOfRange() {
        List<Income> incomes = asList(
            new Income(BigDecimal.valueOf(18_600 / 4), APPLICATION_RAISED_DATE.minusDays(1), null, null, "any employer ref"),
            new Income(BigDecimal.valueOf(18_600 / 4), APPLICATION_RAISED_DATE.minusMonths(7), null, null, "any employer ref")
        );

        IncomeRecord incomeRecord = new IncomeRecord(incomes, emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL);
        List<ApplicantIncome> applicantIncomes = singletonList(new ApplicantIncome(ANY_APPLICANT, incomeRecord));

        assertExpectedResult(new IncomeValidationRequest(applicantIncomes, APPLICATION_RAISED_DATE, 0), CATA_NON_SALARIED_BELOW_THRESHOLD);
    }

    @Test
    public void shouldFailWhenPaymentAfterRaisedDate() {
        List<Income> incomes = singletonList(new Income(BigDecimal.valueOf(18_600 / 2), APPLICATION_RAISED_DATE.plusDays(1), null, null, "any employer ref"));

        IncomeRecord incomeRecord = new IncomeRecord(incomes, emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL);
        List<ApplicantIncome> applicantIncomes = singletonList(new ApplicantIncome(ANY_APPLICANT, incomeRecord));

        assertExpectedResult(new IncomeValidationRequest(applicantIncomes, APPLICATION_RAISED_DATE, 0), NOT_ENOUGH_RECORDS);
    }

    @Test
    public void shouldPassWhenOverThresholdVariableAmounts() {
        List<Income> incomes = asList(
            new Income(amount("18599.99"), APPLICATION_RAISED_DATE.minusDays(1), null, null, "any employer ref"),
            new Income(amount("0.01"), APPLICATION_RAISED_DATE.minusDays(2), null, null, "any employer ref")
        );

        IncomeRecord incomeRecord = new IncomeRecord(incomes, emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL);
        List<ApplicantIncome> applicantIncomes = singletonList(new ApplicantIncome(ANY_APPLICANT, incomeRecord));

        assertExpectedResult(new IncomeValidationRequest(applicantIncomes, APPLICATION_RAISED_DATE, 0), CATA_NON_SALARIED_PASSED);
    }

    @Test
    public void shouldFilterOutDuplicateIncomeEntries() {
        Income income = new Income(BigDecimal.valueOf(18_600 / 4), APPLICATION_RAISED_DATE.minusDays(1), null, null, "any employer ref");
        List<Income> incomes = asList(income, income);

        IncomeRecord incomeRecord = new IncomeRecord(incomes, emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL);
        List<ApplicantIncome> applicantIncomes = singletonList(new ApplicantIncome(ANY_APPLICANT, incomeRecord));

        assertExpectedResult(new IncomeValidationRequest(applicantIncomes, APPLICATION_RAISED_DATE, 0), CATA_NON_SALARIED_BELOW_THRESHOLD);
    }

    @Test
    public void shouldFailWhenMultipleEmployers() {
        List<Income> incomes = asList(
            new Income(amount("9299.99"), APPLICATION_RAISED_DATE.minusDays(1), null, null, "any employer ref"),
            new Income(amount("0.01"), APPLICATION_RAISED_DATE.minusDays(2), null, null, "any other employer ref")
        );

        IncomeRecord incomeRecord = new IncomeRecord(incomes, emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL);
        List<ApplicantIncome> applicantIncomes = singletonList(new ApplicantIncome(ANY_APPLICANT, incomeRecord));

        assertExpectedResult(new IncomeValidationRequest(applicantIncomes, APPLICATION_RAISED_DATE, 0), MULTIPLE_EMPLOYERS);

    }

    @Test
    public void shouldPassWhenPartnerIncomeOnlyOverThreshold() {
        List<Income> incomes = singletonList(new Income(BigDecimal.valueOf(18_600 / 2), APPLICATION_RAISED_DATE.minusDays(1), null, null, "any employer ref"));

        IncomeRecord applicantIncome = new IncomeRecord(emptyList(), emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL);
        IncomeRecord partnerIncome = new IncomeRecord(incomes, emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL);
        List<ApplicantIncome> applicantIncomes = asList(
            new ApplicantIncome(ANY_APPLICANT, applicantIncome),
            new ApplicantIncome(ANY_PARTNER, partnerIncome)
        );

        assertExpectedResult(new IncomeValidationRequest(applicantIncomes, APPLICATION_RAISED_DATE, 0), CATA_NON_SALARIED_PASSED);
    }

    @Test
    public void shouldPassWhenCombinedIncomeOnlyOverThreshold() {
        List<Income> applicantIncome = singletonList(new Income(BigDecimal.valueOf(18_600 / 4), APPLICATION_RAISED_DATE.minusDays(1), null, null, "any employer ref"));
        List<Income> partnerIncome = singletonList(new Income(BigDecimal.valueOf(18_600 / 4), APPLICATION_RAISED_DATE.minusDays(2), null, null, "any other employer ref"));

        IncomeRecord applicantIncomeRecord = new IncomeRecord(applicantIncome, emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL);
        IncomeRecord partnerIncomeRecord = new IncomeRecord(partnerIncome, emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL);
        List<ApplicantIncome> applicantIncomes = asList(
            new ApplicantIncome(ANY_APPLICANT, applicantIncomeRecord),
            new ApplicantIncome(ANY_PARTNER, partnerIncomeRecord)
        );

        assertExpectedResult(new IncomeValidationRequest(applicantIncomes, APPLICATION_RAISED_DATE, 0), CATA_NON_SALARIED_PASSED);
    }

    @Test
    public void checkedIndividualShouldBePartnerWhenPartnerPassOnly() {
        List<Income> incomes = singletonList(new Income(BigDecimal.valueOf(18_600 / 2), APPLICATION_RAISED_DATE.minusDays(1), null, null, "any employer ref"));

        IncomeRecord applicantIncome = new IncomeRecord(emptyList(), emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL);
        IncomeRecord partnerIncome = new IncomeRecord(incomes, emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL_PARTNER);
        List<ApplicantIncome> applicantIncomes = asList(
            new ApplicantIncome(ANY_APPLICANT, applicantIncome),
            new ApplicantIncome(ANY_PARTNER, partnerIncome)
        );

        IncomeValidationResult result = validator.validate(new IncomeValidationRequest(applicantIncomes, APPLICATION_RAISED_DATE, 0));
        assertThat(result.individuals()).hasSize(1);
        assertThat(result.individuals().get(0).nino().equals(ANY_HMRC_INDIVIDUAL_PARTNER.nino()));
    }

    @Test
    public void checkedIndividualShouldBeBothWhenCombinedPass() {
        List<Income> applicantIncome = singletonList(new Income(BigDecimal.valueOf(18_600 / 4), APPLICATION_RAISED_DATE.minusDays(1), null, null, "any employer ref"));
        List<Income> partnerIncome = singletonList(new Income(BigDecimal.valueOf(18_600 / 4), APPLICATION_RAISED_DATE.minusDays(2), null, null, "any other employer ref"));

        IncomeRecord applicantIncomeRecord = new IncomeRecord(applicantIncome, emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL);
        IncomeRecord partnerIncomeRecord = new IncomeRecord(partnerIncome, emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL_PARTNER);
        List<ApplicantIncome> applicantIncomes = asList(
            new ApplicantIncome(ANY_APPLICANT, applicantIncomeRecord),
            new ApplicantIncome(ANY_PARTNER, partnerIncomeRecord)
        );
        assertThat(ANY_HMRC_INDIVIDUAL.nino()).isNotEqualTo(ANY_HMRC_INDIVIDUAL_PARTNER.nino());


        IncomeValidationResult result = validator.validate(new IncomeValidationRequest(applicantIncomes, APPLICATION_RAISED_DATE, 0));
        assertThat(result.individuals()).hasSize(2);
        assertThat(result.individuals().get(0).nino().equals(ANY_HMRC_INDIVIDUAL.nino()));
        assertThat(result.individuals().get(0).nino().equals(ANY_HMRC_INDIVIDUAL_PARTNER.nino()));
    }

    @Test
    public void shouldPassWhenMultipleEmployersButSingleEmployerOverThreshold() {
        List<Income> incomes = asList(
            new Income(BigDecimal.valueOf(18_600 / 2), APPLICATION_RAISED_DATE.minusDays(1), null, null, "an employer ref"),
            new Income(BigDecimal.ONE, APPLICATION_RAISED_DATE.minusDays(2), null, null, "a different employer ref")
        );

        List<ApplicantIncome> applicantIncomes = singletonList(new ApplicantIncome(ANY_APPLICANT, new IncomeRecord(incomes, emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL)));

        IncomeValidationResult result = validator.validate(new IncomeValidationRequest(applicantIncomes, APPLICATION_RAISED_DATE, 0));

        assertThat(result.status()).isEqualTo(CATA_NON_SALARIED_PASSED);
    }

    @Test
    public void shouldFilterDuplicatesJointApplication() {
        List<Income> applicantIncome = asList(
            new Income(BigDecimal.valueOf(18_600 / 8), APPLICATION_RAISED_DATE.minusDays(1), 1, null, "an employer ref"),
            new Income(BigDecimal.valueOf(18_600 / 8), APPLICATION_RAISED_DATE.minusDays(1), 1, null, "an employer ref")
        );
        List<Income> partnerIncome = asList(
            new Income(BigDecimal.valueOf(18_600 / 8), APPLICATION_RAISED_DATE.minusDays(1), 1, null, "another employer ref"),
            new Income(BigDecimal.valueOf(18_600 / 8), APPLICATION_RAISED_DATE.minusDays(1), 1, null, "another employer ref")
        );

        List<ApplicantIncome> applicantIncomes = asList(
            new ApplicantIncome(ANY_APPLICANT, new IncomeRecord(applicantIncome, emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL)),
            new ApplicantIncome(ANY_PARTNER, new IncomeRecord(partnerIncome, emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL_PARTNER))
        );

        IncomeValidationResult result = validator.validate(new IncomeValidationRequest(applicantIncomes, APPLICATION_RAISED_DATE, 0));

        assertThat(result.status()).isEqualTo(CATA_NON_SALARIED_BELOW_THRESHOLD);
    }

    @Test
    public void shouldFilterOutOfRangePaymentsJointApplication() {
        List<Income> applicantIncome = asList(
            new Income(BigDecimal.valueOf(18_600 / 8), APPLICATION_RAISED_DATE.minusMonths(6).minusDays(1), 8, null, "an employer ref"),
            new Income(BigDecimal.valueOf(18_600 / 8), APPLICATION_RAISED_DATE.minusDays(1), 1, null, "an employer ref")
        );
        List<Income> partnerIncome = asList(
            new Income(BigDecimal.valueOf(18_600 / 8), APPLICATION_RAISED_DATE.plusDays(1), 1, null, "another employer ref"),
            new Income(BigDecimal.valueOf(18_600 / 8), APPLICATION_RAISED_DATE.minusDays(1), 1, null, "another employer ref")
        );

        List<ApplicantIncome> applicantIncomes = asList(
            new ApplicantIncome(ANY_APPLICANT, new IncomeRecord(applicantIncome, emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL)),
            new ApplicantIncome(ANY_PARTNER, new IncomeRecord(partnerIncome, emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL_PARTNER))
        );

        IncomeValidationResult result = validator.validate(new IncomeValidationRequest(applicantIncomes, APPLICATION_RAISED_DATE, 0));

        assertThat(result.status()).isEqualTo(CATA_NON_SALARIED_BELOW_THRESHOLD);
    }

    @Test
    public void shouldReturnMultipleEmployersForJointApplicationWhenOverThresholdOnlyForMultipleEmployers() {
        List<Income> applicantIncome = asList(
            new Income(BigDecimal.valueOf(18_600 / 8), APPLICATION_RAISED_DATE.minusDays(1), 8, null, "an employer ref"),
            new Income(BigDecimal.valueOf(18_600 / 8), APPLICATION_RAISED_DATE.minusDays(1), 1, null, "another employer ref")
        );
        List<Income> partnerIncome = asList(
            new Income(BigDecimal.valueOf(18_600 / 8), APPLICATION_RAISED_DATE.minusDays(1), 1, null, "yet another employer ref"),
            new Income(BigDecimal.valueOf(18_600 / 8), APPLICATION_RAISED_DATE.minusDays(1), 1, null, "and yet another employer ref")
        );

        List<ApplicantIncome> applicantIncomes = asList(
            new ApplicantIncome(ANY_APPLICANT, new IncomeRecord(applicantIncome, emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL)),
            new ApplicantIncome(ANY_PARTNER, new IncomeRecord(partnerIncome, emptyList(), emptyList(), ANY_HMRC_INDIVIDUAL_PARTNER))
        );

        IncomeValidationResult result = validator.validate(new IncomeValidationRequest(applicantIncomes, APPLICATION_RAISED_DATE, 0));

        assertThat(result.status()).isEqualTo(MULTIPLE_EMPLOYERS);
    }

    private void assertExpectedResult(IncomeValidationRequest request, IncomeValidationStatus expectedStatus) {
        IncomeValidationResult result = validator.validate(request);
        assertThat(result.status()).isEqualTo(expectedStatus);
    }

    private void expectDependants(int dependants) {
        BigDecimal yearlyThreshold = BigDecimal.valueOf(18600);
        if (dependants == 1) {
            yearlyThreshold = BigDecimal.valueOf(22400);
        }
        else if(dependants > 1) {
            yearlyThreshold = BigDecimal.valueOf(22400 + (dependants - 1) * 2400);
        }
        when(incomeThresholdCalculator.yearlyThreshold(dependants)).thenReturn(yearlyThreshold);
    }
}
