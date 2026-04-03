package ru.practicum.explore.main.stats;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.practicum.explore.main.event.model.Event;
import ru.practicum.explore.stat.client.StatsClient;
import ru.practicum.explore.stat.dto.EndpointHitDto;
import ru.practicum.explore.stat.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsClientService {

    private final StatsClient statsClient;
    private final ObjectMapper objectMapper;

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

    // Ключ — это ID события, а значение — количество уникальных хитов
    public Map<Long, Long> getViews(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyMap();
        }

        // Формируем список URI для запроса
        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        // Ищем самую раннюю дату создания среди всех переданных событий
        LocalDateTime start = events.stream()
                .map(Event::getCreatedOn)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusYears(15));

        // Окончание текущее время
        LocalDateTime end = LocalDateTime.now().plusMinutes(1);

        Map<Long, Long> viewsMap = new HashMap<>();

        try {
            // Отправляем запрос в сервис статистики unique = true
            ResponseEntity<Object> response = statsClient.getStats(start, end, uris, true);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<ViewStatsDto> stats = objectMapper.convertValue(
                        response.getBody(),
                        new TypeReference<List<ViewStatsDto>>() {}
                );

                // Преобразуем ответ в Map<eventId, hits>
                for (ViewStatsDto stat : stats) {
                    String uri = stat.getUri();
                    // Извлекаем ID из конца строки URI
                        String idStr = uri.substring(uri.lastIndexOf("/") + 1);
                        Long eventId = Long.parseLong(idStr);
                        viewsMap.put(eventId, stat.getHits());
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при получении статистики просмотров от stats-server: {}", e.getMessage());
        }
        return viewsMap;
    }
}