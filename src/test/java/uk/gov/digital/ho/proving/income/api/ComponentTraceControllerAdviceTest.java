package uk.gov.digital.ho.proving.income.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@RunWith(MockitoJUnitRunner.class)
public class ComponentTraceControllerAdviceTest {

    private static final MethodParameter ANY_RETURN_TYPE = null;
    private static final Class ANY_CONVERTER_TYPE = ComponentTraceControllerAdvice.class;
    private static final MediaType ANY_MEDIA_TYPE = MediaType.APPLICATION_JSON;

    @Mock
    private ServerHttpRequest mockRequest;
    @Mock
    private ServerHttpResponse mockResponse;
    @Mock
    private RequestData mockRequestData;

    private ComponentTraceControllerAdvice controllerAdvice;

    @Before
    public void setUp() {
        controllerAdvice = new ComponentTraceControllerAdvice(mockRequestData);
    }

    @Test
    public void supports_anyInput_true() {
        assertThat(controllerAdvice.supports(ANY_RETURN_TYPE, ANY_CONVERTER_TYPE)).isTrue();
    }

    @Test
    public void beforeBodyWrite_someBody_returnBody() {
        Object expectedBody = "some body";
        Object actualBody = controllerAdvice.beforeBodyWrite(expectedBody, ANY_RETURN_TYPE, ANY_MEDIA_TYPE, ANY_CONVERTER_TYPE, mockRequest, mockResponse);

        assertThat(actualBody).isEqualTo(expectedBody);
    }

    @Test
    public void beforeBodyWrite_anyResponse_addHeader() {
        HttpHeaders someHeaders = new HttpHeaders();
        someHeaders.add("any key", "any value");
        given(mockResponse.getHeaders()).willReturn(someHeaders);

        controllerAdvice.beforeBodyWrite("any body", ANY_RETURN_TYPE, ANY_MEDIA_TYPE, ANY_CONVERTER_TYPE, mockRequest, mockResponse);

        then(mockRequestData).should().addComponentTraceHeader(someHeaders);
    }
}
