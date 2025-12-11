package ewm.category;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class CategoryService {
    public static void main(String[] args) {
        SpringApplication.run(CategoryService.class, args);
    }
}
