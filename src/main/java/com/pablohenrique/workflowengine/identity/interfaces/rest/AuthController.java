package com.pablohenrique.workflowengine.identity.interfaces.rest;

import com.pablohenrique.workflowengine.identity.application.LoginThrottle;
import com.pablohenrique.workflowengine.identity.application.UserAccountService;
import com.pablohenrique.workflowengine.identity.domain.UserAccount;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * Autenticação do console web por sessão (cookie {@code HttpOnly}) com proteção CSRF (ADR-007).
 * O logout é tratado pela cadeia de segurança em {@code POST /api/v1/auth/logout}.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Sessão do console web")
class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionStrategy;
    private final LoginThrottle loginThrottle;
    private final UserAccountService service;
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

    AuthController(AuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository,
                   CsrfTokenRepository csrfTokenRepository, LoginThrottle loginThrottle,
                   UserAccountService service) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        // Novo id de sessão (contra fixação de sessão) e novo token CSRF a cada login.
        this.sessionStrategy = new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(), new CsrfAuthenticationStrategy(csrfTokenRepository)));
        this.loginThrottle = loginThrottle;
        this.service = service;
    }

    @GetMapping("/csrf")
    @Operation(summary = "Emite o cookie XSRF-TOKEN, a ser devolvido no header X-XSRF-TOKEN")
    ResponseEntity<Void> csrf(CsrfToken token) {
        token.getToken();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    @Operation(summary = "Abre uma sessão do console web")
    MeResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest,
                     HttpServletResponse httpResponse) {
        String origin = httpRequest.getRemoteAddr();
        loginThrottle.checkAllowed(origin);
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        } catch (AuthenticationException failure) {
            loginThrottle.recordFailure(origin);
            throw failure;
        }
        sessionStrategy.onAuthentication(authentication, httpRequest, httpResponse);
        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        contextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        service.recordInteractiveLogin(authentication.getName());
        return MeResponse.from(service.get(authentication.getName()));
    }

    @GetMapping("/me")
    @Operation(summary = "Usuário autenticado")
    MeResponse me(Authentication authentication) {
        return MeResponse.from(service.get(authentication.getName()));
    }

    @PostMapping("/password")
    @Operation(summary = "Troca a própria senha; encerra todas as sessões do usuário")
    ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                        Authentication authentication) {
        service.changeOwnPassword(authentication.getName(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    /** Mesma resposta para usuário inexistente, senha errada, conta bloqueada ou desativada: não revela contas. */
    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail invalidCredentials(AuthenticationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Invalid username or password");
        problem.setTitle("Authentication failed");
        return problem;
    }

    record LoginRequest(@NotBlank @Size(max = 64) String username, @NotBlank @Size(max = 128) String password) {
    }

    record ChangePasswordRequest(@NotBlank @Size(max = 128) String currentPassword,
                                 @NotBlank @Size(max = 128) String newPassword) {
    }

    record MeResponse(String username, String displayName, Set<String> roles) {

        static MeResponse from(UserAccount account) {
            return new MeResponse(account.username(), account.displayName(), account.roles());
        }
    }
}
