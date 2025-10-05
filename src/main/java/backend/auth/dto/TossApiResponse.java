package backend.auth.dto;

import lombok.AllArgsConstructor;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TossApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private String errorCode;
    private LocalDateTime timestamp;

    public static <T> TossApiResponse<T> success(T data) {
        return TossApiResponse.<T>builder()
                .success(true)
                .message("요청이 성공적으로 처리되었습니다")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> TossApiResponse<T> success(T data, String message) {
        return TossApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> TossApiResponse<T> failure(String message) {
        return TossApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> TossApiResponse<T> failure(String message, String errorCode) {
        return TossApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
