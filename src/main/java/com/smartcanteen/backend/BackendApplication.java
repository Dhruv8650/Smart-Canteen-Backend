package com.smartcanteen.backend;

import com.smartcanteen.backend.config.AdminProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AdminProperties.class)
public class BackendApplication {

	public static void main(String[] args) {

        SpringApplication.run(BackendApplication.class, args);
	}

}
