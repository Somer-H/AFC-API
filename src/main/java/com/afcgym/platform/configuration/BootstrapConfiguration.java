package com.afcgym.platform.configuration;

import com.afcgym.platform.persistance.entities.enums.Role;
import com.afcgym.platform.persistance.repositories.IUserRepository;
import com.afcgym.platform.service.IUserService;
import com.afcgym.platform.service.dto.CreateUserCommand;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BootstrapAdminProperties.class)
public class BootstrapConfiguration {

    @Bean
    public ApplicationRunner initialAdminRunner(
            IUserRepository userRepository,
            IUserService userService,
            BootstrapAdminProperties properties) {
        return arguments -> {
            boolean noPasswordConfigured = properties.password() == null || properties.password().isBlank();
            if (noPasswordConfigured || userRepository.count() > 0) {
                return;
            }
            userService.createAdmin(new CreateUserCommand(
                    properties.username(),
                    properties.fullName(),
                    properties.email(),
                    properties.password(),
                    Role.ADMIN));
        };
    }
}
