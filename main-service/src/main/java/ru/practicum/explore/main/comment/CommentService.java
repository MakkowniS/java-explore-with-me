package ru.practicum.explore.main.comment;

import ru.practicum.explore.main.comment.dto.CommentDto;
import ru.practicum.explore.main.comment.dto.NewCommentDto;
import ru.practicum.explore.main.comment.dto.UpdateCommentDto;
import ru.practicum.explore.main.comment.model.CommentState;

import java.util.List;

public interface CommentService {

    // Private (User)
    CommentDto addComment(NewCommentDto dto);

    CommentDto updateComment(UpdateCommentDto dto);

    List<CommentDto> getUserComments(Long userId, int from, int size);

    void deleteUserComment(Long userId, Long commentId);

    // Public
    List<CommentDto> getEventComments(Long eventId, int from, int size);

    // Admin
    CommentDto moderateComment(Long commentId, boolean approved);

    List<CommentDto> getCommentsByState(CommentState state, int from, int size);

    void deleteCommentAdmin(Long commentId);

}
