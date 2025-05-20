package com.obhs.dto;

public class PaymentDTO {
    private Long bookingId;
    private double amount;

    // Getters and Setters
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}