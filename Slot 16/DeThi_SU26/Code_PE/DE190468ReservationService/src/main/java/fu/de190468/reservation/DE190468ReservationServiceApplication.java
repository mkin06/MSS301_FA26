package fu.de190468.reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DE190468ReservationServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(DE190468ReservationServiceApplication.class, args);
  }
}
