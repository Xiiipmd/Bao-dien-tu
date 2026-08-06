package ptit.tmdt.lop6nhom7.baodientu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ptit.tmdt.lop6nhom7.baodientu.dto.ErrorResponse;
import ptit.tmdt.lop6nhom7.baodientu.exception.GlobalExceptionHandler;

class UtilityErrorContractTests {

    @Test
    void preservesUnavailableStatusAndSafeProviderMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        var response = handler.handleResponseStatus(new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Nguồn Xổ số chưa được cấu hình trên server"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(503, body.getStatus());
        assertEquals("Nguồn Xổ số chưa được cấu hình trên server", body.getMessage());
    }
}
