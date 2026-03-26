package ru.practicum.explore.stat.server;

import ru.practicum.explore.stat.dto.EndpointHitDto;
import ru.practicum.explore.stat.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {
    String saveHit(EndpointHitDto hitDto);

    List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique);
}
