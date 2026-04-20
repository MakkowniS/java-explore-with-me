package ru.practicum.explore.main.comment;

import lombok.experimental.UtilityClass;
import ru.practicum.explore.main.comment.dto.CommentDto;
import ru.practicum.explore.main.comment.dto.NewCommentDto;
import ru.practicum.explore.main.comment.model.Comment;
import ru.practicum.explore.main.comment.model.CommentState;
import ru.practicum.explore.main.event.model.Event;
import ru.practicum.explore.main.user.model.User;

import java.time.LocalDateTime;

@UtilityClass
public class CommentMapper {

    public static Comment mapToComment(NewCommentDto newCommentDto, User author, Event event) {
        return Comment.builder()
                .text(newCommentDto.getText())
                .author(author)
                .event(event)
                .createdOn(LocalDateTime.now())
                .state(CommentState.PENDING)
                .build();
    }

    public static CommentDto mapToCommentDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .authorName(comment.getAuthor().getName())
                .eventId(comment.getEvent().getId())
                .createdOn(comment.getCreatedOn())
                .build();
    }

}
