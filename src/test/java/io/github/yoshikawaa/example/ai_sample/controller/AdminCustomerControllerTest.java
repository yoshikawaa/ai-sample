package io.github.yoshikawaa.example.ai_sample.controller;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.nio.charset.StandardCharsets;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import io.github.yoshikawaa.example.ai_sample.config.GlobalExceptionHandler;
import io.github.yoshikawaa.example.ai_sample.config.SecurityConfig;
import io.github.yoshikawaa.example.ai_sample.exception.CustomerNotFoundException;
import io.github.yoshikawaa.example.ai_sample.exception.UnderageCustomerException;
import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.service.ActivityTimelineService;
import io.github.yoshikawaa.example.ai_sample.service.CustomerService;
import io.github.yoshikawaa.example.ai_sample.service.LoginAttemptService;
import io.github.yoshikawaa.example.ai_sample.service.LoginHistoryService;
import io.github.yoshikawaa.example.ai_sample.service.PasswordResetService;

@WebMvcTest(AdminCustomerController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class}) // セキュリティ設定とグローバルエラーハンドラーをインポート
@DisplayName("AdminCustomerController のテスト")
class AdminCustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    @MockitoBean
    private LoginHistoryService loginHistoryService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private ActivityTimelineService activityTimelineService;

    // ========================================
    // 顧客一覧・検索・詳細・CSV出力
    // ========================================

    @Nested
    @DisplayName("顧客一覧・検索・詳細・CSV出力")
    class CustomerListAndSearchTest {

        @Test
        @DisplayName("GET /admin/customers: 顧客一覧を表示する（デフォルトページネーション）")
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

            mockMvc.perform(get("/admin/customers"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-customer-list"))
                    .andExpect(model().attributeExists("customerPage"))
                    .andExpect(model().attribute("customerPage", customerPage));

            verify(customerService, times(1)).getAllCustomersWithPagination(any());
        }

        @Test
        @DisplayName("GET /admin/customers/search: 顧客を検索できる")
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
            mockMvc.perform(get("/admin/customers/search").params(params))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-customer-list"))
                    .andExpect(model().attributeExists("customerPage"))
                    .andExpect(model().attribute("customerPage", customerPage));

            verify(customerService, times(1)).searchCustomersWithPagination(eq("John"), eq("john@example.com"), any());
        }

        @Test
        @DisplayName("GET /admin/customers/search: 検索条件なしで全件取得")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchCustomers_NoConditions() throws Exception {
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

            mockMvc.perform(get("/admin/customers/search"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-customer-list"))
                    .andExpect(model().attributeExists("customerPage"))
                    .andExpect(model().attribute("customerPage", customerPage));

            verify(customerService, times(1)).searchCustomersWithPagination(eq(null), eq(null), any());
        }

        @Test
        @DisplayName("GET /admin/customers/export: CSVをエクスポートできる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testExportCustomersToCSV() throws Exception {
            String csvContent = "\uFEFFEmail,Name,Registration Date,Birth Date,Phone Number,Address\n" +
                            "\"test@example.com\",\"Test User\",\"2023-01-01\",\"1990-01-01\",\"123-4567\",\"Address\"";
            byte[] csvData = csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            when(customerService.exportCustomersToCSV(eq(null), eq(null), any())).thenReturn(csvData);

            mockMvc.perform(get("/admin/customers/export"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                    .andExpect(header().exists("Content-Disposition"))
                    .andExpect(content().bytes(csvData));

            verify(customerService, times(1)).exportCustomersToCSV(eq(null), eq(null), any());
        }

        @Test
        @DisplayName("GET /admin/customers/{email}: 顧客詳細を表示する")
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
            when(loginAttemptService.isLocked("john.doe@example.com")).thenReturn(false);

            mockMvc.perform(get("/admin/customers/{email}", "john.doe@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-customer-detail"))
                    .andExpect(model().attributeExists("customer"))
                    .andExpect(model().attribute("customer", customer));

            verify(customerService, times(1)).getCustomerByEmail("john.doe@example.com");
        }

        @Test
        @DisplayName("GET /admin/customers/{email}: 存在しないメールアドレスの場合、エラー画面を表示する")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowCustomerDetail_NotFound() throws Exception {
            when(customerService.getCustomerByEmail("nonexistent@example.com"))
                .thenThrow(new CustomerNotFoundException("nonexistent@example.com"));

            mockMvc.perform(get("/admin/customers/{email}", "nonexistent@example.com"))
                    .andExpect(status().isNotFound())
                    .andExpect(view().name("error"));

            verify(customerService, times(1)).getCustomerByEmail("nonexistent@example.com");
        }

        @Test
        @DisplayName("GET /admin/customers: ページネーション指定で顧客一覧が表示される")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowCustomers_withPagination() throws Exception {
            Customer customer1 = new Customer(
                "alice@example.com",
                "password",
                "Alice",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(1990, 1, 1),
                "12345678",
                "Address 1",
                Customer.Role.USER
                );
            Customer customer2 = new Customer(
                "bob@example.com",
                "password",
                "Bob",
                LocalDate.of(2023, 2, 2),
                LocalDate.of(1991, 2, 2),
                "87654321",
                "Address 2",
                Customer.Role.USER
                );

            Page<Customer> customerPage = new PageImpl<>(Arrays.asList(customer1, customer2),
                    PageRequest.of(1, 5), 12);
            when(customerService.getAllCustomersWithPagination(any())).thenReturn(customerPage);

            mockMvc.perform(get("/admin/customers")
                            .param("page", "1")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-customer-list"))
                    .andExpect(model().attribute("customerPage", customerPage));
        }

        @Test
        @DisplayName("GET /admin/customers/search: ページネーション指定で検索結果が表示される")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchCustomers_withPagination() throws Exception {
            Customer customer1 = new Customer(
                "alice@example.com",
                "password",
                "Alice",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(1990, 1, 1),
                "12345678",
                "Address 1",
                Customer.Role.USER
                );
            Page<Customer> customerPage = new PageImpl<>(Arrays.asList(customer1),
                    PageRequest.of(0, 5), 1);

            when(customerService.searchCustomersWithPagination(eq("Alice"), any(), any())).thenReturn(customerPage);

            mockMvc.perform(get("/admin/customers/search")
                            .param("name", "Alice")
                            .param("page", "0")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-customer-list"))
                    .andExpect(model().attribute("customerPage", customerPage));
        }

        @Test
        @DisplayName("GET /admin/customers/search: 検索結果が0件の場合")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchCustomers_NoResults() throws Exception {
            Page<Customer> emptyPage = new PageImpl<>(emptyList());
            when(customerService.searchCustomersWithPagination(eq("NonExistent"), any(), any())).thenReturn(emptyPage);

            mockMvc.perform(get("/admin/customers/search")
                            .param("name", "NonExistent"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-customer-list"))
                    .andExpect(model().attribute("customerPage", emptyPage));
        }

        @Test
        @DisplayName("GET /admin/customers: 名前で昇順ソート")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowCustomers_SortByNameAsc() throws Exception {
            Customer alice = new Customer(
                "alice@example.com",
                "password",
                "Alice",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(1990, 1, 1),
                "12345678",
                "Address 1",
                Customer.Role.USER
                );
            Customer bob = new Customer(
                "bob@example.com",
                "password",
                "Bob",
                LocalDate.of(2023, 2, 2),
                LocalDate.of(1991, 2, 2),
                "87654321",
                "Address 2",
                Customer.Role.USER
                );

            Page<Customer> customerPage = new PageImpl<>(Arrays.asList(alice, bob));
            when(customerService.getAllCustomersWithPagination(any())).thenReturn(customerPage);

            mockMvc.perform(get("/admin/customers")
                            .param("sort", "name,asc"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-customer-list"))
                    .andExpect(model().attribute("customerPage", customerPage));
        }

        @Test
        @DisplayName("GET /admin/customers: 登録日で降順ソート")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowCustomers_SortByRegistrationDateDesc() throws Exception {
            Customer customer1 = new Customer(
                "bob@example.com",
                "password",
                "Bob",
                LocalDate.of(2023, 2, 2),
                LocalDate.of(1991, 2, 2),
                "87654321",
                "Address 2",
                Customer.Role.USER
                );
            Customer customer2 = new Customer(
                "alice@example.com",
                "password",
                "Alice",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(1990, 1, 1),
                "12345678",
                "Address 1",
                Customer.Role.USER
                );

            Page<Customer> customerPage = new PageImpl<>(Arrays.asList(customer1, customer2));
            when(customerService.getAllCustomersWithPagination(any())).thenReturn(customerPage);

            mockMvc.perform(get("/admin/customers")
                            .param("sort", "registrationDate,desc"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-customer-list"));
        }

        @Test
        @DisplayName("GET /admin/customers/search: メールアドレスで昇順ソート")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchCustomers_SortByEmailAsc() throws Exception {
            Customer alice = new Customer(
                "alice@example.com",
                "password",
                "Alice",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(1990, 1, 1),
                "12345678",
                "Address 1",
                Customer.Role.USER
                );
            Customer bob = new Customer(
                "bob@example.com",
                "password",
                "Bob",
                LocalDate.of(2023, 2, 2),
                LocalDate.of(1991, 2, 2),
                "87654321",
                "Address 2",
                Customer.Role.USER
                );

            Page<Customer> customerPage = new PageImpl<>(Arrays.asList(alice, bob));
            when(customerService.searchCustomersWithPagination(any(), any(), any())).thenReturn(customerPage);

            mockMvc.perform(get("/admin/customers/search")
                            .param("sort", "email,asc"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-customer-list"));
        }

        @Test
        @DisplayName("GET /admin/customers: ページネーション＋ソート併用")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowCustomers_PaginationWithSort() throws Exception {
            Customer customer = new Customer(
                "alice@example.com",
                "password",
                "Alice",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(1990, 1, 1),
                "12345678",
                "Address 1",
                Customer.Role.USER
                );
            Page<Customer> customerPage = new PageImpl<>(Arrays.asList(customer),
                    PageRequest.of(0, 5), 10);
            when(customerService.getAllCustomersWithPagination(any())).thenReturn(customerPage);

            mockMvc.perform(get("/admin/customers")
                            .param("page", "0")
                            .param("size", "5")
                            .param("sort", "name,asc"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-customer-list"));
        }

        @Test
        @DisplayName("GET /admin/customers/export: 検索条件付きCSVエクスポート")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testExportCustomersToCSV_WithSearchConditions() throws Exception {
            byte[] csvData = "Email,Name\nalice@example.com,Alice\n".getBytes(StandardCharsets.UTF_8);
            when(customerService.exportCustomersToCSV(eq("Alice"), any(), any())).thenReturn(csvData);

            mockMvc.perform(get("/admin/customers/export")
                            .param("name", "Alice"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                    .andExpect(header().exists("Content-Disposition"));
        }

        @Test
        @DisplayName("GET /admin/customers/export: ソート付きCSVエクスポート")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testExportCustomersToCSV_WithSort() throws Exception {
            byte[] csvData = "Email,Name\nalice@example.com,Alice\nbob@example.com,Bob\n".getBytes(StandardCharsets.UTF_8);
            when(customerService.exportCustomersToCSV(any(), any(), any())).thenReturn(csvData);

            mockMvc.perform(get("/admin/customers/export")
                            .param("sort", "email,asc"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"));
        }

        @Test
        @DisplayName("GET /admin/customers/export: CSVファイル名にタイムスタンプが含まれる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testExportCustomersToCSV_CheckFilename() throws Exception {
            byte[] csvData = "Email,Name\n".getBytes(StandardCharsets.UTF_8);
            when(customerService.exportCustomersToCSV(any(), any(), any())).thenReturn(csvData);

            MvcResult result = mockMvc.perform(get("/admin/customers/export"))
                    .andExpect(status().isOk())
                    .andReturn();

            String contentDisposition = result.getResponse().getHeader("Content-Disposition");
            assertThat(contentDisposition).matches("form-data; name=\"attachment\"; filename=\"customers_\\d{8}_\\d{6}\\.csv\"");
        }
    }

    // ========================================
    // 顧客新規登録（管理者による代理登録）
    // ========================================

    @Nested
    @DisplayName("顧客新規登録（管理者による代理登録）")
    class CustomerRegistrationTest {

        @Test
        @DisplayName("GET /admin/customers/registration-input: 登録入力画面を表示する（管理者のみ）")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowRegistrationInputForm() throws Exception {
        mockMvc.perform(get("/admin/customers/registration-input"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-customer-registration-input"))
                .andExpect(model().attributeExists("adminCustomerRegistrationForm"));
        }

        @Test
        @DisplayName("POST /admin/customers/registration-confirm: 登録確認画面を表示する")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowRegistrationConfirmForm() throws Exception {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("email", "newcustomer@example.com");
            params.add("password", "Password123");
            params.add("confirmPassword", "Password123");
            params.add("name", "New Customer");
            params.add("birthDate", "2000-01-01");
            params.add("phoneNumber", "123-456-7890");
            params.add("address", "123 Main St");
            params.add("role", "USER");

            mockMvc.perform(post("/admin/customers/registration-confirm")
                    .params(params)
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-customer-registration-confirm"));
        }

        @Test
        @DisplayName("POST /admin/customers/registration-confirm: バリデーションエラーの場合、入力画面に戻る")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowRegistrationConfirmForm_ValidationError() throws Exception {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("email", "invalid-email");
            params.add("password", "short");
            params.add("confirmPassword", "different");
            params.add("name", "");
            params.add("birthDate", "2000-01-01");
            params.add("phoneNumber", "");
            params.add("address", "");
            params.add("role", "USER");

            mockMvc.perform(post("/admin/customers/registration-confirm")
                    .params(params)
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-customer-registration-input"));
        }

        @Test
        @DisplayName("POST /admin/customers/registration: 顧客を登録できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testRegisterCustomer() throws Exception {
            doNothing().when(customerService).registerCustomer(any(Customer.class));

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("email", "newcustomer@example.com");
            params.add("password", "Password123");
            params.add("confirmPassword", "Password123");
            params.add("name", "New Customer");
            params.add("birthDate", "2000-01-01");
            params.add("phoneNumber", "123-456-7890");
            params.add("address", "123 Main St");
            params.add("role", "USER");

            mockMvc.perform(post("/admin/customers/registration")
                    .params(params)
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/customers/registration-complete"));

            verify(customerService, times(1)).registerCustomer(any(Customer.class));
        }

        @Test
        @DisplayName("POST /admin/customers/registration: バリデーションエラーの場合、入力画面に戻る")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testRegisterCustomer_ValidationError() throws Exception {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("email", "invalid-email");
            params.add("password", "short");
            params.add("confirmPassword", "different");
            params.add("name", "");
            params.add("birthDate", "2000-01-01");
            params.add("phoneNumber", "");
            params.add("address", "");
            params.add("role", "USER");

            mockMvc.perform(post("/admin/customers/registration")
                    .params(params)
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-customer-registration-input"));
        }

        @Test
        @DisplayName("POST /admin/customers/registration: 未成年の顧客を登録しようとした場合、エラー画面を表示する")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testRegisterCustomer_UnderageError() throws Exception {
            doThrow(new UnderageCustomerException())
                    .when(customerService).registerCustomer(any(Customer.class));

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("email", "youngcustomer@example.com");
            params.add("password", "Password123");
            params.add("confirmPassword", "Password123");
            params.add("name", "Young Customer");
            params.add("birthDate", LocalDate.now().minusYears(10).toString());
            params.add("phoneNumber", "123-456-7890");
            params.add("address", "123 Main St");
            params.add("role", "USER");

            mockMvc.perform(post("/admin/customers/registration")
                    .params(params)
                    .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(view().name("admin-customer-registration-error"))
                    .andExpect(model().attributeExists("errorMessage"));
        }

        @Test
        @DisplayName("GET /admin/customers/registration-complete: 登録完了画面を表示する")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowRegistrationCompletePage() throws Exception {
            mockMvc.perform(get("/admin/customers/registration-complete"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-customer-registration-complete"));
        }

        @Test
        @DisplayName("POST /admin/customers/registration-input: 確認画面からBackボタンで入力画面に戻れる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testHandleBackToRegistrationInput() throws Exception {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("email", "newcustomer@example.com");
            params.add("password", "Password123");
            params.add("confirmPassword", "Password123");
            params.add("name", "New Customer");
            params.add("birthDate", "2000-01-01");
            params.add("phoneNumber", "123-456-7890");
            params.add("address", "123 Main St");
            params.add("role", "USER");

            mockMvc.perform(post("/admin/customers/registration-input")
                    .params(params)
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-customer-registration-input"));
        }
    }

    // ========================================
    // 顧客編集機能
    // ========================================

    @Nested
    @DisplayName("顧客編集機能")
    class CustomerEditTest {

        @Test
        @DisplayName("GET /admin/customers/{email}/edit-input: 編集入力画面を表示する")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowEditInputForm() throws Exception {
        Customer customer = new Customer(
                "test@example.com",
                "password",
                "Test Customer",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(1990, 5, 20),
                "123-456-7890",
                "123 Main St",
                Customer.Role.USER
            );
        when(customerService.getCustomerByEmail("test@example.com")).thenReturn(customer);

        mockMvc.perform(get("/admin/customers/test@example.com/edit-input"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-customer-edit-input"))
                .andExpect(model().attributeExists("adminCustomerEditForm"))
                .andExpect(model().attribute("email", "test@example.com"));
        }

        @Test
        @DisplayName("POST /admin/customers/{email}/edit: 顧客情報を更新できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testUpdateCustomer() throws Exception {
            Customer customer = new Customer(
                    "test@example.com",
                    "password",
                    "Test Customer",
                    LocalDate.of(2023, 1, 1),
                    LocalDate.of(1990, 5, 20),
                    "123-456-7890",
                    "123 Main St",
                    Customer.Role.USER
                );
            when(customerService.getCustomerByEmail("test@example.com")).thenReturn(customer);
            doNothing().when(customerService).updateCustomerInfo(any(Customer.class));

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("name", "Updated Name");
            params.add("birthDate", "1990-05-20");
            params.add("phoneNumber", "987-654-3210");
            params.add("address", "456 Elm St");
            params.add("role", "ADMIN");

            mockMvc.perform(post("/admin/customers/test@example.com/edit")
                    .params(params)
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-customer-edit-complete"));

            verify(customerService, times(1)).updateCustomerInfo(any(Customer.class));
        }

        @Test
        @DisplayName("POST /admin/customers/{email}/edit-confirm: 編集確認画面を表示する")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowEditConfirmForm() throws Exception {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("name", "Updated Name");
            params.add("birthDate", "1990-05-20");
            params.add("phoneNumber", "987-654-3210");
            params.add("address", "456 Elm St");
            params.add("role", "ADMIN");

            mockMvc.perform(post("/admin/customers/test@example.com/edit-confirm")
                    .params(params)
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-customer-edit-confirm"))
                    .andExpect(model().attribute("email", "test@example.com"));
        }

        @Test
        @DisplayName("POST /admin/customers/{email}/edit-confirm: バリデーションエラーの場合、入力画面に戻る")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowEditConfirmForm_ValidationError() throws Exception {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("name", "");
            params.add("birthDate", "1990-05-20");
            params.add("phoneNumber", "");
            params.add("address", "");
            params.add("role", "ADMIN");

            mockMvc.perform(post("/admin/customers/test@example.com/edit-confirm")
                    .params(params)
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-customer-edit-input"))
                    .andExpect(model().attribute("email", "test@example.com"));
        }

        @Test
        @DisplayName("POST /admin/customers/{email}/edit-input: 確認画面からBackボタンで入力画面に戻れる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testHandleBackToEditInput() throws Exception {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("name", "Updated Name");
            params.add("birthDate", "1990-05-20");
            params.add("phoneNumber", "987-654-3210");
            params.add("address", "456 Elm St");
            params.add("role", "ADMIN");

            mockMvc.perform(post("/admin/customers/test@example.com/edit-input")
                    .params(params)
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-customer-edit-input"))
                    .andExpect(model().attribute("email", "test@example.com"));
        }

        @Test
        @DisplayName("POST /admin/customers/{email}/edit: バリデーションエラーの場合、入力画面に戻る")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testUpdateCustomer_ValidationError() throws Exception {
            Customer customer = new Customer(
                    "test@example.com",
                    "password",
                    "Test Customer",
                    LocalDate.of(2023, 1, 1),
                    LocalDate.of(1990, 5, 20),
                    "123-456-7890",
                    "123 Main St",
                    Customer.Role.USER
                );
            when(customerService.getCustomerByEmail("test@example.com")).thenReturn(customer);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("name", "");
            params.add("birthDate", "1990-05-20");
            params.add("phoneNumber", "");
            params.add("address", "");
            params.add("role", "ADMIN");

            mockMvc.perform(post("/admin/customers/test@example.com/edit")
                    .params(params)
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-customer-edit-input"))
                    .andExpect(model().attribute("email", "test@example.com"));
        }
    }

    // ========================================
    // 顧客削除機能
    // ========================================

    @Nested
    @DisplayName("顧客削除機能")
    class CustomerDeleteTest {

        @Test
        @DisplayName("GET /admin/customers/{email}/delete-confirm: 削除確認画面を表示する")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowDeleteConfirmForm() throws Exception {
        Customer customer = new Customer(
                "test@example.com",
                "password",
                "Test Customer",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(1990, 5, 20),
                "123-456-7890",
                "123 Main St",
                Customer.Role.USER
            );
        when(customerService.getCustomerByEmail("test@example.com")).thenReturn(customer);

        mockMvc.perform(get("/admin/customers/test@example.com/delete-confirm"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-customer-delete-confirm"))
                .andExpect(model().attributeExists("customer"));
        }

        @Test
        @DisplayName("POST /admin/customers/{email}/delete: 顧客を削除できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testDeleteCustomer() throws Exception {
            doNothing().when(customerService).deleteCustomer("test@example.com");

            mockMvc.perform(post("/admin/customers/test@example.com/delete")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-customer-delete-complete"));

            verify(customerService, times(1)).deleteCustomer("test@example.com");
        }
    }

    // ========================================
    // アカウントロック/アンロック操作
    // ========================================

    @Nested
    @DisplayName("アカウントロック/アンロック操作")
    class AccountLockTest {

        @Test
        @DisplayName("POST /admin/customers/{email}/lock: アカウントをロックできる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testLockAccount() throws Exception {
        doNothing().when(loginAttemptService).lockAccountByAdmin("test@example.com");

        mockMvc.perform(post("/admin/customers/test@example.com/lock")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-account-lock-complete"))
                .andExpect(model().attribute("email", "test@example.com"));

        verify(loginAttemptService, times(1)).lockAccountByAdmin("test@example.com");
        }

        @Test
        @DisplayName("POST /admin/customers/{email}/unlock: アカウントをアンロックできる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testUnlockAccount() throws Exception {
            doNothing().when(loginAttemptService).unlockAccountByAdmin("test@example.com");

            mockMvc.perform(post("/admin/customers/test@example.com/unlock")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-account-unlock-complete"))
                    .andExpect(model().attribute("email", "test@example.com"));

            verify(loginAttemptService, times(1)).unlockAccountByAdmin("test@example.com");
        }
    }

    // ========================================
    // パスワードリセットリンク発行
    // ========================================

    @Nested
    @DisplayName("パスワードリセットリンク発行")
    class PasswordResetTest {

        @Test
        @DisplayName("POST /admin/customers/{email}/password-reset: パスワードリセットリンクを送信できる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSendPasswordResetLink() throws Exception {
            doNothing().when(passwordResetService).sendResetLink("test@example.com");

            mockMvc.perform(post("/admin/customers/test@example.com/password-reset")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-password-reset-complete"))
                    .andExpect(model().attribute("email", "test@example.com"));

            verify(passwordResetService, times(1)).sendResetLink("test@example.com");
        }
    }

    // ========================================
    // 認可テスト
    // ========================================
    // アクティビティタイムライン
    // ========================================

    @Nested
    @DisplayName("アクティビティタイムライン")
    class ActivityTimelineTest {

        @Test
        @DisplayName("GET /{email}/activity-timeline: アクティビティタイムラインを表示する")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowActivityTimeline() throws Exception {
            String email = "test@example.com";
            Page<io.github.yoshikawaa.example.ai_sample.model.ActivityTimeline> mockPage = new PageImpl<>(
                java.util.Collections.singletonList(
                    new io.github.yoshikawaa.example.ai_sample.model.ActivityTimeline(
                        1L,
                        java.time.LocalDateTime.now(),
                        io.github.yoshikawaa.example.ai_sample.model.ActivityTimeline.ActivityType.LOGIN_SUCCESS,
                        "Login successful",
                        "Detail",
                        "192.168.1.1",
                        "SUCCESS"
                    )
                )
            );

            Customer mockCustomer = new Customer(email, "password", "Test User", LocalDate.now(), LocalDate.of(1990, 1, 1), "123-456-7890", "Test Address", Customer.Role.USER);
            when(customerService.getCustomerByEmail(email)).thenReturn(mockCustomer);
            when(activityTimelineService.getActivityTimeline(eq(email), any(), any(), any(), any())).thenReturn(mockPage);

            mockMvc.perform(get("/admin/customers/" + email + "/activity-timeline"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-customer-activity-timeline"))
                .andExpect(model().attributeExists("timelinePage"))
                .andExpect(model().attributeExists("activityTimelineSearchForm"))
                .andExpect(model().attributeExists("customer"));

            verify(activityTimelineService, times(1)).getActivityTimeline(eq(email), any(), any(), any(), any());
        }

        @Test
        @DisplayName("GET /{email}/activity-timeline: ページネーションパラメータが正しく渡される")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testShowActivityTimeline_WithPagination() throws Exception {
            String email = "test@example.com";
            Page<io.github.yoshikawaa.example.ai_sample.model.ActivityTimeline> mockPage = new PageImpl<>(emptyList());

            Customer mockCustomer = new Customer(email, "password", "Test User", LocalDate.now(), LocalDate.of(1990, 1, 1), "123-456-7890", "Test Address", Customer.Role.USER);
            when(customerService.getCustomerByEmail(email)).thenReturn(mockCustomer);
            when(activityTimelineService.getActivityTimeline(eq(email), any(), any(), any(), any())).thenReturn(mockPage);

            mockMvc.perform(get("/admin/customers/" + email + "/activity-timeline")
                    .param("page", "1")
                    .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-customer-activity-timeline"));

            verify(activityTimelineService, times(1)).getActivityTimeline(eq(email), any(), any(), any(), any());
        }

        @Test
        @DisplayName("GET /{email}/activity-timeline: フィルタ検索ができる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void testSearchActivityTimeline() throws Exception {
            String email = "test@example.com";
            Page<io.github.yoshikawaa.example.ai_sample.model.ActivityTimeline> mockPage = new PageImpl<>(emptyList());

            Customer mockCustomer = new Customer(email, "password", "Test User", LocalDate.now(), LocalDate.of(1990, 1, 1), "123-456-7890", "Test Address", Customer.Role.USER);
            when(customerService.getCustomerByEmail(email)).thenReturn(mockCustomer);
            when(activityTimelineService.getActivityTimeline(eq(email), any(), any(), any(), any())).thenReturn(mockPage);

            mockMvc.perform(get("/admin/customers/" + email + "/activity-timeline")
                    .param("startDate", "2024-01-01")
                    .param("endDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-customer-activity-timeline"));

            verify(activityTimelineService, times(1)).getActivityTimeline(eq(email), any(), any(), any(), any());
        }

        @Test
        @DisplayName("GET /{email}/activity-timeline: USER権限ではアクセス拒否される")
        @WithMockUser(username = "user@example.com", roles = "USER")
        void testShowActivityTimeline_AccessDenied() throws Exception {
            mockMvc.perform(get("/admin/customers/test@example.com/activity-timeline"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /{email}/activity-timeline: 未認証ユーザーはリダイレクトされる")
        @WithAnonymousUser
        void testShowActivityTimeline_Unauthenticated() throws Exception {
            mockMvc.perform(get("/admin/customers/test@example.com/activity-timeline"))
                .andExpect(status().is3xxRedirection());
        }
    }

    // ========================================
    // 認可制御（ロールごと）
    // ========================================

    @Nested
    @DisplayName("認可制御（ロールごと）")
    class AuthorizationTest {

        @Test
        @DisplayName("ADMINロールは顧客一覧にアクセスできる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void adminCanAccessCustomers() throws Exception {
            when(customerService.getAllCustomersWithPagination(any())).thenReturn(new PageImpl<>(emptyList()));
            mockMvc.perform(get("/admin/customers"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("USERロールは顧客一覧にアクセスできず403")
        @WithMockUser(username = "user@example.com", roles = "USER")
        void userCannotAccessCustomers() throws Exception {
            mockMvc.perform(get("/admin/customers"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("未認証は顧客一覧にアクセスできずリダイレクト")
        @WithAnonymousUser
        void anonymousCannotAccessCustomers() throws Exception {
            mockMvc.perform(get("/admin/customers"))
                .andExpect(status().is3xxRedirection());
        }

        @Test
        @DisplayName("ADMINロールは顧客検索にアクセスできる")
        @WithMockUser(username = "admin@example.com", roles = "ADMIN")
        void adminCanAccessCustomersSearch() throws Exception {
            when(customerService.searchCustomersWithPagination(any(), any(), any())).thenReturn(new PageImpl<>(emptyList()));
            mockMvc.perform(get("/admin/customers/search"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("USERロールは顧客検索にアクセスできず403")
        @WithMockUser(username = "user@example.com", roles = "USER")
        void userCannotAccessCustomersSearch() throws Exception {
            mockMvc.perform(get("/admin/customers/search"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("未認証は顧客検索にアクセスできずリダイレクト")
        @WithAnonymousUser
        void anonymousCannotAccessCustomersSearch() throws Exception {
            mockMvc.perform(get("/admin/customers/search"))
                .andExpect(status().is3xxRedirection());
        }

        @Test
        @DisplayName("GET /admin/customers/registration-input: USER権限ではアクセス拒否される")
        @WithMockUser(username = "user@example.com", roles = "USER")
        void testShowRegistrationInputForm_AccessDenied() throws Exception {
            mockMvc.perform(get("/admin/customers/registration-input"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /admin/customers/{email}/lock: 未認証ユーザーはアクセス拒否される")
        void testLockAccount_Unauthenticated() throws Exception {
            mockMvc.perform(post("/admin/customers/test@example.com/lock")
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection());
        }
    }
}
