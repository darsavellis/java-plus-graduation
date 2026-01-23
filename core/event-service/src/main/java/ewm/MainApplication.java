package ewm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication(scanBasePackages = {"ewm", "ru.practicum.ewm.stats.client"})
public class MainApplication {
    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(MainApplication.class, args);
    }
}
