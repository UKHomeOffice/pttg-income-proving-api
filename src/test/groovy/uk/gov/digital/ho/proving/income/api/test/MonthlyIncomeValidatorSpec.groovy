package uk.gov.digital.ho.proving.income.api.test

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification
import uk.gov.digital.ho.proving.income.api.FinancialCheckValues
import uk.gov.digital.ho.proving.income.api.IncomeValidator
import uk.gov.digital.ho.proving.income.domain.Income

import java.time.LocalDate
import java.time.Month

import static MockDataUtils.getDate
import static MockDataUtils.getConsecutiveIncomes
import static MockDataUtils.getConsecutiveIncomesButDifferentEmployers
import static MockDataUtils.getConsecutiveIncomesButLowAmounts
import static MockDataUtils.getConsecutiveIncomesWithDifferentMonthlyPayDay
import static MockDataUtils.getConsecutiveIncomesWithExactlyTheAmount
import static MockDataUtils.getNoneConsecutiveIncomes
import static MockDataUtils.getNotEnoughConsecutiveIncomes
import static MockDataUtils.subtractDaysFromDate

class MonthlyIncomeValidatorSpec extends Specification {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonthlyIncomeValidatorSpec.class);

    int days = 182

    def "valid category A individual is accepted"() {

        given:
        List<Income> incomes = getConsecutiveIncomes()
        LocalDate raisedDate = getDate(2015, Month.SEPTEMBER, 23)
        LocalDate pastDate = subtractDaysFromDate(raisedDate, days)

        when:
        FinancialCheckValues categoryAIndividual = IncomeValidator.validateCategoryAMonthlySalaried(incomes, pastDate, raisedDate, 0)

        then:
        categoryAIndividual.equals(FinancialCheckValues.MONTHLY_SALARIED_PASSED)

    }

    def "invalid category A individual is rejected (non consecutive)"() {

        given:
        List<Income> incomes = getNoneConsecutiveIncomes()
        LocalDate raisedDate = getDate(2015, Month.SEPTEMBER, 23)
        LocalDate pastDate = subtractDaysFromDate(raisedDate, days)

        when:
        FinancialCheckValues categoryAIndividual = IncomeValidator.validateCategoryAMonthlySalaried(incomes, pastDate, raisedDate, 0)

        then:
        categoryAIndividual.equals(FinancialCheckValues.NON_CONSECUTIVE_MONTHS)

    }

    def "invalid category A individual is rejected (not enough records)"() {

        given:
        List<Income> incomes = getNotEnoughConsecutiveIncomes()
        LocalDate raisedDate = getDate(2015, Month.SEPTEMBER, 23)
        LocalDate pastDate = subtractDaysFromDate(raisedDate, days)

        when:
        FinancialCheckValues categoryAIndividual = IncomeValidator.validateCategoryAMonthlySalaried(incomes, pastDate, raisedDate, 0)

        then:
        categoryAIndividual.equals(FinancialCheckValues.NOT_ENOUGH_RECORDS)

    }

    def "invalid category A individual is rejected (consecutive but not same employer)"() {

        given:
        List<Income> incomes = getConsecutiveIncomesButDifferentEmployers()
        LocalDate raisedDate = getDate(2015, Month.SEPTEMBER, 23)
        LocalDate pastDate = subtractDaysFromDate(raisedDate, days)

        when:
        FinancialCheckValues categoryAIndividual = IncomeValidator.validateCategoryAMonthlySalaried(incomes, pastDate, raisedDate, 0)

        then:
        categoryAIndividual.equals(FinancialCheckValues.NON_CONSECUTIVE_MONTHS)

    }

    def "invalid category A individual is rejected (consecutive but not enough earnings)"() {

        given:
        List<Income> incomes = getConsecutiveIncomesButLowAmounts()
        LocalDate raisedDate = getDate(2015, Month.SEPTEMBER, 23)
        LocalDate pastDate = subtractDaysFromDate(raisedDate, days)

        when:
        FinancialCheckValues categoryAIndividual = IncomeValidator.validateCategoryAMonthlySalaried(incomes, pastDate, raisedDate, 0)

        then:
        categoryAIndividual.equals(FinancialCheckValues.MONTHLY_VALUE_BELOW_THRESHOLD)

    }

    def "valid category A individual is accepted with different monthly payLocalDates"() {

        given:
        List<Income> incomes = getConsecutiveIncomesWithDifferentMonthlyPayDay()
        LocalDate raisedDate = getDate(2015, Month.SEPTEMBER, 23)
        LocalDate pastDate = subtractDaysFromDate(raisedDate, days)

        when:
        FinancialCheckValues categoryAIndividual = IncomeValidator.validateCategoryAMonthlySalaried(incomes, pastDate, raisedDate, 0)

        then:
        categoryAIndividual.equals(FinancialCheckValues.MONTHLY_SALARIED_PASSED)

    }

    def "valid category A individual is accepted with exactly the threshold values"() {

        given:
        List<Income> incomes = getConsecutiveIncomesWithExactlyTheAmount()
        LocalDate raisedDate = getDate(2015, Month.SEPTEMBER, 23)
        LocalDate pastDate = subtractDaysFromDate(raisedDate, days)

        when:
        FinancialCheckValues categoryAIndividual = IncomeValidator.validateCategoryAMonthlySalaried(incomes, pastDate, raisedDate, 0)

        then:
        categoryAIndividual.equals(FinancialCheckValues.MONTHLY_SALARIED_PASSED)

    }


}
