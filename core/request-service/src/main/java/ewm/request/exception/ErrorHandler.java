package ewm.request.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class,
        ConstraintViolationException.class, WebExchangeBindException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentValidException(Exception exception) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        exception.printStackTrace(printWriter);
        String errors = stringWriter.toString();
        String cause = "Invalid input values";
        log.warn("Status 400 - {}: {}", cause, exception.getMessage(), exception);
        return ApiError.builder()
            .errors(errors)
            .message(exception.getMessage())
            .reason(cause)
            .status(HttpStatus.BAD_REQUEST.toString())
            .timestamp(LocalDateTime.now())
            .build();
    }

    @ExceptionHandler({ValidationException.class, DataAccessException.class, WrongSortMethodException.class,
        HandlerMethodValidationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidationException(Exception exception) {
        String cause = "Data validation error";
        log.warn("Status 400 - {}: {}", cause, exception.getMessage(), exception);
        return ApiError.builder()
            .message(exception.getMessage())
            .reason(cause)
            .status(HttpStatus.BAD_REQUEST.toString())
            .timestamp(LocalDateTime.now())
            .build();
    }

    @ExceptionHandler({ConflictException.class, DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDataIntegrityViolationException(Exception exception) {
        String cause = "Data integrity violation";
        log.warn("Status 409 - {}: {}", cause, exception.getMessage(), exception);
        return ApiError.builder()
            .message(exception.getMessage())
            .reason(cause)
            .status(HttpStatus.CONFLICT.toString())
            .timestamp(LocalDateTime.now())
            .build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFoundException(NotFoundException exception) {
        String cause = "Resource not found";
        log.warn("Status 404 - {}: {}", cause, exception.getMessage(), exception);
        return ApiError.builder()
            .message(exception.getMessage())
            .reason(cause)
            .status(HttpStatus.NOT_FOUND.toString())
            .timestamp(LocalDateTime.now())
            .build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleException(Exception exception) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        exception.printStackTrace(printWriter);
        String errors = stringWriter.toString();
        String cause = "Internal server error";
        log.error("Status 500 - {}: {}", cause, exception.getMessage(), exception);
        return ApiError.builder()
            .errors(errors)
            .message(exception.getMessage())
            .reason(cause)
            .status(HttpStatus.INTERNAL_SERVER_ERROR.toString())
            .timestamp(LocalDateTime.now())
            .build();
    }
}
