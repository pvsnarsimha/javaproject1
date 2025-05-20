package com.obhs.service;

import com.obhs.entity.Booking;
import com.obhs.entity.BookingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${manager.email}")
    private String managerEmail;

    public void sendBookingPendingNotification(String userEmail, Booking booking) throws MessagingException {
        logger.info("Sending pending booking notification to: {}", userEmail);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(userEmail);
        helper.setSubject("Booking Submitted - " + booking.getHotel().getName());

        String htmlContent = """
            <h2>Booking Submitted</h2>
            <p>Dear Customer,</p>
            <p>Your booking at <strong>%s</strong> has been submitted and is pending admin approval.</p>
            <h3>Booking Details:</h3>
            <ul>
                <li><strong>Booking ID:</strong> %d</li>
                <li><strong>Hotel:</strong> %s</li>
                <li><strong>Room Type:</strong> %s</li>
                <li><strong>Check-In Date:</strong> %s</li>
                <li><strong>Check-Out Date:</strong> %s</li>
                <li><strong>Number of Rooms:</strong> %d</li>
                <li><strong>Total Cost:</strong> ₹%.2f</li>
                <li><strong>Status:</strong> %s</li>
            </ul>
            <p>You will receive a confirmation email once your booking is approved.</p>
            <p>Best regards,<br>OHBS Team</p>
            """.formatted(
                booking.getHotel().getName(),
                booking.getId(),
                booking.getHotel().getName(),
                booking.getRoom().getType(),
                booking.getCheckInDate().toString(),
                booking.getCheckOutDate().toString(),
                booking.getNumberOfRooms(),
                booking.getTotalCost(),
                booking.getStatus().toString()
            );

        helper.setText(htmlContent, true);
        mailSender.send(message);
        logger.info("Pending booking notification sent to: {}", userEmail);
    }

    public void sendBookingConfirmation(String userEmail, Booking booking) throws MessagingException {
        logger.info("Sending booking confirmation to: {}", userEmail);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(userEmail);
        helper.setSubject("Booking Confirmation - " + booking.getHotel().getName());

        String htmlContent = """
            <h2>Booking Confirmation</h2>
            <p>Dear Customer,</p>
            <p>Your booking at <strong>%s</strong> has been confirmed!</p>
            <h3>Booking Details:</h3>
            <ul>
                <li><strong>Booking ID:</strong> %d</li>
                <li><strong>Hotel:</strong> %s</li>
                <li><strong>Room Type:</strong> %s</li>
                <li><strong>Check-In Date:</strong> %s</li>
                <li><strong>Check-Out Date:</strong> %s</li>
                <li><strong>Number of Rooms:</strong> %d</li>
                <li><strong>Total Cost:</strong> ₹%.2f</li>
                <li><strong>Status:</strong> %s</li>
            </ul>
            <p>Thank you for choosing our service!</p>
            <p>Best regards,<br>OHBS Team</p>
            """.formatted(
                booking.getHotel().getName(),
                booking.getId(),
                booking.getHotel().getName(),
                booking.getRoom().getType(),
                booking.getCheckInDate().toString(),
                booking.getCheckOutDate().toString(),
                booking.getNumberOfRooms(),
                booking.getTotalCost(),
                booking.getStatus().toString()
            );

        helper.setText(htmlContent, true);
        mailSender.send(message);
        logger.info("Booking confirmation email sent to: {}", userEmail);
    }

    public void sendBookingNotificationToManager(String customerName, Booking booking) throws MessagingException {
        logger.info("Sending booking notification to manager: {}", managerEmail);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(managerEmail);
        helper.setSubject("Booking Notification - " + booking.getHotel().getName());

        String statusMessage = booking.getStatus() == BookingStatus.PENDING 
            ? "A new booking is pending approval by <strong>%s</strong> at <strong>%s</strong>."
            : "A booking by <strong>%s</strong> at <strong>%s</strong> has been updated to %s.";

        String htmlContent = """
            <h2>Booking Notification</h2>
            <p>Dear Manager,</p>
            <p>%s</p>
            <h3>Booking Details:</h3>
            <ul>
                <li><strong>Booking ID:</strong> %d</li>
                <li><strong>Customer:</strong> %s</li>
                <li><strong>Hotel:</strong> %s</li>
                <li><strong>Room Type:</strong> %s</li>
                <li><strong>Check-In Date:</strong> %s</li>
                <li><strong>Check-Out Date:</strong> %s</li>
                <li><strong>Number of Rooms:</strong> %d</li>
                <li><strong>Total Cost:</strong> ₹%.2f</li>
                <li><strong>Status:</strong> %s</li>
            </ul>
            <p>Please review the booking details in the system.</p>
            <p>Best regards,<br>OHBS Team</p>
            """.formatted(
                String.format(statusMessage, customerName, booking.getHotel().getName(), booking.getStatus().toString()),
                booking.getId(),
                customerName,
                booking.getHotel().getName(),
                booking.getRoom().getType(),
                booking.getCheckInDate().toString(),
                booking.getCheckOutDate().toString(),
                booking.getNumberOfRooms(),
                booking.getTotalCost(),
                booking.getStatus().toString()
            );

        helper.setText(htmlContent, true);
        mailSender.send(message);
        logger.info("Booking notification sent to manager: {}", managerEmail);
    }

    public void sendCancellationNotification(String userEmail, Booking booking) throws MessagingException {
        logger.info("Sending cancellation notification to: {}", userEmail);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(userEmail);
        helper.setSubject("Booking Cancellation - " + booking.getHotel().getName());

        String htmlContent = """
            <h2>Booking Cancellation</h2>
            <p>Dear Customer,</p>
            <p>Your booking at <strong>%s</strong> has been canceled.</p>
            <h3>Booking Details:</h3>
            <ul>
                <li><strong>Booking ID:</strong> %d</li>
                <li><strong>Hotel:</strong> %s</li>
                <li><strong>Room Type:</strong> %s</li>
                <li><strong>Check-In Date:</strong> %s</li>
                <li><strong>Check-Out Date:</strong> %s</li>
                <li><strong>Number of Rooms:</strong> %d</li>
                <li><strong>Total Cost:</strong> ₹%.2f</li>
                <li><strong>Status:</strong> %s</li>
            </ul>
            <p>If you have any questions, please contact our support team.</p>
            <p>Best regards,<br>OHBS Team</p>
            """.formatted(
                booking.getHotel().getName(),
                booking.getId(),
                booking.getHotel().getName(),
                booking.getRoom().getType(),
                booking.getCheckInDate().toString(),
                booking.getCheckOutDate().toString(),
                booking.getNumberOfRooms(),
                booking.getTotalCost(),
                booking.getStatus().toString()
            );

        helper.setText(htmlContent, true);
        mailSender.send(message);
        logger.info("Cancellation notification email sent to: {}", userEmail);
    }

    public void sendCancellationNotificationToManager(String customerName, Booking booking) throws MessagingException {
        logger.info("Sending cancellation notification to manager: {}", managerEmail);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(managerEmail);
        helper.setSubject("Booking Cancellation Notification - " + booking.getHotel().getName());

        String htmlContent = """
            <h2>Booking Cancellation Notification</h2>
            <p>Dear Manager,</p>
            <p>A booking by <strong>%s</strong> at <strong>%s</strong> has been canceled.</p>
            <h3>Booking Details:</h3>
            <ul>
                <li><strong>Booking ID:</strong> %d</li>
                <li><strong>Customer:</strong> %s</li>
                <li><strong>Hotel:</strong> %s</li>
                <li><strong>Room Type:</strong> %s</li>
                <li><strong>Check-In Date:</strong> %s</li>
                <li><strong>Check-Out Date:</strong> %s</li>
                <li><strong>Number of Rooms:</strong> %d</li>
                <li><strong>Total Cost:</strong> ₹%.2f</li>
                <li><strong>Status:</strong> %s</li>
            </ul>
            <p>Please update the system records accordingly.</p>
            <p>Best regards,<br>OHBS Team</p>
            """.formatted(
                customerName,
                booking.getHotel().getName(),
                booking.getId(),
                customerName,
                booking.getHotel().getName(),
                booking.getRoom().getType(),
                booking.getCheckInDate().toString(),
                booking.getCheckOutDate().toString(),
                booking.getNumberOfRooms(),
                booking.getTotalCost(),
                booking.getStatus().toString()
            );

        helper.setText(htmlContent, true);
        mailSender.send(message);
        logger.info("Cancellation notification sent to manager: {}", managerEmail);
    }
}