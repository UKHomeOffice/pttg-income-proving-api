package uk.gov.digital.ho.proving.income.audit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.logstash.logback.marker.ObjectAppendingMarker;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.digital.ho.proving.income.api.RequestData;
import uk.gov.digital.ho.proving.income.application.LogEvent;

import java.net.URI;
import java.time.*;
import java.util.*;


import static ch.qos.logback.classic.Level.ERROR;
import static ch.qos.logback.classic.Level.INFO;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.digital.ho.proving.income.application.LogEvent.*;
import static uk.gov.digital.ho.proving.income.audit.AuditEventType.INCOME_PROVING_FINANCIAL_STATUS_REQUEST;
import static uk.gov.digital.ho.proving.income.audit.AuditEventType.INCOME_PROVING_FINANCIAL_STATUS_RESPONSE;

@RunWith(MockitoJUnitRunner.class)
public class AuditClientTest {

    private static TimeZone defaultTimeZone;
    private static final UUID UUID = new UUID(1, 1);

    @Mock
    private static ObjectMapper mockObjectMapper;
    @Mock
    private RestTemplate mockRestTemplate;
    @Mock
    private RequestData mockRequestData;
    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Captor
    private ArgumentCaptor<HttpEntity> captorHttpEntity;
    @Captor private ArgumentCaptor<URI> captorUri;

    private AuditClient auditClient;

    private static final String SOME_ENDPOINT = "http://some-endpoint";
    private static final String SOME_HISTORY_ENDPOINT = "http://some-history-endpoint";
    private static final String SOME_ARCHIVE_ENDPOINT = "http://some-archive-endpoint";

    @BeforeClass
    public static void beforeAllTests() {
        defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterClass
    public static void afterAllTests() {
        TimeZone.setDefault(defaultTimeZone);
    }

    @Before
    public void setup() {
        auditClient = new AuditClient(Clock.fixed(Instant.parse("2017-08-29T08:00:00Z"), ZoneId.of("UTC")),
            mockRestTemplate,
            mockRequestData,
            SOME_ENDPOINT,
                                        SOME_HISTORY_ENDPOINT,
                                        SOME_ARCHIVE_ENDPOINT,
            mockObjectMapper);

        Logger rootLogger = (Logger) LoggerFactory.getLogger(AuditClient.class);
        rootLogger.setLevel(Level.INFO);
        rootLogger.addAppender(mockAppender);
    }

    @Test
    public void shouldUseCollaborators() {
        auditClient.add(INCOME_PROVING_FINANCIAL_STATUS_REQUEST, UUID, null);

        verify(mockRestTemplate).exchange(eq(SOME_ENDPOINT), eq(POST), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    public void shouldSetHeaders() {

        when(mockRequestData.auditBasicAuth()).thenReturn("some basic auth header value");
        when(mockRequestData.correlationId()).thenReturn("some correlation id");
        auditClient.add(INCOME_PROVING_FINANCIAL_STATUS_REQUEST, UUID, null);

        verify(mockRestTemplate).exchange(eq(SOME_ENDPOINT), eq(POST), captorHttpEntity.capture(), eq(Void.class));

        HttpHeaders headers = captorHttpEntity.getValue().getHeaders();
        assertThat(headers.get("Authorization").get(0)).isEqualTo("some basic auth header value");
        assertThat(headers.get("Content-Type").get(0)).isEqualTo(APPLICATION_JSON_VALUE);
        assertThat(headers.get("x-correlation-id").get(0)).isEqualTo("some correlation id");
    }

    @Test
    public void shouldSetAuditableData() throws JsonProcessingException {

        when(mockRequestData.sessionId()).thenReturn("some session id");
        when(mockRequestData.correlationId()).thenReturn("some correlation id");
        when(mockRequestData.userId()).thenReturn("some user id");
        when(mockRequestData.deploymentName()).thenReturn("some deployment name");
        when(mockRequestData.deploymentNamespace()).thenReturn("some deployment namespace");
        when(mockObjectMapper.writeValueAsString(any(Object.class))).thenReturn("{}");

        auditClient.add(INCOME_PROVING_FINANCIAL_STATUS_REQUEST, UUID, Collections.emptyMap());

        verify(mockRestTemplate).exchange(eq(SOME_ENDPOINT), eq(POST), captorHttpEntity.capture(), eq(Void.class));

        AuditableData auditableData = (AuditableData) captorHttpEntity.getValue().getBody();
        assertThat(auditableData.getEventId()).isNotEmpty();
        assertThat(auditableData.getTimestamp()).isEqualTo(LocalDateTime.parse("2017-08-29T08:00:00"));
        assertThat(auditableData.getSessionId()).isEqualTo("some session id");
        assertThat(auditableData.getCorrelationId()).isEqualTo("some correlation id");
        assertThat(auditableData.getUserId()).isEqualTo("some user id");
        assertThat(auditableData.getDeploymentName()).isEqualTo("some deployment name");
        assertThat(auditableData.getDeploymentNamespace()).isEqualTo("some deployment namespace");
        assertThat(auditableData.getEventType()).isEqualTo(INCOME_PROVING_FINANCIAL_STATUS_REQUEST);
        assertThat(auditableData.getData()).isEqualTo("{}");
    }

    @Test
    public void shouldRetrieveAuditHistory() {
        List<AuditEventType> eventTypes = Arrays.asList(INCOME_PROVING_FINANCIAL_STATUS_REQUEST, INCOME_PROVING_FINANCIAL_STATUS_RESPONSE);
        List<AuditRecord> results = new ArrayList<>();
        ResponseEntity<List<AuditRecord>> resultsEntity = ResponseEntity.ok(results);
        when(mockRestTemplate.exchange(captorUri.capture(), eq(GET), any(HttpEntity.class), eq(new ParameterizedTypeReference<List<AuditRecord>>() {}))).thenReturn(resultsEntity);

        List<AuditRecord> auditRecords = auditClient.getAuditHistory(LocalDate.now(), eventTypes);

        URI uri = captorUri.getValue();
        assertThat(uri).hasHost(SOME_HISTORY_ENDPOINT.replace("http://", ""));
        assertThat(uri).hasQuery(String.format("toDate=%s&eventTypes=%s", LocalDate.now().toString(), eventTypes.toString()));
        assertThat(auditRecords).isEqualTo(results);
    }

    @Test
    public void shouldRequestAuditArchive() {
        ArchiveAuditRequest request = new ArchiveAuditRequest("any_nino", LocalDate.now().minusMonths(6), Arrays.asList("corr1", "corr2"), "PASS", LocalDate.now());
        when(mockRestTemplate.exchange(eq(SOME_ARCHIVE_ENDPOINT), eq(POST), captorHttpEntity.capture(), eq(new ParameterizedTypeReference<ArchiveAuditResponse>() {}))).thenReturn(ResponseEntity.ok(new ArchiveAuditResponse()));

        auditClient.archiveAudit(request);

        verify(mockRestTemplate).exchange(eq(SOME_ARCHIVE_ENDPOINT), eq(POST), captorHttpEntity.capture(), eq(new ParameterizedTypeReference<ArchiveAuditResponse>() {}));
        ArchiveAuditRequest actual = (ArchiveAuditRequest) captorHttpEntity.getValue().getBody();
        assertThat(actual).isEqualTo(request);
    }

    @Test
    public void shouldLogWhenAddToAuditServiceEvent() {
        auditClient.add(INCOME_PROVING_FINANCIAL_STATUS_REQUEST, UUID, Collections.emptyMap());

        verifyLogMessage("POST data for 00000000-0000-0001-0000-000000000001 to audit service", INCOME_PROVING_AUDIT_REQUEST, INFO);
    }

    @Test
    public void shouldLogAfterSuccessfulAuditServiceCall() {
        auditClient.add(INCOME_PROVING_FINANCIAL_STATUS_REQUEST, UUID, Collections.emptyMap());

        verifyLogMessage("data POSTed to audit service", INCOME_PROVING_AUDIT_SUCCESS, INFO);
    }

    @Test
    public void shouldLogAfterFailureToAudit() throws JsonProcessingException {
        when(mockObjectMapper.writeValueAsString(any(Object.class))).thenThrow(JsonProcessingException.class);

        auditClient.add(INCOME_PROVING_FINANCIAL_STATUS_REQUEST, UUID, Collections.emptyMap());

        verifyLogMessage("Failed to create json representation of audit data", INCOME_PROVING_AUDIT_FAILURE, ERROR);
    }

    private void verifyLogMessage(final String message, LogEvent event, Level logLevel) {
        verify(mockAppender).doAppend(argThat(argument -> {
            LoggingEvent loggingEvent = (LoggingEvent) argument;
            return loggingEvent.getLevel().equals(logLevel) &&
                loggingEvent.getFormattedMessage().equals(message) &&
                Arrays.asList(loggingEvent.getArgumentArray()).contains(new ObjectAppendingMarker("event_id", event));
        }));
    }

    @Test
    public void getAuditHistoryPaginated_givenParams_expectedUri() {
        when(mockRestTemplate.exchange(captorUri.capture(), eq(GET), any(HttpEntity.class), eq(new ParameterizedTypeReference<List<AuditRecord>>() {})))
            .thenReturn(ResponseEntity.ok(emptyList()));

        List<AuditEventType> eventTypes = Arrays.asList(INCOME_PROVING_FINANCIAL_STATUS_REQUEST, INCOME_PROVING_FINANCIAL_STATUS_RESPONSE);
        int page = 23;
        int size = 8;
        auditClient.getAuditHistoryPaginated(eventTypes, page, size);

        URI uri = captorUri.getValue();
        assertThat(uri).hasHost(SOME_HISTORY_ENDPOINT.replace("http://", ""));

        String[] queryStringComponents = uri.getQuery().split("&");
        assertThat(queryStringComponents).containsExactlyInAnyOrder(
            "eventTypes=" + eventTypes,
            "page=" + page,
            "size=" + size
        );
    }

    @Test
    public void getAuditHistoryPaginated_givenResponse_returnRecords() {
        List<AuditRecord> results = emptyList();
        stubResponse(results);

        List<AuditEventType> someEventTypes = Arrays.asList(INCOME_PROVING_FINANCIAL_STATUS_REQUEST, INCOME_PROVING_FINANCIAL_STATUS_RESPONSE);
        int somePage = 1;
        int someSize = 1;

        assertThat(auditClient.getAuditHistoryPaginated(someEventTypes, somePage, someSize))
            .isEqualTo(results);
    }

    private void stubResponse(List<AuditRecord> results) {
        ResponseEntity<List<AuditRecord>> response = ResponseEntity.ok(results);
        when(mockRestTemplate.exchange(captorUri.capture(), eq(GET), any(HttpEntity.class), eq(new ParameterizedTypeReference<List<AuditRecord>>() {})))
            .thenReturn(response);
    }
}
