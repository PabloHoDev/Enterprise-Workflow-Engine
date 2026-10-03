package com.pablohenrique.workflowengine.infrastructure.security;

import com.pablohenrique.workflowengine.audit.contract.AuditEvent;
import com.pablohenrique.workflowengine.audit.contract.AuditRecorder;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Cadeia de segurança HTTP (docs/architecture/SECURITY.md, ADR-007).
 *
 * <ul>
 *   <li>Console web: sessão em cookie {@code HttpOnly} persistida no PostgreSQL, com proteção CSRF.</li>
 *   <li>Integrações: HTTP Basic sem sessão. Requisições com header {@code Authorization} não carregam
 *       credencial ambiente (cookie), por isso dispensam o token CSRF.</li>
 * </ul>
 */
@Configuration
class SecurityConfiguration {

    static final String ROLE_ADMIN = "ADMIN";

    private static final Logger log = LoggerFactory.getLogger(SecurityConfiguration.class);

    // A interface web e o Swagger UI só carregam recursos da própria origem.
    private static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data:",
            "font-src 'self'",
            "connect-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationManager authenticationManager,
            CsrfTokenRepository csrfTokenRepository,
            SecurityContextRepository securityContextRepository,
            AuditRecorder auditRecorder,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) throws Exception {
        // Erros de segurança seguem o mesmo formato (RFC 9457) dos demais erros da API.
        AuthenticationEntryPoint entryPoint =
                (request, response, exception) -> exceptionResolver.resolveException(request, response, null, exception);
        AccessDeniedHandler accessDeniedHandler = (request, response, exception) -> {
            auditAccessDenied(auditRecorder, request, exception.getClass().getSimpleName());
            exceptionResolver.resolveException(request, response, null, exception);
        };
        LogoutHandler auditLogout = (request, response, authentication) -> {
            if (authentication != null) {
                auditRecorder.record(AuditEvent.success(authentication.getName(), "LOGOUT", "USER",
                        authentication.getName(), "web session"));
            }
        };

        http
                .authenticationManager(authenticationManager)
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .csrf(csrf -> csrf
                        .spa()
                        .csrfTokenRepository(csrfTokenRepository)
                        .ignoringRequestMatchers(request -> request.getHeader(HttpHeaders.AUTHORIZATION) != null))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                        .permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/v1/auth/csrf", "/api/v1/auth/login").permitAll()
                        .requestMatchers("/actuator/**").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/v1/workflow-definitions/**").hasRole(ROLE_ADMIN)
                        .requestMatchers("/api/v1/audit-records/**").hasRole(ROLE_ADMIN)
                        .requestMatchers("/api/v1/users/**").hasRole(ROLE_ADMIN)
                        .requestMatchers("/api/**").authenticated()
                        // Demais caminhos: arquivos estáticos e rotas da interface web, que não contêm dados.
                        .anyRequest().permitAll())
                .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .addLogoutHandler(auditLogout)
                        .deleteCookies("EWE_SESSION")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicyHeader(permissions -> permissions
                                .policy("camera=(), microphone=(), geolocation=(), payment=()")));
        return http.build();
    }

    private static void auditAccessDenied(AuditRecorder auditRecorder, HttpServletRequest request, String reason) {
        try {
            Authentication authentication = request.getUserPrincipal() instanceof Authentication value ? value : null;
            String actor = authentication == null ? "anonymous" : authentication.getName();
            auditRecorder.record(AuditEvent.rejected(actor, "ACCESS_DENIED", "HTTP_ENDPOINT",
                    request.getMethod() + " " + request.getRequestURI(), reason));
        } catch (RuntimeException e) {
            log.warn("Could not audit access denial", e);
        }
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder,
                                                AuthenticationEventPublisher eventPublisher) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        ProviderManager manager = new ProviderManager(provider);
        // Publica sucesso/falha para o módulo Identity aplicar bloqueio por tentativas e auditoria.
        manager.setAuthenticationEventPublisher(eventPublisher);
        return manager;
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        // Legível pelo JavaScript da própria origem, que o devolve no header X-XSRF-TOKEN.
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.sameSite("Strict").path("/"));
        return repository;
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
