package com.obhs.controller;

import com.obhs.entity.Hotel;
import com.obhs.entity.Room;
import com.obhs.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("totalUsers", adminService.getTotalUsers());
        model.addAttribute("totalHotels", adminService.getTotalHotels());
        model.addAttribute("totalRooms", adminService.getTotalRooms());
        model.addAttribute("totalBookings", adminService.getTotalBookings());
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", adminService.getAllUsers());
        return "admin/users";
    }

    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return "redirect:/admin/users";
    }

    @GetMapping("/hotels")
    public String listHotels(Model model) {
        model.addAttribute("hotels", adminService.getAllHotels());
        return "admin/hotels";
    }

    @GetMapping("/hotels/new")
    public String createHotelForm(Model model) {
        model.addAttribute("hotel", new Hotel());
        return "admin/new-hotel";
    }

    @PostMapping("/hotels/new")
    public String createHotel(@Valid @ModelAttribute("hotel") Hotel hotel, BindingResult result,
                              @RequestParam(value = "amenities", required = false) String amenitiesInput,
                              @RequestParam(value = "images", required = false) String imagesInput, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("hotel", hotel);
            return "admin/new-hotel";
        }
        adminService.createHotel(hotel, amenitiesInput, imagesInput);
        return "redirect:/admin/hotels";
    }

    @GetMapping("/hotels/edit/{id}")
    public String editHotelForm(@PathVariable Long id, Model model) {
        Hotel hotel = adminService.getAllHotels().stream()
                .filter(h -> h.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Hotel not found"));
        model.addAttribute("hotel", hotel);
        return "admin/edit-hotel";
    }

    @PostMapping("/hotels/edit")
    public String updateHotel(@Valid @ModelAttribute("hotel") Hotel hotel, BindingResult result,
                              @RequestParam(value = "amenities", required = false) String amenitiesInput,
                              @RequestParam(value = "images", required = false) String imagesInput, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("hotel", hotel);
            return "admin/edit-hotel";
        }
        adminService.updateHotel(hotel, amenitiesInput, imagesInput);
        return "redirect:/admin/hotels";
    }

    @PostMapping("/hotels/delete/{id}")
    public String deleteHotel(@PathVariable Long id) {
        adminService.deleteHotel(id);
        return "redirect:/admin/hotels";
    }

    @GetMapping("/rooms")
    public String listRooms(Model model) {
        model.addAttribute("rooms", adminService.getAllRooms());
        return "admin/rooms";
    }

    @GetMapping("/rooms/edit/{id}")
    public String editRoomForm(@PathVariable Long id, Model model) {
        Room room = adminService.getAllRooms().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        model.addAttribute("room", room);
        return "admin/edit-room";
    }

    @PostMapping("/rooms/edit")
    public String updateRoom(@Valid @ModelAttribute("room") Room room, BindingResult result, 
                            @RequestParam(value = "images", required = false) String imagesInput, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("room", room);
            return "admin/edit-room";
        }
        adminService.updateRoom(room, imagesInput);
        return "redirect:/admin/rooms";
    }

    @PostMapping("/rooms/delete/{id}")
    public String deleteRoom(@PathVariable Long id) {
        adminService.deleteRoom(id);
        return "redirect:/admin/rooms";
    }

    @GetMapping("/bookings")
    public String listBookings(@RequestParam(value = "status", required = false) String status, Model model) {
        model.addAttribute("bookings", adminService.getAllBookings(status));
        model.addAttribute("selectedStatus", status);
        return "admin/bookings";
    }

    @PostMapping("/bookings/{id}/approve")
    public String approveBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.approveBooking(id);
            redirectAttributes.addFlashAttribute("successMessage", "Booking approved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to approve booking: " + e.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    @PostMapping("/bookings/{id}/reject")
    public String rejectBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.rejectBooking(id);
            redirectAttributes.addFlashAttribute("successMessage", "Booking rejected successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reject booking: " + e.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    @GetMapping("/notifications/failed")
    public String listFailedNotifications(Model model) {
        model.addAttribute("notifications", adminService.getAllFailedNotifications());
        return "admin/failed-notifications";
    }
}