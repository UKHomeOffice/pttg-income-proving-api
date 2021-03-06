package uk.gov.digital.ho.proving.income.validator;

import com.google.common.collect.ImmutableList;
import uk.gov.digital.ho.proving.income.api.domain.Applicant;
import uk.gov.digital.ho.proving.income.hmrc.domain.*;
import uk.gov.digital.ho.proving.income.validator.domain.ApplicantIncome;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class CatBSharedTestData {


    final static String PIZZA_HUT = "Pizza Hut";
    final static String BURGER_KING = "Burger King";
    final static String PIZZA_HUT_PAYE_REF = "Pizza Hut/ref";
    final static String BURGER_KING_PAYE_REF = "Pizza Hut/ref";
    final static Employer PIZZA_HUT_EMPLOYER = new Employer(PIZZA_HUT, PIZZA_HUT_PAYE_REF);
    final static Employer BURGER_KING_EMPLOYER = new Employer(BURGER_KING, BURGER_KING_PAYE_REF);
    final static String NINO = "AA123456A";
    final static String NINO_PARTNER = "BB123456B";
    final static LocalDate DOB = LocalDate.of(1970, Month.JANUARY, 1);
    final static HmrcIndividual HMRC_INDIVIDUAL_PARTNER = new HmrcIndividual("Denise", "Smith", NINO_PARTNER, DOB);
    final static HmrcIndividual HMRC_INDIVIDUAL = new HmrcIndividual("Duncan", "Smith", NINO, DOB);
    final static Applicant PARTNER = new Applicant("Denise", "Smith", DOB, NINO_PARTNER);
    final static Applicant APPLICANT = new Applicant("Duncan", "Smith", DOB, NINO);

    static BigDecimal amount(String i) {
        return new BigDecimal(i);
    }

    static List<ApplicantIncome> getApplicantIncomes(List<Income> paye, Employer... employers) {
        List<Employments> employments = Arrays.stream(employers).map(Employments::new).collect(Collectors.toList());
        IncomeRecord incomeRecord = new IncomeRecord(paye, new ArrayList<>(), employments, HMRC_INDIVIDUAL);
        return ImmutableList.of(new ApplicantIncome(APPLICANT, incomeRecord));
    }

    static List<ApplicantIncome> getPartnerIncomes(List<Income> paye, Employer... employers) {
        List<Employments> employments = Arrays.stream(employers).map(Employments::new).collect(Collectors.toList());
        IncomeRecord incomeRecord = new IncomeRecord(paye, new ArrayList<>(), employments, HMRC_INDIVIDUAL_PARTNER);
        return ImmutableList.of(new ApplicantIncome(PARTNER, incomeRecord));
    }
}
