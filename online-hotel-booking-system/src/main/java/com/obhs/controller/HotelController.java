package com.obhs.controller;

import com.obhs.dto.HotelDTO;
import com.obhs.dto.RoomDTO;
import com.obhs.service.HotelService;
import com.obhs.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/manager")
public class HotelController {
    private static final Logger logger = LoggerFactory.getLogger(HotelController.class);

    @Autowired
    private HotelService hotelService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/hotels/edit")
    public String handleInvalidEdit(RedirectAttributes redirectAttributes) {
        logger.warn("Invalid access to /manager/hotels/edit without hotelId");
        redirectAttributes.addFlashAttribute("errorMessage", "Please select a hotel to edit from the hotel list.");
        return "redirect:/manager/hotels";
    }

    @GetMapping("/hotels")
    public String listHotels(Model model) {
        logger.info("Listing all hotels for manager");
        model.addAttribute("hotels", hotelService.getAllHotels());
        return "hotel/hotel-list";
    }

    @GetMapping("/hotels/new")
    public String newHotelForm(Model model) {
        logger.info("Displaying new hotel form");
        model.addAttribute("hotel", new HotelDTO());
        return "hotel/hotel-form";
    }

    @PostMapping("/hotels")
    public String saveHotel(
            @ModelAttribute("hotel") HotelDTO hotelDTO,
            RedirectAttributes redirectAttributes) {
        logger.info("Saving hotel: {}", hotelDTO.getName());
        try {
            if (hotelDTO.getName() == null || hotelDTO.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Hotel name is required");
            }
            if (hotelDTO.getAddress() == null || hotelDTO.getAddress().trim().isEmpty()) {
                throw new IllegalArgumentException("Hotel address is required");
            }

            hotelService.saveHotel(hotelDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Hotel saved successfully!");
            return "redirect:/manager/hotels";
        } catch (IllegalArgumentException e) {
            logger.error("Error saving hotel: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/hotels/new";
        }
    }

    @GetMapping("/hotels/{hotelId}/edit")
    public String editHotelForm(
            @PathVariable Long hotelId,
            Model model,
            RedirectAttributes redirectAttributes) {
        logger.info("Displaying edit hotel form for hotelId: {}", hotelId);
        try {
            HotelDTO hotel = hotelService.getHotelById(hotelId);
            model.addAttribute("hotel", hotel);
            return "hotel/hotel-edit";
        } catch (IllegalArgumentException e) {
            logger.error("Error retrieving hotel: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/hotels";
        }
    }

    @PostMapping("/hotels/{hotelId}/edit")
    public String editHotel(
            @PathVariable Long hotelId,
            @ModelAttribute("hotel") HotelDTO hotelDTO,
            RedirectAttributes redirectAttributes) {
        logger.info("Updating hotel ID: {}", hotelId);
        try {
            if (hotelDTO.getName() == null || hotelDTO.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Hotel name is required");
            }
            if (hotelDTO.getAddress() == null || hotelDTO.getAddress().trim().isEmpty()) {
                throw new IllegalArgumentException("Hotel address is required");
            }

            hotelDTO.setId(hotelId);
            hotelService.updateHotel(hotelDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Hotel updated successfully!");
            return "redirect:/manager/hotels";
        } catch (IllegalArgumentException e) {
            logger.error("Error updating hotel: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/hotels/{hotelId}/edit";
        }
    }

    @GetMapping("/hotels/{hotelId}/delete")
    public String deleteHotelForm(
            @PathVariable Long hotelId,
            Model model,
            RedirectAttributes redirectAttributes) {
        logger.info("Displaying delete hotel confirmation for hotelId: {}", hotelId);
        try {
            HotelDTO hotel = hotelService.getHotelById(hotelId);
            model.addAttribute("hotel", hotel);
            return "hotel/hotel-delete-confirm";
        } catch (IllegalArgumentException e) {
            logger.error("Error retrieving hotel: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/hotels";
        }
    }

    @PostMapping("/hotels/{hotelId}/delete")
    public String deleteHotel(
            @PathVariable Long hotelId,
            RedirectAttributes redirectAttributes) {
        logger.info("Deleting hotel ID: {}", hotelId);
        try {
            hotelService.deleteHotel(hotelId);
            redirectAttributes.addFlashAttribute("successMessage", "Hotel deleted successfully!");
        } catch (IllegalArgumentException e) {
            logger.error("Error deleting hotel: {}", e.getMessage());
            String errorMessage = e.getMessage().equals("Cannot delete hotel with active bookings")
                ? "Cannot delete hotel because it has active bookings."
                : "Cannot delete hotel due to associated dependencies (e.g., amenities, images, rooms).";
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
        } catch (Exception e) {
            logger.error("Unexpected error deleting hotel: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Failed to delete hotel due to database constraints. Ensure all amenities, images, and rooms are removed.");
        }
        return "redirect:/manager/hotels";
    }

    @GetMapping("/dashboard")
    public String managerDashboard(Model model) {
        logger.info("Accessing manager dashboard");
        model.addAttribute("hotels", hotelService.getAllHotels());
        return "hotel/hotel-manager-dashboard";
    }

    @GetMapping("/hotels/{hotelId}/rooms")
    public String manageRooms(@PathVariable Long hotelId, Model model) {
        logger.info("Managing rooms for hotel ID: {}", hotelId);
        try {
            HotelDTO hotel = hotelService.getHotelById(hotelId);
            model.addAttribute("hotel", hotel);
            model.addAttribute("room", new RoomDTO());
            return "hotel/room-management";
        } catch (IllegalArgumentException e) {
            logger.error("Error retrieving hotel: {}", e.getMessage());
            return "redirect:/manager/hotels";
        }
    }

    @PostMapping("/hotels/{hotelId}/rooms")
    public String addRoom(
            @PathVariable Long hotelId,
            @ModelAttribute("room") RoomDTO roomDTO,
            RedirectAttributes redirectAttributes) {
        logger.info("Adding room to hotel ID: {}", hotelId);
        try {
            if (roomDTO.getType() == null || roomDTO.getType().trim().isEmpty()) {
                throw new IllegalArgumentException("Room type is required");
            }
            if (roomDTO.getNumberOfRooms() <= 0) {
                throw new IllegalArgumentException("Number of rooms must be positive");
            }
            if (roomDTO.getPricePerNight() <= 0) {
                throw new IllegalArgumentException("Price per night must be positive");
            }

            hotelService.addRoom(hotelId, roomDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Room added successfully!");
            return "redirect:/manager/hotels/{hotelId}/rooms";
        } catch (IllegalArgumentException e) {
            logger.error("Error adding room: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/hotels/{hotelId}/rooms";
        }
    }

    @GetMapping("/hotels/{hotelId}/rooms/{roomId}/edit")
    public String editRoomForm(
            @PathVariable Long hotelId,
            @PathVariable Long roomId,
            Model model,
            RedirectAttributes redirectAttributes) {
        logger.info("Displaying edit room form for hotelId: {}, roomId: {}", hotelId, roomId);
        try {
            HotelDTO hotel = hotelService.getHotelById(hotelId);
            RoomDTO room = hotelService.getRoomById(hotelId, roomId);
            model.addAttribute("hotel", hotel);
            model.addAttribute("room", room);
            return "hotel/room-edit";
        } catch (IllegalArgumentException e) {
            logger.error("Error retrieving room: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/hotels/{hotelId}/rooms";
        }
    }

    @PostMapping("/hotels/{hotelId}/rooms/{roomId}/edit")
    public String editRoom(
            @PathVariable Long hotelId,
            @PathVariable Long roomId,
            @ModelAttribute("room") RoomDTO roomDTO,
            RedirectAttributes redirectAttributes) {
        logger.info("Updating room ID: {} for hotel ID: {}", roomId, hotelId);
        try {
            if (roomDTO.getType() == null || roomDTO.getType().trim().isEmpty()) {
                throw new IllegalArgumentException("Room type is required");
            }
            if (roomDTO.getNumberOfRooms() <= 0) {
                throw new IllegalArgumentException("Number of rooms must be positive");
            }
            if (roomDTO.getPricePerNight() <= 0) {
                throw new IllegalArgumentException("Price per night must be positive");
            }

            roomDTO.setId(roomId);
            hotelService.updateRoom(hotelId, roomDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Room updated successfully!");
            return "redirect:/manager/hotels/{hotelId}/rooms";
        } catch (IllegalArgumentException e) {
            logger.error("Error updating room: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/hotels/{hotelId}/rooms";
        }
    }

    @PostMapping("/hotels/{hotelId}/rooms/{roomId}/delete")
    public String deleteRoom(
            @PathVariable Long hotelId,
            @PathVariable Long roomId,
            RedirectAttributes redirectAttributes) {
        logger.info("Deleting room ID: {} from hotel ID: {}", roomId, hotelId);
        try {
            hotelService.deleteRoom(hotelId, roomId);
            redirectAttributes.addFlashAttribute("successMessage", "Room deleted successfully!");
        } catch (IllegalArgumentException e) {
            logger.error("Error deleting room: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/hotels/{hotelId}/rooms";
    }
}