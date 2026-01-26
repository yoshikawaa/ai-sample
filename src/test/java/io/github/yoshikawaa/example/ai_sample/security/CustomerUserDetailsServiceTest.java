package io.github.yoshikawaa.example.ai_sample.security;

import io.github.yoshikawaa.example.ai_sample.model.Customer;
import io.github.yoshikawaa.example.ai_sample.repository.CustomerRepository;
import io.github.yoshikawaa.example.ai_sample.service.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@DisplayName("CustomerUserDetailsService のテスト")
class CustomerUserDetailsServiceTest {

    @Autowired
    private CustomerUserDetailsService customerUserDetailsService;

    @MockitoBean
    private CustomerRepository customerRepository;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

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
    void testLoadUserByUsername_Success() {
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
    @DisplayName("ロック状態のユーザーはisAccountNonLockedがfalseを返す")
    void testLoadUserByUsername_Locked() {
        // given
        Customer lockedCustomer = new Customer(
            "locked@example.com",
            "encodedPassword",
            "Locked User",
            LocalDate.now(),
            LocalDate.of(1990, 1, 1),
            "123-456-7890",
            "123 Test St"
        );
        when(customerRepository.findByEmail("locked@example.com"))
            .thenReturn(Optional.of(lockedCustomer));
        when(loginAttemptService.isLocked("locked@example.com")).thenReturn(true);

        // when
        UserDetails loaded = customerUserDetailsService.loadUserByUsername("locked@example.com");
        CustomerUserDetails userDetails = (CustomerUserDetails) loaded;

        // then
        assertThat(userDetails.isAccountNonLocked()).isFalse();
    }
}
