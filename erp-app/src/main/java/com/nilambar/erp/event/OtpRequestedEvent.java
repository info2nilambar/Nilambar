package com.nilambar.erp.event;

/**
 * Emitted whenever an OTP is generated and dispatched. The code itself is never included.
 */
public record OtpRequestedEvent(String eventId, String mobile, String channel, String requestedAt) {
}
