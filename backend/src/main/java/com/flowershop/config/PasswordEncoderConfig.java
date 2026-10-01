package com.flowershop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Split out from SecurityConfig: OAuth2LoginSuccessHandler needs a PasswordEncoder,
 * and SecurityConfig needs OAuth2LoginSuccessHandler, so defining this bean inside
 * SecurityConfig itself creates a circular dependency at startup.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
