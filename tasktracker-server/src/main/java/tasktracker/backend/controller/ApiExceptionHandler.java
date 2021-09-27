package tasktracker.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tasktracker.backend.controller.exception.ApiException;

import static tasktracker.backend.controller.exception.ApiException.*;


@ControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @ExceptionHandler(value = {
            RuntimeException.class
    })
    protected ResponseEntity<Object> serviceError(final RuntimeException ex, final WebRequest request) {
        return handleExceptionInternal(
                ex,
                Response.fail(new Response.Error(500, "INTERNAL SERVICE ERROR", ex.getMessage())),
                new HttpHeaders(),
                HttpStatus.INTERNAL_SERVER_ERROR,
                request
        );
    }

    @ExceptionHandler(value = {
            ProjectAlreadyExists.class
    })
    protected ResponseEntity<Object> alreadyExists(final ApiException ex, final WebRequest request) {
        return handleExceptionInternal(
                ex,
                Response.fail(new Response.Error(666, ex.getMessage(), "")),
                new HttpHeaders(),
                HttpStatus.CONFLICT,
                request
        );
    }


    @ExceptionHandler(value = {
            ProjectNotFound.class,
            TaskStateNotFound.class,
            TaskMetricNotFound.class,
            TaskErrorNotFound.class
    })
    protected ResponseEntity<Object> notFound(final ApiException ex, final WebRequest request) {
        return handleExceptionInternal(
                ex,
                Response.fail(new Response.Error(666, ex.getMessage(), "")),
                new HttpHeaders(),
                HttpStatus.NOT_FOUND,
                request
        );
    }

    @ExceptionHandler(value = {
            InvalidInputData.class
    })
    protected ResponseEntity<Object> invalidData(final ApiException ex, final WebRequest request) {
        return handleExceptionInternal(
                ex,
                Response.fail(new Response.Error(400, ex.getMessage(), "")),
                new HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                request
        );
    }

    @ExceptionHandler(value = {
            InvalidDataQualityRuleException.class,
            InvalidDataQualityConditionException.class,
            TaskStatsMetricIsNullException.class
    })
    protected ResponseEntity<Object> invalidDataQualityCondition(
            ApiException ex,
            WebRequest request) {
        String message = ex.getMessage();
        final ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder();
        builder.addError(message);
        return handleExceptionInternal(
                ex,
                builder.build(),
                new HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request
    ) {
        logger.info("LALALA");
        String message = ex.getMessage();
        final ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder();
        builder.addError(message);
        return handleExceptionInternal(ex, builder.build(), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            final MissingServletRequestParameterException ex,
            final HttpHeaders headers,
            final HttpStatus status,
            final WebRequest request
    ) {
        String message = ex.getParameterName();
        final ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder();
        builder.paramMissing(message);
        return handleExceptionInternal(ex, builder.build(), headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        String message = ex.getPropertyName();
        final ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder();
        builder.badParam(message);
        return handleExceptionInternal(ex, builder.build(), headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request
    ) {
        logger.error("Error processing request", ex);
        return super.handleExceptionInternal(ex, body, headers, status, request);
    }

}
