package ru.practicum.explore.main.event;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explore.main.category.CategoryRepository;
import ru.practicum.explore.main.category.model.Category;
import ru.practicum.explore.main.error.model.exception.ConflictException;
import ru.practicum.explore.main.error.model.exception.NotFoundException;
import ru.practicum.explore.main.error.model.exception.DeniedAccessException;
import ru.practicum.explore.main.event.dto.EventFullDto;
import ru.practicum.explore.main.event.dto.EventShortDto;
import ru.practicum.explore.main.event.dto.NewEventDto;
import ru.practicum.explore.main.event.dto.updateRequest.UpdateEventRequest;
import ru.practicum.explore.main.event.model.Event;
import ru.practicum.explore.main.event.model.EventState;
import ru.practicum.explore.main.event.model.Location;
import ru.practicum.explore.main.event.model.StateAction;
import ru.practicum.explore.main.user.UserRepository;
import ru.practicum.explore.main.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

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

        PageRequest pageable = PageRequest.of(from / size, size);

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
        if (request.getEventDate() != null){
            if (request.getEventDate().isBefore(LocalDateTime.now().plusHours(1))){
                throw new ConflictException("Дата начала события должна быть не ранее чем за час от даты публикации.");
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
        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))){
            throw new ConflictException("Дата и время события не могут быть раньше, чем через 2 часа от текущего момента.");
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
                throw new ConflictException("Дата и время намечающегося события не может быть раньше, чем через два часа от текущего момента.");
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
        PageRequest pageRequest = PageRequest.of(from / size, size);
        List<Event> eventList = eventRepository.findAllByInitiatorId(userId, pageRequest);
        return eventList.stream()
                .map(EventMapper::mapToEventShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        return EventMapper.mapToEventFullDto(
                eventRepository.findByIdAndInitiatorId(eventId, userId)
                    .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"))
        );
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
