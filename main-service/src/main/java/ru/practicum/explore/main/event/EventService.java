package ru.practicum.explore.main.event;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.practicum.explore.main.event.dto.EventFullDto;
import ru.practicum.explore.main.event.model.Event;
import ru.practicum.explore.main.event.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {



    List<EventFullDto> getEventsAdmin(List<Long> users, List<EventState> states,
                                      List<Long> categories, LocalDateTime rangeStart,
                                      LocalDateTime rangeEnd, int from, int size);}
