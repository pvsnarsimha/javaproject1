package com.obhs.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.obhs.dto.UserDTO;
import com.obhs.service.UserService;

import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
@RequestMapping("/customer")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/profile")
    public String viewProfile(Model model) {
        model.addAttribute("user", userService.getCurrentUser());
        return "user/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute("user") UserDTO userDTO) {
        userService.updateUser(userDTO);
        return "redirect:/customer/profile";
    }
}