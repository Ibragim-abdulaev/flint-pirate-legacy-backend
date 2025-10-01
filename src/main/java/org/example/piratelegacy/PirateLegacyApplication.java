package org.example.piratelegacy;

import org.example.piratelegacy.auth.config.RedisConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableCaching
@Import(RedisConfig.class)
public class PirateLegacyApplication {

    public static void main(String[] args) {
        SpringApplication.run(PirateLegacyApplication.class, args);
    }

}