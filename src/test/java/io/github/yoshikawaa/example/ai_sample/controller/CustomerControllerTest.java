package io.github.yoshikawaa.example.ai_sample.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import io.github.yoshikawaa.example.ai_sample.config.SecurityConfig;
import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.service.CustomerService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.Arrays;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
@Import(SecurityConfig.class) // セキュリティ設定をインポート
@DisplayName("CustomerController のテスト")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @Test
    @DisplayName("GET /customers: 顧客一覧を表示する")
    void testShowCustomers() throws Exception {
        // モックの動作を定義
        Customer customer1 = new Customer(
            "john.doe@example.com",
            "password123",
            "John Doe",
            LocalDate.of(2023, 3, 1), // registrationDate
            LocalDate.of(1990, 5, 20), // birthDate
            "123-456-7890",
            "123 Main St"
        );
        Customer customer2 = new Customer(
            "jane.doe@example.com",
            "password456",
            "Jane Doe",
            LocalDate.of(2023, 3, 2), // registrationDate
            LocalDate.of(1992, 7, 15), // birthDate
            "987-654-3210",
            "456 Elm St"
        );
        when(customerService.getAllCustomers()).thenReturn(Arrays.asList(customer1, customer2));

        // テスト実行
        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk()) // HTTP ステータスが 200 OK であることを確認
                .andExpect(view().name("customer-list")) // ビュー名が "customer-list" であることを確認
                .andExpect(model().attributeExists("customers")) // モデルに "customers" 属性が存在することを確認
                .andExpect(model().attribute("customers", Arrays.asList(customer1, customer2))); // モデルの "customers" 属性が正しいことを確認

        // サービス呼び出しの検証
        verify(customerService, times(1)).getAllCustomers();
    }
}
