package com.pablohenrique.workflowengine.identity.interfaces.rest;

import com.pablohenrique.workflowengine.identity.application.IdentityOperationException;
import com.pablohenrique.workflowengine.identity.application.TooManyLoginAttemptsException;
import com.pablohenrique.workflowengine.identity.domain.InvalidUserException;
import com.pablohenrique.workflowengine.identity.domain.WeakPasswordException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduz os erros do módulo Identity para respostas da API (RFC 9457). */
@RestControllerAdvice
@Order(0)
class IdentityExceptionHandler {

    @ExceptionHandler(IdentityOperationException.class)
    ProblemDetail operation(IdentityOperationException exception) {
        HttpStatus status = switch (exception.reason()) {
            case USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case USERNAME_TAKEN, LAST_ADMINISTRATOR, SELF_LOCKOUT -> HttpStatus.CONFLICT;
            case WRONG_CURRENT_PASSWORD -> HttpStatus.BAD_REQUEST;
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle(switch (exception.reason()) {
            case USER_NOT_FOUND -> "User not found";
            case USERNAME_TAKEN -> "Username already in use";
            case LAST_ADMINISTRATOR -> "Last administrator";
            case SELF_LOCKOUT -> "Operation not allowed on own account";
            case WRONG_CURRENT_PASSWORD -> "Incorrect current password";
        });
        problem.setProperty("code", exception.reason().name());
        return problem;
    }

    @ExceptionHandler(InvalidUserException.class)
    ProblemDetail invalidUser(InvalidUserException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT,
                "The user data is not valid");
        problem.setTitle("Invalid user");
        problem.setProperty("violations", exception.violations());
        return problem;
    }

    @ExceptionHandler(WeakPasswordException.class)
    ProblemDetail weakPassword(WeakPasswordException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT,
                exception.getMessage());
        problem.setTitle("Weak password");
        problem.setProperty("violations", exception.violations());
        return problem;
    }

    @ExceptionHandler(TooManyLoginAttemptsException.class)
    ResponseEntity<ProblemDetail> tooManyAttempts(TooManyLoginAttemptsException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage());
        problem.setTitle("Too many login attempts");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(exception.retryAfterSeconds()))
                .body(problem);
    }
}
