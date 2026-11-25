package dev.yamyam.controller;

import java.util.NoSuchElementException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> invalid(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(new ApiError(400, error.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Object> missing(NoSuchElementException error) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ApiError(404, error.getMessage()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Object> duplicate(DuplicateKeyException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ApiError(409, "이미 사용 중인 아이디입니다."));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Object> database(DataAccessException error) {
        logger.error("MySQL 요청 처리 실패: " + error.getClass().getSimpleName());
        return ResponseEntity.internalServerError().body(new ApiError(500, "데이터 처리에 실패했습니다."));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception error, Object body,
        HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message;
        if (error instanceof ResponseStatusException) {
            message = ((ResponseStatusException) error).getReason();
        } else {
            switch (status.value()) {
                case 400:
                    message = "요청 값 또는 JSON 형식을 확인하세요.";
                    break;
                case 404:
                    message = "요청한 경로를 찾을 수 없습니다.";
                    break;
                case 405:
                    message = "이 경로에서 지원하는 HTTP 메서드를 사용하세요.";
                    break;
                case 415:
                    message = "Content-Type을 application/json으로 지정하세요.";
                    break;
                default:
                    message = "요청을 처리하지 못했습니다.";
            }
        }
        return super.handleExceptionInternal(error, new ApiError(status.value(), message), headers,
            status, request);
    }

    public static final class ApiError {

        private final int status;
        private final String message;

        public ApiError(int status, String message) {
            this.status = status;
            this.message = message;
        }

        public int getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }
}
