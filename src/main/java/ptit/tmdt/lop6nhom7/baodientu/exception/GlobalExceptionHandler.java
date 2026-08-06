package ptit.tmdt.lop6nhom7.baodientu.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import ptit.tmdt.lop6nhom7.baodientu.dto.ErrorResponse;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
  
  // Your custom exceptions
  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse.of(400, ex.getMessage()));
  }
  
  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
    return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(ErrorResponse.of(401, ex.getMessage()));
  }
  
  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
    return ResponseEntity
        .status(HttpStatus.FORBIDDEN)
        .body(ErrorResponse.of(403, ex.getMessage()));
  }
  
  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of(404, ex.getMessage()));
  }
  
  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of(409, ex.getMessage()));
  }
  
  // Bean validation failures (@Valid annotation)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(
      MethodArgumentNotValidException ex) {

    // Collect all field errors into one message
    String message = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(err -> err.getField() + ": " + err.getDefaultMessage())
        .collect(Collectors.joining(", "));

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse.of(400, message));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
    int status = ex.getStatusCode().value();
    String message = ex.getReason() == null ? "Yêu cầu không thể xử lý" : ex.getReason();
    return ResponseEntity
        .status(ex.getStatusCode())
        .body(ErrorResponse.of(status, message));
  }
  
  // Catch-all — unexpected exceptions
  // Logs the full stack trace but returns a safe message to the client
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
    // Log full details internally
    log.error("Unexpected error", ex);
    
    // Return safe message to client — never expose stack trace
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.of(500, "An unexpected error occurred"));
  }
}
