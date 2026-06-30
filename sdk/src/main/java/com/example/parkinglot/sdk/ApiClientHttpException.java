package com.example.parkinglot.sdk;

public class ApiClientHttpException extends ApiClientException {

    private final int statusCode;
    private final String responseBody;

    public ApiClientHttpException(int statusCode, String responseBody) {
        super("HTTP request failed with status " + statusCode);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}

