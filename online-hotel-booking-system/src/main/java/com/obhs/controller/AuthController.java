package com.obhs.controller;

import com.obhs.dto.UserDTO;
import com.obhs.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String homePage(HttpSession session, Model model) {
        Boolean hasLoggedOut = (Boolean) session.getAttribute("hasLoggedOut");
        Boolean hasVisitedLogin = (Boolean) session.getAttribute("hasVisitedLogin");
        boolean showExitOption = hasLoggedOut != null && hasLoggedOut && hasVisitedLogin != null && hasVisitedLogin;
        model.addAttribute("showExitOption", showExitOption);
        logger.info("Accessing homepage, hasLoggedOut: {}, hasVisitedLogin: {}, showExitOption: {}", hasLoggedOut, hasVisitedLogin, showExitOption);
        return "index";
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        // Set a flag to indicate the user has visited the login page
        Boolean hasLoggedOut = (Boolean) session.getAttribute("hasLoggedOut");
        if (hasLoggedOut != null && hasLoggedOut) {
            session.setAttribute("hasVisitedLogin", true);
        }
        logger.info("Accessing login page, hasLoggedOut: {}, hasVisitedLogin: {}", hasLoggedOut, session.getAttribute("hasVisitedLogin"));
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new UserDTO());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("user") UserDTO userDTO) {
        userService.registerUser(userDTO);
        return "redirect:/login";
    }

    @GetMapping("/faqs")
    public String faqsPage() {
        return "faqs";
    }

    @GetMapping("/user-guide")
    public String userGuide() {
        return "redirect:/assets/users4.pdf";
    }

    @PostMapping(value = "/exit", produces = "application/json")
    @ResponseBody
    public String exitApplication(HttpSession session) {
        logger.info("Exit option triggered. Initiating application termination...");
        try {
            session.setAttribute("hasLoggedOut", true);
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    logger.info("Application termination in progress...");
                    System.exit(0);
                } catch (InterruptedException e) {
                    logger.error("Termination thread interrupted: {}", e.getMessage(), e);
                    System.exit(1);
                }
            }).start();
            return "{\"status\": \"success\", \"message\": \"Application is terminating\"}";
        } catch (Exception e) {
            logger.error("Failed to initiate termination: {}", e.getMessage(), e);
            return "{\"status\": \"error\", \"message\": \"Failed to terminate application\"}";
        }
    }
  
}
