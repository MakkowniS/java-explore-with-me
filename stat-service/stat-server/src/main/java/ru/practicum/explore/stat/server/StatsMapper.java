package ru.practicum.explore.stat.server;

import ru.practicum.explore.stat.dto.EndpointHitDto;
import ru.practicum.explore.stat.server.model.EndpointHit;

public class StatsMapper {

    public static EndpointHit mapDtoToEndpointHit(EndpointHitDto hitDto) {
        return EndpointHit.builder()
                .app(hitDto.getApp())
                .uri(hitDto.getUri())
                .ip(hitDto.getIp())
                .timestamp(hitDto.getTimestamp())
                .build();
    }

}
