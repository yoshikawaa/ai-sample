package io.github.yoshikawaa.example.ai_sample.security;

import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
@DisplayName("CustomerUserDetailsService のテスト")
class CustomerUserDetailsServiceTest {

    @Autowired
    private CustomerUserDetailsService customerUserDetailsService;

    @MockitoBean
    private CustomerRepository customerRepository;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer(
            "test@example.com",
            "encodedPassword",
            "Test User",
            LocalDate.now(),
            LocalDate.of(1990, 1, 1),
            "123-456-7890",
            "123 Test St"
        );
    }

    @Test
    @DisplayName("ユーザー名（メールアドレス）でユーザーを取得できる")
    void loadUserByUsername_Success() {
        // given
        when(customerRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testCustomer));

        // when
        UserDetails userDetails = customerUserDetailsService.loadUserByUsername("test@example.com");

        // then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("test@example.com");
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword");
        assertThat(userDetails).isInstanceOf(CustomerUserDetails.class);
        
        CustomerUserDetails customerUserDetails = (CustomerUserDetails) userDetails;
        assertThat(customerUserDetails.getCustomer().getName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("存在しないユーザー名の場合UsernameNotFoundExceptionをスローする")
    void loadUserByUsername_UserNotFound() {
        // given
        when(customerRepository.findByEmail("nonexistent@example.com"))
            .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> customerUserDetailsService.loadUserByUsername("nonexistent@example.com"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("User not found: nonexistent@example.com");
    }
}
