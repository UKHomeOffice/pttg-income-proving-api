package uk.gov.digital.ho.proving.income.validator;

import org.junit.Test;
import uk.gov.digital.ho.proving.income.api.SalariedThresholdCalculator;
import uk.gov.digital.ho.proving.income.api.domain.CheckedIndividual;
import uk.gov.digital.ho.proving.income.validator.domain.ApplicantIncome;
import uk.gov.digital.ho.proving.income.validator.domain.IncomeValidationRequest;
import uk.gov.digital.ho.proving.income.validator.domain.IncomeValidationResult;
import uk.gov.digital.ho.proving.income.validator.domain.IncomeValidationStatus;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.digital.ho.proving.income.validator.CatASalariedTestData.getDate;
import static uk.gov.digital.ho.proving.income.validator.EmploymentCheckTestData.*;

public class EmploymentCheckIncomeValidatorTest {

    private EmploymentCheckIncomeValidator validator = new EmploymentCheckIncomeValidator();

    @Test
    public void thatResultDetailsAreReturned() {
        LocalDate raisedDate = getDate(2018, Month.SEPTEMBER, 23);
        List<ApplicantIncome> incomes = noIncome(raisedDate);

        IncomeValidationRequest request = new IncomeValidationRequest(incomes, raisedDate, 0);
        IncomeValidationResult result = validator.validate(request);

        assertThat(result.calculationType()).isEqualTo(EmploymentCheckIncomeValidator.CALCULATION_TYPE)
            .withFailMessage("The correct calculation type should be returned");
        assertThat(result.assessmentStartDate()).isEqualTo(raisedDate.minusDays(EmploymentCheckIncomeValidator.ASSESSMENT_START_DAYS_PREVIOUS))
            .withFailMessage("The assessment start date should be the correct number of days before the raised date");
        assertThat(result.threshold()).isEqualTo(new SalariedThresholdCalculator(0).getMonthlyThreshold())
            .withFailMessage("The monthly threshold should be returned");
    }


    @Test
    public void thatSingleApplicantDetailsAreReturned() {
        LocalDate raisedDate = getDate(2018, Month.SEPTEMBER, 23);
        List<ApplicantIncome> incomes = noIncome(raisedDate);

        IncomeValidationRequest request = new IncomeValidationRequest(incomes, raisedDate, 0);
        IncomeValidationResult result = validator.validate(request);

        assertThat(result.individuals().size()).isEqualTo(1)
            .withFailMessage("A single checked individual should be returned");
        CheckedIndividual individual = result.individuals().get(0);
        assertThat(individual.nino()).isEqualTo(EmploymentCheckTestData.NINO)
            .withFailMessage("The checked individual should have the correct nino");
        assertThat(individual.employers().size()).isEqualTo(1)
            .withFailMessage("The checked individual should have a single employer");
        assertThat(individual.employers().get(0)).isEqualTo(EmploymentCheckTestData.PIZZA_HUT)
            .withFailMessage("The checked individual should have the correct employer");
    }

    @Test
    public void thatTwoApplicantsDetailsAreReturned() {
        LocalDate raisedDate = getDate(2018, Month.SEPTEMBER, 23);
        List<ApplicantIncome> incomes = noIncomeTwoApplicants(raisedDate);

        IncomeValidationRequest request = new IncomeValidationRequest(incomes, raisedDate, 0);
        IncomeValidationResult result = validator.validate(request);

        assertThat(result.individuals().size()).isEqualTo(2)
            .withFailMessage("Two checked individuals should be returned");

        CheckedIndividual applicant = result.individuals().get(0);
        assertThat(applicant.nino()).isEqualTo(EmploymentCheckTestData.NINO)
            .withFailMessage("The checked individual should have the correct nino");
        assertThat(applicant.employers().size()).isEqualTo(1)
            .withFailMessage("The checked individual should have a single employer");
        assertThat(applicant.employers().get(0)).isEqualTo(EmploymentCheckTestData.PIZZA_HUT)
            .withFailMessage("The checked individual should have the correct employer");

        CheckedIndividual partner = result.individuals().get(1);
        assertThat(partner.nino()).isEqualTo(EmploymentCheckTestData.NINO_PARTNER)
            .withFailMessage("The checked individual should have the correct nino");
        assertThat(partner.employers().size()).isEqualTo(1)
            .withFailMessage("The checked individual should have a single employer");
        assertThat(partner.employers().get(0)).isEqualTo(EmploymentCheckTestData.BURGER_KING)
            .withFailMessage("The checked individual should have the correct employer");
    }

    @Test
    public void thatNoIncomeFails() {
        LocalDate raisedDate = getDate(2018, Month.SEPTEMBER, 23);
        List<ApplicantIncome> incomes = noIncome(raisedDate);

        IncomeValidationRequest request = new IncomeValidationRequest(incomes, raisedDate, 0);
        IncomeValidationResult result = validator.validate(request);

        assertThat(result.status()).isEqualTo(IncomeValidationStatus.EMPLOYMENT_CHECK_FAILED);
    }

    @Test
    public void thatIncomeEqualsThresholdPriorToAssessmentStartFails() {
        LocalDate raisedDate = getDate(2018, Month.SEPTEMBER, 23);
        List<ApplicantIncome> incomes = incomePriorToAssessmentStart(raisedDate);

        IncomeValidationRequest request = new IncomeValidationRequest(incomes, raisedDate, 0);
        IncomeValidationResult result = validator.validate(request);

        assertThat(result.status()).isEqualTo(IncomeValidationStatus.EMPLOYMENT_CHECK_FAILED);
    }

    @Test
    public void thatIncomeEqualsThresholdOnAssessmentStartDayPasses() {
        LocalDate raisedDate = getDate(2018, Month.SEPTEMBER, 23);
        List<ApplicantIncome> incomes = singleIncomeOnAssessmentStartDay(raisedDate);

        IncomeValidationRequest request = new IncomeValidationRequest(incomes, raisedDate, 0);
        IncomeValidationResult result = validator.validate(request);

        assertThat(result.status()).isEqualTo(IncomeValidationStatus.EMPLOYMENT_CHECK_PASSED);
    }

    @Test
    public void thatIncomeEqualsThresholdAfterAssessmentStartDayPasses() {
        LocalDate raisedDate = getDate(2018, Month.SEPTEMBER, 23);
        List<ApplicantIncome> incomes = singleIncomeAfterAssessmentStartDay(raisedDate);

        IncomeValidationRequest request = new IncomeValidationRequest(incomes, raisedDate, 0);
        IncomeValidationResult result = validator.validate(request);

        assertThat(result.status()).isEqualTo(IncomeValidationStatus.EMPLOYMENT_CHECK_PASSED);
    }

    @Test
    public void thatIncomeBelowThresholdOnAssessmentStartDayFails() {
        LocalDate raisedDate = getDate(2018, Month.SEPTEMBER, 23);
        List<ApplicantIncome> incomes = singleIncomeBelowThresholdOnAssessmentStartDay(raisedDate);

        IncomeValidationRequest request = new IncomeValidationRequest(incomes, raisedDate, 0);
        IncomeValidationResult result = validator.validate(request);

        assertThat(result.status()).isEqualTo(IncomeValidationStatus.EMPLOYMENT_CHECK_FAILED);
    }

    @Test
    public void thatIncomeEqualsThresholdWithMultiplePaymentsPasses() {
        LocalDate raisedDate = getDate(2018, Month.SEPTEMBER, 23);
        List<ApplicantIncome> incomes = multipleIncomeOnAssessmentStartDay(raisedDate);

        IncomeValidationRequest request = new IncomeValidationRequest(incomes, raisedDate, 0);
        IncomeValidationResult result = validator.validate(request);

        assertThat(result.status()).isEqualTo(IncomeValidationStatus.EMPLOYMENT_CHECK_PASSED);
    }

    @Test
    public void thatIncomeEqualsThresholdWithSomeBeforeAssessmentStartDayFails() {
        LocalDate raisedDate = getDate(2018, Month.SEPTEMBER, 23);
        List<ApplicantIncome> incomes = incomeBeforeAndAfterAssessmentStartDay(raisedDate);

        IncomeValidationRequest request = new IncomeValidationRequest(incomes, raisedDate, 0);
        IncomeValidationResult result = validator.validate(request);

        assertThat(result.status()).isEqualTo(IncomeValidationStatus.EMPLOYMENT_CHECK_FAILED);
    }

    @Test
    public void thatSingleDependantThresholdPaymentPasses() {
        LocalDate raisedDate = getDate(2018, Month.SEPTEMBER, 23);
        List<ApplicantIncome> incomes = singleIncomeWithSingleDependantOnAssessmentStartDate(raisedDate);

        IncomeValidationRequest request = new IncomeValidationRequest(incomes, raisedDate, 0);
        IncomeValidationResult result = validator.validate(request);

        assertThat(result.status()).isEqualTo(IncomeValidationStatus.EMPLOYMENT_CHECK_PASSED);
    }

    @Test
    public void thatTwoDependantsBelowThresholdPaymentFails() {
        LocalDate raisedDate = getDate(2018, Month.SEPTEMBER, 23);
        List<ApplicantIncome> incomes = singleIncomeWithTwoDependantsOnAssessmentStartDate(raisedDate);

        IncomeValidationRequest request = new IncomeValidationRequest(incomes, raisedDate, 0);
        IncomeValidationResult result = validator.validate(request);

        assertThat(result.status()).isEqualTo(IncomeValidationStatus.EMPLOYMENT_CHECK_FAILED);
    }

    @Test
    public void thatPartnerIncomeEqualsThresholdOnAssessmentStartDayPasses() {
        LocalDate raisedDate = getDate(2018, Month.SEPTEMBER, 23);
        List<ApplicantIncome> incomes = singlePartnerIncomeOnAssessmentStartDay(raisedDate);

        IncomeValidationRequest request = new IncomeValidationRequest(incomes, raisedDate, 0);
        IncomeValidationResult result = validator.validate(request);

        assertThat(result.status()).isEqualTo(IncomeValidationStatus.EMPLOYMENT_CHECK_PASSED);
    }

    @Test
    public void thatPartnerIncomeEqualsThresholdBeforeAssessmentStartDayFails() {
        LocalDate raisedDate = getDate(2018, Month.SEPTEMBER, 23);
        List<ApplicantIncome> incomes = singlePartnerIncomeBeforeAssessmentStartDay(raisedDate);

        IncomeValidationRequest request = new IncomeValidationRequest(incomes, raisedDate, 0);
        IncomeValidationResult result = validator.validate(request);

        assertThat(result.status()).isEqualTo(IncomeValidationStatus.EMPLOYMENT_CHECK_FAILED);
    }

    @Test
    public void thatJointIncomeEqualsThresholdOnAssessmentStartDayPasses() {
        LocalDate raisedDate = getDate(2018, Month.SEPTEMBER, 23);
        List<ApplicantIncome> incomes = jointIncomeOnAssessmentStartDay(raisedDate);

        IncomeValidationRequest request = new IncomeValidationRequest(incomes, raisedDate, 0);
        IncomeValidationResult result = validator.validate(request);

        assertThat(result.status()).isEqualTo(IncomeValidationStatus.EMPLOYMENT_CHECK_PASSED);
    }

    @Test
    public void thatJointIncomeEqualsThresholdBeforeAndAfterAssessmentStartDayFails() {
        LocalDate raisedDate = getDate(2018, Month.SEPTEMBER, 23);
        List<ApplicantIncome> incomes = jointIncomeBeforeAndAfterAssessmentStartDay(raisedDate);

        IncomeValidationRequest request = new IncomeValidationRequest(incomes, raisedDate, 0);
        IncomeValidationResult result = validator.validate(request);

        assertThat(result.status()).isEqualTo(IncomeValidationStatus.EMPLOYMENT_CHECK_FAILED);
    }

    @Test
    public void thatJointIncomeEqualsThresholdMultiplePaymentsPasses() {
        LocalDate raisedDate = getDate(2018, Month.SEPTEMBER, 23);
        List<ApplicantIncome> incomes = jointIncomeMultiplePayments(raisedDate);

        IncomeValidationRequest request = new IncomeValidationRequest(incomes, raisedDate, 0);
        IncomeValidationResult result = validator.validate(request);

        assertThat(result.status()).isEqualTo(IncomeValidationStatus.EMPLOYMENT_CHECK_PASSED);
    }

    @Test
    public void thatTwoApplicantsOnlyApplicantPassesReturnsApplicantDetailsOnly() {
        LocalDate raisedDate = getDate(2018, Month.SEPTEMBER, 23);
        List<ApplicantIncome> incomes = jointIncomeApplicantOnlyProvidesIncome(raisedDate);

        IncomeValidationRequest request = new IncomeValidationRequest(incomes, raisedDate, 0);
        IncomeValidationResult result = validator.validate(request);

        assertThat(result.individuals().size()).isEqualTo(1)
            .withFailMessage("Two checked individuals should be returned");

        CheckedIndividual applicant = result.individuals().get(0);
        assertThat(applicant.nino()).isEqualTo(EmploymentCheckTestData.NINO)
            .withFailMessage("The checked individual should have the correct nino");
        assertThat(applicant.employers().size()).isEqualTo(1)
            .withFailMessage("The checked individual should have a single employer");
        assertThat(applicant.employers().get(0)).isEqualTo(EmploymentCheckTestData.PIZZA_HUT)
            .withFailMessage("The checked individual should have the correct employer");
    }

    @Test
    public void thatTwoApplicantsOnlyPartnerPassesReturnsPartnerDetailsOnly() {
        LocalDate raisedDate = getDate(2018, Month.SEPTEMBER, 23);
        List<ApplicantIncome> incomes = singlePartnerIncomeOnAssessmentStartDay(raisedDate);

        IncomeValidationRequest request = new IncomeValidationRequest(incomes, raisedDate, 0);
        IncomeValidationResult result = validator.validate(request);

        assertThat(result.individuals().size()).isEqualTo(1)
            .withFailMessage("A single checked individual should be returned");

        CheckedIndividual partner = result.individuals().get(0);
        assertThat(partner.nino()).isEqualTo(EmploymentCheckTestData.NINO_PARTNER)
            .withFailMessage("The checked individual should have the correct nino");
        assertThat(partner.employers().size()).isEqualTo(1)
            .withFailMessage("The checked individual should have a single employer");
        assertThat(partner.employers().get(0)).isEqualTo(EmploymentCheckTestData.BURGER_KING)
            .withFailMessage("The checked individual should have the correct employer");
    }

    @Test
    public void thatTwoApplicantsJointPassReturnsBothDetails() {
        LocalDate raisedDate = getDate(2018, Month.SEPTEMBER, 23);
        List<ApplicantIncome> incomes = jointIncomeOnAssessmentStartDay(raisedDate);

        IncomeValidationRequest request = new IncomeValidationRequest(incomes, raisedDate, 0);
        IncomeValidationResult result = validator.validate(request);

        assertThat(result.individuals().size()).isEqualTo(2)
            .withFailMessage("A single checked individual should be returned");

        CheckedIndividual applicant = result.individuals().get(0);
        assertThat(applicant.nino()).isEqualTo(EmploymentCheckTestData.NINO)
            .withFailMessage("The checked individual should have the correct nino");
        assertThat(applicant.employers().size()).isEqualTo(1)
            .withFailMessage("The checked individual should have a single employer");
        assertThat(applicant.employers().get(0)).isEqualTo(EmploymentCheckTestData.PIZZA_HUT)
            .withFailMessage("The checked individual should have the correct employer");

        CheckedIndividual partner = result.individuals().get(1);
        assertThat(partner.nino()).isEqualTo(EmploymentCheckTestData.NINO_PARTNER)
            .withFailMessage("The checked individual should have the correct nino");
        assertThat(partner.employers().size()).isEqualTo(1)
            .withFailMessage("The checked individual should have a single employer");
        assertThat(partner.employers().get(0)).isEqualTo(EmploymentCheckTestData.BURGER_KING)
            .withFailMessage("The checked individual should have the correct employer");
    }


}