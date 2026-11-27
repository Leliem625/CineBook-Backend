package com.cinebook.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Phản hồi IPN theo đúng định dạng VNPay yêu cầu: {@code {"RspCode":"00","Message":"Confirm Success"}}. */
public record IpnResponse(@JsonProperty("RspCode") String rspCode, @JsonProperty("Message") String message) {

    public static final IpnResponse SUCCESS = new IpnResponse("00", "Confirm Success");
    public static final IpnResponse ORDER_NOT_FOUND = new IpnResponse("01", "Order not found");
    public static final IpnResponse ALREADY_CONFIRMED = new IpnResponse("02", "Order already confirmed");
    public static final IpnResponse INVALID_AMOUNT = new IpnResponse("04", "Invalid amount");
    public static final IpnResponse INVALID_SIGNATURE = new IpnResponse("97", "Invalid signature");
    public static final IpnResponse UNKNOWN_ERROR = new IpnResponse("99", "Unknown error");
}
