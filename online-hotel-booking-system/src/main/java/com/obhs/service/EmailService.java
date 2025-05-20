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
import java.io.File;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;
 
    public void sendBookingPendingNotification(String userEmail, String customerName, Booking booking) throws MessagingException {
        logger.info("Sending pending booking notification to: {}", userEmail);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(userEmail);
        helper.setSubject("Booking Submitted - " + booking.getHotel().getName());

        String htmlContent = """
            <h2>---THIS IS SYSTEM GENERATED MAIL---</h2>
            <h2>Booking Submitted</h2>
            <p>Dear %s,</p>
            <p>Your booking at <strong>%s</strong> has been submitted and is pending admin approval.</p>
            <h3>Booking Details:</h3>
            <ul>
                <li><strong>Booking ID:</strong> %d</li>
                <li><strong>Hotel:</strong> %s</li>
                <li><strong>Room Type:</strong> %s</li>
                <li><strong>Check-In Date:</strong> %s</li>
                <li><strong>Check-Out Date:</strong> %s</li>
                <li><strong>Number of Rooms:</strong> %d</li>
                <li><strong>Total Cost:</strong> Rs %.2f</li>
                <li><strong>Status:</strong> %s</li>
            </ul>
            <p>You will receive a confirmation email once your booking is approved.</p>
            <p>Best regards,<br>OHBS Team</p>
            <p>do not reply</p>
            """.formatted(
                customerName,
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

    public void sendBookingConfirmation(String userEmail, String customerName, Booking booking) throws MessagingException {
        logger.info("Sending booking confirmation to: {}", userEmail);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(userEmail);
        helper.setSubject("Booking Confirmation - " + booking.getHotel().getName());

        String htmlContent = """
            <h2>---THIS IS SYSTEM GENERATED MAIL---</h2>
            <h2>Booking Confirmation</h2>
            <p>Dear %s,</p>
            <p>Your booking at <strong>%s</strong> has been confirmed!</p>
            <h3>Booking Details:</h3>
            <ul>
                <li><strong>Booking ID:</strong> %d</li>
                <li><strong>Hotel:</strong> %s</li>
                <li><strong>Room Type:</strong> %s</li>
                <li><strong>Check-In Date:</strong> %s</li>
                <li><strong>Check-Out Date:</strong> %s</li>
                <li><strong>Number of Rooms:</strong> %d</li>
                <li><strong>Total Cost:</strong> Rs %.2f</li>
                <li><strong>Status:</strong> %s</li>
            </ul>
            <p>Thank you for choosing our service!</p>
            <p>Best regards,<br>OHBS Team</p>
            <p>do not reply</p>
            """.formatted(
                customerName,
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
        String hotelContactEmail = booking.getHotel().getContactInfo();
        logger.info("Sending booking notification to hotel manager: {}", hotelContactEmail);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(hotelContactEmail);
        helper.setSubject("Booking Notification - " + booking.getHotel().getName());

        String statusMessage = booking.getStatus() == BookingStatus.PENDING 
            ? "A new booking by <strong>%s</strong> at <strong>%s</strong> is pending approval by admin."
            : "A booking by <strong>%s</strong> at <strong>%s</strong> has been approved by admin.";

        String htmlContent = """
            <h2>---THIS IS SYSTEM GENERATED MAIL---</h2>
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
                <li><strong>Total Cost:</strong> Rs %.2f</li>
                <li><strong>Status:</strong> %s</li>
            </ul>
            <p>Please review the booking details in the system.</p>
            <p>Best regards,<br>OHBS Team</p>
            <p>do not reply</p>
            """.formatted(
                String.format(statusMessage, customerName, booking.getHotel().getName()),
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
        logger.info("Booking notification sent to hotel manager: {}", hotelContactEmail);
    }

    public void sendCancellationNotification(String userEmail, String customerName, Booking booking) throws MessagingException {
        logger.info("Sending cancellation notification to: {}", userEmail);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(userEmail);
        helper.setSubject("Booking Cancellation - " + booking.getHotel().getName());

        String htmlContent = """
            <h2>---THIS IS SYSTEM GENERATED MAIL---</h2>
            <h2>Booking Cancellation</h2>
            <p>Dear %s,</p>
            <p>Your booking at <strong>%s</strong> has been canceled.</p>
            <h3>Booking Details:</h3>
            <ul>
                <li><strong>Booking ID:</strong> %d</li>
                <li><strong>Hotel:</strong> %s</li>
                <li><strong>Room Type:</strong> %s</li>
                <li><strong>Check-In Date:</strong> %s</li>
                <li><strong>Check-Out Date:</strong> %s</li>
                <li><strong>Number of Rooms:</strong> %d</li>
                <li><strong>Total Cost:</strong> Rs %.2f</li>
                <li><strong>Status:</strong> %s</li>
            </ul>
            <p>If you have any questions, please contact our support team.</p>
            <p>Best regards,<br>OHBS Team</p>
            <p>do not reply</p>
            """.formatted(
                customerName,
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
        String hotelContactEmail = booking.getHotel().getContactInfo();
        logger.info("Sending cancellation notification to hotel manager: {}", hotelContactEmail);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(hotelContactEmail);
        helper.setSubject("Booking Cancellation Notification - " + booking.getHotel().getName());

        String htmlContent = """
            <h2>---THIS IS SYSTEM GENERATED MAIL---</h2>
            <h2>Booking Cancellation Notification</h2>
            <p>Dear Manager,</p>
            <p>A booking by <strong>%s</strong> at <strong>%s</strong> has been canceled by admin.</p>
            <h3>Booking Details:</h3>
            <ul>
                <li><strong>Booking ID:</strong> %d</li>
                <li><strong>Customer:</strong> %s</li>
                <li><strong>Hotel:</strong> %s</li>
                <li><strong>Room Type:</strong> %s</li>
                <li><strong>Check-In Date:</strong> %s</li>
                <li><strong>Check-Out Date:</strong> %s</li>
                <li><strong>Number of Rooms:</strong> %d</li>
                <li><strong>Total Cost:</strong> Rs %.2f</li>
                <li><strong>Status:</strong> %s</li>
            </ul>
            <p>Please update the system records accordingly.</p>
            <p>Best regards,<br>OHBS Team</p>
            <p>do not reply</p>
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
        logger.info("Cancellation notification sent to hotel manager: {}", hotelContactEmail);
    }

    public void sendPaymentNotification(String userEmail, String customerName, Booking booking, double paidAmount, String billAbsolutePath) throws MessagingException {
        logger.info("Sending payment notification to: {}", userEmail);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(userEmail);
        helper.setSubject("Payment Status - " + booking.getHotel().getName());

        String statusMessage = booking.getPaymentStatus() == Booking.PaymentStatus.SUCCESSFUL
            ? "Your payment for the booking at <strong>%s</strong> has been successfully processed."
            : "Your payment for the booking at <strong>%s</strong> is pending due to an incorrect amount.";

        String htmlContent = """
            <h2>---THIS IS SYSTEM GENERATED MAIL---</h2>
            <h2>Payment Status</h2>
            <p>Dear %s,</p>
            <p>%s</p>
            <h3>Payment Details:</h3>
            <ul>
                <li><strong>Booking ID:</strong> %d</li>
                <li><strong>Hotel:</strong> %s</li>
                <li><strong>Paid Amount:</strong> Rs %.2f</li>
                <li><strong>Total Cost:</strong> Rs %.2f</li>
                <li><strong>Payment Status:</strong> %s</li>
            </ul>
            %s
            <p>If you have any questions, please contact our support team.</p>
            <p>Best regards,<br>OHBS Team</p>
            <p>do not reply</p>
            """.formatted(
                customerName,
                String.format(statusMessage, booking.getHotel().getName()),
                booking.getId(),
                booking.getHotel().getName(),
                paidAmount,
                booking.getTotalCost(),
                booking.getPaymentStatus().toString(),
                booking.getPaymentStatus() == Booking.PaymentStatus.SUCCESSFUL && booking.getId() >= 98 
                    ? "<p>We have attached your bill PDF for your records.</p>" 
                    : ""
            );

        helper.setText(htmlContent, true);

        // Attach the bill PDF if provided (already ensured to exist for booking IDs >= 98 and payment successful)
        if (billAbsolutePath != null && new File(billAbsolutePath).exists()) {
            helper.addAttachment("booking_" + booking.getId() + "_bill.pdf", new File(billAbsolutePath));
            logger.info("Attached bill PDF to email for booking ID: {} to customer: {}", booking.getId(), userEmail);
        }

        mailSender.send(message);
        logger.info("Payment notification sent to: {}", userEmail);
    }

    public void sendPaymentNotificationToManager(String customerName, Booking booking, double paidAmount, String billAbsolutePath) throws MessagingException {
        String hotelContactEmail = booking.getHotel().getContactInfo();
        logger.info("Sending payment notification to hotel manager: {}", hotelContactEmail);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(hotelContactEmail);
        helper.setSubject("Payment Status Notification - " + booking.getHotel().getName());

        String statusMessage = booking.getPaymentStatus() == Booking.PaymentStatus.SUCCESSFUL
            ? "A payment by <strong>%s</strong> for the booking at <strong>%s</strong> has been successfully processed and approved by admin."
            : "A payment by <strong>%s</strong> for the booking at <strong>%s</strong> is pending due to an incorrect amount, awaiting admin review.";

        String htmlContent = """
            <h2>---THIS IS SYSTEM GENERATED MAIL---</h2>
            <h2>Payment Status Notification</h2>
            <p>Dear Manager,</p>
            <p>%s</p>
            <h3>Payment Details:</h3>
            <ul>
                <li><strong>Booking ID:</strong> %d</li>
                <li><strong>Customer:</strong> %s</li>
                <li><strong>Hotel:</strong> %s</li>
                <li><strong>Paid Amount:</strong> Rs %.2f</li>
                <li><strong>Total Cost:</strong> Rs %.2f</li>
                <li><strong>Payment Status:</strong> %s</li>
            </ul>
            %s
            <p>Please review the payment details in the system.</p>
            <p>Best regards,<br>OHBS Team</p>
            <p>do not reply</p>
            """.formatted(
                String.format(statusMessage, customerName, booking.getHotel().getName()),
                booking.getId(),
                customerName,
                booking.getHotel().getName(),
                paidAmount,
                booking.getTotalCost(),
                booking.getPaymentStatus().toString(),
                booking.getPaymentStatus() == Booking.PaymentStatus.SUCCESSFUL && booking.getId() >= 98 
                    ? "<p>We have attached the bill PDF for your records.</p>" 
                    : ""
            );

        helper.setText(htmlContent, true);

        // Attach the bill PDF if provided (already ensured to exist for booking IDs >= 98 and payment successful)
        if (billAbsolutePath != null && new File(billAbsolutePath).exists()) {
            helper.addAttachment("booking_" + booking.getId() + "_bill.pdf", new File(billAbsolutePath));
            logger.info("Attached bill PDF to email for booking ID: {} to manager: {}", booking.getId(), hotelContactEmail);
        }

        mailSender.send(message);
        logger.info("Payment notification sent to hotel manager: {}", hotelContactEmail);
    }

    public void sendRefundNotification(String userEmail, String customerName, Booking booking) throws MessagingException {
        logger.info("Sending refund notification to: {}", userEmail);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(userEmail);
        helper.setSubject("Refund Notification - " + booking.getHotel().getName());

        String htmlContent = """
            <h2>---THIS IS SYSTEM GENERATED MAIL---</h2>
            <h2>Refund Notification</h2>
            <p>Dear %s,</p>
            <p>Your booking at <strong>%s</strong> has been canceled, and a refund has been processed.</p>
            <h3>Refund Details:</h3>
            <ul>
                <li><strong>Booking ID:</strong> %d</li>
                <li><strong>Hotel:</strong> %s</li>
                <li><strong>Refund Amount:</strong> Rs %.2f</li>
                <li><strong>Status:</strong> %s</li>
            </ul>
            <p>The refund will be credited to your original payment method within 5-7 business days.</p>
            <p>Best regards,<br>OHBS Team</p>
            <p>do not reply</p>
            """.formatted(
                customerName,
                booking.getHotel().getName(),
                booking.getId(),
                booking.getHotel().getName(),
                booking.getTotalCost(),
                booking.getStatus().toString()
            );

        helper.setText(htmlContent, true);
        mailSender.send(message);
        logger.info("Refund notification sent to: {}", userEmail);
    }

    public void sendRefundNotificationToManager(String customerName, Booking booking) throws MessagingException {
        String hotelContactEmail = booking.getHotel().getContactInfo();
        logger.info("Sending refund notification to hotel manager: {}", hotelContactEmail);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(hotelContactEmail);
        helper.setSubject("Refund Notification - " + booking.getHotel().getName());

        String htmlContent = """
            <h2>---THIS IS SYSTEM GENERATED MAIL---</h2>
            <h2>Refund Notification</h2>
            <p>Dear Manager,</p>
            <p>A refund has been processed for a canceled booking by <strong>%s</strong> at <strong>%s</strong> by admin.</p>
            <h3>Refund Details:</h3>
            <ul>
                <li><strong>Booking ID:</strong> %d</li>
                <li><strong>Customer:</strong> %s</li>
                <li><strong>Hotel:</strong> %s</li>
                <li><strong>Refund Amount:</strong> Rs %.2f</li>
                <li><strong>Status:</strong> %s</li>
            </ul>
            <p>Please update the system records accordingly.</p>
            <p>Best regards,<br>OHBS Team</p>
            <p>do not reply</p>
            """.formatted(
                customerName,
                booking.getHotel().getName(),
                booking.getId(),
                customerName,
                booking.getHotel().getName(),
                booking.getTotalCost(),
                booking.getStatus().toString()
            );

        helper.setText(htmlContent, true);
        mailSender.send(message);
        logger.info("Refund notification sent to hotel manager: {}", hotelContactEmail);
    }
    public void sendNewRoomsNotification(String userEmail, String customerName, String hotelName, int numberOfNewRooms) throws MessagingException {
        logger.info("Sending new rooms notification to: {}", userEmail);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(userEmail);
        helper.setSubject("Exciting News! New Rooms Available at " + hotelName);

        String htmlContent = """
            <h2>Discover New Rooms at %s!</h2>
            <p>Dear %s,</p>
            <p>We're thrilled to announce that <strong>%s</strong> has just added <strong>%d new rooms</strong> to its offerings! Now is the perfect time to plan your next getaway and experience the comfort and luxury of our latest accommodations.</p>
            <h3>Why Choose %s?</h3>
            <ul>
                <li><strong>Modern Comfort:</strong> Our new rooms are designed with your relaxation in mind, featuring state-of-the-art amenities and stunning views.</li>
                <li><strong>Prime Location:</strong> Enjoy the best of what %s has to offer, from vibrant city life to serene escapes.</li>
                <li><strong>Exclusive Offers:</strong> Book now to take advantage of special rates and promotions!</li>
            </ul>
            <p>Are you interested in staying with us? Don't miss out—secure your room today and embark on an unforgettable journey!</p>
            <p><a href="http://www.ohbs.com/book-now?hotel=%s" style="background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Book Now</a></p>
            <p>We can't wait to welcome you to <strong>%s</strong>!</p>
            <p>Best regards,<br>OHBS Team</p>
            """.formatted(
                hotelName,
                customerName,
                hotelName,
                numberOfNewRooms,
                hotelName,
                hotelName,
                hotelName,
                hotelName
            );

        helper.setText(htmlContent, true);
        mailSender.send(message);
        logger.info("New rooms notification sent to: {}", userEmail);
    }
    
}
