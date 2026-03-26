package ru.practicum.explore.stat.server;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explore.stat.dto.EndpointHitDto;
import ru.practicum.explore.stat.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private final StatsRepository repository;

    @Override
    @Transactional
    public String saveHit(EndpointHitDto hitDto) {
        repository.save(StatsMapper.mapDtoToEndpointHit(hitDto));
        return "Информация сохранена";
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Дата начала не может быть раньше даты окончания");
        }

        if (uris == null || uris.isEmpty()){
            return unique ?
                    repository.findAllStatsUniqueIp(start, end) : repository.findAllStats(start, end);
        } else {
            return unique ?
                    repository.findStatsByUrisUniqueIp(start, end, uris) : repository.findStatsByUris(start, end, uris);
        }
    }
}
