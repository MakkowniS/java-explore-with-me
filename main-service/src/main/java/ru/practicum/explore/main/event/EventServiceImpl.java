package ru.practicum.explore.main.event;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
import ru.practicum.explore.main.stats.StatsClientService;
import ru.practicum.explore.main.user.UserRepository;
import ru.practicum.explore.main.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final StatsClientService statsClientService;
    private final RequestRepository requestRepository;

    // Admin
    @Override
    public List<EventFullDto> getEventsAdmin(List<Long> users, List<EventState> states,
                                             List<Long> categories, LocalDateTime rangeStart,
                                             LocalDateTime rangeEnd, int from, int size) {
        Specification<Event> spec = Specification.where(EventSpecification.hasUsers(users))
                .and(EventSpecification.hasStates(states))
                .and(EventSpecification.hasCategories(categories))
                .and(EventSpecification.isAfterStart(rangeStart))
                .and(EventSpecification.isBeforeEnd(rangeEnd));

        Pageable pageable = PageRequest.of(from / size, size);

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

        Event event = EventMapper.mapToEvent(newEventDto);
        event.setInitiator(initiator);
        event.setCategory(category);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(EventState.PENDING); // Новое событие ждёт модерации
        event.setConfirmedRequests(0);
        event.setViews(0L);

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
    public List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid, LocalDateTime rangeStart, LocalDateTime rangeEnd, Boolean onlyAvailable, String sort, int from, int size, HttpServletRequest request) {
        // Если дата начала не указана, берётся настоящая
        LocalDateTime start = (rangeStart != null) ? rangeStart : LocalDateTime.now();

        // Сборка спецификации
        Specification<Event> spec = Specification.where(EventSpecification.isPublished())
                .and(EventSpecification.textSearch(text))
                .and(EventSpecification.hasCategories(categories))
                .and(EventSpecification.isPaid(paid))
                .and(EventSpecification.isAfterStart(start))
                .and(EventSpecification.isBeforeEnd(rangeEnd))
                .and(EventSpecification.isAvailable(onlyAvailable));

        // Сортировка (EVENT_DATE или VIEWS)
        Sort sorting = Sort.unsorted();
        if (sort != null) {
            if (sort.equalsIgnoreCase("EVENT_DATE")) {
                sorting = Sort.by(Sort.Direction.ASC, "eventDate");
            } else if (sort.equalsIgnoreCase("VIEWS")) {
                sorting = Sort.by(Sort.Direction.DESC, "views");
            }
        }

        Pageable pageable = PageRequest.of(from / size, size, sorting);

        // Запрос
        List<Event> events = eventRepository.findAll(spec, pageable).getContent();

        // Отправка статистики
        statsClientService.sendHit(request);

        return events.stream()
                .map(EventMapper::mapToEventShortDto)
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto getEventByIdPublic(Long eventId, HttpServletRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }

        statsClientService.sendHit(request);

        Map<Long, Long> viewsMap = statsClientService.getViews(List.of(event));
        Long views = viewsMap.getOrDefault(eventId, 0L);
        if (views == 0) views = 1L;

        if (event.getViews() < views) {
            eventRepository.incrementViews(eventId, views);
        }
        EventFullDto dto = EventMapper.mapToEventFullDto(event);
        dto.setViews(views);

        return dto;
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest updateRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        List<ParticipationRequest> requests = requestRepository.findAllByIdIn(updateRequest.getRequestIds());

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();

        for (ParticipationRequest req : requests) {
            if (!req.getStatus().equals(RequestStatus.PENDING)) {
                throw new ConflictException("Статус можно изменить только у заявок, находящихся в состоянии PENDING");
            }
            if (updateRequest.getStatus() == RequestStatus.CONFIRMED) {
                if (event.getParticipantLimit() != 0 && event.getConfirmedRequests() >= event.getParticipantLimit()) {
                    throw new ConflictException("Лимит участников исчерпан");
                }
                req.setStatus(RequestStatus.CONFIRMED);
                event.setConfirmedRequests(event.getConfirmedRequests() + 1);
                result.getConfirmedRequests().add(RequestMapper.mapToRequestDto(req));
            } else {
                req.setStatus(RequestStatus.REJECTED);
                result.getRejectedRequests().add(RequestMapper.mapToRequestDto(req));
            }
        }

        eventRepository.save(event);
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
