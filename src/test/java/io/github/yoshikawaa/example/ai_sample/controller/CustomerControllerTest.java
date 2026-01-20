package io.github.yoshikawaa.example.ai_sample.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import io.github.yoshikawaa.example.ai_sample.config.SecurityConfig;
import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.service.CustomerService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
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
    @DisplayName("GET /customers: 顧客一覧を表示する（デフォルトページネーション）")
    void testShowCustomers() throws Exception {
        // モックの動作を定義
        Customer customer1 = new Customer(
            "john.doe@example.com",
            "password123",
            "John Doe",
            LocalDate.of(2023, 3, 1),
            LocalDate.of(1990, 5, 20),
            "123-456-7890",
            "123 Main St"
        );
        Customer customer2 = new Customer(
            "jane.doe@example.com",
            "password456",
            "Jane Doe",
            LocalDate.of(2023, 3, 2),
            LocalDate.of(1992, 7, 15),
            "987-654-3210",
            "456 Elm St"
        );
        Page<Customer> customerPage = new PageImpl<>(Arrays.asList(customer1, customer2), PageRequest.of(0, 10), 2);
        when(customerService.getAllCustomersWithPagination(any())).thenReturn(customerPage);

        // テスト実行
        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer-list"))
                .andExpect(model().attributeExists("customerPage"))
                .andExpect(model().attribute("customerPage", customerPage));

        // サービス呼び出しの検証
        verify(customerService, times(1)).getAllCustomersWithPagination(any());
    }

    @Test
    @DisplayName("GET /customers: 顧客一覧を表示する（ページネーションパラメータ指定）")
    void testShowCustomers_withPagination() throws Exception {
        // モックの動作を定義
        Customer customer = new Customer(
            "john.doe@example.com",
            "password123",
            "John Doe",
            LocalDate.of(2023, 3, 1),
            LocalDate.of(1990, 5, 20),
            "123-456-7890",
            "123 Main St"
        );
        Page<Customer> customerPage = new PageImpl<>(Arrays.asList(customer), PageRequest.of(1, 5), 15);
        when(customerService.getAllCustomersWithPagination(any())).thenReturn(customerPage);

        // テスト実行（page=1, size=5を指定）
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", "1");
        params.add("size", "5");
        mockMvc.perform(get("/customers").params(params))
                .andExpect(status().isOk())
                .andExpect(view().name("customer-list"))
                .andExpect(model().attributeExists("customerPage"))
                .andExpect(model().attribute("customerPage", customerPage));

        // サービス呼び出しの検証
        verify(customerService, times(1)).getAllCustomersWithPagination(any());
    }

    @Test
    @DisplayName("GET /customers/search: 顧客を検索できる")
    void testSearchCustomers() throws Exception {
        // モックの動作を定義
        Customer customer = new Customer(
            "john.doe@example.com",
            "password123",
            "John Doe",
            LocalDate.of(2023, 3, 1),
            LocalDate.of(1990, 5, 20),
            "123-456-7890",
            "123 Main St"
        );
        Page<Customer> customerPage = new PageImpl<>(Arrays.asList(customer), PageRequest.of(0, 10), 1);
        when(customerService.searchCustomersWithPagination(eq("John"), eq("john@example.com"), any())).thenReturn(customerPage);

        // テスト実行
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "John");
        params.add("email", "john@example.com");
        mockMvc.perform(get("/customers/search").params(params))
                .andExpect(status().isOk())
                .andExpect(view().name("customer-list"))
                .andExpect(model().attributeExists("customerPage"))
                .andExpect(model().attribute("customerPage", customerPage));

        // サービス呼び出しの検証
        verify(customerService, times(1)).searchCustomersWithPagination(eq("John"), eq("john@example.com"), any());
    }

    @Test
    @DisplayName("GET /customers/search: 顧客を検索できる（ページネーションパラメータ指定）")
    void testSearchCustomers_withPagination() throws Exception {
        // モックの動作を定義
        Customer customer = new Customer(
            "john.doe@example.com",
            "password123",
            "John Doe",
            LocalDate.of(2023, 3, 1),
            LocalDate.of(1990, 5, 20),
            "123-456-7890",
            "123 Main St"
        );
        Page<Customer> customerPage = new PageImpl<>(Arrays.asList(customer), PageRequest.of(1, 5), 10);
        when(customerService.searchCustomersWithPagination(eq("John"), eq(null), any())).thenReturn(customerPage);

        // テスト実行（page=1, size=5を指定）
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "John");
        params.add("page", "1");
        params.add("size", "5");
        mockMvc.perform(get("/customers/search").params(params))
                .andExpect(status().isOk())
                .andExpect(view().name("customer-list"))
                .andExpect(model().attributeExists("customerPage"))
                .andExpect(model().attribute("customerPage", customerPage));

        // サービス呼び出しの検証
        verify(customerService, times(1)).searchCustomersWithPagination(eq("John"), eq(null), any());
    }

    @Test
    @DisplayName("GET /customers/search: 検索条件なしで全件取得")
    void testSearchCustomers_検索条件なし() throws Exception {
        // モックの動作を定義
        Customer customer1 = new Customer(
            "john.doe@example.com",
            "password123",
            "John Doe",
            LocalDate.of(2023, 3, 1),
            LocalDate.of(1990, 5, 20),
            "123-456-7890",
            "123 Main St"
        );
        Customer customer2 = new Customer(
            "jane.doe@example.com",
            "password456",
            "Jane Doe",
            LocalDate.of(2023, 3, 2),
            LocalDate.of(1992, 7, 15),
            "987-654-3210",
            "456 Elm St"
        );
        Page<Customer> customerPage = new PageImpl<>(Arrays.asList(customer1, customer2), PageRequest.of(0, 10), 2);
        when(customerService.searchCustomersWithPagination(eq(null), eq(null), any())).thenReturn(customerPage);

        // テスト実行
        mockMvc.perform(get("/customers/search"))
                .andExpect(status().isOk())
                .andExpect(view().name("customer-list"))
                .andExpect(model().attributeExists("customerPage"))
                .andExpect(model().attribute("customerPage", customerPage));

        // サービス呼び出しの検証
        verify(customerService, times(1)).searchCustomersWithPagination(eq(null), eq(null), any());
    }

    @Test
    @DisplayName("GET /customers/search: 検索結果がない場合")
    void testSearchCustomers_検索結果なし() throws Exception {
        // モックの動作を定義（空のリスト）
        Page<Customer> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 10), 0);
        when(customerService.searchCustomersWithPagination(eq("NonExistent"), eq("nonexistent@example.com"), any())).thenReturn(emptyPage);

        // テスト実行
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "NonExistent");
        params.add("email", "nonexistent@example.com");
        mockMvc.perform(get("/customers/search").params(params))
                .andExpect(status().isOk())
                .andExpect(view().name("customer-list"))
                .andExpect(model().attributeExists("customerPage"))
                .andExpect(model().attribute("customerPage", emptyPage));

        // サービス呼び出しの検証
        verify(customerService, times(1)).searchCustomersWithPagination(eq("NonExistent"), eq("nonexistent@example.com"), any());
    }
}
