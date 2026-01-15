package com.apexpay.apigateway.dto;

public record ErrorResponse(int status, String error, String message, String path, String dateTime) {
}
