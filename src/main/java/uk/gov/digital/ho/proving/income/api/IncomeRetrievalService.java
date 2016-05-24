package uk.gov.digital.ho.proving.income.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.digital.ho.proving.income.acl.*;
import uk.gov.digital.ho.proving.income.domain.IncomeProvingResponse;

import java.text.ParseException;
import java.util.Date;
import java.util.Optional;

@RestController
@ControllerAdvice
public class IncomeRetrievalService extends AbstractIncomeProvingController {

    @Autowired
    private EarningsService earningsService;

    @Autowired
    private IndividualService individualService;

    @RequestMapping(value = "/incomeproving/v1/individual/{nino}/income", method = RequestMethod.GET)
    public ResponseEntity<IncomeRetrievalResponse> getIncome(
        @PathVariable(value = "nino") String nino,
        @RequestParam(value = "fromDate") String fromDateAsString,
        @RequestParam(value = "toDate") String toDateAsString) {

        LOGGER.info(String.format("Income Proving Service API for Income Retrieval invoked for %s nino between %s and %s", nino, fromDateAsString, toDateAsString));

        HttpHeaders headers = new HttpHeaders();
        headers.set(CONTENT_TYPE, APPLICATION_JSON);


        try {
            String cleanNino = sanitiseNino(nino);
            validateNino(cleanNino);

            Optional<Date> fromDate = parseDate(fromDateAsString);
            if (!fromDate.isPresent()) {
                return buildErrorResponse(headers, "0002", "Parameter error: From date is invalid", HttpStatus.BAD_REQUEST);
            }

            Optional<Date> toDate = parseDate(toDateAsString);
            if (!toDate.isPresent()) {
                return buildErrorResponse(headers, "0002", "Parameter error: To date is invalid", HttpStatus.BAD_REQUEST);
            }


            Optional<IncomeProvingResponse> incomeProvingResponse = fromDate.flatMap(from ->
                toDate.map(to ->
                    individualService.lookup(cleanNino, from, to)
                )
            );

            return incomeProvingResponse.map(ips -> {
                    IncomeRetrievalResponse incomeRetrievalResponse = new IncomeRetrievalResponse();
                    incomeRetrievalResponse.setIndividual(ips.getindividual());
                    incomeRetrievalResponse.setIncomes(ips.getIncomes());
                    return new ResponseEntity<>(incomeRetrievalResponse, headers, HttpStatus.OK);
                }
            ).orElse(buildErrorResponse(headers, "0002", "Invalid NINO", HttpStatus.NOT_FOUND));
        } catch (EarningsServiceFailedToMapDataToDomainClass | EarningsServiceNoUniqueMatch e) {
            LOGGER.error("Could not retrieve earning details.", e);
            return buildErrorResponse(headers, "0004", "Resource not found", HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException iae) {
            LOGGER.error(iae.getMessage(), iae);
            return buildErrorResponse(headers, "0004", "Parameter error: " + iae.getLocalizedMessage(), HttpStatus.BAD_REQUEST);
        } catch (UnknownPaymentFrequencyType upte) {
            LOGGER.error("Unknown payment frequency type " + upte);
            return buildErrorResponse(headers, "0004", "Unknown payment frequency type", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (RuntimeException e) {
            LOGGER.error("NINO is not valid", e);
            return buildErrorResponse(headers, "0001", "Parameter error: NINO is invalid", HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    protected ResponseEntity<IncomeRetrievalResponse> buildErrorResponse(HttpHeaders headers, String statusCode, String statusMessage, HttpStatus status) {
        ResponseStatus error = new ResponseStatus(statusCode, statusMessage);
        IncomeRetrievalResponse response = new IncomeRetrievalResponse();
        response.setStatus(error);
        return new ResponseEntity<>(response, headers, status);
    }

}
