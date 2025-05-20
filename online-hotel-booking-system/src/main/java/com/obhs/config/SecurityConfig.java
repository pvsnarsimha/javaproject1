package com.obhs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import java.util.logging.Logger;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger LOGGER = Logger.getLogger(SecurityConfig.class.getName());

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/hotels", "/register", "/login", "/css/**", "/js/**", "/", "/assets/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/manager/**").hasRole("MANAGER")
                .requestMatchers("/customer/**").hasRole("CUSTOMER")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler((request, response, authentication) -> {
                    String redirect = request.getParameter("redirect");
                    String hotelId = request.getParameter("hotelId");
                    String roomId = request.getParameter("roomId");

                    LOGGER.info("Login success for user: " + authentication.getName() + ", redirect param: " + redirect);

                    boolean isCustomer = authentication.getAuthorities().stream()
                            .anyMatch(auth -> auth.getAuthority().equals("ROLE_CUSTOMER"));
                    boolean isManager = authentication.getAuthorities().stream()
                            .anyMatch(auth -> auth.getAuthority().equals("ROLE_MANAGER"));
                    boolean isAdmin = authentication.getAuthorities().stream()
                            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

                    if (isCustomer && redirect != null && redirect.equals("/hotels") && redirect.startsWith("/")) {
                        LOGGER.info("Redirecting customer to /hotels");
                        response.sendRedirect("/hotels");
                    } else if (isCustomer && redirect != null && redirect.equals("/customer/bookings/new") && hotelId != null && roomId != null && redirect.startsWith("/")) {
                        LOGGER.info("Redirecting customer to /customer/bookings/new?hotelId=" + hotelId + "&roomId=" + roomId);
                        response.sendRedirect("/customer/bookings/new?hotelId=" + hotelId + "&roomId=" + roomId);
                    } else if (isAdmin) {
                        LOGGER.info("Redirecting admin to /admin/dashboard");
                        response.sendRedirect("/home");
                    } else if (isManager) {
                        LOGGER.info("Redirecting manager to /manager/dashboard");
                        response.sendRedirect("/home");
                    } else if (isCustomer) {
                        LOGGER.info("Redirecting customer to /home");
                        response.sendRedirect("/home");
                    } else {
                        LOGGER.warning("No valid role found, redirecting to /login?error=true");
                        response.sendRedirect("/login?error=true");
                    }
                })
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}