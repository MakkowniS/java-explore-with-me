package ru.practicum.explore.stat.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.explore.stat.dto.EndpointHitDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class StatsClient extends BaseClient {

    private final String appName;

    @Autowired
    public StatsClient(@Value("${stats.server.url}") String serverUrl,
                       // Читаем имя приложения из конфига. Если его нет, по дефолту будет ewm-main-service
                       @Value("${spring.application.name:ewm-main-service}") String appName,
                       RestTemplateBuilder builder) {
        super(
                builder
                        .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
                        .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                        .build()
        );
        this.appName = appName;
    }

    public void saveHit(String uri, String ip) {
        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app(appName)
                .uri(uri)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build();
        post("/hit", hitDto);
    }

    public ResponseEntity<Object> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        String urisStr = (uris != null) ? String.join(",", uris) : "";

        Map<String, Object> params = Map.of(
                "start", start.format(DateTimeFormatter.ofPattern(("yyyy-MM-dd HH:mm:ss"))),
                "end", end.format(DateTimeFormatter.ofPattern(("yyyy-MM-dd HH:mm:ss"))),
                "uris", urisStr,
                "unique", unique
        );
        return get("/stats?start={start}&end={end}&uris={uris}&unique={unique}", params);
    }
}
