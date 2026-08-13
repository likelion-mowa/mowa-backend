package com.mowa.backend.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mowa.backend.common.exception.ErrorCode;

public class ApiResponse<T> {

    private static final String DEFAULT_SUCCESS_MESSAGE = "요청이 성공적으로 처리되었습니다.";
    private static final String DEFAULT_FAILURE_MESSAGE = "요청 처리 중 오류가 발생했습니다.";

    private final boolean success;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final ErrorResponse error;

    private ApiResponse(boolean success, String message, T data, ErrorResponse error) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(DEFAULT_SUCCESS_MESSAGE, data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static ApiResponse<Void> failure(ErrorCode errorCode, String detail) {
        return failure(errorCode.getCode(), detail);
    }

    public static ApiResponse<Void> failure(String code, String detail) {
        return new ApiResponse<>(false, DEFAULT_FAILURE_MESSAGE, null, new ErrorResponse(code, detail));
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public ErrorResponse getError() {
        return error;
    }

    public static class ErrorResponse {

        private final String code;
        private final String detail;

        private ErrorResponse(String code, String detail) {
            this.code = code;
            this.detail = detail;
        }

        public String getCode() {
            return code;
        }

        public String getDetail() {
            return detail;
        }
    }
}
