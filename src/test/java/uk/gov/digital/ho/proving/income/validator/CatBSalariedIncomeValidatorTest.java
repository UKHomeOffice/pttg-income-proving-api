package uk.gov.digital.ho.proving.income.validator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.digital.ho.proving.income.validator.domain.ApplicantIncome;
import uk.gov.digital.ho.proving.income.validator.domain.IncomeValidationRequest;
import uk.gov.digital.ho.proving.income.validator.domain.IncomeValidationResult;
import uk.gov.digital.ho.proving.income.validator.domain.IncomeValidationStatus;

import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.digital.ho.proving.income.validator.CatBSalariedTestData.twelveMonthsOverThreshold;
import static uk.gov.digital.ho.proving.income.validator.CatBSalariedTestData.twelveMonthsOverThresholdApplicantInJoint;
import static uk.gov.digital.ho.proving.income.validator.CatBSalariedTestData.twelveMonthsOverThresholdPartnerInJoint;
import static uk.gov.digital.ho.proving.income.validator.domain.IncomeValidationStatus.EMPLOYMENT_CHECK_FAILED;
import static uk.gov.digital.ho.proving.income.validator.domain.IncomeValidationStatus.EMPLOYMENT_CHECK_PASSED;

@RunWith(MockitoJUnitRunner.class)
public class CatBSalariedIncomeValidatorTest {

    @Mock private EmploymentCheckIncomeValidator employmentCheckIncomeValidator;

    @InjectMocks private CatBSalariedIncomeValidator catBSalariedIncomeValidator;

    private final LocalDate applicationDate = LocalDate.of(2018, Month.AUGUST, 24);

    public void employmentCheckPasses() {
        IncomeValidationResult incomeValidationResult = mock(IncomeValidationResult.class);
        when(incomeValidationResult.status()).thenReturn(EMPLOYMENT_CHECK_PASSED);

        when(employmentCheckIncomeValidator.validate(any())).thenReturn(incomeValidationResult);
    }

    public void employmentCheckFails() {
        IncomeValidationResult incomeValidationResult = mock(IncomeValidationResult.class);
        when(incomeValidationResult.status()).thenReturn(EMPLOYMENT_CHECK_FAILED);

        when(employmentCheckIncomeValidator.validate(any())).thenReturn(incomeValidationResult);

    }

    @Test
    public void catBSalariedCheckFailsIfEmploymentCheckFails() {
        employmentCheckFails();

        assertStatus(Collections.emptyList(), IncomeValidationStatus.EMPLOYMENT_CHECK_FAILED);
    }

    @Test
    public void checkPassesIf12MonthsOverThresholdApplicantOnly() {
        employmentCheckPasses();

        List<ApplicantIncome> applicantIncomes = twelveMonthsOverThreshold(applicationDate);
        assertStatus(applicantIncomes, IncomeValidationStatus.CATB_SALARIED_PASSED);
    }

    @Test
    public void checkPassesIf12MonthsOverThresholdForApplicantJointApplication() {
        employmentCheckPasses();

        List<ApplicantIncome> applicantIncomes = twelveMonthsOverThresholdApplicantInJoint(applicationDate);
        assertStatus(applicantIncomes, IncomeValidationStatus.CATB_SALARIED_PASSED);
    }

    @Test
    public void checkPassesIf12MonthsOverThresholdForPartnerJointApplication() {
        employmentCheckPasses();

        List<ApplicantIncome> applicantIncomes = twelveMonthsOverThresholdPartnerInJoint(applicationDate);
        assertStatus(applicantIncomes, IncomeValidationStatus.CATB_SALARIED_PASSED);
    }

    @Test
    public void checkPassesIf12MonthsOverThresholdCombinedForJointApplication() {}

    @Test
    public void checkFailsIfMonthMissing() {}

    @Test
    public void checkFailsIfMonthBelowThresholdMissing() {}

    @Test
    public void checkPassesMixedFrequencyButOverThreshold() {}

    @Test
    public void checkFailsMixedFrequencyButOneMonthMissing() {}

    @Test
    public void checkFailsMixedFrequencyButOneMonthUnderThreshold() {}

    @Test
    public void checkPassesIf12MonthsOverThresholdMultipleEmployersSolo() {}

    @Test
    public void checkPassesIf12MonthsOverThresholdMultipleEmployersForApplicantInJoint() {}

    @Test
    public void checkPassesIf12MonthsOverThresholdMultipleEmployersForPartnerInJoint() {}

    @Test
    public void checkPassesIf12MonthsOverThresholdMultipleEmployersForApplicantAndPartnerInJoint() {}

    @Test
    public void checkFailsIfMonthUnderThresholdMultipleEmployersSolo() {}

    @Test
    public void checkFailsIfMonthUnderThresholdMultipleEmployersForApplicantInJoint() {}

    @Test
    public void checkFailsIfMonthUnderThresholdMultipleEmployersForPartnerInJoint() {}

    @Test
    public void checkFailsIfMonthUnderThresholdMultipleEmployersForApplicantAndPartnerInJoint() {}

    @Test
    public void checkFailsIfMonthMissingMultipleEmployersSolo() {}

    @Test
    public void checkFailsIfMonthMissingMultipleEmployersForApplicantInJoint() {}

    @Test
    public void checkFailsIfMonthMissingMultipleEmployersForPartnerInJoint() {}

    @Test
    public void checkFailsIfMonthMissingMultipleEmployersForApplicantAndPartnerInJoint() {}

    @Test
    public void checkPassesFor1DependantIfOverThresholdSoloApplication() {}

    @Test
    public void checkPassesFor2DependantIfOverThresholdSoloApplication() {}

    @Test
    public void checkPassesFor3DependantIfOverThresholdSoloApplication() {}

    @Test
    public void checkPassesFor4DependantIfOverThresholdSoloApplication() {}

    @Test
    public void checkPassesFor5DependantIfOverThresholdSoloApplication() {}

    @Test
    public void checkPassesFor1DependantIfOverThresholdJointApplication() {}

    @Test
    public void checkPassesFor2DependantIfOverThresholdJointApplication() {}

    @Test
    public void checkPassesFor3DependantIfOverThresholdJointApplication() {}

    @Test
    public void checkPassesFor4DependantIfOverThresholdJointApplication() {}

    @Test
    public void checkPassesFor5DependantIfOverThresholdJointApplication() {}

    // TODO OJR: Check day calculation is correct (minus 366 days), check that works for unsorted income data

    private void assertStatus(List<ApplicantIncome> applicantIncomes, IncomeValidationStatus status) {
        assertStatus(applicantIncomes, status, 0);
    }

    private void assertStatus(List<ApplicantIncome> applicantIncomes, IncomeValidationStatus status, int dependants) {
        IncomeValidationRequest request = new IncomeValidationRequest(applicantIncomes, applicationDate, 0);

        IncomeValidationResult result = catBSalariedIncomeValidator.validate(request);

        assertThat(result.status()).isEqualTo(status);
    }
}
