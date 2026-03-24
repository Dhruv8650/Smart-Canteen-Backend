package com.smartcanteen.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/**")

                // Allowed Frontend Origins (NO *)
                .allowedOrigins(
                        "http://localhost:3000",   // React dev
                        "http://localhost:5173",   // Vite dev
                        "http://localhost:5500",   // Live Server
                        "http://127.0.0.1:5500",   // Live Server alt
                        "https://your-frontend-domain.com" // 🔥 replace in prod
                )

                // Allowed HTTP Methods
                .allowedMethods(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "PATCH",
                        "OPTIONS"
                )

                // Allowed Headers
                .allowedHeaders(
                        "Authorization",
                        "Content-Type",
                        "X-Requested-With"
                )

                // Allow credentials (JWT / cookies)
                .allowCredentials(true)

                // Cache preflight request (1 hour)
                .maxAge(3600);
    }
}