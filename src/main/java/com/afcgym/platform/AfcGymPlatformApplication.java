package com.afcgym.platform;

import java.time.ZoneId;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AfcGymPlatformApplication {

    private static final String TIME_ZONE_VARIABLE = "APP_TIME_ZONE";
    private static final String DEFAULT_TIME_ZONE = "America/Mexico_City";

    public static void main(String[] args) {
        String configured = System.getenv(TIME_ZONE_VARIABLE);
        String timeZone = configured == null || configured.isBlank() ? DEFAULT_TIME_ZONE : configured;
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(timeZone)));
        SpringApplication.run(AfcGymPlatformApplication.class, args);
    }
}
