package ewm.request;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication(scanBasePackages = {"ewm.request", "ru.practicum.ewm.stats.client"})
public class RequestApplication {
    public static void main(String[] args) {
        SpringApplication.run(RequestApplication.class, args);
    }
}
