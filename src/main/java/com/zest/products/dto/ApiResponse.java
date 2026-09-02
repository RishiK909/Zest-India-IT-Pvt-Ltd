package com.zest.products.dto;

public class ApiResponse<T> {

        private String message;
        private boolean status;
        private T data;

    public ApiResponse() {
    }

    public ApiResponse(String message, boolean status) {
        this.message = message;
        this.status = status;
    }

    public ApiResponse(String message, boolean status, T data) {
        this.message = message;
        this.status = status;
        this.data = data;
    }

        public String getMessage() {
        return message;
    }

        public void setMessage(String message) {
        this.message = message;
    }

        public boolean isStatus() {
        return status;
    }

        public void setStatus(boolean status) {
        this.status = status;
    }

        public T getData() {
        return data;
    }

        public void setData(T data) {
        this.data = data;
    }

}
