package ru.practicum.explore.stat.server;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.explore.stat.dto.ViewStatsDto;
import ru.practicum.explore.stat.server.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsRepository extends JpaRepository<EndpointHit, Long> {

    // Все IP и все URI
    @Query("""
            SELECT new ru.practicum.explore.stat.dto.ViewStatsDto(h.app, h.uri, COUNT(h.ip))
            FROM EndpointHit h
            WHERE h.timestamp BETWEEN :start AND :end
            GROUP BY h.app, h.uri
            ORDER BY COUNT(h.ip) DESC
            """)
    List<ViewStatsDto> findAllStats(LocalDateTime start, LocalDateTime end);

    // Уникальные IP и все URI
    @Query("""
            SELECT new ru.practicum.explore.stat.dto.ViewStatsDto(h.app, h.uri, COUNT(DISTINCT h.ip))
            FROM EndpointHit h
            WHERE h.timestamp BETWEEN :start AND :end
            GROUP BY h.app, h.uri
            ORDER BY COUNT(DISTINCT h.ip) DESC
            """)
    List<ViewStatsDto> findAllStatsUniqueIp(LocalDateTime start, LocalDateTime end);

    // Все IP и URI из списка
    @Query("""
            SELECT new ru.practicum.explore.stat.dto.ViewStatsDto(h.app, h.uri, COUNT(h.ip))
            FROM EndpointHit h
            WHERE h.timestamp BETWEEN :start AND :end
            AND h.uri IN :uris
            GROUP BY h.app, h.uri
            ORDER BY COUNT(h.ip) DESC
            """)
    List<ViewStatsDto> findStatsByUris(LocalDateTime start, LocalDateTime end, List<String> uris);

    // Уникальные IP и URI из списка
    @Query("""
            SELECT new ru.practicum.explore.stat.dto.ViewStatsDto(h.app, h.uri, COUNT(DISTINCT h.ip))
            FROM EndpointHit h
            WHERE h.timestamp BETWEEN :start AND :end
            AND h.uri IN :uris
            GROUP BY h.app, h.uri
            ORDER BY COUNT(DISTINCT h.ip) DESC
            """)
    List<ViewStatsDto> findStatsByUrisUniqueIp(LocalDateTime start, LocalDateTime end, List<String> uris);
}
