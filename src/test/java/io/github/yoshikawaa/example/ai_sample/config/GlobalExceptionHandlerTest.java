package io.github.yoshikawaa.example.ai_sample.config;

import io.github.yoshikawaa.example.ai_sample.exception.CustomerNotFoundException;
import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.repository.CustomerRepository;
import io.github.yoshikawaa.example.ai_sample.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GlobalExceptionHandler のテスト")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private CustomerRepository customerRepository;

    private Customer testCustomer;

    /**
     * テスト用のコントローラを定義
     */
    @TestConfiguration
    static class TestConfig {
        
        /**
         * GlobalExceptionHandlerのテスト用コントローラ
         */
        @Controller
        @RequestMapping("/test")
        static class TestController {
            
            @Autowired
            private CustomerService customerService;
            
            @GetMapping("/customer-not-found")
            public String customerNotFound() {
                customerService.getCustomerByEmail("notfound@example.com");
                return "success";
            }
        }
    }

    @BeforeEach
    void setUp() {
        testCustomer = new Customer("test@example.com", "password", "Test User", 
            LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "123-4567", "Test Address");
        when(customerRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testCustomer));
    }

    @Test
    @WithUserDetails(value = "test@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("CustomerNotFoundException: 404エラーと汎用エラーページを返す")
    void testHandleCustomerNotFoundException() throws Exception {
        when(customerService.getCustomerByEmail("notfound@example.com"))
            .thenThrow(new CustomerNotFoundException("notfound@example.com"));

        mockMvc.perform(get("/test/customer-not-found"))
            .andExpect(status().isNotFound())
            .andExpect(view().name("error"))
            .andExpect(model().attributeExists("errorMessage"))
            .andExpect(model().attribute("errorCode", "404"));
    }
}
