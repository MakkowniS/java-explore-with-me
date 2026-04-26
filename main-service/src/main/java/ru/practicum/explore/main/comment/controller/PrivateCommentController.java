package ru.practicum.explore.main.comment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explore.main.comment.CommentService;
import ru.practicum.explore.main.comment.dto.CommentDto;
import ru.practicum.explore.main.comment.dto.NewCommentDto;
import ru.practicum.explore.main.comment.dto.UpdateCommentDto;

@RestController
@RequiredArgsConstructor
@Validated
@Slf4j
@RequestMapping("/users/{userId}/comments")
public class PrivateCommentController {

    private final CommentService commentService;

    @PostMapping("/{eventId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto addComment(@PathVariable Long userId,
                                 @PathVariable Long eventId,
                                 @Valid @RequestBody NewCommentDto dto) {
        dto.setUserId(userId);
        dto.setEventId(eventId);
        log.info("Private: Создание комментария от пользователя {} для события {}", userId, eventId);
        return commentService.addComment(dto);
    }

    @PatchMapping("/{commentId}")
    public CommentDto updateComment(@PathVariable Long userId,
                                    @PathVariable Long commentId,
                                    @Valid @RequestBody UpdateCommentDto dto) {
        dto.setUserId(userId);
        dto.setCommentId(commentId);
        log.info("Private: Обновление комментария {} от пользователя {}", commentId, userId);
        return commentService.updateComment(dto);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long userId, @PathVariable Long commentId) {
        log.info("Private: Удаление комментария {} пользователем {}", commentId, userId);
        commentService.deleteUserComment(userId, commentId);
    }

}
