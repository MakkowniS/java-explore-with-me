package ru.practicum.explore.main.request;

import ru.practicum.explore.main.request.dto.ParticipationRequestDto;
import ru.practicum.explore.main.request.model.ParticipationRequest;

import java.time.format.DateTimeFormatter;

public class RequestMapper {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static ParticipationRequestDto mapToRequestDto(ParticipationRequest participationRequest) {
        return ParticipationRequestDto.builder()
                .id(participationRequest.getId())
                .created(participationRequest.getCreated().format(formatter))
                .event(participationRequest.getEvent().getId())
                .requester(participationRequest.getRequester().getId())
                .status(participationRequest.getStatus().toString())
                .build();
    }

}
