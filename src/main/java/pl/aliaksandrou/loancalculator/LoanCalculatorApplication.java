package pl.aliaksandrou.loancalculator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class LoanCalculatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanCalculatorApplication.class, args);
    }

}
