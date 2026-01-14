package ru.practicum.commentservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.commentservice.dto.CommentDtoResponse;
import ru.practicum.commentservice.service.CommentService;


import java.util.List;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentControllerPublic {

    private final CommentService commentService;

    @GetMapping("/{commentId}")
    public CommentDtoResponse findCommentById(@PathVariable Long commentId) {
        return commentService.findCommentById(commentId);
    }

    @GetMapping("/events/{eventId}")
    public List<CommentDtoResponse> findCommentsByEventId(@PathVariable Long eventId) {
        return commentService.findCommentsByEventId(eventId);
    }
}