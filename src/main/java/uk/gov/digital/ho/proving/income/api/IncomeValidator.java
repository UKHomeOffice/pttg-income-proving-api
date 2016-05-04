package uk.gov.digital.ho.proving.income.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.digital.ho.proving.income.domain.Income;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IncomeValidator {

    private static final int NUMBER_OF_MONTHS = 6;
    private static final int NUMBER_OF_WEEKS = 26;

    private static final Logger LOGGER = LoggerFactory.getLogger(IncomeValidator.class);

    private IncomeValidator() {
    }

    public static FinancialCheckValues validateCategoryAMonthlySalaried(List<Income> incomes, Date lower, Date upper, int dependants) {
        SalariedThresholdCalculator thresholdCalculator = new SalariedThresholdCalculator(dependants);
        BigDecimal monthlyThreshold = thresholdCalculator.getMonthlyThreshold();
        return financialCheckForLastXMonths(incomes, NUMBER_OF_MONTHS, monthlyThreshold, lower, upper);
    }

    public static FinancialCheckValues validateCategoryAWeeklySalaried(List<Income> incomes, Date lower, Date upper, int dependants) {
        SalariedThresholdCalculator thresholdCalculator = new SalariedThresholdCalculator(dependants);
        BigDecimal weeklyThreshold = thresholdCalculator.getWeeklyThreshold();
        return financialCheckForWeeklySalaried(incomes, NUMBER_OF_WEEKS, weeklyThreshold, lower, upper);
    }

    //TODO Refactor date handling once we know more about the back end and test env
    private static FinancialCheckValues financialCheckForLastXMonths(List<Income> incomes, int numOfMonths, BigDecimal threshold, Date lower, Date upper) {
        Stream<Income> individualIncome = filterIncomesByDates(incomes, lower, upper);
        List<Income> lastXMonths = individualIncome.limit(numOfMonths).collect(Collectors.toList());
        if (lastXMonths.size() >= numOfMonths) {

            // Do we have NUMBER_OF_MONTHS consecutive months with the same employer
            for (int i = 0; i < numOfMonths - 1; i++) {
                if (!isSuccessiveMonths(lastXMonths.get(i), lastXMonths.get(i + 1))) {
                    LOGGER.debug("FAILED: Months not consecutive");
                    return FinancialCheckValues.NON_CONSECUTIVE_MONTHS;
                }
            }

            EmploymentCheck employmentCheck =checkIncomesPassThresholdWithSameEmployer(lastXMonths, threshold);
            if (employmentCheck.equals(EmploymentCheck.PASS)) {
                return FinancialCheckValues.MONTHLY_SALARIED_PASSED;
            } else {
                return employmentCheck.equals(EmploymentCheck.FAILED_THRESHOLD) ? FinancialCheckValues.MONTHLY_VALUE_BELOW_THRESHOLD : FinancialCheckValues.NON_CONSECUTIVE_MONTHS;
            }

        } else {
            return FinancialCheckValues.NOT_ENOUGH_RECORDS;
        }
    }


    private static FinancialCheckValues financialCheckForWeeklySalaried(List<Income> incomes, int numOfWeeks, BigDecimal threshold, Date lower, Date upper) {
        Stream<Income> individualIncome = filterIncomesByDates(incomes, lower, upper);
        List<Income> lastXWeeks = individualIncome.collect(Collectors.toList());

        if (lastXWeeks.size() >= numOfWeeks) {
            EmploymentCheck employmentCheck = checkIncomesPassThresholdWithSameEmployer(lastXWeeks, threshold);
            if (employmentCheck.equals(EmploymentCheck.PASS)) {
                return FinancialCheckValues.WEEKLY_SALARIED_PASSED;
            } else {
                return employmentCheck.equals(EmploymentCheck.FAILED_THRESHOLD) ? FinancialCheckValues.WEEKLY_VALUE_BELOW_THRESHOLD : FinancialCheckValues.NON_CONSECUTIVE_MONTHS;
            }
        } else {
            return FinancialCheckValues.NOT_ENOUGH_RECORDS;
        }

    }

    private static EmploymentCheck checkIncomesPassThresholdWithSameEmployer(List<Income> incomes, BigDecimal threshold) {
        String employer = incomes.get(0).getEmployer();
        for (Income income : incomes) {
            if (!checkValuePassesThreshold(new BigDecimal(income.getIncome()), threshold)) {
                LOGGER.debug("FAILED: Income value = " + new BigDecimal(income.getIncome()) + " is below threshold: " + threshold);
                return EmploymentCheck.FAILED_THRESHOLD;
            }

            if (!employer.equalsIgnoreCase(income.getEmployer())) {
                LOGGER.debug("FAILED: Different employers = " + employer + " is not the same as " + income.getEmployer());
                return EmploymentCheck.FAILED_EMPLOYER;
            }
        }
        return EmploymentCheck.PASS;
    }

    private static boolean checkValuePassesThreshold(BigDecimal value, BigDecimal threshold) {
        return (value.compareTo(threshold) >= 0);
    }


    private static Stream<Income> filterIncomesByDates(List<Income> incomes, Date lower, Date upper) {
        return incomes.stream()
                .sorted((income1, income2) -> income2.getPayDate().compareTo(income1.getPayDate()))
                .filter(income -> isDateInRange(income.getPayDate(), lower, upper));
    }

    private static boolean isSuccessiveMonths(Income first, Income second) {
//        if (!first.getEmployer().toLowerCase().trim().equals(second.getEmployer().toLowerCase().trim())) {
//            return false;
//        } else {
        return getDifferenceInMonthsBetweenDates(first.getPayDate(), second.getPayDate()) == 1;
//        }
    }

    public static long getDifferenceInMonthsBetweenDates(Date date1, Date date2) {

        // Period.toTotalMonths() only returns integer month differences so for 14/07/2015 and 17/06/2015 it returns 0
        // We need it to return 1, so we set both dates to the first of the month
        LocalDate toDate = date1.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().withDayOfMonth(1);
        LocalDate fromDate = date2.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().withDayOfMonth(1);
        Period period = fromDate.until(toDate);
        LOGGER.debug("fromDate: " + fromDate);
        LOGGER.debug("toDate: " + toDate);
        LOGGER.debug("Months: " + period.toTotalMonths());
        return period.toTotalMonths();

    }

    private static boolean isDateInRange(Date date, Date lower, Date upper) {
        boolean inRange = !(date.before(lower) || date.after(upper));
        LOGGER.debug(String.format("%s: %s in range of %s & %s", inRange, date, lower, upper));
        return inRange;
    }

    private enum EmploymentCheck {
        PASS, FAILED_THRESHOLD, FAILED_EMPLOYER
    }

}
