package com.obhs.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import jakarta.annotation.PostConstruct;
import java.util.Properties;

@Configuration
public class MailConfig {
    private static final Logger logger = LoggerFactory.getLogger(MailConfig.class);

    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.port}")
    private String mailPort;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${spring.mail.password}")
    private String mailPassword;

    @PostConstruct
    public void logEmailConfig() {
        logger.info("Email Configuration Loaded:");
        logger.info("spring.mail.host: {}", mailHost);
        logger.info("spring.mail.port: {}", mailPort);
        logger.info("spring.mail.username: {}", mailUsername);
        logger.info("spring.mail.password: [PROTECTED]");
    }

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailHost);
        mailSender.setPort(Integer.parseInt(mailPort));
        mailSender.setUsername(mailUsername);
        mailSender.setPassword(mailPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true"); // Enable SSL for port 465
        props.put("mail.smtp.starttls.enable", "false"); // Disable STARTTLS since we're using SSL
        props.put("mail.debug", "true"); // Enable debug for troubleshooting

        logger.info("JavaMailSender configured with properties: {}", props);
        return mailSender;
    }
}