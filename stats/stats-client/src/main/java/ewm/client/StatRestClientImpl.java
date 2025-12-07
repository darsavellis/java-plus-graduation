package ewm.client;

import ewm.client.exception.StatsServerUnavailable;
import ewm.dto.EndpointHitDto;
import ewm.dto.ViewStatsDto;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StatRestClientImpl implements StatRestClient {
    final RestTemplate restTemplate;
    final DiscoveryClient discoveryClient;
    final RetryTemplate retryTemplate;

    public StatRestClientImpl(DiscoveryClient discoveryClient) {
        RetryTemplate retryTemplate = new RetryTemplate();
        this.discoveryClient = discoveryClient;
        this.retryTemplate = retryTemplate;
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(3000L);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        this.restTemplate = new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(10))
            .build();
    }

    public void addHit(EndpointHitDto hitDto) {
        try {
            restTemplate.postForObject(makeUri("/hit"), hitDto, Void.class);
        } catch (Exception e) {
            log.info("Ошибка при обращении к эндпоинту /hit {}", e.getMessage(), e);
        }
    }

    public List<ViewStatsDto> stats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        try {
            String uri = UriComponentsBuilder.fromUri(makeUri("/stats"))
                .queryParam("start", start)
                .queryParam("end", end)
                .queryParam("uris", String.join(",", uris))
                .queryParam("unique", unique)
                .toUriString();

            ResponseEntity<List<ViewStatsDto>> response = restTemplate.exchange(
                uri, HttpMethod.GET, null, new ParameterizedTypeReference<>() {
                });
            return response.getBody();
        } catch (Exception e) {
            log.info("Ошибка при запросе к эндпоинту /stats {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    ServiceInstance getInstance() {
        try {
            return discoveryClient
                .getInstances("stats-server")
                .getFirst();
        } catch (Exception exception) {
            throw new StatsServerUnavailable(
                "Ошибка обнаружения адреса сервиса статистики с id: stats-server",
                exception
            );
        }
    }

    URI makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(cxt -> getInstance());
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
    }
}
