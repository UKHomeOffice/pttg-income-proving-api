package uk.gov.digital.ho.proving.income.validator;

import lombok.extern.slf4j.Slf4j;
import uk.gov.digital.ho.proving.income.hmrc.domain.Employments;
import uk.gov.digital.ho.proving.income.hmrc.domain.Income;
import uk.gov.digital.ho.proving.income.validator.domain.ApplicantIncome;
import uk.gov.digital.ho.proving.income.validator.domain.EmploymentCheck;
import uk.gov.digital.ho.proving.income.validator.domain.IncomeValidationRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class IncomeValidationHelper {

    static boolean isSuccessiveMonths(Income first, Income second) {
        return getDifferenceInMonthsBetweenDates(first.paymentDate(), second.paymentDate()) == 1;
    }

    static long getDifferenceInMonthsBetweenDates(LocalDate date1, LocalDate date2) {

        // Period.toTotalMonths() only returns integer month differences so for 14/07/2015 and 17/06/2015 it returns 0
        // We need it to return 1, so we set both dates to the first of the month

        LocalDate toDate = date1.withDayOfMonth(1);
        LocalDate fromDate = date2.withDayOfMonth(1);

        Period period = fromDate.until(toDate);
        return period.toTotalMonths();

    }

    static List<String> toEmployerNames(List<Employments> employments) {
        return employments.stream()
            .map(employment -> employment.employer().name())
            .distinct()
            .collect(Collectors.toList());
    }

    static EmploymentCheck checkIncomesPassThresholdWithSameEmployer(List<List<Income>> incomes, BigDecimal threshold) {
        String employerPayeReference = incomes.get(0).get(0).employerPayeReference();
        for (List<Income> periodicIncome : incomes) {
            BigDecimal payment = periodicIncome.stream()
                .map(Income::payment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (!checkValuePassesThreshold(payment, threshold)) {
                log.debug("FAILED: Income value = " + payment + " is below threshold: " + threshold);
                return EmploymentCheck.FAILED_THRESHOLD;
            }

            if (!employerPayeReference.equalsIgnoreCase(periodicIncome.get(0).employerPayeReference())) { // TODO OJR 2018/09/07: need to check all entries in period.
                log.debug("FAILED: Different employerPayeReference = " + employerPayeReference + " is not the same as " + periodicIncome.get(0).employerPayeReference());
                return EmploymentCheck.FAILED_EMPLOYER;
            }
        }
        return EmploymentCheck.PASS;
    }

    static List<Income> filterIncomesByDates(List<Income> incomes, LocalDate lower, LocalDate upper) {
        return incomes.stream()
            .sorted((income1, income2) -> income2.paymentDate().compareTo(income1.paymentDate())) // TODO OJR 2018/09/07: If all code paths do their own sorting maybe drop this.
            .filter(income -> isDateInRange(income.paymentDate(), lower, upper))
            .collect(Collectors.toList());
    }

    private static boolean isDateInRange(LocalDate date, LocalDate lower, LocalDate upper) {
        boolean inRange = !(date.isBefore(lower) || date.isAfter(upper));
        log.debug(String.format("%s: %s in range of %s & %s", inRange, date, lower, upper));
        return inRange;
    }

    static boolean checkValuePassesThreshold(BigDecimal value, BigDecimal threshold) {
        return (value.compareTo(threshold) >= 0);
    }

    static List<Income> removeDuplicates(List<Income> incomes) {
        return incomes.stream().distinct().collect(Collectors.toList());
    }

    static List<Income> getAllPayeIncomes(IncomeValidationRequest incomeValidationRequest) {
        return incomeValidationRequest.allIncome()
            .stream()
            .flatMap(applicantIncome -> applicantIncome.incomeRecord().paye().stream())
            .collect(Collectors.toList());
    }

    static List<Income> getAllPayeInDateRange(IncomeValidationRequest incomeValidationRequest, LocalDate applicationStartDate) {
        List<Income> paye = getAllPayeIncomes(incomeValidationRequest);
        LocalDate applicationRaisedDate = incomeValidationRequest.applicationRaisedDate();
        return new ArrayList<>(filterIncomesByDates(paye, applicationStartDate, applicationRaisedDate));
    }

    static List<List<Income>> sortAndGroupIncomesByMonth(List<Income> incomes) {
        return sortAndGroupIncomesByMonth(incomes, false);
    }

    static List<List<Income>> sortAndGroupIncomesByMonth(Stream<Income> incomes) {
        return sortAndGroupIncomesByMonth(incomes, false);
    }

    static List<List<Income>> sortAndGroupIncomesByMonth(List<Income> incomes, boolean reversed) {
        return sortAndGroupIncomesByMonth(incomes.stream(), reversed);
    }

    private static List<List<Income>> sortAndGroupIncomesByMonth(Stream<Income> incomes, boolean reversed) {

        List<List<Income>> monthlyIncomes = new ArrayList<>();
        incomes.collect(Collectors.groupingBy(Income::yearAndMonth))
            .forEach((yearAndMonth, income) -> monthlyIncomes.add(income));

        Comparator<List<Income>> ordering = Comparator.comparingInt(monthlyIncome -> monthlyIncome.get(0).yearAndMonth());
        if (reversed) {
            ordering = ordering.reversed();
        }
        monthlyIncomes.sort(ordering);
        return monthlyIncomes;
    }
}
