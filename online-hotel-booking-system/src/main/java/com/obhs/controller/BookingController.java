package com.obhs.controller;

import com.obhs.dto.BookingDTO;
import com.obhs.dto.HotelDTO;
import com.obhs.dto.RoomDTO;
import com.obhs.dto.PaymentDTO;
import com.obhs.entity.Booking;
import com.obhs.entity.BookingStatus;
import com.obhs.entity.User;
import com.obhs.repository.UserRepository;
import com.obhs.service.BookingService;
import com.obhs.service.HotelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.constants.StandardFonts;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/customer")
public class BookingController {
    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

    @Autowired
    private BookingService bookingService;

    @Autowired
    private HotelService hotelService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/bookings/new")
    public String newBookingForm(
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false) Long roomId,
            Model model,
            RedirectAttributes redirectAttributes) {
        logger.info("Displaying new booking form for hotelId: {}, roomId: {}", hotelId, roomId);
        try {
            if (hotelId == null || roomId == null) {
                throw new IllegalArgumentException("Hotel and room must be selected");
            }

            HotelDTO hotel = hotelService.getHotelById(hotelId);
            if (hotel == null) {
                throw new IllegalArgumentException("Hotel not found");
            }

            RoomDTO room = hotelService.getRoomById(hotelId, roomId);
            if (room == null) {
                throw new IllegalArgumentException("Room not found");
            }

            BookingDTO bookingDTO = new BookingDTO();
            bookingDTO.setHotelId(hotelId);
            bookingDTO.setRoomId(roomId);
            bookingDTO.setNumberOfRooms(1);

            model.addAttribute("booking", bookingDTO);
            model.addAttribute("hotel", hotel);
            model.addAttribute("room", room);
            return "booking/booking-form";
        } catch (IllegalArgumentException e) {
            logger.error("Error retrieving hotel or room: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/hotels";
        }
    }

    @PostMapping("/bookings")
    public String createBooking(
            @ModelAttribute("booking") BookingDTO bookingDTO,
            RedirectAttributes redirectAttributes) {
        logger.info("Creating booking for roomId: {}", bookingDTO.getRoomId());
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            if (username == null || username.isEmpty()) {
                logger.error("Authentication username is null or empty");
                return "redirect:/login?sessionExpired";
            }
            logger.info("Authenticated username: {}", username);
            User user = userRepository.findByEmail(username).orElse(null);
            if (user == null) {
                logger.warn("User not found for username: {}", username);
                return "redirect:/login?sessionExpired";
            }
            Long userId = user.getId();
            logger.info("Setting userId: {} for username: {}", userId, username);
            bookingDTO.setUserId(userId);

            RoomDTO room = hotelService.getRoomById(bookingDTO.getHotelId(), bookingDTO.getRoomId());
            if (room == null) {
                throw new IllegalArgumentException("Room not found");
            }
            if (bookingDTO.getNumberOfRooms() <= 0 || bookingDTO.getNumberOfRooms() > room.getNumberOfRooms()) {
                throw new IllegalArgumentException("Invalid number of rooms requested");
            }
            if (bookingDTO.getCheckInDate().isAfter(bookingDTO.getCheckOutDate())) {
                throw new IllegalArgumentException("Check-out date must be after check-in date");
            }

            int availableRooms = bookingService.getAvailableRooms(
                    bookingDTO.getRoomId(),
                    bookingDTO.getCheckInDate(),
                    bookingDTO.getCheckOutDate());
            if (bookingDTO.getNumberOfRooms() > availableRooms) {
                throw new IllegalArgumentException("Not enough rooms available for the selected dates");
            }

            Long bookingId = bookingService.saveBooking(bookingDTO);

            redirectAttributes.addFlashAttribute("successMessage",
                "Booking submitted successfully! Booking ID: " + bookingId + ". Awaiting admin approval.");
            return "redirect:/customer/bookings/history";
        } catch (IllegalArgumentException e) {
            if ("User not found".equals(e.getMessage())) {
                logger.warn("User not found during booking creation");
                return "redirect:/login?sessionExpired";
            } else {
                logger.error("Error creating booking: {}", e.getMessage());
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
                return "redirect:/customer/bookings/new?hotelId=" + bookingDTO.getHotelId() + "&roomId=" + bookingDTO.getRoomId();
            }
        } catch (IllegalStateException e) {
            logger.error("Error creating booking: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/customer/bookings/new?hotelId=" + bookingDTO.getHotelId() + "&roomId=" + bookingDTO.getRoomId();
        }
    }

    @GetMapping("/bookings/history")
    public String bookingHistory(Model model) {
        logger.info("Displaying booking history");
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (username == null || username.isEmpty()) {
            logger.error("Authentication username is null or empty for booking history");
            return "redirect:/login?sessionExpired";
        }
        Long userId = getUserIdByUsername(username);
        if (userId == null) {
            logger.warn("User not authenticated or does not exist for username: {}", username);
            return "redirect:/login?sessionExpired";
        }
        model.addAttribute("bookings", bookingService.getUserBookings(userId));
        return "booking/booking-history";
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public String cancelBooking(@PathVariable Long bookingId, RedirectAttributes redirectAttributes) {
        logger.info("Canceling booking ID: {}", bookingId);
        try {
            Booking booking = bookingService.getBookingById(bookingId);
            if (booking == null) {
                throw new IllegalArgumentException("Booking not found");
            }

            bookingService.cancelBooking(bookingId);

            redirectAttributes.addFlashAttribute("successMessage",
                "Booking canceled successfully! You will receive a cancellation email shortly.");
        } catch (IllegalArgumentException e) {
            logger.error("Error canceling booking: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/customer/bookings/history";
    }

    @GetMapping("/bookings/{bookingId}/pay")
    public String paymentForm(@PathVariable Long bookingId, Model model, RedirectAttributes redirectAttributes) {
        logger.info("Displaying payment form for booking ID: {}", bookingId);
        try {
            Booking booking = bookingService.getBookingById(bookingId);
            if (booking == null) {
                throw new IllegalArgumentException("Booking not found");
            }
            logger.debug("Booking ID: {}, Payment Status: {}", bookingId, booking.getPaymentStatus());
            if (booking.getPaymentStatus() == Booking.PaymentStatus.SUCCESSFUL) {
                throw new IllegalStateException("Payment already completed");
            }
            if (booking.getStatus() == BookingStatus.CANCELED) {
                throw new IllegalStateException("Cannot process payment for a canceled booking");
            }
            PaymentDTO paymentDTO = new PaymentDTO();
            paymentDTO.setBookingId(bookingId);
            paymentDTO.setAmount(booking.getTotalCost());
            model.addAttribute("payment", paymentDTO);
            model.addAttribute("booking", booking);
            return "booking/payment-form";
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.error("Error displaying payment form: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/customer/bookings/history";
        }
    }

    @PostMapping("/bookings/{bookingId}/pay")
    public String processPayment(@PathVariable Long bookingId, @ModelAttribute("payment") PaymentDTO paymentDTO, RedirectAttributes redirectAttributes) {
        logger.info("Processing payment for booking ID: {}", bookingId);
        try {
            if (!bookingId.equals(paymentDTO.getBookingId())) {
                throw new IllegalArgumentException("Booking ID mismatch");
            }
            bookingService.processPayment(paymentDTO);
            String status = bookingService.getBookingById(paymentDTO.getBookingId()).getPaymentStatus().toString();
            if ("SUCCESSFUL".equals(status)) {
                Map<String, Object> billData = bookingService.generateBillData(bookingId);
                String[] paths = generateBillPDF(bookingId, billData);
                String billRelativePath = paths[0]; // For redirect
                String billAbsolutePath = paths[1]; // For email attachment
                redirectAttributes.addFlashAttribute("billPath", billRelativePath);
                bookingService.sendPaymentNotificationsWithBill(bookingId, paymentDTO.getAmount(), billAbsolutePath);
                redirectAttributes.addFlashAttribute("successMessage", "Payment processed successfully! Status: " + status + ". Download your bill <a href='/customer/bookings/" + bookingId + "/bill'>here</a>.");
            } else {
                bookingService.sendPaymentNotificationsWithBill(bookingId, paymentDTO.getAmount(), null);
                redirectAttributes.addFlashAttribute("successMessage", "Payment processed successfully! Status: " + status);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.error("Error processing payment: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/customer/bookings/history";
    }

    private String[] generateBillPDF(Long bookingId, Map<String, Object> billData) {
        logger.info("Generating bill PDF for booking ID: {}", bookingId);
        try {
            // Use the system's temporary directory for writing files
            String tempDirPath = System.getProperty("java.io.tmpdir") + File.separator + "bills" + File.separator;
            File tempDir = new File(tempDirPath);
            if (!tempDir.exists()) {
                boolean created = tempDir.mkdirs();
                if (!created) {
                    throw new IOException("Failed to create bills directory: " + tempDirPath);
                }
            }

            String pdfFilePath = tempDirPath + "booking_" + bookingId + "_bill.pdf";
            String pdfRelativePath = "/bills/booking_" + bookingId + "_bill.pdf";

            // Extract bill data
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

            // Calculate tax (18% GST: 9% CGST + 9% SGST)
            double baseAmount = totalCost / 1.18; // Remove 18% GST to get base
            double cgst = baseAmount * 0.09; // 9% CGST
            double sgst = baseAmount * 0.09; // 9% SGST
            baseAmount = Math.round(baseAmount * 100.0) / 100.0;
            cgst = Math.round(cgst * 100.0) / 100.0;
            sgst = Math.round(sgst * 100.0) / 100.0;

            // Create PDF using iText
            PdfWriter writer = new PdfWriter(pdfFilePath);
            PdfDocument pdf = new PdfDocument(writer);
            pdf.setDefaultPageSize(PageSize.A4);
            Document document = new Document(pdf);

            // Fonts
            PdfFont titleFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont bodyFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont smallFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // Header with Logo
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

            // Hotel Details
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

            // Invoice Title
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

            // Guest Details
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

            // Booking Details
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

            // Calculate rate per night
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

            // Tax and Total
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

            // Payment Status
            document.add(new Paragraph("Payment Status: " + paymentStatus)
                .setFont(bodyFont)
                .setFontSize(10)
                .setBold());
            document.add(new Paragraph("\n"));

            // Terms and Conditions
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

            // Footer
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
            logger.info("Successfully generated PDF for booking ID: {} at {}", bookingId, pdfFilePath);

            return new String[]{pdfRelativePath, pdfFilePath};
        } catch (IOException e) {
            logger.error("Error generating bill PDF for booking ID: {}: {}", bookingId, e.getMessage());
            throw new RuntimeException("Failed to generate bill PDF", e);
        }
    }

    @GetMapping("/bookings/{bookingId}/bill")
    public ResponseEntity<Resource> downloadBill(@PathVariable Long bookingId) {
        logger.info("Request to download bill for booking ID: {}", bookingId);
        try {
            Booking booking = bookingService.getBookingById(bookingId);
            if (booking == null) {
                throw new IllegalArgumentException("Booking not found");
            }
            if (booking.getPaymentStatus() != Booking.PaymentStatus.SUCCESSFUL) {
                throw new IllegalStateException("Payment not completed for this booking");
            }

            String tempDirPath = System.getProperty("java.io.tmpdir") + File.separator + "bills" + File.separator;
            String pdfFilePath = tempDirPath + "booking_" + bookingId + "_bill.pdf";
            File pdfFile = new File(pdfFilePath);

            if (!pdfFile.exists()) {
                logger.info("PDF not found, generating bill for booking ID: {}", bookingId);
                Map<String, Object> billData = bookingService.generateBillData(bookingId);
                generateBillPDF(bookingId, billData);
            }

            Resource resource = new FileSystemResource(pdfFilePath);
            if (!resource.exists()) {
                throw new IllegalStateException("Bill PDF not found for booking ID: " + bookingId + " after generation attempt");
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=booking_" + bookingId + "_bill.pdf")
                    .body(resource);
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.error("Error downloading bill for booking ID: {}: {}", bookingId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    private Long getUserIdByUsername(String username) {
        logger.debug("Looking up user by email: {}", username);
        User user = userRepository.findByEmail(username).orElse(null);
        return user != null ? user.getId() : null;
    }
}