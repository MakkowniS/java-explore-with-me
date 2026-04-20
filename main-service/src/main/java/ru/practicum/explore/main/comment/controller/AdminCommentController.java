package ru.practicum.explore.main.comment.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explore.main.comment.CommentService;
import ru.practicum.explore.main.comment.dto.CommentDto;
import ru.practicum.explore.main.comment.model.CommentState;

import java.util.List;

@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
@Slf4j
public class AdminCommentController {

    private final CommentService commentService;

    @PatchMapping("/{commentId}")
    public CommentDto moderate(@PathVariable Long commentId,
                               @RequestParam boolean approved) {
        log.info("Admin: Модерация комментария {}. Подтверждён: {}", commentId, approved);
        return commentService.moderateComment(commentId, approved);
    }

    @GetMapping
    public List<CommentDto> getAllByState(@RequestParam @NotBlank CommentState state,
                                          @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                          @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("Admin: Получение комментариев со статусом {}", state);
        return commentService.getCommentsByState(state, from, size);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteByAdmin(@PathVariable Long commentId) {
        log.info("Admin: Удаление комментария {} администратором", commentId);
        commentService.deleteCommentAdmin(commentId);
    }
}
