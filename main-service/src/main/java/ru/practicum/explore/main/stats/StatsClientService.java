package ru.practicum.explore.main.stats;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.practicum.explore.stat.client.StatsClient;
import ru.practicum.explore.stat.dto.EndpointHitDto;
import ru.practicum.explore.stat.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsClientService {

    private final StatsClient statsClient;
    private final ObjectMapper objectMapper;

    @Async
    public void sendHit(HttpServletRequest request) {
        log.info("Отправка статистики для URI: {}", request.getRequestURI());
        EndpointHitDto endpointHitDto = EndpointHitDto.builder()
                .app("EWM-Main-Service")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build();
        try {
            statsClient.saveHit(endpointHitDto);
        } catch (Exception e) {
            log.error("Ошибка при отправке статистики: {}", e.getMessage());
        }
    }

    public boolean isNewUniqueVisit(HttpServletRequest request) {
        LocalDateTime start = LocalDateTime.now().minusYears(15);
        LocalDateTime end = LocalDateTime.now().plusMinutes(1);
        List<String> uris = List.of(request.getRequestURI());
        Boolean unique = true;

        try {
            ResponseEntity<Object> response = statsClient.getStats(start, end, uris, unique);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<ViewStatsDto> stats = objectMapper.convertValue(
                        response.getBody(),
                        new TypeReference<List<ViewStatsDto>>() {}
                );
                return stats.isEmpty();
            }

        } catch (Exception e) {
            log.error("Ошибка при проверке уникальности визита: {}", e.getMessage());
            return false;
        }
        return false;
    }
}