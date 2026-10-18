package com.vetcare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync // permite enviar el correo de confirmación sin hacer esperar al usuario
@EnableScheduling // recordatorio diario de vacunas (RF-13)
public class VetCareApplication {

    public static void main(String[] args) {
        SpringApplication.run(VetCareApplication.class, args);
    }
}
