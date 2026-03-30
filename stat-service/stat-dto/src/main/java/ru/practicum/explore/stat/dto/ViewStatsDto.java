package ru.practicum.explore.stat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ViewStatsDto {

    private String app; // название сервиса
    private String uri; // URI сервиса
    private Long hits; // количество просмотров

}
