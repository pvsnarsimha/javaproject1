package com.obhs.service;

import com.obhs.dto.HotelDTO;
import com.obhs.dto.RoomDTO;
import com.obhs.entity.Booking;
import com.obhs.entity.Hotel;
import com.obhs.entity.Room;
import com.obhs.repository.HotelRepository;
import com.obhs.repository.BookingRepository;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HotelService {
    private static final Logger logger = LoggerFactory.getLogger(HotelService.class);
    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public List<HotelDTO> getAllHotels() {
        return hotelRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<HotelDTO> getAllHotelsWithAvailableRooms() {
        List<Hotel> hotels = hotelRepository.findAll();
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        return hotels.stream().map(hotel -> {
            HotelDTO dto = convertToDTO(hotel);
            List<Room> rooms = hotel.getRooms() != null ? hotel.getRooms() : new ArrayList<>();
            List<RoomDTO> availableRooms = rooms.stream()
                .map(room -> {
                    RoomDTO roomDTO = convertRoomToDTO(room);
                    long bookedRooms = bookingRepository.findByRoomAndDateRange(room, today, tomorrow).size();
                    roomDTO.setNumberOfRooms((int) (room.getNumberOfRooms() - bookedRooms));
                    return roomDTO;
                })
                .filter(roomDTO -> roomDTO.getNumberOfRooms() > 0)
                .collect(Collectors.toList());
            dto.setRooms(availableRooms);
            return dto;
        }).collect(Collectors.toList());
    }

    public HotelDTO getHotelById(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
            .orElseThrow(() -> new IllegalArgumentException("Hotel not found"));
        return convertToDTO(hotel);
    }

    public RoomDTO getRoomById(Long hotelId, Long roomId) {
        Hotel hotel = hotelRepository.findById(hotelId)
            .orElseThrow(() -> new IllegalArgumentException("Hotel not found"));
        Room room = hotel.getRooms().stream()
            .filter(r -> r.getId().equals(roomId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        return convertRoomToDTO(room);
    }

    @Transactional
    public void saveHotel(HotelDTO hotelDTO) {
        if (!isValidEmail(hotelDTO.getContactInfo())) {
            logger.error("Invalid manager email: {}", hotelDTO.getContactInfo());
            throw new IllegalArgumentException("Manager email must be a valid email address");
        }
        Hotel hotel = new Hotel();
        hotel.setName(hotelDTO.getName());
        hotel.setAddress(hotelDTO.getAddress());
        hotel.setContactInfo(hotelDTO.getContactInfo());
        hotel.setDescription(hotelDTO.getDescription());
        hotel.setAmenities(hotelDTO.getAmenities());
        hotel.setImages(hotelDTO.getImages());
        hotel.setRooms(new ArrayList<>());
        hotelRepository.save(hotel);
        logger.info("Hotel saved: {}", hotel.getName());
    }

    @Transactional
    public void updateHotel(HotelDTO hotelDTO) {
        if (!isValidEmail(hotelDTO.getContactInfo())) {
            logger.error("Invalid manager email: {}", hotelDTO.getContactInfo());
            throw new IllegalArgumentException("Manager email must be a valid email address");
        }
        Hotel hotel = hotelRepository.findById(hotelDTO.getId())
            .orElseThrow(() -> new IllegalArgumentException("Hotel not found"));
        hotel.setName(hotelDTO.getName());
        hotel.setAddress(hotelDTO.getAddress());
        hotel.setContactInfo(hotelDTO.getContactInfo());
        hotel.setDescription(hotelDTO.getDescription());
        hotel.setAmenities(hotelDTO.getAmenities());
        hotel.setImages(hotelDTO.getImages());
        hotelRepository.save(hotel);
        logger.info("Hotel updated: {}", hotel.getName());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteHotel(Long hotelId) {
        logger.debug("Starting deletion of hotel ID: {}", hotelId);
        Hotel hotel = hotelRepository.findById(hotelId)
            .orElseThrow(() -> new IllegalArgumentException("Hotel not found"));

        // Initialize collections
        logger.debug("Initializing collections for hotel ID: {}", hotelId);
        Hibernate.initialize(hotel.getRooms());
        Hibernate.initialize(hotel.getAmenities());
        Hibernate.initialize(hotel.getImages());

        // Delete bookings for all rooms in the hotel
        if (hotel.getRooms() != null) {
            for (Room room : hotel.getRooms()) {
                Long roomId = room.getId();
                logger.debug("Fetching bookings for room ID: {} in hotel ID: {}", roomId, hotelId);
                List<Booking> bookings = bookingRepository.findByRoomId(roomId);
                logger.debug("Found {} bookings for room ID: {}", bookings.size(), roomId);
                for (Booking booking : bookings) {
                    logger.debug("Deleting booking ID: {} for room ID: {}", booking.getId(), roomId);
                    bookingRepository.delete(booking);
                    entityManager.flush();
                }
            }
        }

        // Manually delete dependent records
        logger.debug("Deleting rooms for hotel ID: {}", hotelId);
        hotelRepository.deleteRoomsByHotelId(hotelId);
        entityManager.flush();
        entityManager.clear();

        logger.debug("Deleting amenities for hotel ID: {}", hotelId);
        hotelRepository.deleteAmenitiesByHotelId(hotelId);
        entityManager.flush();
        entityManager.clear();

        logger.debug("Deleting images for hotel ID: {}", hotelId);
        hotelRepository.deleteImagesByHotelId(hotelId);
        entityManager.flush();
        entityManager.clear();

        // Delete the hotel
        logger.debug("Deleting hotel ID: {}", hotelId);
        hotelRepository.delete(hotel);
        entityManager.flush();
        logger.info("Hotel deleted: {}", hotel.getName());
    }

    @Transactional
    public void addRoom(Long hotelId, RoomDTO roomDTO) {
        Hotel hotel = hotelRepository.findById(hotelId)
            .orElseThrow(() -> new IllegalArgumentException("Hotel not found"));
        Room room = new Room();
        room.setType(roomDTO.getType());
        room.setNumberOfRooms(roomDTO.getNumberOfRooms());
        room.setPricePerNight(roomDTO.getPricePerNight());
        room.setImages(roomDTO.getImages());
        room.setHotel(hotel);
        if (hotel.getRooms() == null) {
            hotel.setRooms(new ArrayList<>());
        }
        hotel.getRooms().add(room);
        hotelRepository.save(hotel);
        logger.info("Room added to hotel: {}", hotel.getName());
    }

    @Transactional
    public void updateRoom(Long hotelId, RoomDTO roomDTO) {
        Hotel hotel = hotelRepository.findById(hotelId)
            .orElseThrow(() -> new IllegalArgumentException("Hotel not found"));
        Room room = hotel.getRooms().stream()
            .filter(r -> r.getId().equals(roomDTO.getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        room.setType(roomDTO.getType());
        room.setNumberOfRooms(roomDTO.getNumberOfRooms());
        room.setPricePerNight(roomDTO.getPricePerNight());
        room.setImages(roomDTO.getImages());
        hotelRepository.save(hotel);
        logger.info("Room updated for hotel: {}", hotel.getName());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteRoom(Long hotelId, Long roomId) {
        logger.debug("Starting deletion of room ID: {} from hotel ID: {}", roomId, hotelId);
        Hotel hotel = hotelRepository.findById(hotelId)
            .orElseThrow(() -> new IllegalArgumentException("Hotel not found"));
        Room room = hotel.getRooms().stream()
            .filter(r -> r.getId().equals(roomId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        // Ensure room is initialized
        Hibernate.initialize(room);
        logger.debug("Room ID: {} initialized, belongs to hotel: {}", roomId, hotel.getName());

        // Fetch and delete all bookings associated with this room
        logger.debug("Fetching bookings for room ID: {}", roomId);
        List<Booking> bookings = bookingRepository.findByRoomId(roomId);
        logger.debug("Found {} bookings for room ID: {}", bookings.size(), roomId);
        for (Booking booking : bookings) {
            logger.debug("Deleting booking ID: {} for room ID: {}", booking.getId(), roomId);
            bookingRepository.delete(booking);
            entityManager.flush();
        }

        // Verify no bookings remain
        bookings = bookingRepository.findByRoomId(roomId);
        logger.debug("After deletion, {} bookings remain for room ID: {}", bookings.size(), roomId);
        if (!bookings.isEmpty()) {
            logger.error("Failed to delete all bookings for room ID: {}. Aborting room deletion.", roomId);
            throw new IllegalStateException("Failed to delete all bookings for room ID: " + roomId);
        }

        // Remove the room from the hotel's room list and save
        hotel.getRooms().remove(room);
        hotelRepository.save(hotel);
        entityManager.flush();
        logger.info("Room deleted from hotel: {}", hotel.getName());
    }

    private HotelDTO convertToDTO(Hotel hotel) {
        HotelDTO dto = new HotelDTO();
        dto.setId(hotel.getId());
        dto.setName(hotel.getName());
        dto.setAddress(hotel.getAddress());
        dto.setContactInfo(hotel.getContactInfo());
        dto.setDescription(hotel.getDescription());
        dto.setAmenities(hotel.getAmenities());
        dto.setImages(hotel.getImages());
        List<RoomDTO> roomDTOs = hotel.getRooms() != null
            ? hotel.getRooms().stream().map(this::convertRoomToDTO).collect(Collectors.toList())
            : new ArrayList<>();
        dto.setRooms(roomDTOs);
        return dto;
    }

    private RoomDTO convertRoomToDTO(Room room) {
        RoomDTO dto = new RoomDTO();
        dto.setId(room.getId());
        dto.setType(room.getType());
        dto.setNumberOfRooms(room.getNumberOfRooms());
        dto.setPricePerNight(room.getPricePerNight());
        dto.setImages(room.getImages());
        return dto;
    }

    private boolean isValidEmail(String email) {
        if (email == null) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(emailRegex);
    }
}