package ru.practicum.explore.main.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewCommentDto {

    @NotBlank(message = "Комментарий не может быть пустым")
    @Size(min = 2, max = 2000, message = "Длина комментария должна быть от 2 до 2000 символов")
    private String text;
}
