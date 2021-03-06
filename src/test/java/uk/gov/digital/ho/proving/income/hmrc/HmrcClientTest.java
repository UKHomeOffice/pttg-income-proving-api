package uk.gov.digital.ho.proving.income.hmrc;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import net.logstash.logback.marker.ObjectAppendingMarker;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import uk.gov.digital.ho.proving.income.api.RequestData;
import uk.gov.digital.ho.proving.income.application.ApplicationExceptions.EarningsServiceNoUniqueMatchException;
import uk.gov.digital.ho.proving.income.application.LogEvent;
import uk.gov.digital.ho.proving.income.hmrc.domain.HmrcIndividual;
import uk.gov.digital.ho.proving.income.hmrc.domain.Identity;
import uk.gov.digital.ho.proving.income.hmrc.domain.IncomeRecord;

import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.List;

import static ch.qos.logback.classic.Level.ERROR;
import static ch.qos.logback.classic.Level.INFO;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.digital.ho.proving.income.api.RequestData.*;
import static uk.gov.digital.ho.proving.income.application.LogEvent.*;

@RunWith(MockitoJUnitRunner.class)
public class HmrcClientTest {

    private static final String SOME_SESSION_ID = "some session id";
    private static final String SOME_CORRELATION_ID = "some correlation id";
    private static final String SOME_USER_ID = "some user id";
    private static final String SOME_BASIC_AUTH = "some basic auth";
    private static final String SOME_FIRST_NAME = "John";
    private static final String SOME_LAST_NAME = "Smith";
    private static final String SOME_NINO = "some nino";
    private static final LocalDate SOME_DOB = LocalDate.of(1965, Month.JULY, 19);
    private static final LocalDate SOME_FROM_DATE = LocalDate.of(2017, Month.JANUARY, 1);
    private static final LocalDate SOME_TO_DATE = LocalDate.of(2017, Month.JULY, 1);
    private static final String SOME_COMPONENT_TRACE = "smoke-tests,pttg-ip-api";

    private static final Identity ANY_IDENTITY = new Identity(
        "John",
        "Smith",
        LocalDate.of(1965, Month.JULY, 19), "NE121212A");
    private static final LocalDate ANY_DATE = LocalDate.now();

    @Mock private RestTemplate mockRestTemplate;
    @Mock private RequestData mockRequestData;
    @Mock private ServiceResponseLogger mockServiceResponseLogger;
    @Mock private Appender<ILoggingEvent> mockAppender;

    @Captor private ArgumentCaptor<IncomeRecord> captorResponseBody;
    @Captor private ArgumentCaptor<HttpEntity> captorEntity;

    private HmrcClient service;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void before() {

        when(mockRequestData.sessionId()).thenReturn(SOME_SESSION_ID);
        when(mockRequestData.correlationId()).thenReturn(SOME_CORRELATION_ID);
        when(mockRequestData.userId()).thenReturn(SOME_USER_ID);
        when(mockRequestData.hmrcBasicAuth()).thenReturn(SOME_BASIC_AUTH);
        when(mockRequestData.componentTrace()).thenReturn(SOME_COMPONENT_TRACE);

        service = new HmrcClient(mockRestTemplate, "http://income-service/income", mockRequestData, mockServiceResponseLogger, simpleRetryTemplate());

        when(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), ArgumentMatchers.<Class<IncomeRecord>>any()))
            .thenReturn(new ResponseEntity<>(anyIncomeRecord(), OK));

        service.getIncomeRecord(
            new Identity(
                SOME_FIRST_NAME,
                SOME_LAST_NAME,
                SOME_DOB,
                SOME_NINO),
            SOME_FROM_DATE,
            SOME_TO_DATE
        );

        Logger rootLogger = (Logger) LoggerFactory.getLogger(HmrcClient.class);
        rootLogger.setLevel(INFO);
        rootLogger.addAppender(mockAppender);
    }

    /*
    * It is difficult to use a mock of the RetryTemplate because then we'd need to stub all its internal workings. Instead
    * we just use a RetryTemplate that is configrured never to retry.
    * */
    private RetryTemplate simpleRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(1));
        return retryTemplate;
    }

    @Test
    public void shouldSendServiceResponseToLogger() {
        verify(mockServiceResponseLogger).record(eq(new Identity(SOME_FIRST_NAME, SOME_LAST_NAME, SOME_DOB, SOME_NINO)),
                                                    captorResponseBody.capture());

        assertThat(captorResponseBody.getValue()).isInstanceOf(IncomeRecord.class);
        assertThat(captorResponseBody.getValue().paye()).isEmpty();
        assertThat(captorResponseBody.getValue().employments()).isEmpty();
    }

    @Test
    public void shouldMakePostRequestToHmrcService() {
        verify(mockRestTemplate).exchange(
            eq("http://income-service/income"),
            eq(POST),
            any(HttpEntity.class),
            eq(IncomeRecord.class));
    }

    @Test
    public void shouldSendHttpHeaders() {
        verify(mockRestTemplate).exchange(anyString(),
            any(HttpMethod.class),
            captorEntity.capture(),
            ArgumentMatchers.<Class<IncomeRecord>>any());

        assertThat(captorEntity.getValue().getHeaders()).containsEntry(CONTENT_TYPE, singletonList(APPLICATION_JSON_VALUE));
        assertThat(captorEntity.getValue().getHeaders()).containsEntry(SESSION_ID_HEADER, singletonList("some session id"));
        assertThat(captorEntity.getValue().getHeaders()).containsEntry(CORRELATION_ID_HEADER, singletonList("some correlation id"));
        assertThat(captorEntity.getValue().getHeaders()).containsEntry(USER_ID_HEADER, singletonList("some user id"));
        assertThat(captorEntity.getValue().getHeaders()).containsEntry(AUTHORIZATION, singletonList("some basic auth"));
        assertThat(captorEntity.getValue().getHeaders()).containsEntry(COMPONENT_TRACE_HEADER, singletonList(SOME_COMPONENT_TRACE));
    }

    @Test
    public void shouldSendIncomeDataRequest() {
        verify(mockRestTemplate).exchange(
            anyString(),
            any(HttpMethod.class),
            captorEntity.capture(),
            ArgumentMatchers.<Class<IncomeRecord>>any());

        Object body = captorEntity.getValue().getBody();
        assertThat(body).isInstanceOf(IncomeDataRequest.class);
        IncomeDataRequest data = (IncomeDataRequest) body;
        assertThat(data).isEqualTo(new IncomeDataRequest(SOME_FIRST_NAME, SOME_LAST_NAME, SOME_NINO, SOME_DOB, SOME_FROM_DATE, SOME_TO_DATE));
    }


    @Test(expected = HttpStatusCodeException.class)
    public void forbiddenShouldNotBeMappedToNoMatch() {
        when(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), ArgumentMatchers.<Class<IncomeRecord>>any()))
            .thenThrow(new HttpClientErrorException(FORBIDDEN));

        service.getIncomeRecord(
            ANY_IDENTITY,
            LocalDate.of(2017, Month.JANUARY, 1),
            LocalDate.of(2017, Month.JULY, 1)
        );
    }

    @Test(expected = EarningsServiceNoUniqueMatchException.class)
    public void notFoundShouldBeMappedToNoMatch() {
        when(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), ArgumentMatchers.<Class<IncomeRecord>>any()))
            .thenThrow(new HttpClientErrorException(NOT_FOUND));

        service.getIncomeRecord(
            ANY_IDENTITY,
            LocalDate.of(2017, Month.JANUARY, 1),
            LocalDate.of(2017, Month.JULY, 1)
        );
    }

    private HmrcIndividual aIndividual() {
        return new HmrcIndividual("Joe", "Bloggs", "NE121212C", LocalDate.now());
    }


    @Test
    public void shouldRethrowHttpServerErrorException() {
        thrown.expect(HttpServerErrorException.class);

        HttpServerErrorException serverErrorException = new HttpServerErrorException(BAD_GATEWAY);
        given(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), ArgumentMatchers.<Class<IncomeRecord>>any()))
            .willThrow(serverErrorException);

        service.getIncomeRecord(ANY_IDENTITY, ANY_DATE, ANY_DATE);
    }

    @Test
    public void shouldLogWhenHmrcRequestSent() {
        service.getIncomeRecord(
            ANY_IDENTITY,
            LocalDate.of(2017, Month.JANUARY, 1),
            LocalDate.of(2017, Month.JULY, 1)
        );

        verifyLogMessage("About to call HMRC Service at http://income-service/income", HMRC_REQUEST_SENT, INFO);
    }

    @Test
    public void shouldLogWhenHmrcResponseReceived() {
        service.getIncomeRecord(
            ANY_IDENTITY,
            LocalDate.of(2017, Month.JANUARY, 1),
            LocalDate.of(2017, Month.JULY, 1)
        );

        verifyLogMessage("Received 0 incomes and 0 employments", HMRC_RESPONSE_SUCCESS, INFO);
    }

    @Test
    public void shouldLogWhenHmrcNotFoundResponseReceived() {
        when(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), ArgumentMatchers.<Class<IncomeRecord>>any()))
            .thenThrow(new HttpClientErrorException(NOT_FOUND));

        try {
            service.getIncomeRecord(
                ANY_IDENTITY,
                LocalDate.of(2017, Month.JANUARY, 1),
                LocalDate.of(2017, Month.JULY, 1)
            );
        }
        catch(EarningsServiceNoUniqueMatchException e){
            //not used for the purpose of this test
        }

       verifyLogMessage("HMRC Service found no match", HMRC_NOT_FOUND_RESPONSE, ERROR);
    }

    @Test
    public void shouldLogWhenHmrcServiceFails() {
        when(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), ArgumentMatchers.<Class<IncomeRecord>>any()))
            .thenThrow(new HttpServerErrorException(INTERNAL_SERVER_ERROR));

        try {
            service.getIncomeRecord(
                ANY_IDENTITY,
                LocalDate.of(2017, Month.JANUARY, 1),
                LocalDate.of(2017, Month.JULY, 1)
            );
        }
        catch(HttpServerErrorException e){
            //not used for the purpose of this test
        }

        verifyLogMessage("HMRC Service failed", HMRC_ERROR_REPSONSE, ERROR);
    }

    @Test
    public void getIncomeRecord_anyInput_shouldUseRetryTemplate() {
        RetryTemplate mockRetryTemplate = mock(RetryTemplate.class);
        HmrcClient client = new HmrcClient(mockRestTemplate, "http://income-service/income", mockRequestData, mockServiceResponseLogger, mockRetryTemplate);

        client.getIncomeRecord(ANY_IDENTITY, ANY_DATE, ANY_DATE);
        then(mockRetryTemplate).should().execute(any(), any(), any());
    }

    @Test
    public void getIncomeRecord_responseSuccess_updateComponentTrace() {
        reset(mockRequestData);
        HttpHeaders headers = new HttpHeaders();
        headers.add(COMPONENT_TRACE_HEADER, "some-component");
        headers.add(COMPONENT_TRACE_HEADER, "some-other-component");

        ResponseEntity<IncomeRecord> responseEntity = new ResponseEntity<>(anyIncomeRecord(), headers, OK);
        given(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), ArgumentMatchers.<Class<IncomeRecord>>any()))
            .willReturn(responseEntity);

        Identity anyIdentity = new Identity(SOME_FIRST_NAME, SOME_LAST_NAME, SOME_DOB, SOME_NINO);
        LocalDate anyDate = LocalDate.now();
        service.getIncomeRecord(anyIdentity, anyDate, anyDate);

        then(mockRequestData).should().updateComponentTrace(responseEntity);
    }

    @Test
    public void getIncomeRecord_notFound_updateComponentTrace() {
        reset(mockRequestData);
        HttpHeaders headers = new HttpHeaders();
        headers.add(COMPONENT_TRACE_HEADER, "some-component");
        headers.add(COMPONENT_TRACE_HEADER, "some-other-component");

        HttpStatusCodeException notFoundException = new HttpClientErrorException(NOT_FOUND, "any status text", headers, null, null);
        given(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), ArgumentMatchers.<Class<IncomeRecord>>any()))
            .willThrow(notFoundException);

        Identity anyIdentity = new Identity(SOME_FIRST_NAME, SOME_LAST_NAME, SOME_DOB, SOME_NINO);
        LocalDate anyDate = LocalDate.now();
        try {
            service.getIncomeRecord(anyIdentity, anyDate, anyDate);
        } catch (EarningsServiceNoUniqueMatchException ignored) {
            // Exception not of interest to this test.
        }

        then(mockRequestData).should().updateComponentTrace(notFoundException);
    }

    @Test
    public void getIncomeRecord_otherClientErrorException_updateComponentTrace() {
        reset(mockRequestData);
        HttpHeaders headers = new HttpHeaders();
        headers.add(COMPONENT_TRACE_HEADER, "some-component");
        headers.add(COMPONENT_TRACE_HEADER, "some-other-component");

        HttpStatusCodeException httpException = new HttpClientErrorException(BAD_REQUEST, "any status text", headers, null, null);
        given(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), ArgumentMatchers.<Class<IncomeRecord>>any()))
            .willThrow(httpException);

        Identity anyIdentity = new Identity(SOME_FIRST_NAME, SOME_LAST_NAME, SOME_DOB, SOME_NINO);
        LocalDate anyDate = LocalDate.now();
        try {
            service.getIncomeRecord(anyIdentity, anyDate, anyDate);
        } catch (HttpClientErrorException ignored) {
            // Exception not of interest to this test.
        }

        ArgumentCaptor<HttpStatusCodeException> argumentCaptor = ArgumentCaptor.forClass(HttpStatusCodeException.class);
        then(mockRequestData).should().updateComponentTrace(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isEqualTo(httpException);
    }

    @Test
    public void getIncomeRecord_retriesExhausted_log() {
        HttpStatusCodeException httpException = new HttpServerErrorException(INTERNAL_SERVER_ERROR, "any status text", new HttpHeaders(), null, null);
        given(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), ArgumentMatchers.<Class<IncomeRecord>>any()))
            .willThrow(httpException);

        Identity anyIdentity = new Identity(SOME_FIRST_NAME, SOME_LAST_NAME, SOME_DOB, SOME_NINO);
        LocalDate anyDate = LocalDate.now();
        try {
            service.getIncomeRecord(anyIdentity, anyDate, anyDate);
        } catch (HttpServerErrorException ignored) {
            // Exception not of interest to this test.
        }

        ArgumentCaptor<LoggingEvent> logCaptor = ArgumentCaptor.forClass(LoggingEvent.class);
        then(mockAppender).should(atLeastOnce()).doAppend(logCaptor.capture());

        LoggingEvent errorLog = getLogStartingWith(logCaptor.getAllValues(), "Failed to retrieve HMRC data");
        assertThat(errorLog.getLevel()).isEqualTo(ERROR);
        assertThat(errorLog.getArgumentArray()).contains(new ObjectAppendingMarker(EVENT, HMRC_ERROR_REPSONSE));
    }

    private IncomeRecord anyIncomeRecord() {
        return new IncomeRecord(emptyList(), emptyList(), emptyList(), aIndividual());
    }

    public void verifyLogMessage(String message, LogEvent event, Level logLevel) {
        verify(mockAppender).doAppend(argThat(argument -> {
            LoggingEvent loggingEvent = (LoggingEvent) argument;
            return loggingEvent.getLevel().equals(logLevel) &&
                loggingEvent.getFormattedMessage().equals(message) &&
                Arrays.asList(loggingEvent.getArgumentArray()).contains(new ObjectAppendingMarker("event_id", event));
        }));
    }

    private LoggingEvent getLogStartingWith(List<LoggingEvent> loggingEvents, String messageStart) {
        return loggingEvents.stream()
                            .filter(loggingEvent -> loggingEvent.getFormattedMessage().startsWith(messageStart))
                            .findFirst()
                            .orElseThrow(AssertionError::new);
    }
}
