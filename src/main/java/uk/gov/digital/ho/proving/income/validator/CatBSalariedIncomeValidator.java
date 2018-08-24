package uk.gov.digital.ho.proving.income.validator;

import uk.gov.digital.ho.proving.income.api.IncomeThresholdCalculator;
import uk.gov.digital.ho.proving.income.hmrc.domain.Income;
import uk.gov.digital.ho.proving.income.validator.domain.IncomeValidationRequest;
import uk.gov.digital.ho.proving.income.validator.domain.IncomeValidationResult;
import uk.gov.digital.ho.proving.income.validator.domain.IncomeValidationStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.digital.ho.proving.income.validator.IncomeValidationHelper.isSuccessiveMonths;

public class CatBSalariedIncomeValidator implements ActiveIncomeValidator {

    private static final String CALCULATION_TYPE = "Category B salaried";
    private static final String CATEGORY = "B";
    public static final int INCOME_CHECK_START_DAYS_AGO = 366;

    private final EmploymentCheckIncomeValidator employmentCheckIncomeValidator;

    public CatBSalariedIncomeValidator(EmploymentCheckIncomeValidator employmentCheckIncomeValidator) {
        this.employmentCheckIncomeValidator = employmentCheckIncomeValidator;
    }

    @Override
    public IncomeValidationResult validate(IncomeValidationRequest incomeValidationRequest) {
        IncomeValidationResult employmentCheckValidation = employmentCheckIncomeValidator.validate(incomeValidationRequest);
        if (!employmentCheckValidation.status().isPassed()) {
            return employmentCheckValidation;
        }

        LocalDate incomePeriodStartDate = incomeValidationRequest.applicationRaisedDate().minusDays(INCOME_CHECK_START_DAYS_AGO); // TODO OJR 2018/08/24 Check this cut off correct - probably a test

        IncomeValidationResult applicantResult = validateForApplicant(incomeValidationRequest, incomePeriodStartDate);
        if (applicantResult.status().isPassed()) {
            return applicantResult;
        }
        return validateForPartner(incomeValidationRequest, incomePeriodStartDate);
    }

    private IncomeValidationResult validateForApplicant(IncomeValidationRequest incomeValidationRequest, LocalDate incomePeriodStartDate) {
        return validateForIndividual(incomeValidationRequest, incomePeriodStartDate, incomeValidationRequest.applicantIncome().incomeRecord().paye());
    }

    private IncomeValidationResult validateForPartner(IncomeValidationRequest incomeValidationRequest, LocalDate incomePeriodStartDate) {
        return validateForIndividual(incomeValidationRequest, incomePeriodStartDate, incomeValidationRequest.partnerIncome().incomeRecord().paye());
    }


    private IncomeValidationResult validateForIndividual(IncomeValidationRequest incomeValidationRequest, LocalDate incomePeriodStartDate, List<Income> paye) {

        paye.sort(Comparator.comparingInt(Income::yearAndMonth));
        paye = paye.stream().filter(income -> income.paymentDate().isAfter(incomePeriodStartDate)).collect(Collectors.toList());

        if (paye.size() < 12) {
            return validationResult(incomeValidationRequest, IncomeValidationStatus.NOT_ENOUGH_RECORDS);
        }
        if (monthMissing(paye)) {
            return validationResult(incomeValidationRequest, IncomeValidationStatus.NON_CONSECUTIVE_MONTHS);
        }

        return validationResult(incomeValidationRequest, IncomeValidationStatus.CATB_SALARIED_PASSED);
    }

    private IncomeValidationResult validationResult(IncomeValidationRequest incomeValidationRequest, IncomeValidationStatus validationStatus) {
        return new IncomeValidationResult(
            validationStatus,
            new IncomeThresholdCalculator(incomeValidationRequest.dependants()).getMonthlyThreshold(),
            incomeValidationRequest.getCheckedIndividuals(),
            incomeValidationRequest.applicationRaisedDate(),
            CATEGORY,
            CALCULATION_TYPE
        );
    }

    private boolean monthMissing(List<Income> incomeValidationRequest) {
        for (int i = 0; i < incomeValidationRequest.size() - 1; i++) {
            if (!isSuccessiveMonths(incomeValidationRequest.get(i + 1), incomeValidationRequest.get(i))) {
                return true;
            }
        }
        return false;
    }
}
