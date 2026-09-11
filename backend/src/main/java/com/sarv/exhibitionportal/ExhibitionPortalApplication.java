package com.sarv.exhibitionportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Process entry. Enables the outbox scheduler. Run with {@code backend/run.ps1} locally. */
@SpringBootApplication
@EnableScheduling
public class ExhibitionPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExhibitionPortalApplication.class, args);
    }
}
