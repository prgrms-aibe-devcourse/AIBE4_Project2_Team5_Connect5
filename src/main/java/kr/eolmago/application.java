package kr.eolmago;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class application {

    public static void main(String[] args) {
        SpringApplication.run(application.class, args);
    }

}
