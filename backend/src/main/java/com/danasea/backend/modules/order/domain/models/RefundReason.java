package com.danasea.backend.modules.order.domain.models;

public enum RefundReason {
    CUSTOMER_CANCEL,
    CUSTOMER_REQUEST,
    WEATHER,
    VENDOR_FAULT,
    SYSTEM_FAULT,
    ADMIN_OVERRIDE,
    DISPUTE,
    COMPENSATION
}
