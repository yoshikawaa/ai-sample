package io.github.yoshikawaa.example.ai_sample.controller;

import java.time.LocalDate;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import io.github.yoshikawaa.example.ai_sample.config.GlobalExceptionHandler;
import io.github.yoshikawaa.example.ai_sample.config.SecurityConfig;
import io.github.yoshikawaa.example.ai_sample.exception.CustomerNotFoundException;
import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.service.CustomerService;
import io.github.yoshikawaa.example.ai_sample.service.LoginAttemptService;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CustomerController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class}) // セキュリティ設定とエラーハンドラーをインポート
@DisplayName("CustomerController のテスト")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    @Nested
    @DisplayName("標準機能テスト")
    class DefaultTest {

        @Test
        @DisplayName("GET /customers: 顧客一覧を表示する（デフォルトページネーション）")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowCustomers() throws Exception {
            Customer customer1 = new Customer(
                "john.doe@example.com",
                "password123",
                "John Doe",
                LocalDate.of(2023, 3, 1),
                LocalDate.of(1990, 5, 20),
                "123-456-7890",
                "123 Main St",
                Customer.Role.USER
            );
            Customer customer2 = new Customer(
                "jane.doe@example.com",
                "password456",
                "Jane Doe",
                LocalDate.of(2023, 3, 2),
                LocalDate.of(1992, 7, 15),
                "987-654-3210",
                "456 Elm St",
                Customer.Role.USER
            );
            Page<Customer> customerPage = new PageImpl<>(Arrays.asList(customer1, customer2), PageRequest.of(0, 10), 2);
            when(customerService.getAllCustomersWithPagination(any())).thenReturn(customerPage);

            mockMvc.perform(get("/customers"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("customer-list"))
                    .andExpect(model().attributeExists("customerPage"))
                    .andExpect(model().attribute("customerPage", customerPage));

            verify(customerService, times(1)).getAllCustomersWithPagination(any());
        }

        @Test
        @DisplayName("GET /customers: 顧客一覧を表示する（ページネーションパラメータ指定）")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowCustomers_withPagination() throws Exception {
            Customer customer = new Customer(
                "john.doe@example.com",
                "password123",
                "John Doe",
                LocalDate.of(2023, 3, 1),
                LocalDate.of(1990, 5, 20),
                "123-456-7890",
                "123 Main St",
                Customer.Role.USER
            );
            Page<Customer> customerPage = new PageImpl<>(Arrays.asList(customer), PageRequest.of(1, 5), 15);
            when(customerService.getAllCustomersWithPagination(any())).thenReturn(customerPage);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("page", "1");
            params.add("size", "5");
            mockMvc.perform(get("/customers").params(params))
                    .andExpect(status().isOk())
                    .andExpect(view().name("customer-list"))
                    .andExpect(model().attributeExists("customerPage"))
                    .andExpect(model().attribute("customerPage", customerPage));

            verify(customerService, times(1)).getAllCustomersWithPagination(any());
        }

        @Test
        @DisplayName("GET /customers/search: 顧客を検索できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchCustomers() throws Exception {
            Customer customer = new Customer(
                "john.doe@example.com",
                "password123",
                "John Doe",
                LocalDate.of(2023, 3, 1),
                LocalDate.of(1990, 5, 20),
                "123-456-7890",
                "123 Main St",
                Customer.Role.USER
            );
            Page<Customer> customerPage = new PageImpl<>(Arrays.asList(customer), PageRequest.of(0, 10), 1);
            when(customerService.searchCustomersWithPagination(eq("John"), eq("john@example.com"), any())).thenReturn(customerPage);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("name", "John");
            params.add("email", "john@example.com");
            mockMvc.perform(get("/customers/search").params(params))
                    .andExpect(status().isOk())
                    .andExpect(view().name("customer-list"))
                    .andExpect(model().attributeExists("customerPage"))
                    .andExpect(model().attribute("customerPage", customerPage));

            verify(customerService, times(1)).searchCustomersWithPagination(eq("John"), eq("john@example.com"), any());
        }

        @Test
        @DisplayName("GET /customers/search: 顧客を検索できる（ページネーションパラメータ指定）")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchCustomers_withPagination() throws Exception {
            Customer customer = new Customer(
                "john.doe@example.com",
                "password123",
                "John Doe",
                LocalDate.of(2023, 3, 1),
                LocalDate.of(1990, 5, 20),
                "123-456-7890",
                "123 Main St",
                Customer.Role.USER
            );
            Page<Customer> customerPage = new PageImpl<>(Arrays.asList(customer), PageRequest.of(1, 5), 10);
            when(customerService.searchCustomersWithPagination(eq("John"), eq(null), any())).thenReturn(customerPage);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("name", "John");
            params.add("page", "1");
            params.add("size", "5");
            mockMvc.perform(get("/customers/search").params(params))
                    .andExpect(status().isOk())
                    .andExpect(view().name("customer-list"))
                    .andExpect(model().attributeExists("customerPage"))
                    .andExpect(model().attribute("customerPage", customerPage));

            verify(customerService, times(1)).searchCustomersWithPagination(eq("John"), eq(null), any());
        }

        @Test
        @DisplayName("GET /customers/search: 検索条件なしで全件取得")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchCustomers_検索条件なし() throws Exception {
            Customer customer1 = new Customer(
                "john.doe@example.com",
                "password123",
                "John Doe",
                LocalDate.of(2023, 3, 1),
                LocalDate.of(1990, 5, 20),
                "123-456-7890",
                "123 Main St",
                Customer.Role.USER
            );
            Customer customer2 = new Customer(
                "jane.doe@example.com",
                "password456",
                "Jane Doe",
                LocalDate.of(2023, 3, 2),
                LocalDate.of(1992, 7, 15),
                "987-654-3210",
                "456 Elm St",
                Customer.Role.USER
            );
            Page<Customer> customerPage = new PageImpl<>(Arrays.asList(customer1, customer2), PageRequest.of(0, 10), 2);
            when(customerService.searchCustomersWithPagination(eq(null), eq(null), any())).thenReturn(customerPage);

            mockMvc.perform(get("/customers/search"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("customer-list"))
                    .andExpect(model().attributeExists("customerPage"))
                    .andExpect(model().attribute("customerPage", customerPage));

            verify(customerService, times(1)).searchCustomersWithPagination(eq(null), eq(null), any());
        }

        @Test
        @DisplayName("GET /customers/search: 検索結果がない場合")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchCustomers_検索結果なし() throws Exception {
            Page<Customer> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 10), 0);
            when(customerService.searchCustomersWithPagination(eq("NonExistent"), eq("nonexistent@example.com"), any())).thenReturn(emptyPage);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("name", "NonExistent");
            params.add("email", "nonexistent@example.com");
            mockMvc.perform(get("/customers/search").params(params))
                    .andExpect(status().isOk())
                    .andExpect(view().name("customer-list"))
                    .andExpect(model().attributeExists("customerPage"))
                    .andExpect(model().attribute("customerPage", emptyPage));

            verify(customerService, times(1)).searchCustomersWithPagination(eq("NonExistent"), eq("nonexistent@example.com"), any());
        }

        @Test
        @DisplayName("GET /customers/export: CSVをエクスポートできる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testExportCustomersToCSV() throws Exception {
            String csvContent = "\uFEFFEmail,Name,Registration Date,Birth Date,Phone Number,Address\n" +
                               "\"test@example.com\",\"Test User\",\"2023-01-01\",\"1990-01-01\",\"123-4567\",\"Address\"";
            byte[] csvData = csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            when(customerService.exportCustomersToCSV(eq(null), eq(null), any())).thenReturn(csvData);

            mockMvc.perform(get("/customers/export"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                    .andExpect(header().exists("Content-Disposition"))
                    .andExpect(content().bytes(csvData));

            verify(customerService, times(1)).exportCustomersToCSV(eq(null), eq(null), any());
        }

        @Test
        @DisplayName("GET /customers/export: 検索条件付きでCSVをエクスポートできる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testExportCustomersToCSV_WithSearchConditions() throws Exception {
            String csvContent = "\uFEFFEmail,Name,Registration Date,Birth Date,Phone Number,Address\n" +
                               "\"alice@example.com\",\"Alice\",\"2023-01-01\",\"1990-01-01\",\"111-1111\",\"Address1\"";
            byte[] csvData = csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            when(customerService.exportCustomersToCSV(eq("Alice"), eq(null), any())).thenReturn(csvData);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("name", "Alice");
            mockMvc.perform(get("/customers/export").params(params))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                    .andExpect(content().bytes(csvData));

            verify(customerService, times(1)).exportCustomersToCSV(eq("Alice"), eq(null), any());
        }

        @Test
        @DisplayName("GET /customers/export: ソート条件付きでCSVをエクスポートできる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testExportCustomersToCSV_WithSort() throws Exception {
            String csvContent = "\uFEFFEmail,Name,Registration Date,Birth Date,Phone Number,Address\n" +
                               "\"alice@example.com\",\"Alice\",\"2023-01-01\",\"1990-01-01\",\"111-1111\",\"Address1\"";
            byte[] csvData = csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            when(customerService.exportCustomersToCSV(eq(null), eq(null), any())).thenReturn(csvData);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("sort", "name,asc");
            mockMvc.perform(get("/customers/export").params(params))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"));

            verify(customerService, times(1)).exportCustomersToCSV(eq(null), eq(null), any());
        }

        @Test
        @DisplayName("GET /customers/export: Content-Dispositionヘッダーにファイル名が含まれる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testExportCustomersToCSV_CheckFilename() throws Exception {
            byte[] csvData = "test".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            when(customerService.exportCustomersToCSV(eq(null), eq(null), any())).thenReturn(csvData);

            mockMvc.perform(get("/customers/export"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", 
                        org.hamcrest.Matchers.containsString("filename=\"customers_")))
                    .andExpect(header().string("Content-Disposition", 
                        org.hamcrest.Matchers.containsString(".csv\"")));

            verify(customerService, times(1)).exportCustomersToCSV(eq(null), eq(null), any());
        }

        @Test
        @DisplayName("GET /customers/{email}: 顧客詳細を表示する")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowCustomerDetail() throws Exception {
            Customer customer = new Customer(
                "john.doe@example.com",
                "password123",
                "John Doe",
                LocalDate.of(2023, 3, 1),
                LocalDate.of(1990, 5, 20),
                "123-456-7890",
                "123 Main St",
                Customer.Role.USER
            );
            when(customerService.getCustomerByEmail("john.doe@example.com")).thenReturn(customer);

            mockMvc.perform(get("/customers/{email}", "john.doe@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("customer-detail"))
                    .andExpect(model().attributeExists("customer"))
                    .andExpect(model().attribute("customer", customer));

            verify(customerService, times(1)).getCustomerByEmail("john.doe@example.com");
        }

        @Test
        @DisplayName("GET /customers/{email}: 存在しないメールアドレスの場合、エラー画面を表示する")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowCustomerDetail_NotFound() throws Exception {
            when(customerService.getCustomerByEmail("nonexistent@example.com"))
                .thenThrow(new CustomerNotFoundException("nonexistent@example.com"));

            mockMvc.perform(get("/customers/{email}", "nonexistent@example.com"))
                    .andExpect(status().isNotFound())
                    .andExpect(view().name("error"))
                    .andExpect(model().attributeExists("errorMessage"))
                    .andExpect(model().attribute("errorCode", "404"));

            verify(customerService, times(1)).getCustomerByEmail("nonexistent@example.com");
        }

        @Test
        @DisplayName("GET /customers?sort=name,asc: 名前で昇順ソートできる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowCustomers_SortByNameAsc() throws Exception {
            Customer customer1 = new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1", Customer.Role.USER);
            Customer customer2 = new Customer("bob@example.com", "password", "Bob", LocalDate.of(2023, 2, 2), LocalDate.of(1992, 2, 2), "222-2222", "Address2", Customer.Role.USER);
            Page<Customer> customerPage = new PageImpl<>(Arrays.asList(customer1, customer2), 
                PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("name").ascending()), 2);
            when(customerService.getAllCustomersWithPagination(any())).thenReturn(customerPage);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("sort", "name,asc");
            mockMvc.perform(get("/customers").params(params))
                    .andExpect(status().isOk())
                    .andExpect(view().name("customer-list"))
                    .andExpect(model().attributeExists("customerPage"));

            verify(customerService, times(1)).getAllCustomersWithPagination(any());
        }

        @Test
        @DisplayName("GET /customers?sort=registrationDate,desc: 登録日で降順ソートできる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowCustomers_SortByRegistrationDateDesc() throws Exception {
            Customer customer1 = new Customer("bob@example.com", "password", "Bob", LocalDate.of(2023, 2, 2), LocalDate.of(1992, 2, 2), "222-2222", "Address2", Customer.Role.USER);
            Customer customer2 = new Customer("alice@example.com", "password", "Alice", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1", Customer.Role.USER);
            Page<Customer> customerPage = new PageImpl<>(Arrays.asList(customer1, customer2), 
                PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("registrationDate").descending()), 2);
            when(customerService.getAllCustomersWithPagination(any())).thenReturn(customerPage);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("sort", "registrationDate,desc");
            mockMvc.perform(get("/customers").params(params))
                    .andExpect(status().isOk())
                    .andExpect(view().name("customer-list"))
                    .andExpect(model().attributeExists("customerPage"));

            verify(customerService, times(1)).getAllCustomersWithPagination(any());
        }

        @Test
        @DisplayName("GET /customers/search?sort=email,asc: 検索結果をメールアドレスで昇順ソートできる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchCustomers_SortByEmailAsc() throws Exception {
            Customer customer1 = new Customer("alice@example.com", "password", "Alice Test", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "111-1111", "Address1", Customer.Role.USER);
            Customer customer2 = new Customer("bob@example.com", "password", "Bob Test", LocalDate.of(2023, 2, 2), LocalDate.of(1992, 2, 2), "222-2222", "Address2", Customer.Role.USER);
            Page<Customer> customerPage = new PageImpl<>(Arrays.asList(customer1, customer2), 
                PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("email").ascending()), 2);
            when(customerService.searchCustomersWithPagination(anyString(), any(), any())).thenReturn(customerPage);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("name", "test");
            params.add("sort", "email,asc");
            mockMvc.perform(get("/customers/search").params(params))
                    .andExpect(status().isOk())
                    .andExpect(view().name("customer-list"))
                    .andExpect(model().attributeExists("customerPage"));

            verify(customerService, times(1)).searchCustomersWithPagination(anyString(), any(), any());
        }

        @Test
        @DisplayName("GET /customers?page=1&sort=name,asc: ページネーションとソートが正しく連携する")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowCustomers_PaginationWithSort() throws Exception {
            Customer customer = new Customer("test@example.com", "password", "Test User", LocalDate.of(2023, 1, 1), LocalDate.of(1990, 1, 1), "123-4567", "Address", Customer.Role.USER);
            Page<Customer> customerPage = new PageImpl<>(Arrays.asList(customer), 
                PageRequest.of(1, 10, org.springframework.data.domain.Sort.by("name").ascending()), 15);
            when(customerService.getAllCustomersWithPagination(any())).thenReturn(customerPage);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("page", "1");
            params.add("sort", "name,asc");
            mockMvc.perform(get("/customers").params(params))
                    .andExpect(status().isOk())
                    .andExpect(view().name("customer-list"))
                    .andExpect(model().attributeExists("customerPage"));

            verify(customerService, times(1)).getAllCustomersWithPagination(any());
        }
    }
    // --- 認可テスト ---
    @Nested
    @DisplayName("認可制御（ロールごと）")
    class AuthorizationTest {

        @Test
        @DisplayName("ADMINロールは顧客一覧にアクセスできる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void adminCanAccessCustomers() throws Exception {
            when(customerService.getAllCustomersWithPagination(any())).thenReturn(new PageImpl<>(emptyList()));
            mockMvc.perform(get("/customers"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("USERロールは顧客一覧にアクセスできず403")
        @WithMockUser(username = "user@example.com", roles = "USER")
        void userCannotAccessCustomers() throws Exception {
            mockMvc.perform(get("/customers"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("未認証は顧客一覧にアクセスできずリダイレクト")
        @WithAnonymousUser
        void anonymousCannotAccessCustomers() throws Exception {
            mockMvc.perform(get("/customers"))
                .andExpect(status().is3xxRedirection());
        }

        @Test
        @DisplayName("ADMINロールは顧客検索にアクセスできる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void adminCanAccessCustomersSearch() throws Exception {
            when(customerService.searchCustomersWithPagination(any(), any(), any())).thenReturn(new PageImpl<>(emptyList()));
            mockMvc.perform(get("/customers/search"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("USERロールは顧客検索にアクセスできず403")
        @WithMockUser(username = "user@example.com", roles = "USER")
        void userCannotAccessCustomersSearch() throws Exception {
            mockMvc.perform(get("/customers/search"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("未認証は顧客検索にアクセスできずリダイレクト")
        @WithAnonymousUser
        void anonymousCannotAccessCustomersSearch() throws Exception {
            mockMvc.perform(get("/customers/search"))
                .andExpect(status().is3xxRedirection());
        }
    }
}