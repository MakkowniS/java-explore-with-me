package ru.practicum.explore.main.event;

import ru.practicum.explore.main.event.dto.EventFullDto;
import ru.practicum.explore.main.event.dto.EventShortDto;
import ru.practicum.explore.main.event.dto.NewEventDto;
import ru.practicum.explore.main.event.dto.updateRequest.UpdateEventRequest;
import ru.practicum.explore.main.event.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {

    List<EventFullDto> getEventsAdmin(List<Long> users, List<EventState> states,
                                      List<Long> categories, LocalDateTime rangeStart,
                                      LocalDateTime rangeEnd, int from, int size);

    EventFullDto updateEventAdmin(Long eventId, UpdateEventRequest request);

    EventFullDto createEvent(Long userId, NewEventDto newEventDto);

    EventFullDto updateEventUser(Long userId, Long eventId, UpdateEventRequest request);

    List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size);

    EventFullDto getUserEventById(Long userId, Long eventId);
}