package kr.eolmago.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@ControllerAdvice
public class StaticResourceExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(
            NoResourceFoundException e,
            HttpServletRequest request
    ) {
        log.debug("[NoResourceFound] {} {}", request.getMethod(), request.getRequestURI());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}