package ru.practicum.explore.main.stats;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.practicum.explore.stat.client.StatsClient;
import ru.practicum.explore.stat.dto.EndpointHitDto;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsClientService {

    private final StatsClient statsClient; // Тот самый клиент из библиотеки

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
            log.error("Ошибка при отправке статистики в фоновом режиме: {}", e.getMessage());
        }
    }
}