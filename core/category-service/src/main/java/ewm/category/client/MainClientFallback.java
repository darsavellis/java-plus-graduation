package ewm.category.client;

import org.springframework.stereotype.Component;

@Component
public class MainClientFallback implements MainClient {
    @Override
    public boolean existsByCategoryId(long categoryId) {
        return false;
    }
}
