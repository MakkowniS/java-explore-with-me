package ru.practicum.explore.main.event;

import ru.practicum.explore.main.category.CategoryMapper;
import ru.practicum.explore.main.event.dto.EventFullDto;
import ru.practicum.explore.main.event.dto.EventShortDto;
import ru.practicum.explore.main.event.dto.NewEventDto;
import ru.practicum.explore.main.event.model.Event;
import ru.practicum.explore.main.user.UserMapper;

public class EventMapper {

    public static EventFullDto mapToEventFullDto(Event event) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.mapToCategoryDto(event.getCategory()))
                .confirmedRequests(event.getConfirmedRequests())
                .createdOn(event.getCreatedOn())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .initiator(UserMapper.mapToUserShortDto(event.getInitiator()))
                .location(event.getLocation())
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn())
                .requestModeration(event.getRequestModeration())
                .state(event.getState())
                .title(event.getTitle())
                .build();
    }

    public static Event mapToEvent(NewEventDto newEventDto) {
        return Event.builder().annotation(newEventDto.getAnnotation()).description(newEventDto.getDescription()).eventDate(newEventDto.getEventDate()).location(newEventDto.getLocation()).paid(newEventDto.getPaid()).participantLimit(newEventDto.getParticipantLimit()).requestModeration(newEventDto.getRequestModeration()).title(newEventDto.getTitle()).build();
    }

    public static EventShortDto mapToEventShortDto(Event event) {
        return EventShortDto.builder().id(event.getId()).annotation(event.getAnnotation()).category(CategoryMapper.mapToCategoryDto(event.getCategory())).confirmedRequests(event.getConfirmedRequests()).eventDate(event.getEventDate()).initiator(UserMapper.mapToUserShortDto(event.getInitiator())).paid(event.getPaid()).title(event.getTitle()).views(event.getViews()).build();
    }

}
