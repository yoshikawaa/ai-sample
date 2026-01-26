package io.github.yoshikawaa.example.ai_sample.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.yoshikawaa.example.ai_sample.repository.CustomerRepository;
import io.github.yoshikawaa.example.ai_sample.service.LoginAttemptService;

@Service
public class CustomerUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;
    private final LoginAttemptService loginAttemptService;

    public CustomerUserDetailsService(CustomerRepository customerRepository, 
                                      LoginAttemptService loginAttemptService) {
        this.customerRepository = customerRepository;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var customer = customerRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        // ロック状態をチェックし、UserDetailsに設定
        // AuthenticationProviderのUserDetailsCheckerが isAccountNonLocked() をチェックし、
        // falseの場合にLockedExceptionをスローする
        boolean locked = loginAttemptService.isLocked(username);
        return new CustomerUserDetails(customer, locked);
    }
}
