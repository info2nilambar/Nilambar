package com.nilambar.erp.event;

public record UserRegisteredEvent(String eventId, Long userId, String mobile, String registeredAt) {
}
