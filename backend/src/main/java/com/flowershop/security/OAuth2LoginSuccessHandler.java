package com.flowershop.security;

import com.flowershop.entity.Role;
import com.flowershop.entity.User;
import com.flowershop.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Finishes a "Sign in with Google" flow (see SecurityConfig's oauth2Login()).
 * Google redirects the browser here once the user has consented; there is no
 * XHR response to return (this is a top-level browser navigation), so instead
 * of issuing JSON like AuthController does for password login, this mints the
 * same kind of JWT and hands it back to the React SPA via a redirect query
 * param, which the frontend's /customer/oauth2-callback route picks up and
 * stores exactly like a normal login response.
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.oauth2.frontend-redirect-uri}")
    private String frontendRedirectUri;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> provisionCustomer(email, name != null ? name : email));

        var userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .disabled(!user.isActive())
                .build();

        String token = jwtService.generateToken(userDetails, Map.of("role", user.getRole().name()));

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("token", URLEncoder.encode(token, StandardCharsets.UTF_8))
                .build().toUriString();
        response.sendRedirect(redirectUrl);
    }

    // A Google-provisioned account has no usable password until the user sets one
    // (there's no password-reset flow yet -- see docs/REQUIREMENTS.md scope notes);
    // the random BCrypt hash just satisfies the NOT NULL column and guarantees a
    // password-based login attempt can never succeed by guessing it.
    private User provisionCustomer(String email, String fullName) {
        User user = User.builder()
                .fullName(fullName)
                .email(email)
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.CUSTOMER)
                .active(true)
                .build();
        return userRepository.save(user);
    }
}
