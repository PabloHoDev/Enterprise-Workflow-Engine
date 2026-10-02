package com.pablohenrique.workflowengine.execution.interfaces.rest;

import com.pablohenrique.workflowengine.execution.application.DefinitionUnavailableException;
import com.pablohenrique.workflowengine.execution.application.WorkflowNotFoundException;
import com.pablohenrique.workflowengine.execution.domain.ActorNotAuthorizedException;
import com.pablohenrique.workflowengine.execution.domain.InvalidWorkflowStatusException;
import com.pablohenrique.workflowengine.execution.domain.RuleNotSatisfiedException;
import com.pablohenrique.workflowengine.execution.domain.TransitionNotAvailableException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduz os erros do módulo Workflow Execution para respostas da API (RFC 9457). */
@RestControllerAdvice
@Order(0)
class WorkflowExceptionHandler {

    @ExceptionHandler(WorkflowNotFoundException.class)
    ProblemDetail notFound(WorkflowNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Workflow not found", exception);
    }

    @ExceptionHandler(DefinitionUnavailableException.class)
    ProblemDetail definitionUnavailable(DefinitionUnavailableException exception) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Workflow definition unavailable", exception);
    }

    @ExceptionHandler(InvalidWorkflowStatusException.class)
    ProblemDetail invalidStatus(InvalidWorkflowStatusException exception) {
        return problem(HttpStatus.CONFLICT, "Operation incompatible with workflow status", exception);
    }

    @ExceptionHandler(TransitionNotAvailableException.class)
    ProblemDetail transitionNotAvailable(TransitionNotAvailableException exception) {
        return problem(HttpStatus.CONFLICT, "Action not available", exception);
    }

    @ExceptionHandler(ActorNotAuthorizedException.class)
    ProblemDetail notAuthorized(ActorNotAuthorizedException exception) {
        return problem(HttpStatus.FORBIDDEN, "Actor not authorized", exception);
    }

    @ExceptionHandler(RuleNotSatisfiedException.class)
    ProblemDetail ruleNotSatisfied(RuleNotSatisfiedException exception) {
        ProblemDetail problem = problem(HttpStatus.UNPROCESSABLE_CONTENT, "Rule not satisfied", exception);
        problem.setProperty("unsatisfiedRules", exception.unsatisfiedRules());
        return problem;
    }

    private ProblemDetail problem(HttpStatus status, String title, RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle(title);
        return problem;
    }
}
