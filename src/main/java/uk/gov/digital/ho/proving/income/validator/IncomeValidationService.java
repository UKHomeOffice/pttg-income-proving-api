package uk.gov.digital.ho.proving.income.validator;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.digital.ho.proving.income.api.domain.CategoryCheck;
import uk.gov.digital.ho.proving.income.validator.domain.IncomeValidationRequest;
import uk.gov.digital.ho.proving.income.validator.domain.IncomeValidationResult;

import java.util.ArrayList;
import java.util.List;

@Service
public class IncomeValidationService {

    private IncomeValidator catASalariedIncomeValidator;
    private IncomeValidator catBNonSalariedIncomeValidator;

    public IncomeValidationService(
        IncomeValidator catASalariedIncomeValidator,
        IncomeValidator catBNonSalariedIncomeValidator
    ) {
        this.catASalariedIncomeValidator = catASalariedIncomeValidator;
        this.catBNonSalariedIncomeValidator = catBNonSalariedIncomeValidator;
    }

    public List<CategoryCheck> validate(IncomeValidationRequest incomeValidationRequest) {

        List<CategoryCheck> categoryChecks = new ArrayList<>();

        categoryChecks.add(checkCategory(incomeValidationRequest, catASalariedIncomeValidator, "A"));
        categoryChecks.add(checkCategory(incomeValidationRequest, catBNonSalariedIncomeValidator, "B"));

        return categoryChecks;

    }

    private CategoryCheck checkCategory(IncomeValidationRequest incomeValidationRequest, IncomeValidator incomeValidator, String category) {
        IncomeValidationResult result = incomeValidator.validate(incomeValidationRequest);
        return new CategoryCheck(
            category,
            result.calculationType(),
            result.status().isPassed(),
            incomeValidationRequest.applicationRaisedDate(),
            result.assessmentStartDate(),
            result.status(),
            result.threshold(),
            result.individuals());
    }
}
