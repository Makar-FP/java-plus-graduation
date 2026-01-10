package ru.practicum.commentservice.service;

import ru.practicum.commentservice.dto.CommentDtoRequest;
import ru.practicum.commentservice.dto.CommentDtoResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentService {

    CommentDtoResponse create(Long userId, Long eventId, CommentDtoRequest dto);

    CommentDtoResponse update(Long userId, Long eventId, Long commId, CommentDtoRequest dto);

    void delete(Long userId, Long eventId, Long commId);

    void deleteCommentAdmin(Long commId);

    CommentDtoResponse findCommentById(Long commentId);

    List<CommentDtoResponse> findCommentsByEventId(Long eventId);

    List<CommentDtoResponse> findCommentsByUserIdAndEventId(Long userId, Long eventId);

    List<CommentDtoResponse> findCommentsAdmin(List<Integer> users, List<Integer> events, LocalDateTime rangeStart,
                                               LocalDateTime rangeEnd, Integer from, Integer size);

}
