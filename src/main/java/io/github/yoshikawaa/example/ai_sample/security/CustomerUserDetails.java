package io.github.yoshikawaa.example.ai_sample.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import io.github.yoshikawaa.example.ai_sample.model.Customer;

public class CustomerUserDetails implements UserDetails {

    private final Customer customer;
    private final boolean locked;

    public CustomerUserDetails(Customer customer) {
        this(customer, false);
    }

    public CustomerUserDetails(Customer customer, boolean locked) {
        this.customer = customer;
        this.locked = locked;
    }

    public Customer getCustomer() {
        return customer;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null; // 権限が必要な場合はここに設定
    }

    @Override
    public String getUsername() {
        return customer.getEmail();
    }

    @Override
    public String getPassword() {
        return customer.getPassword();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}