package ru.practicum.explore.main.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCommentDto {

    private Long userId;
    private Long commentId;

    @NotBlank(message = "Комментарий не может быть пустым")
    @Size(min = 2, max = 2000, message = "Длина комментария должна быть от 2 до 2000 символов")
    private String text;
}
