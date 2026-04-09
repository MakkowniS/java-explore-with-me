package ru.practicum.explore.main.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explore.main.error.model.exception.ValidationException;
import ru.practicum.explore.main.event.EventService;
import ru.practicum.explore.main.event.dto.EventFullDto;
import ru.practicum.explore.main.event.dto.EventShortDto;
import ru.practicum.explore.main.event.dto.eventFilters.PublicEventFilter;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Validated
public class PublicEventController {

    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> getEvents(PublicEventFilter filter, HttpServletRequest request) {

        if (filter.getRangeStart() != null && filter.getRangeEnd() != null && filter.getRangeStart().isAfter(filter.getRangeEnd())) {
            throw new ValidationException("Start date cannot be after end date"); // Конец не может быть раньше начала
        }

        return eventService.getEventsPublic(filter, request);
    }

    @GetMapping("/{id}")
    public EventFullDto getEventById(@PathVariable Long id, HttpServletRequest request) {
        return eventService.getEventByIdPublic(id, request);
    }

}
