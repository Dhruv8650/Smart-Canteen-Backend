package com.smartcanteen.backend.config;

import com.smartcanteen.backend.entity.Role;
import com.smartcanteen.backend.entity.User;
import com.smartcanteen.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataLoader {

    @Bean
    CommandLineRunner loadAdmin(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                AdminProperties adminProperties) {
        return args -> {

            // use adminProperties instead of adminEmail
            if (userRepository.findByEmail(adminProperties.getEmail()).isEmpty()) {

                User admin = new User();
                admin.setName(adminProperties.getName());
                admin.setEmail(adminProperties.getEmail());
                admin.setPassword(passwordEncoder.encode(adminProperties.getPassword()));
                admin.setRole(Role.ADMIN);

                userRepository.save(admin);

                System.out.println("Default ADMIN created successfully.");

            } else {
                System.out.println("ADMIN already exists.");
            }
        };
    }
}