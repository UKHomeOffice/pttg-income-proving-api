package uk.gov.digital.ho.proving.income.validator;

import org.springframework.stereotype.Service;
import uk.gov.digital.ho.proving.income.api.IncomeThresholdCalculator;
import uk.gov.digital.ho.proving.income.hmrc.domain.Income;
import uk.gov.digital.ho.proving.income.validator.domain.IncomeValidationRequest;
import uk.gov.digital.ho.proving.income.validator.domain.IncomeValidationResult;
import uk.gov.digital.ho.proving.income.validator.domain.IncomeValidationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.digital.ho.proving.income.validator.IncomeValidationHelper.*;

@Service
public class CatBSalariedIncomeValidator implements ActiveIncomeValidator {

    private static final int INCOME_PERIOD_START_DATE_YEARS_AGO = 1;
    private static final String CALCULATION_TYPE = "Category B salaried";
    private static final String CATEGORY = "B";

    private final EmploymentCheckIncomeValidator employmentCheckIncomeValidator;

    private final IncomeThresholdCalculator incomeThresholdCalculator;

    public CatBSalariedIncomeValidator(
        EmploymentCheckIncomeValidator employmentCheckIncomeValidator,
        IncomeThresholdCalculator incomeThresholdCalculator
    ) {
        this.employmentCheckIncomeValidator = employmentCheckIncomeValidator;
        this.incomeThresholdCalculator = incomeThresholdCalculator;
    }

    @Override
    public IncomeValidationResult validate(IncomeValidationRequest incomeValidationRequest) {
        IncomeValidationResult employmentCheckValidation = employmentCheckIncomeValidator.validate(incomeValidationRequest);
        if (!employmentCheckValidation.status().isPassed()) {
            return employmentCheckValidation;
        }

        final List<Income> paye = getAllPayeInDateRange(incomeValidationRequest, getApplicationStartDate(incomeValidationRequest));
        if (paye.size() < 12) {
            return validationResult(incomeValidationRequest, IncomeValidationStatus.NOT_ENOUGH_RECORDS);
        }

        List<List<Income>> monthlyIncomes = sortAndGroupIncomesByMonth(paye);

        if (monthMissing(monthlyIncomes)) {
            return validationResult(incomeValidationRequest, IncomeValidationStatus.NON_CONSECUTIVE_MONTHS);
        }

        if (monthBelowThreshold(monthlyIncomes, getMonthlyThreshold(incomeValidationRequest))) {
            return validationResult(incomeValidationRequest, IncomeValidationStatus.CATB_SALARIED_BELOW_THRESHOLD);
        }

        return validationResult(incomeValidationRequest, IncomeValidationStatus.CATB_SALARIED_PASSED);
    }

    private List<List<Income>> sortAndGroupIncomesByMonth(List<Income> incomes) {
        List<List<Income>> monthlyIncomes = new ArrayList<>();
        incomes.stream()
            .collect(Collectors.groupingBy(Income::yearAndMonth))
            .forEach((yearAndMonth, income) -> monthlyIncomes.add(income));

        monthlyIncomes.sort(Comparator.comparingInt(monthlyIncome -> monthlyIncome.get(0).yearAndMonth()));
        return monthlyIncomes;
    }

    private IncomeValidationResult validationResult(IncomeValidationRequest incomeValidationRequest, IncomeValidationStatus validationStatus) {
        return IncomeValidationResult.builder()
            .status(validationStatus)
            .threshold(incomeThresholdCalculator.yearlyThreshold(incomeValidationRequest.dependants()))
            .assessmentStartDate(getApplicationStartDate(incomeValidationRequest))
            .individuals(incomeValidationRequest.getCheckedIndividuals())
            .category(CATEGORY)
            .calculationType(CALCULATION_TYPE)
            .build();
    }

    private LocalDate getApplicationStartDate(IncomeValidationRequest incomeValidationRequest) {
        return incomeValidationRequest.applicationRaisedDate().minusYears(INCOME_PERIOD_START_DATE_YEARS_AGO);
    }

    private boolean monthMissing(List<List<Income>> monthlyIncomes) {
        for (int i = 0; i < monthlyIncomes.size() - 1; i++) {
            if (!isSuccessiveMonths(monthlyIncomes.get(i + 1).get(0), monthlyIncomes.get(i).get(0))) {
                return true;
            }
        }
        return false;
    }

    private boolean monthBelowThreshold(List<List<Income>> monthlyIncomes, BigDecimal monthlyThreshold) {
        for (List<Income> monthlyIncome : monthlyIncomes) {
            BigDecimal totalMonthlyIncome = totalPayment(monthlyIncome);
            if (!checkValuePassesThreshold(totalMonthlyIncome, monthlyThreshold)) {
                return true;
            }
        }
        return false;
    }

    private BigDecimal getMonthlyThreshold(IncomeValidationRequest incomeValidationRequest) {
        return incomeThresholdCalculator.monthlyThreshold(incomeValidationRequest.dependants());
    }
}
