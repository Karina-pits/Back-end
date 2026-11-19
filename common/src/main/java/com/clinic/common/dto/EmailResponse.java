package com.clinic.common.dto;

/**
 * Відповідь notification-service: SENT або INVALID_REQUEST.
 */
public record EmailResponse(String status) {
}
