package tw.teddysoft.aiscrum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application entry point.
 * Location: Root package (required by Spring Boot component scanning)
 */
@SpringBootApplication
public class AiScrumApp {

    public static void main(String[] args) {
        SpringApplication.run(AiScrumApp.class, args);
    }
}
