package com.obhs.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "number_of_rooms", nullable = false)
    private int numberOfRooms;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "total_cost", nullable = false)
    private double totalCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Convert(converter = BookingStatusConverter.class)
    private BookingStatus status = BookingStatus.PENDING; // Default to PENDING

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING; // Default to PENDING

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Hotel getHotel() { return hotel; }
    public void setHotel(Hotel hotel) { this.hotel = hotel; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public int getNumberOfRooms() { return numberOfRooms; }
    public void setNumberOfRooms(int numberOfRooms) { this.numberOfRooms = numberOfRooms; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; }
    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public enum PaymentStatus {
        PENDING, SUCCESSFUL, FAILED, REFUNDED
    }

    // Converter for handling legacy CANCELED values
    @Converter(autoApply = false)
    public static class BookingStatusConverter implements AttributeConverter<BookingStatus, String> {
        @Override
        public String convertToDatabaseColumn(BookingStatus status) {
            return status == null ? null : status.name();
        }

        @Override
        public BookingStatus convertToEntityAttribute(String dbData) {
            if (dbData == null) {
                return null;
            }
            // Handle legacy CANCELED by mapping to CANCELLED
            if ("CANCELLED".equalsIgnoreCase(dbData)) {
                return BookingStatus.CANCELED;
            }
            try {
                return BookingStatus.valueOf(dbData);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown status value: " + dbData, e);
            }
        }
    }
}