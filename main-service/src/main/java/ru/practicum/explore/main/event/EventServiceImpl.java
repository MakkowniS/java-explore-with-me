package ru.practicum.explore.main.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explore.main.category.CategoryRepository;
import ru.practicum.explore.main.category.model.Category;
import ru.practicum.explore.main.error.model.exception.ConflictException;
import ru.practicum.explore.main.error.model.exception.NotFoundException;
import ru.practicum.explore.main.error.model.exception.DeniedAccessException;
import ru.practicum.explore.main.error.model.exception.ValidationException;
import ru.practicum.explore.main.event.dto.EventFullDto;
import ru.practicum.explore.main.event.dto.EventShortDto;
import ru.practicum.explore.main.event.dto.NewEventDto;
import ru.practicum.explore.main.event.dto.UpdateEventRequest;
import ru.practicum.explore.main.event.dto.eventFilters.AdminEventFilter;
import ru.practicum.explore.main.event.dto.eventFilters.PublicEventFilter;
import ru.practicum.explore.main.event.model.Event;
import ru.practicum.explore.main.event.model.EventState;
import ru.practicum.explore.main.event.model.Location;
import ru.practicum.explore.main.event.model.StateAction;
import ru.practicum.explore.main.request.RequestMapper;
import ru.practicum.explore.main.request.RequestRepository;
import ru.practicum.explore.main.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.explore.main.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.explore.main.request.dto.ParticipationRequestDto;
import ru.practicum.explore.main.request.model.ParticipationRequest;
import ru.practicum.explore.main.request.model.RequestStatus;
import ru.practicum.explore.main.user.UserRepository;
import ru.practicum.explore.main.user.model.User;
import ru.practicum.explore.stat.client.StatsClient;
import ru.practicum.explore.stat.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final RequestRepository requestRepository;

    private final StatsClient statsClient;
    private final ObjectMapper objectMapper;

    // Admin
    @Override
    public List<EventFullDto> getEventsAdmin(AdminEventFilter filter) {

        Specification<Event> spec = Specification.where(EventSpecification.hasUsers(filter.getUsers()))
                .and(EventSpecification.hasStates(filter.getStates()))
                .and(EventSpecification.hasCategories(filter.getCategories()))
                .and(EventSpecification.isAfterStart(filter.getRangeStart()))
                .and(EventSpecification.isBeforeEnd(filter.getRangeEnd()));

        Pageable pageable = PageRequest.of(filter.getFrom() / filter.getSize(), filter.getSize());

        List<Event> events = eventRepository.findAll(spec, pageable).getContent();

        return events.stream()
                .map(EventMapper::mapToEventFullDto)
                .collect(Collectors.toList());

    }

    @Override
    @Transactional
    public EventFullDto updateEventAdmin(Long eventId, UpdateEventRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        // Валидация времени
        if (request.getEventDate() != null) {
            if (request.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                throw new ValidationException("Дата начала события должна быть не ранее чем за час от даты публикации.");
            }
        }

        // Смена статуса
        if (request.getStateAction() != null) {
            if (request.getStateAction() == StateAction.PUBLISH_EVENT) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Событие можно публиковать, только если оно в состоянии PENDING.");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (request.getStateAction() == StateAction.REJECT_EVENT) {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Событие нельзя отклонить, так как оно уже опубликовано.");
                }
                event.setState(EventState.CANCELED);
            }
        }

        updateEventFields(event, request);
        Event savedEvent = eventRepository.save(event);
        return EventMapper.mapToEventFullDto(savedEvent);
    }

    // Private
    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        // Проверка даты
        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Дата и время события не могут быть раньше, чем через 2 часа от текущего момента.");
        }

        // Получаем юзера и категорию
        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category with id=" + newEventDto.getCategory() + " was not found"));

        Event event = EventMapper.mapToEvent(newEventDto, initiator, category);

        Event savedEvent = eventRepository.save(event);
        return EventMapper.mapToEventFullDto(savedEvent);
    }

    @Override
    @Transactional
    public EventFullDto updateEventUser(Long userId, Long eventId, UpdateEventRequest request) {
        // Проверяем что событие принадлежит юзеру
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new DeniedAccessException("User don't have permission to update this event");
        }

        // Проверка статуса
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Изменить можно только CANCELED события или события в состоянии PENDING.");
        }

        // Проверка даты
        if (request.getEventDate() != null) {
            if (request.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ValidationException("Дата и время намечающегося события не может быть раньше, чем через два часа от текущего момента.");
            }
        }

        if (request.getStateAction() != null) {
            if (request.getStateAction() == StateAction.SEND_TO_REVIEW) {
                event.setState(EventState.PENDING);
            } else if (request.getStateAction() == StateAction.CANCEL_REVIEW) {
                event.setState(EventState.CANCELED);
            }
        }

        updateEventFields(event, request);
        return EventMapper.mapToEventFullDto(eventRepository.save(event));
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size) {
        Pageable pageRequest = PageRequest.of(from / size, size);
        List<Event> eventList = eventRepository.findAllByInitiatorId(userId, pageRequest);

        return eventList.stream()
                .map(EventMapper::mapToEventShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        return EventMapper.mapToEventFullDto(eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"))
        );
    }

    // Public
    @Override
    @Transactional
    public List<EventShortDto> getEventsPublic(PublicEventFilter filter, HttpServletRequest request) {
        // Если дата начала не указана, берётся настоящая
        LocalDateTime start = (filter.getRangeStart() != null) ? filter.getRangeStart() : LocalDateTime.now();

        // Сборка спецификации
        Specification<Event> spec = Specification.where(EventSpecification.isPublished())
                .and(EventSpecification.textSearch(filter.getText()))
                .and(EventSpecification.hasCategories(filter.getCategories()))
                .and(EventSpecification.isPaid(filter.getPaid()))
                .and(EventSpecification.isAfterStart(start))
                .and(EventSpecification.isBeforeEnd(filter.getRangeEnd()))
                .and(EventSpecification.isAvailable(filter.getOnlyAvailable()));

        Pageable pageable = PageRequest.of(filter.getFrom() / filter.getSize(), filter.getSize());

        // Запрос
        List<Event> events = eventRepository.findAll(spec, pageable).getContent();

        // Отправка статистики
        try {
            statsClient.saveHit(request.getRequestURI(), request.getRemoteAddr());
        } catch (Exception e) {
            log.error("Не удалось сохранить хит: {}", e.getMessage());
        }


        Map<Long, Long> viewsMap = getViews(events);

        List<EventShortDto> dtos = events.stream()
                .map(event -> {
                    EventShortDto dto = EventMapper.mapToEventShortDto(event);
                    dto.setViews(viewsMap.get(event.getId()));
                    return dto;
                })
                .toList();

        // Сортировка (EVENT_DATE или VIEWS)
        if (filter.getSort() != null) {
            if (filter.getSort().equalsIgnoreCase("EVENT_DATE")) {
                dtos.sort(Comparator.comparing(EventShortDto::getEventDate));
            } else if (filter.getSort().equalsIgnoreCase("VIEWS")) {
                dtos.sort(Comparator.comparing(EventShortDto::getViews).reversed());
            }
        }

        return dtos;
    }

    @Override
    @Transactional
    public EventFullDto getEventByIdPublic(Long eventId, HttpServletRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }

        try {
            statsClient.saveHit(request.getRequestURI(), request.getRemoteAddr());
        } catch (Exception e) {
            log.error("Не удалось сохранить хит: {}", e.getMessage());
        }

        Map<Long, Long> viewsMap = getViews(List.of(event));
        Long views = viewsMap.get(event.getId());

        if (views == 0) views = 1L;

        EventFullDto dto = EventMapper.mapToEventFullDto(event);
        dto.setViews(views);

        return dto;
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest updateRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        // Если лимит уже достигнут, обработки нет
        if (event.getParticipantLimit() != 0 && event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConflictException("Лимит одобренных заявок достигнут.");
        }

        List<ParticipationRequest> requests = requestRepository.findAllByIdIn(updateRequest.getRequestIds());
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();

        // Текущее количество подтвержденных заявок и лимит
        int confirmedCount = event.getConfirmedRequests();
        long limit = event.getParticipantLimit();

        for (ParticipationRequest req : requests) {
            if (!req.getStatus().equals(RequestStatus.PENDING)) {
                throw new ConflictException("Статус может быть изменён только для PENDING заявок.");
            }

            // ПОДТВЕРДИТЬ
            if (updateRequest.getStatus() == RequestStatus.CONFIRMED) {
                // Если лимита нет (0) ИЛИ мы еще не уперлись в лимит
                if (limit == 0 || confirmedCount < limit) {
                    req.setStatus(RequestStatus.CONFIRMED);
                    confirmedCount++;
                    result.getConfirmedRequests().add(RequestMapper.mapToRequestDto(req));
                } else {
                    // Лимит закончился, отклоняем остаток
                    req.setStatus(RequestStatus.REJECTED);
                    result.getRejectedRequests().add(RequestMapper.mapToRequestDto(req));
                }
            } else {
                // ОТКЛОНИТЬ
                req.setStatus(RequestStatus.REJECTED);
                result.getRejectedRequests().add(RequestMapper.mapToRequestDto(req));
            }
        }

        // Сохраняем новое количество подтвержденных заявок
        event.setConfirmedRequests(confirmedCount);
        requestRepository.saveAll(requests);

        return result;
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        // Проверяем, что запрашивающий — это автор события
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Только инициатор события может просматривать запросы на участие.");
        }

        // Получаем все запросы для этого события
        List<ParticipationRequest> requests = requestRepository.findAllByEventId(eventId);

        return requests.stream()
                .map(RequestMapper::mapToRequestDto)
                .collect(Collectors.toList());
    }

    private Map<Long, Long> getViews(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        LocalDateTime start = LocalDateTime.now().minusYears(10);
        LocalDateTime end = LocalDateTime.now().plusMinutes(1);
        Map<Long, Long> viewsMap = new HashMap<>(); // <eventId, кол-во просмотров>

        try {
            ResponseEntity<Object> response = statsClient.getStats(start, end, uris, true);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<ViewStatsDto> stats = objectMapper.convertValue(
                        response.getBody(),
                        new TypeReference<List<ViewStatsDto>>() {
                        }
                );

                for (ViewStatsDto stat : stats) {
                    String uri = stat.getUri();
                    String idStr = uri.substring(uri.lastIndexOf("/") + 1);
                    Long eventId = Long.parseLong(idStr);
                    viewsMap.put(eventId, stat.getHits());
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при получении статистики просмотров: {}", e.getMessage());
        }

        return viewsMap;
    }

    // Вспомогательный метод
    private void updateEventFields(Event event, UpdateEventRequest request) {
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventDate() != null) event.setEventDate(request.getEventDate());
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());
        if (request.getTitle() != null) event.setTitle(request.getTitle());

        if (request.getLocation() != null) {
            event.setLocation(new Location(request.getLocation().getLat(), request.getLocation().getLon()));
        }

        if (request.getCategory() != null) {
            Category category = categoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            event.setCategory(category);
        }
    }

}
