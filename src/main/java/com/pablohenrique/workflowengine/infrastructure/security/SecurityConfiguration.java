package com.pablohenrique.workflowengine.infrastructure.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;

/**
 * Autenticação HTTP Basic stateless e autorização por papel (docs/architecture/SECURITY.md).
 */
@Configuration
@EnableConfigurationProperties(SecurityUsersProperties.class)
class SecurityConfiguration {

    static final String ROLE_ADMIN = "ADMIN";

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) throws Exception {
        // Erros de segurança seguem o mesmo formato (RFC 9457) dos demais erros da API.
        AuthenticationEntryPoint entryPoint =
                (request, response, exception) -> exceptionResolver.resolveException(request, response, null, exception);
        AccessDeniedHandler accessDeniedHandler =
                (request, response, exception) -> exceptionResolver.resolveException(request, response, null, exception);

        http
                // API stateless autenticada por header: não há sessão/cookie que um CSRF possa explorar.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                        .permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/actuator/**").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/v1/workflow-definitions/**").hasRole(ROLE_ADMIN)
                        .requestMatchers("/api/v1/audit-records/**").hasRole(ROLE_ADMIN)
                        .anyRequest().authenticated())
                .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    @Bean
    UserDetailsService userDetailsService(SecurityUsersProperties properties) {
        List<UserDetails> users = properties.users().stream()
                .map(user -> User.withUsername(user.username())
                        .password(user.password())
                        .roles(user.roles().toArray(String[]::new))
                        .build())
                .toList();
        return new InMemoryUserDetailsManager(users);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
