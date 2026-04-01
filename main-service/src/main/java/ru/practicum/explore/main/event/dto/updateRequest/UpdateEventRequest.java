package ru.practicum.explore.main.event.dto.updateRequest;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.explore.main.event.model.Location;
import ru.practicum.explore.main.event.model.StateAction;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateEventRequest {

    @Size(min = 20, max = 2000)
    private String annotation;

    private Long category;

    @Size(min = 20, max = 7000)
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    private Location location;
    private Boolean paid;
    private Integer participantLimit;
    private Boolean requestModeration;

    private StateAction stateAction; // PUBLISH_EVENT или REJECT_EVENT

    @Size(min = 3, max = 120)
    private String title;

}