package com.obhs.service;

import com.obhs.dto.UserDTO;
import com.obhs.entity.Role;
import com.obhs.entity.User;
import com.obhs.repository.RoleRepository;
import com.obhs.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    public void registerUser(UserDTO userDTO) {
        logger.info("Registering user with email: {}", userDTO.getEmail());
        User user = new User();
        user.setName(userDTO.getName());
        user.setEmail(userDTO.getEmail());
        user.setPhoneNumber(userDTO.getPhoneNumber());
        user.setPassword(userDTO.getPassword()); // Store password as plain text
        user.setProfilePicture(userDTO.getProfilePicture());

        // Assign role based on userDTO.roleName
        String roleName = userDTO.getRoleName() != null ? userDTO.getRoleName() : "ROLE_CUSTOMER";
        Optional<Role> roleOpt = roleRepository.findByName(roleName);
        if (roleOpt.isEmpty()) {
            logger.warn("Role {} not found, creating it", roleName);
            Role role = new Role();
            role.setName(roleName);
            roleRepository.save(role);
            user.setRoles(Collections.singleton(role));
        } else {
            user.setRoles(Collections.singleton(roleOpt.get()));
        }

        userRepository.save(user);
        logger.info("User registered successfully with role: {}", roleName);
    }

    public void updateUser(UserDTO userDTO) {
        User user = userRepository.findById(userDTO.getId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setName(userDTO.getName());
        user.setPhoneNumber(userDTO.getPhoneNumber());
        user.setProfilePicture(userDTO.getProfilePicture());
        userRepository.save(user);
        logger.info("User updated: {}", user.getEmail());
    }

    public UserDTO getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("Current user not found"));
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setName(user.getName());
        userDTO.setEmail(user.getEmail());
        userDTO.setPhoneNumber(user.getPhoneNumber());
        userDTO.setProfilePicture(user.getProfilePicture());
        userDTO.setRoleName(user.getRoles().stream().findFirst().map(Role::getName).orElse(""));
        return userDTO;
    }
}