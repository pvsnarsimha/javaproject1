package com.obhs.service;

import com.obhs.entity.*;
import com.obhs.entity.Booking.PaymentStatus;
import com.obhs.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private FailedNotificationRepository failedNotificationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BookingService bookingService;

    // User Management
    public List<User> getAllUsers(String role) {
        List<User> users = userRepository.findAll();
        if (role != null && !role.isEmpty()) {
            users = users.stream()
                .filter(user -> user.getRoles() != null && user.getRoles().stream()
                    .anyMatch(r -> r.getName().equals(role)))
                .collect(Collectors.toList());
            logger.info("Fetched {} users with role {}", users.size(), role);
        } else {
            logger.info("Fetched {} users", users.size());
        }
        users.forEach(user -> logger.info("User {} ({}): Roles = {}", user.getId(), user.getEmail(), 
            user.getRoles() != null ? user.getRoles().stream().map(Role::getName).collect(Collectors.joining(", ")) : "None"));
        return users;
    }
    public User getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        logger.info("Fetched user {} ({}): Roles = {}", user.getId(), user.getEmail(), 
            user.getRoles() != null ? user.getRoles().stream().map(Role::getName).collect(Collectors.joining(", ")) : "None");
        return user;
    }

    @Transactional
    public void updateUser(User user, String roleName) {
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        existingUser.setName(user.getName());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhoneNumber(user.getPhoneNumber());
        if (roleName != null && !roleName.isEmpty()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
            existingUser.setRoles(new HashSet<>()); // Clear existing roles
            existingUser.getRoles().add(role); // Add the new role
            logger.info("Updated user {} ({}): Set role to {}", existingUser.getId(), existingUser.getEmail(), roleName);
        }
        userRepository.save(existingUser);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN"))) {
            throw new IllegalStateException("Cannot delete an admin user");
        }
        userRepository.delete(user);
    }

    // Hotel Management
    public List<Hotel> getAllHotels() {
        return hotelRepository.findAll();
    }

    public void createHotel(Hotel hotel, String amenitiesInput, String imagesInput) {
        List<String> amenities = amenitiesInput != null ? Arrays.asList(amenitiesInput.split(",\\s*")) : new ArrayList<>();
        List<String> images = imagesInput != null ? Arrays.asList(imagesInput.split(",\\s*")) : new ArrayList<>();
        hotel.setAmenities(amenities);
        hotel.setImages(images);
        hotelRepository.save(hotel);
    }

    @Transactional
    public void updateHotel(Hotel hotel, String amenitiesInput, String imagesInput) {
        Hotel existingHotel = hotelRepository.findById(hotel.getId())
                .orElseThrow(() -> new IllegalArgumentException("Hotel not found"));
        existingHotel.setName(hotel.getName());
        existingHotel.setAddress(hotel.getAddress());
        existingHotel.setContactInfo(hotel.getContactInfo());
        existingHotel.setDescription(hotel.getDescription());
        if (amenitiesInput != null && !amenitiesInput.isEmpty()) {
            List<String> amenities = Arrays.stream(amenitiesInput.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            existingHotel.setAmenities(amenities);
        }
        if (imagesInput != null && !imagesInput.isEmpty()) {
            List<String> images = Arrays.stream(imagesInput.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            existingHotel.setImages(images);
        }
        hotelRepository.save(existingHotel);
    }

    @Transactional
    public void deleteHotel(Long hotelId) {
        hotelRepository.deleteById(hotelId);
    }

    // Room Management
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @Transactional
    public void updateRoom(Room room, String imagesInput) {
        Room existingRoom = roomRepository.findById(room.getId())
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        existingRoom.setType(room.getType());
        existingRoom.setNumberOfRooms(room.getNumberOfRooms());
        existingRoom.setPricePerNight(room.getPricePerNight());
        if (imagesInput != null && !imagesInput.isEmpty()) {
            List<String> images = Arrays.stream(imagesInput.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            existingRoom.setImages(images);
        }
        existingRoom.setLastUpdated(java.time.LocalDateTime.now());
        roomRepository.save(existingRoom);
    }

    @Transactional
    public void deleteRoom(Long roomId) {
        roomRepository.deleteById(roomId);
    }

    // Booking Management
    public List<Booking> getAllBookings(String status, String paymentStatus) {
        List<Booking> bookings = bookingRepository.findAll();
        logger.debug("Retrieved {} bookings", bookings.size());

        // Filter by status if provided
        if (status != null && !status.isEmpty()) {
            try {
                String normalizedStatus = status.equalsIgnoreCase("CANCELED") ? "CANCELED" : status.toUpperCase();
                logger.debug("Filtering bookings by normalized status: {}", normalizedStatus);
                BookingStatus bookingStatus = BookingStatus.valueOf(normalizedStatus);
                bookings = bookings.stream()
                    .filter(b -> b.getStatus() == bookingStatus)
                    .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                logger.error("Invalid status: {}", status, e);
                throw new IllegalArgumentException("Invalid status: " + status, e);
            }
        }

        // Filter by payment status if provided
        if (paymentStatus != null && !paymentStatus.isEmpty()) {
            try {
                String normalizedPaymentStatus = paymentStatus.toUpperCase();
                logger.debug("Filtering bookings by payment status: {}", normalizedPaymentStatus);
                PaymentStatus paymentStatusEnum = PaymentStatus.valueOf(normalizedPaymentStatus);
                bookings = bookings.stream()
                    .filter(b -> b.getPaymentStatus() == paymentStatusEnum)
                    .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                logger.error("Invalid payment status: {}", paymentStatus, e);
                throw new IllegalArgumentException("Invalid payment status: " + paymentStatus, e);
            }
        }

        logger.debug("After filtering: {} bookings", bookings.size());
        return bookings;
    }
    @Transactional
    public void approveBooking(Long bookingId) {
        bookingService.confirmBooking(bookingId);
    }

    @Transactional
    public void rejectBooking(Long bookingId) {
        bookingService.cancelBooking(bookingId);
    }

    // Failed Notifications
    public List<FailedNotification> getAllFailedNotifications() {
        return failedNotificationRepository.findAll();
    }

    // Dashboard Statistics
    public long getTotalUsers() {
        return userRepository.count();
    }

    public long getTotalHotels() {
        return hotelRepository.count();
    }

    public long getTotalRooms() {
        return roomRepository.count();
    }

    public long getTotalBookings() {
        return bookingRepository.count();
    }
}
