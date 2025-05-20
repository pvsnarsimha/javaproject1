package com.obhs.controller;

import com.obhs.service.HotelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PublicController {

    private static final Logger logger = LoggerFactory.getLogger(PublicController.class);

    @Autowired
    private HotelService hotelService;

    @GetMapping("/hotels")
    public String listHotels(Model model) {
        logger.info("Accessing public hotel listing");
        model.addAttribute("hotels", hotelService.getAllHotelsWithAvailableRooms());
        return "hotel/public_hotel_list";
    }
}