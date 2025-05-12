package com.obhs.service;

import com.obhs.dto.BookingDTO;
import com.obhs.entity.Booking;
import com.obhs.entity.Hotel;
import com.obhs.entity.Room;
import com.obhs.entity.User;
import com.obhs.entity.Booking.BookingStatus;
import com.obhs.repository.BookingRepository;
import com.obhs.repository.HotelRepository;
import com.obhs.repository.RoomRepository;
import com.obhs.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

        // Fetch entities
        User user = userRepository.findById(bookingDTO.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Hotel hotel = hotelRepository.findById(bookingDTO.getHotelId())
            .orElseThrow(() -> new IllegalArgumentException("Hotel not found"));
        Room room = roomRepository.findById(bookingDTO.getRoomId())
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        // Validate room availability
        if (bookingDTO.getNumberOfRooms() > room.getNumberOfRooms()) {
            throw new IllegalArgumentException("Not enough rooms available. Only " + room.getNumberOfRooms() + " room(s) left.");
        }

        // Create booking entity
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setHotel(hotel);
        booking.setRoom(room);
        booking.setNumberOfRooms(bookingDTO.getNumberOfRooms());
        booking.setCheckInDate(bookingDTO.getCheckInDate());
        booking.setCheckOutDate(bookingDTO.getCheckOutDate());
        booking.setStatus(BookingStatus.PENDING); // Set status to PENDING

        // Calculate total cost (in rupees)
        long days = ChronoUnit.DAYS.between(bookingDTO.getCheckInDate(), bookingDTO.getCheckOutDate());
        double totalCost = days * room.getPricePerNight() * bookingDTO.getNumberOfRooms();
        booking.setTotalCost(totalCost);

        // Save booking
        Booking savedBooking = bookingRepository.save(booking);

        // Notify manager of new pending booking
        try {
            String customerName = user.getName();
            emailService.sendBookingNotificationToManager(customerName, savedBooking);
            logger.info("Pending booking notification sent to manager for booking ID: {}", savedBooking.getId());
        } catch (Exception e) {
            logger.error("Failed to send manager notification for booking ID: {}: {}", 
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

        // Update booking status to CONFIRMED
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        // Update room availability
        Room room = booking.getRoom();
        room.setNumberOfRooms(room.getNumberOfRooms() - booking.getNumberOfRooms());
        roomRepository.save(room);
        logger.info("Updated room availability for roomId: {}. New availability: {}", 
            room.getId(), room.getNumberOfRooms());

        // Send confirmation emails
        try {
            User user = userRepository.findById(booking.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
            String customerName = user.getName();
            emailService.sendBookingConfirmation(user.getEmail(), booking);
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

        // Fetch user for email notifications
        User user = userRepository.findById(booking.getUser().getId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // If booking is CONFIRMED, restore room availability
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            Room room = booking.getRoom();
            int roomsToRestore = booking.getNumberOfRooms();
            room.setNumberOfRooms(room.getNumberOfRooms() + roomsToRestore);
            roomRepository.save(room);
            logger.info("Restored room availability for roomId: {}. New availability: {}", 
                room.getId(), room.getNumberOfRooms());
        }

        // Update status to CANCELED
        booking.setStatus(BookingStatus.CANCELED);
        bookingRepository.save(booking);

        // Send cancellation notifications
        try {
            String customerName = user.getName();
            emailService.sendCancellationNotification(user.getEmail(), booking);
            emailService.sendCancellationNotificationToManager(customerName, booking);
            logger.info("Cancellation notifications sent for booking ID: {}", bookingId);
        } catch (Exception e) {
            logger.error("Failed to send cancellation notifications for booking ID: {}: {}", 
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
        return bookings.stream()
            .filter(booking -> booking.getStatus() != BookingStatus.CANCELED) // Exclude CANCELED bookings
            .map(booking -> {
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
}