package io.github.yoshikawaa.example.ai_sample.security;

import io.github.yoshikawaa.example.ai_sample.model.Customer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustomerUserDetails のテスト")
class CustomerUserDetailsTest {

    @Test
    @DisplayName("同じemailを持つUserDetailsはequals()でtrueを返す")
    void testEquals_SameEmail() {
        Customer customer1 = new Customer("test@example.com", "password", "Test User", 
            LocalDate.of(1990, 1, 1), LocalDate.of(1990, 1, 1), "000-0000-0000", "Tokyo", Customer.Role.USER);
        Customer customer2 = new Customer("test@example.com", "different-password", "Different Name",
            LocalDate.of(1985, 5, 5), LocalDate.of(1985, 5, 5), "111-1111-1111", "Osaka", Customer.Role.USER);
        
        CustomerUserDetails userDetails1 = new CustomerUserDetails(customer1);
        CustomerUserDetails userDetails2 = new CustomerUserDetails(customer2);
        
        assertThat(userDetails1).isEqualTo(userDetails2);
    }

    @Test
    @DisplayName("異なるemailを持つUserDetailsはequals()でfalseを返す")
    void testEquals_DifferentEmail() {
        Customer customer1 = new Customer("test1@example.com", "password", "Test User 1",
            LocalDate.of(1990, 1, 1), LocalDate.of(1990, 1, 1), "000-0000-0000", "Tokyo", Customer.Role.USER);
        Customer customer2 = new Customer("test2@example.com", "password", "Test User 2",
            LocalDate.of(1990, 1, 1), LocalDate.of(1990, 1, 1), "000-0000-0000", "Tokyo", Customer.Role.USER);
        
        CustomerUserDetails userDetails1 = new CustomerUserDetails(customer1);
        CustomerUserDetails userDetails2 = new CustomerUserDetails(customer2);
        
        assertThat(userDetails1).isNotEqualTo(userDetails2);
    }

    @Test
    @DisplayName("同じオブジェクトはequals()でtrueを返す")
    void testEquals_SameObject() {
        Customer customer = new Customer("test@example.com", "password", "Test User",
            LocalDate.of(1990, 1, 1), LocalDate.of(1990, 1, 1), "000-0000-0000", "Tokyo", Customer.Role.USER);
        CustomerUserDetails userDetails = new CustomerUserDetails(customer);
        
        assertThat(userDetails).isEqualTo(userDetails);
    }

    @Test
    @DisplayName("nullとの比較はequals()でfalseを返す")
    void testEquals_Null() {
        Customer customer = new Customer("test@example.com", "password", "Test User",
            LocalDate.of(1990, 1, 1), LocalDate.of(1990, 1, 1), "000-0000-0000", "Tokyo", Customer.Role.USER);
        CustomerUserDetails userDetails = new CustomerUserDetails(customer);
        
        assertThat(userDetails).isNotEqualTo(null);
    }

    @Test
    @DisplayName("異なるクラスのオブジェクトとの比較はequals()でfalseを返す")
    void testEquals_DifferentClass() {
        Customer customer = new Customer("test@example.com", "password", "Test User",
            LocalDate.of(1990, 1, 1), LocalDate.of(1990, 1, 1), "000-0000-0000", "Tokyo", Customer.Role.USER);
        CustomerUserDetails userDetails = new CustomerUserDetails(customer);
        String differentObject = "test@example.com";
        
        assertThat(userDetails).isNotEqualTo(differentObject);
    }

    @Test
    @DisplayName("同じemailを持つUserDetailsは同じhashCode()を返す")
    void testHashCode_SameEmail() {
        Customer customer1 = new Customer("test@example.com", "password", "Test User",
            LocalDate.of(1990, 1, 1), LocalDate.of(1990, 1, 1), "000-0000-0000", "Tokyo", Customer.Role.USER);
        Customer customer2 = new Customer("test@example.com", "different-password", "Different Name",
            LocalDate.of(1985, 5, 5), LocalDate.of(1985, 5, 5), "111-1111-1111", "Osaka", Customer.Role.USER);
        
        CustomerUserDetails userDetails1 = new CustomerUserDetails(customer1);
        CustomerUserDetails userDetails2 = new CustomerUserDetails(customer2);
        
        assertThat(userDetails1.hashCode()).isEqualTo(userDetails2.hashCode());
    }

    @Test
    @DisplayName("異なるemailを持つUserDetailsは異なるhashCode()を返す（ほぼ確実）")
    void testHashCode_DifferentEmail() {
        Customer customer1 = new Customer("test1@example.com", "password", "Test User 1",
            LocalDate.of(1990, 1, 1), LocalDate.of(1990, 1, 1), "000-0000-0000", "Tokyo", Customer.Role.USER);
        Customer customer2 = new Customer("test2@example.com", "password", "Test User 2",
            LocalDate.of(1990, 1, 1), LocalDate.of(1990, 1, 1), "000-0000-0000", "Tokyo", Customer.Role.USER);
        
        CustomerUserDetails userDetails1 = new CustomerUserDetails(customer1);
        CustomerUserDetails userDetails2 = new CustomerUserDetails(customer2);
        
        // hashCode()が異なることを期待（衝突の可能性もあるが極めて低い）
        assertThat(userDetails1.hashCode()).isNotEqualTo(userDetails2.hashCode());
    }

    @Test
    @DisplayName("locked状態が異なってもemailが同じならequals()でtrueを返す")
    void testEquals_DifferentLockedState() {
        Customer customer = new Customer("test@example.com", "password", "Test User",
            LocalDate.of(1990, 1, 1), LocalDate.of(1990, 1, 1), "000-0000-0000", "Tokyo", Customer.Role.USER);
        
        CustomerUserDetails userDetails1 = new CustomerUserDetails(customer, false);
        CustomerUserDetails userDetails2 = new CustomerUserDetails(customer, true);
        
        // emailが同じなら、locked状態が異なっても同一プリンシパルと見なす
        assertThat(userDetails1).isEqualTo(userDetails2);
    }
}
