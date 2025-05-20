package com.obhs.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Image;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.constants.StandardFonts;
import com.obhs.dto.BookingDTO;
import com.obhs.dto.PaymentDTO;
import com.obhs.entity.Booking;
import com.obhs.entity.Hotel;
import com.obhs.entity.Room;
import com.obhs.entity.User;
import com.obhs.entity.BookingStatus;
import com.obhs.repository.BookingRepository;
import com.obhs.repository.HotelRepository;
import com.obhs.repository.RoomRepository;
import com.obhs.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BookingService {
    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private EmailService emailService;

    @Transactional
    public Long saveBooking(BookingDTO bookingDTO) {
        logger.debug("Saving booking for userId: {}, hotelId: {}, roomId: {}", 
            bookingDTO.getUserId(), bookingDTO.getHotelId(), bookingDTO.getRoomId());

        User user = userRepository.findById(bookingDTO.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Hotel hotel = hotelRepository.findById(bookingDTO.getHotelId())
            .orElseThrow(() -> new IllegalArgumentException("Hotel not found"));
        Room room = roomRepository.findById(bookingDTO.getRoomId())
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (bookingDTO.getNumberOfRooms() > room.getNumberOfRooms()) {
            throw new IllegalArgumentException("Not enough rooms available. Only " + room.getNumberOfRooms() + " room(s) left.");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setHotel(hotel);
        booking.setRoom(room);
        booking.setNumberOfRooms(bookingDTO.getNumberOfRooms());
        booking.setCheckInDate(bookingDTO.getCheckInDate());
        booking.setCheckOutDate(bookingDTO.getCheckOutDate());
        booking.setStatus(BookingStatus.PENDING);

        long days = ChronoUnit.DAYS.between(bookingDTO.getCheckInDate(), bookingDTO.getCheckOutDate());
        double totalCost = days * room.getPricePerNight() * bookingDTO.getNumberOfRooms();
        booking.setTotalCost(totalCost);

        Booking savedBooking = bookingRepository.save(booking);

        try {
            String customerName = user.getName();
            emailService.sendBookingNotificationToManager(customerName, savedBooking);
            emailService.sendBookingPendingNotification(user.getEmail(), customerName, savedBooking);
            logger.info("Pending booking notifications sent for booking ID: {}", savedBooking.getId());
        } catch (Exception e) {
            logger.error("Failed to send notifications for booking ID: {}: {}", 
                savedBooking.getId(), e.getMessage());
        }

        return savedBooking.getId();
    }

    @Transactional
    public void confirmBooking(Long bookingId) {
        logger.debug("Confirming booking ID: {}", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking is not in PENDING status");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        Room room = booking.getRoom();
        room.setNumberOfRooms(room.getNumberOfRooms() - booking.getNumberOfRooms());
        roomRepository.save(room);
        logger.info("Updated room availability for roomId: {}. New availability: {}", 
            room.getId(), room.getNumberOfRooms());

        try {
            User user = userRepository.findById(booking.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
            String customerName = user.getName();
            emailService.sendBookingConfirmation(user.getEmail(), customerName, booking);
            emailService.sendBookingNotificationToManager(customerName, booking);
            logger.info("Confirmation notifications sent for booking ID: {}", bookingId);
        } catch (Exception e) {
            logger.error("Failed to send confirmation notifications for booking ID: {}: {}", 
                bookingId, e.getMessage());
        }
    }

    @Transactional
    public void cancelBooking(Long bookingId) {
        logger.debug("Canceling booking ID: {}", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        User user = userRepository.findById(booking.getUser().getId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            Room room = booking.getRoom();
            int roomsToRestore = booking.getNumberOfRooms();
            room.setNumberOfRooms(room.getNumberOfRooms() + roomsToRestore);
            roomRepository.save(room);
            logger.info("Restored room availability for roomId: {}. New availability: {}", 
                room.getId(), room.getNumberOfRooms());
        }

        booking.setStatus(BookingStatus.CANCELED);

        if (booking.getPaymentStatus() == Booking.PaymentStatus.SUCCESSFUL) {
            booking.setPaymentStatus(Booking.PaymentStatus.REFUNDED);
        }

        bookingRepository.save(booking);

        try {
            String customerName = user.getName();
            emailService.sendCancellationNotification(user.getEmail(), customerName, booking);
            emailService.sendCancellationNotificationToManager(customerName, booking);
            if (booking.getPaymentStatus() == Booking.PaymentStatus.REFUNDED) {
                emailService.sendRefundNotification(user.getEmail(), customerName, booking);
                emailService.sendRefundNotificationToManager(customerName, booking);
            }
            logger.info("Cancellation and refund notifications sent for booking ID: {}", bookingId);
        } catch (Exception e) {
            logger.error("Failed to send cancellation or refund notifications for booking ID: {}: {}", 
                bookingId, e.getMessage());
        }
    }

    public Booking getBookingById(Long bookingId) {
        logger.debug("Retrieving booking ID: {}", bookingId);
        return bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
    }

    public List<BookingDTO> getUserBookings(Long userId) {
        logger.debug("Retrieving bookings for user ID: {}", userId);
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        logger.debug("Retrieved {} bookings for user ID: {}", bookings.size(), userId);
        return bookings.stream()
            .map(booking -> {
                logger.debug("Mapping booking ID: {} with status: {}", booking.getId(), booking.getStatus());
                BookingDTO dto = new BookingDTO();
                dto.setId(booking.getId());
                dto.setUserId(booking.getUser().getId());
                dto.setHotelId(booking.getHotel().getId());
                dto.setRoomId(booking.getRoom().getId());
                dto.setHotelName(booking.getHotel().getName());
                dto.setRoomType(booking.getRoom().getType());
                dto.setNumberOfRooms(booking.getNumberOfRooms());
                dto.setCheckInDate(booking.getCheckInDate());
                dto.setCheckOutDate(booking.getCheckOutDate());
                dto.setTotalCost(booking.getTotalCost());
                dto.setStatus(booking.getStatus().toString());
                dto.setPaymentStatus(booking.getPaymentStatus().toString());
                return dto;
            }).collect(Collectors.toList());
    }

    public int getAvailableRooms(Long roomId, LocalDate checkInDate, LocalDate checkOutDate) {
        logger.debug("Checking available rooms for roomId: {}, checkIn: {}, checkOut: {}", 
            roomId, checkInDate, checkOutDate);
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        return room.getNumberOfRooms();
    }

    @Transactional
    public void processPayment(PaymentDTO paymentDTO) {
        logger.debug("Processing payment for booking ID: {}", paymentDTO.getBookingId());
        Booking booking = bookingRepository.findById(paymentDTO.getBookingId())
            .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        
        if (booking.getPaymentStatus() == Booking.PaymentStatus.SUCCESSFUL || 
            booking.getPaymentStatus() == Booking.PaymentStatus.REFUNDED) {
            throw new IllegalStateException("Payment already completed or refunded");
        }

        User user = userRepository.findById(booking.getUser().getId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        BigDecimal paymentAmount = BigDecimal.valueOf(paymentDTO.getAmount())
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCost = BigDecimal.valueOf(booking.getTotalCost())
            .setScale(2, RoundingMode.HALF_UP);

        logger.debug("Payment amount: {}, Booking total cost: {}", paymentAmount, totalCost);

        if (paymentAmount.compareTo(totalCost) == 0) {
            booking.setPaymentStatus(Booking.PaymentStatus.SUCCESSFUL);
            logger.info("Payment status updated to SUCCESSFUL for booking ID: {}", booking.getId());
        } else {
            booking.setPaymentStatus(Booking.PaymentStatus.PENDING);
            logger.warn("Payment amount {} does not match total cost {} for booking ID: {}. Status remains PENDING.", 
                paymentAmount, totalCost, booking.getId());
        }

        bookingRepository.save(booking);
    }

    @Transactional
    public void sendPaymentNotificationsWithBill(Long bookingId, double paidAmount, String billAbsolutePath) {
        logger.debug("Sending payment notifications for booking ID: {}", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        User user = userRepository.findById(booking.getUser().getId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (booking.getPaymentStatus() == Booking.PaymentStatus.SUCCESSFUL) {
            if (billAbsolutePath == null || !new File(billAbsolutePath).exists()) {
                logger.warn("Bill PDF not found or not provided for booking ID: {}. Regenerating PDF.", bookingId);
                Map<String, Object> billData = generateBillData(bookingId);
                billAbsolutePath = regenerateBillPDF(bookingId, billData);
            }
        }

        try {
            String customerName = user.getName();
            emailService.sendPaymentNotification(user.getEmail(), customerName, booking, paidAmount, billAbsolutePath);
            emailService.sendPaymentNotificationToManager(customerName, booking, paidAmount, billAbsolutePath);
            logger.info("Payment notifications sent for booking ID: {}", booking.getId());
        } catch (Exception e) {
            logger.error("Failed to send payment notifications for booking ID: {}: {}", 
                booking.getId(), e.getMessage());
            throw new RuntimeException("Failed to send payment notifications", e);
        }
    }

    private String regenerateBillPDF(Long bookingId, Map<String, Object> billData) {
        logger.info("Regenerating bill PDF for booking ID: {}", bookingId);
        try {
            String tempDirPath = System.getProperty("java.io.tmpdir") + File.separator + "bills" + File.separator;
            File tempDir = new File(tempDirPath);
            if (!tempDir.exists()) {
                boolean created = tempDir.mkdirs();
                if (!created) {
                    throw new RuntimeException("Failed to create bills directory: " + tempDirPath);
                }
            }

            String pdfFilePath = tempDirPath + "booking_" + bookingId + "_bill.pdf";
            File pdfFile = new File(pdfFilePath);
            if (pdfFile.exists()) {
                boolean deleted = pdfFile.delete();
                if (!deleted) {
                    logger.warn("Failed to delete existing PDF at: {}. Proceeding with regeneration.", pdfFilePath);
                }
            }

            String userName = (String) billData.get("userName");
            String userEmail = (String) billData.get("userEmail");
            String hotelName = (String) billData.get("hotelName");
            String hotelAddress = (String) billData.get("hotelAddress");
            String hotelContact = (String) billData.get("hotelContact");
            String roomType = (String) billData.get("roomType");
            String checkInDate = (String) billData.get("checkInDate");
            String checkOutDate = (String) billData.get("checkOutDate");
            Integer numberOfRooms = (Integer) billData.get("numberOfRooms");
            Double totalCost = (Double) billData.get("totalCost");
            String paymentStatus = (String) billData.get("paymentStatus");

            double baseAmount = totalCost / 1.18;
            double cgst = baseAmount * 0.09;
            double sgst = baseAmount * 0.09;
            baseAmount = Math.round(baseAmount * 100.0) / 100.0;
            cgst = Math.round(cgst * 100.0) / 100.0;
            sgst = Math.round(sgst * 100.0) / 100.0;

            PdfWriter writer = new PdfWriter(pdfFilePath);
            PdfDocument pdf = new PdfDocument(writer);
            pdf.setDefaultPageSize(PageSize.A4);
            Document document = new Document(pdf);

            PdfFont titleFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont bodyFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont smallFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            String logoPath = "src/main/resources/static/images/logo.png";
            File logoFile = new File(logoPath);
            if (logoFile.exists()) {
                Image logo = new Image(ImageDataFactory.create(logoPath))
                    .scaleToFit(100, 50)
                    .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                document.add(logo);
            } else {
                logger.warn("Logo file not found at: {}", logoPath);
                document.add(new Paragraph("OHBS Hotel")
                    .setFont(titleFont)
                    .setFontSize(20)
                    .setTextAlignment(TextAlignment.CENTER));
            }

            document.add(new Paragraph(hotelName)
                .setFont(titleFont)
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph(hotelAddress)
                .setFont(bodyFont)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Contact: " + hotelContact + " | GSTIN: 12ABCDE1234F1Z5")
                .setFont(bodyFont)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Tax Invoice")
                .setFont(titleFont)
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Invoice No: OHBS/" + bookingId + "/" + LocalDateTime.now().getYear())
                .setFont(bodyFont)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.LEFT));
            document.add(new Paragraph("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")))
                .setFont(bodyFont)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.LEFT));
            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Guest Details")
                .setFont(titleFont)
                .setFontSize(14)
                .setBold());
            float[] guestColumnWidths = {1, 2};
            Table guestTable = new Table(UnitValue.createPercentArray(guestColumnWidths)).useAllAvailableWidth();
            guestTable.addCell(new Cell().add(new Paragraph("Name").setFont(bodyFont).setFontSize(10).setBold()));
            guestTable.addCell(new Cell().add(new Paragraph(userName).setFont(bodyFont).setFontSize(10)));
            guestTable.addCell(new Cell().add(new Paragraph("Email").setFont(bodyFont).setFontSize(10).setBold()));
            guestTable.addCell(new Cell().add(new Paragraph(userEmail).setFont(bodyFont).setFontSize(10)));
            document.add(guestTable);
            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Booking Details")
                .setFont(titleFont)
                .setFontSize(14)
                .setBold());
            float[] bookingColumnWidths = {3, 1, 1, 1};
            Table bookingTable = new Table(UnitValue.createPercentArray(bookingColumnWidths)).useAllAvailableWidth();
            bookingTable.addCell(new Cell().add(new Paragraph("Description").setFont(bodyFont).setFontSize(10).setBold()));
            bookingTable.addCell(new Cell().add(new Paragraph("Quantity").setFont(bodyFont).setFontSize(10).setBold()));
            bookingTable.addCell(new Cell().add(new Paragraph("Rate (INR)").setFont(bodyFont).setFontSize(10).setBold()));
            bookingTable.addCell(new Cell().add(new Paragraph("Amount (INR)").setFont(bodyFont).setFontSize(10).setBold()));

            long nights = java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDate.parse(checkInDate),
                java.time.LocalDate.parse(checkOutDate)
            );
            double ratePerNight = baseAmount / (numberOfRooms * nights);

            bookingTable.addCell(new Cell().add(new Paragraph(roomType + " (Room Charges)").setFont(bodyFont).setFontSize(10)));
            bookingTable.addCell(new Cell().add(new Paragraph(numberOfRooms + " x " + nights + " night(s)").setFont(bodyFont).setFontSize(10)));
            bookingTable.addCell(new Cell().add(new Paragraph(String.format("%.2f", ratePerNight)).setFont(bodyFont).setFontSize(10)));
            bookingTable.addCell(new Cell().add(new Paragraph(String.format("%.2f", baseAmount)).setFont(bodyFont).setFontSize(10)));

            document.add(bookingTable);
            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Tax Details")
                .setFont(titleFont)
                .setFontSize(14)
                .setBold());
            float[] taxColumnWidths = {4, 1};
            Table taxTable = new Table(UnitValue.createPercentArray(taxColumnWidths)).useAllAvailableWidth();
            taxTable.addCell(new Cell().add(new Paragraph("CGST (9%)").setFont(bodyFont).setFontSize(10)));
            taxTable.addCell(new Cell().add(new Paragraph(String.format("%.2f", cgst)).setFont(bodyFont).setFontSize(10)));
            taxTable.addCell(new Cell().add(new Paragraph("SGST (9%)").setFont(bodyFont).setFontSize(10)));
            taxTable.addCell(new Cell().add(new Paragraph(String.format("%.2f", sgst)).setFont(bodyFont).setFontSize(10)));
            taxTable.addCell(new Cell().add(new Paragraph("Total Amount").setFont(bodyFont).setFontSize(10).setBold()));
            taxTable.addCell(new Cell().add(new Paragraph(String.format("%.2f", totalCost)).setFont(bodyFont).setFontSize(10).setBold()));
            document.add(taxTable);
            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Payment Status: " + paymentStatus)
                .setFont(bodyFont)
                .setFontSize(10)
                .setBold());
            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Terms and Conditions")
                .setFont(titleFont)
                .setFontSize(12)
                .setBold());
            document.add(new Paragraph("- Payment is due upon receipt unless otherwise agreed.")
                .setFont(smallFont)
                .setFontSize(8));
            document.add(new Paragraph("- Cancellations made within 48 hours of check-in are non-refunded.")
                .setFont(smallFont)
                .setFontSize(8));
            document.add(new Paragraph("- All disputes are subject to [City] jurisdiction.")
                .setFont(smallFont)
                .setFontSize(8));
            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Thank you for choosing " + hotelName + "!")
                .setFont(bodyFont)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setItalic());
            document.add(new Paragraph("For inquiries, contact us at: " + hotelContact)
                .setFont(smallFont)
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Experience Luxury with OHBS")
                .setFont(smallFont)
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER));

            document.close();
            logger.info("Successfully regenerated PDF for booking ID: {} at {}", bookingId, pdfFilePath);
            return pdfFilePath;
        } catch (Exception e) {
            logger.error("Error regenerating bill PDF for booking ID: {}: {}", bookingId, e.getMessage());
            throw new RuntimeException("Failed to regenerate bill PDF", e);
        }
    }

    public Map<String, Object> generateBillData(Long bookingId) {
        logger.debug("Generating bill data for booking ID: {}", bookingId);
        Map<String, Object> billData = new HashMap<>();

        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (booking.getPaymentStatus() != Booking.PaymentStatus.SUCCESSFUL) {
            throw new IllegalStateException("Payment not completed for this booking");
        }

        User user = userRepository.findById(booking.getUser().getId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Hotel hotel = booking.getHotel();
        Room room = booking.getRoom();

        billData.put("bookingId", booking.getId());
        billData.put("userName", user.getName());
        billData.put("userEmail", user.getEmail());
        billData.put("hotelName", hotel.getName());
        billData.put("hotelAddress", hotel.getAddress());
        billData.put("hotelContact", hotel.getContactInfo());
        billData.put("roomType", room.getType());
        billData.put("checkInDate", booking.getCheckInDate().toString());
        billData.put("checkOutDate", booking.getCheckOutDate().toString());
        billData.put("numberOfRooms", booking.getNumberOfRooms());
        billData.put("totalCost", booking.getTotalCost());
        billData.put("paymentStatus", booking.getPaymentStatus().toString());

        return billData;
    }
}