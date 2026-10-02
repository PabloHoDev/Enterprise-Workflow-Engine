package com.pablohenrique.workflowengine.definition.interfaces.rest;

import com.pablohenrique.workflowengine.definition.application.DefinitionKeyAlreadyExistsException;
import com.pablohenrique.workflowengine.definition.application.DefinitionNotFoundException;
import com.pablohenrique.workflowengine.definition.domain.DefinitionVersionNotFoundException;
import com.pablohenrique.workflowengine.definition.domain.InvalidDefinitionException;
import com.pablohenrique.workflowengine.definition.domain.InvalidVersionStatusException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduz os erros do módulo Workflow Definition para respostas da API (RFC 9457). */
@RestControllerAdvice
@Order(0)
class DefinitionExceptionHandler {

    @ExceptionHandler({DefinitionNotFoundException.class, DefinitionVersionNotFoundException.class})
    ProblemDetail notFound(RuntimeException exception) {
        return problem(HttpStatus.NOT_FOUND, "Workflow definition not found", exception);
    }

    @ExceptionHandler(DefinitionKeyAlreadyExistsException.class)
    ProblemDetail keyConflict(DefinitionKeyAlreadyExistsException exception) {
        return problem(HttpStatus.CONFLICT, "Workflow definition key already in use", exception);
    }

    @ExceptionHandler(InvalidVersionStatusException.class)
    ProblemDetail statusConflict(InvalidVersionStatusException exception) {
        return problem(HttpStatus.CONFLICT, "Invalid version status", exception);
    }

    @ExceptionHandler(InvalidDefinitionException.class)
    ProblemDetail invalid(InvalidDefinitionException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT,
                "The workflow definition structure is not valid for execution");
        problem.setTitle("Invalid workflow definition");
        problem.setProperty("violations", exception.violations());
        return problem;
    }

    private ProblemDetail problem(HttpStatus status, String title, RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle(title);
        return problem;
    }
}
