package com.obhs;
import com.obhs.entity.*;
import com.obhs.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class OnlineHotelBookingSystemApplication {

    private static final Logger logger = LoggerFactory.getLogger(OnlineHotelBookingSystemApplication.class);

    public static void main(String[] args) {
        SpringApplication.run( OnlineHotelBookingSystemApplication.class, args);
    }

    @Bean
    public CommandLineRunner initializeRoles(RoleRepository roleRepository) {
        return args -> {
            logger.info("Starting role initialization...");

            // Seed ROLE_ADMIN
            if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
                Role adminRole = new Role();
                adminRole.setName("ROLE_ADMIN");
                roleRepository.save(adminRole);
                logger.info("Created ROLE_ADMIN");
            } else {
                logger.info("ROLE_ADMIN already exists");
            }

            // Seed ROLE_MANAGER
            if (roleRepository.findByName("ROLE_MANAGER").isEmpty()) {
                Role managerRole = new Role();
                managerRole.setName("ROLE_MANAGER");
                roleRepository.save(managerRole);
                logger.info("Created ROLE_MANAGER");
            } else {
                logger.info("ROLE_MANAGER already exists");
            }

            // Seed ROLE_CUSTOMER
            if (roleRepository.findByName("ROLE_CUSTOMER").isEmpty()) {
                Role customerRole = new Role();
                customerRole.setName("ROLE_CUSTOMER");
                roleRepository.save(customerRole);
                logger.info("Created ROLE_CUSTOMER");
            } else {
                logger.info("ROLE_CUSTOMER already exists");
            }

            logger.info("Role initialization completed.");
        };
    }
}