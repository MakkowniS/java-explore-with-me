package ru.practicum.explore.main.compilation;

import ru.practicum.explore.main.compilation.dto.CompilationDto;
import ru.practicum.explore.main.compilation.model.Compilation;
import ru.practicum.explore.main.event.EventMapper;

public class CompilationMapper {

    public static CompilationDto mapToCompilationDto(Compilation c) {
        return CompilationDto.builder()
                .id(c.getId())
                .events(c.getEvents().stream().map(EventMapper::mapToEventShortDto).toList())
                .pinned(c.getPinned())
                .title(c.getTitle())
                .build();
    }

}
