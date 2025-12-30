package ru.practicum.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.HitDto;
import ru.practicum.dto.StatsDto;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Service
public class StatRestClient {

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;
    private final String serviceName;

    public StatRestClient(
            DiscoveryClient discoveryClient,
            @Value("${stats.serviceName}") String serviceName
    ) {
        this.discoveryClient = discoveryClient;
        this.serviceName = serviceName;

        this.restClient = RestClient.builder()
                .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                    setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
                    setReadTimeout((int) Duration.ofSeconds(3).toMillis());
                }})
                .build();
    }

    public void saveHit(HitDto hitDto) {
        URI uri = makeUri("/hit");
        try {
            restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(hitDto)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        String body = safeBody(resp);
                        throw new RuntimeException("Stats saveHit failed: " + resp.getStatusCode() + " body=" + body);
                    })
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw new RuntimeException("Stats saveHit failed: " + ex.getRawStatusCode() + " body=" + ex.getResponseBodyAsString(), ex);
        }
    }

    public List<StatsDto> getStats(String start, String end, List<String> uris, boolean unique) {
        URI base = makeUri("/stats");

        UriComponentsBuilder b = UriComponentsBuilder.fromUri(base)
                .queryParam("start", start)
                .queryParam("end", end)
                .queryParam("unique", unique);

        if (uris != null) {
            for (String u : uris) {
                if (u != null && !u.isBlank()) {
                    b.queryParam("uris", u);
                }
            }
        }

        URI uri = b.build(true).toUri();

        try {
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        String body = safeBody(resp);
                        throw new RuntimeException("Stats getStats failed: " + resp.getStatusCode() + " body=" + body);
                    })
                    .body(new ParameterizedTypeReference<List<StatsDto>>() {});
        } catch (RestClientResponseException ex) {
            throw new RuntimeException("Stats getStats failed: " + ex.getRawStatusCode() + " body=" + ex.getResponseBodyAsString(), ex);
        }
    }

    private URI makeUri(String path) {
        ServiceInstance instance = getInstance();
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
    }

    private ServiceInstance getInstance() {
        try {
            return discoveryClient
                    .getInstances(serviceName)
                    .getFirst();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static String safeBody(ClientHttpResponse resp) {
        try {
            if (resp.getBody() == null) return "";
            return StreamUtils.copyToString(resp.getBody(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "<unreadable>";
        }
    }

}
