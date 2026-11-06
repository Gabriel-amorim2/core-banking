package com.bankingPlatform.core.account.infra;

import com.bankingPlatform.core.account.infra.exepition.ExistentAccount;
import com.bankingPlatform.core.account.infra.exepition.NonExistentAccount;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestHandlerExeption {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String errorMessage = fieldError != null ? fieldError.getDefaultMessage() : "Erro de validação";

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                errorMessage
        );
        problemDetail.setTitle("Bad Request - Validation Error");
        return problemDetail;
    }

    @ExceptionHandler(NonExistentAccount.class)
    public ProblemDetail handleNonExistentAccount(NonExistentAccount ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setTitle("Account Not Found");
        return problemDetail;
    }

    @ExceptionHandler(ExistentAccount.class)
    public ProblemDetail handleExistentAccount(ExistentAccount ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problemDetail.setTitle("Account Already Exists");
        return problemDetail;
    }
}

