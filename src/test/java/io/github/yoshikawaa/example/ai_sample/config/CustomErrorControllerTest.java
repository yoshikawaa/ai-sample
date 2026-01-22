package io.github.yoshikawaa.example.ai_sample.config;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CustomErrorController のテスト
 */
@SpringBootTest
@DisplayName("CustomErrorController のテスト")
class CustomErrorControllerTest {

    @Autowired
    private CustomErrorController customErrorController;

    @Test
    @DisplayName("404エラーの場合、error/404.html を表示する")
    void testNotFoundError() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.NOT_FOUND.value());
        Model model = new ExtendedModelMap();

        // When
        String viewName = customErrorController.handleError(request, model);

        // Then
        assertThat(viewName).isEqualTo("error/404");
        assertThat(model.getAttribute("status")).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("500エラーの場合、error/500.html を表示する")
    void testInternalServerError() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.INTERNAL_SERVER_ERROR.value());
        Model model = new ExtendedModelMap();

        // When
        String viewName = customErrorController.handleError(request, model);

        // Then
        assertThat(viewName).isEqualTo("error/500");
        assertThat(model.getAttribute("status")).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    @DisplayName("502エラーの場合、error/500.html を表示する")
    void testBadGatewayError() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.BAD_GATEWAY.value());
        Model model = new ExtendedModelMap();

        // When
        String viewName = customErrorController.handleError(request, model);

        // Then
        assertThat(viewName).isEqualTo("error/500");
        assertThat(model.getAttribute("status")).isEqualTo(HttpStatus.BAD_GATEWAY.value());
    }

    @Test
    @DisplayName("503エラーの場合、error/500.html を表示する")
    void testServiceUnavailableError() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.SERVICE_UNAVAILABLE.value());
        Model model = new ExtendedModelMap();

        // When
        String viewName = customErrorController.handleError(request, model);

        // Then
        assertThat(viewName).isEqualTo("error/500");
        assertThat(model.getAttribute("status")).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
    }

    @Test
    @DisplayName("400エラーの場合、error.html を表示する")
    void testBadRequestError() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.BAD_REQUEST.value());
        Model model = new ExtendedModelMap();

        // When
        String viewName = customErrorController.handleError(request, model);

        // Then
        assertThat(viewName).isEqualTo("error");
        assertThat(model.getAttribute("status")).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("403エラーの場合、error.html を表示する")
    void testForbiddenError() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.FORBIDDEN.value());
        Model model = new ExtendedModelMap();

        // When
        String viewName = customErrorController.handleError(request, model);

        // Then
        assertThat(viewName).isEqualTo("error");
        assertThat(model.getAttribute("status")).isEqualTo(HttpStatus.FORBIDDEN.value());
    }
}
