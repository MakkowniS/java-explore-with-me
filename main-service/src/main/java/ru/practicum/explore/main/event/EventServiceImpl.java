package ru.practicum.explore.main.event;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.practicum.explore.main.event.dto.EventFullDto;
import ru.practicum.explore.main.event.model.Event;
import ru.practicum.explore.main.event.model.EventState;

import java.awt.print.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;

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
}
