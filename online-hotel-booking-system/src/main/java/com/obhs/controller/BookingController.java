package com.obhs.controller;

import com.obhs.dto.BookingDTO;
import com.obhs.dto.HotelDTO;
import com.obhs.dto.RoomDTO;
import com.obhs.entity.Booking;
import com.obhs.entity.User;
import com.obhs.repository.UserRepository;
import com.obhs.service.BookingService;
import com.obhs.service.HotelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

            // Validate hotel
            HotelDTO hotel = hotelService.getHotelById(hotelId);
            if (hotel == null) {
                throw new IllegalArgumentException("Hotel not found");
            }

            // Validate room
            RoomDTO room = hotelService.getRoomById(hotelId, roomId);
            if (room == null) {
                throw new IllegalArgumentException("Room not found");
            }

            // Initialize booking DTO
            BookingDTO bookingDTO = new BookingDTO();
            bookingDTO.setHotelId(hotelId);
            bookingDTO.setRoomId(roomId);
            bookingDTO.setNumberOfRooms(1); // Default to 1 room

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
            // Set user ID and retrieve user email
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

            // Validate booking
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

            // Check room availability
            int availableRooms = bookingService.getAvailableRooms(
                    bookingDTO.getRoomId(),
                    bookingDTO.getCheckInDate(),
                    bookingDTO.getCheckOutDate());
            if (bookingDTO.getNumberOfRooms() > availableRooms) {
                throw new IllegalArgumentException("Not enough rooms available for the selected dates");
            }

            // Save booking
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
            // Retrieve booking to ensure it exists
            Booking booking = bookingService.getBookingById(bookingId);
            if (booking == null) {
                throw new IllegalArgumentException("Booking not found");
            }

            // Cancel booking
            bookingService.cancelBooking(bookingId);

            redirectAttributes.addFlashAttribute("successMessage", 
                "Booking canceled successfully! You will receive a cancellation email shortly.");
        } catch (IllegalArgumentException e) {
            logger.error("Error canceling booking: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/customer/bookings/history";
    }

    private Long getUserIdByUsername(String username) {
        logger.debug("Looking up user by email: {}", username);
        User user = userRepository.findByEmail(username).orElse(null);
        return user != null ? user.getId() : null;
    }
}