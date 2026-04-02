package ru.practicum.explore.main.request;

import ru.practicum.explore.main.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.explore.main.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.explore.main.request.dto.ParticipationRequestDto;

import java.util.List;

public interface RequestService {

    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto createRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

}
