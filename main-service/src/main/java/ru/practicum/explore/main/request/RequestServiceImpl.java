package ru.practicum.explore.main.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explore.main.error.model.exception.ConflictException;
import ru.practicum.explore.main.error.model.exception.DeniedAccessException;
import ru.practicum.explore.main.error.model.exception.NotFoundException;
import ru.practicum.explore.main.event.EventRepository;
import ru.practicum.explore.main.event.model.Event;
import ru.practicum.explore.main.event.model.EventState;
import ru.practicum.explore.main.request.dto.ParticipationRequestDto;
import ru.practicum.explore.main.request.model.ParticipationRequest;
import ru.practicum.explore.main.request.model.RequestStatus;
import ru.practicum.explore.main.user.UserRepository;
import ru.practicum.explore.main.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;


    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        List<ParticipationRequest> requests = requestRepository.findAllByRequesterId(userId);
        return requests.isEmpty()
                ? List.of()
                : requests.stream().map(RequestMapper::mapToRequestDto).toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        // Нельзя подать повторную заявку
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Запрос на участие уже существует.");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        // Инициатор события не может подать заявку на участие в своём событии
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор не может одобрять свою заявку.");
        }

        // Нельзя участвовать в неопубликованном событии
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии.");
        }

        // Проверка лимита участников
        if (event.getParticipantLimit() != 0 && event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит запросов на участие.");
        }

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requester(requester)
                .build();

        // Установка статуса
        // Если пре-модерация не нужна или лимит 0 — подтверждается сразу
        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
            eventRepository.save(event); // Обновляем счетчик в событии
        } else {
            request.setStatus(RequestStatus.PENDING);
        }

        return RequestMapper.mapToRequestDto(requestRepository.save(request));
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));
        if (!request.getRequester().getId().equals(userId)) {
            throw new DeniedAccessException("User is not allowed to cancel this request.");
        }

        if (request.getStatus() == RequestStatus.CONFIRMED) {
            Event event = request.getEvent();
            event.setConfirmedRequests(event.getConfirmedRequests() - 1);
            eventRepository.save(event);
        }
        request.setStatus(RequestStatus.CANCELED);

        return RequestMapper.mapToRequestDto(requestRepository.save(request));
    }

}
