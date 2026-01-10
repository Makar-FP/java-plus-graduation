package ru.practicum.commentservice.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.commentservice.dto.*;
import ru.practicum.commentservice.model.Comment;

import java.time.LocalDateTime;

@Component
public class CommentMapper {

    public static CommentDtoResponse toDto(Comment comment, UserRequestDto user, EventFullDto event) {
        return CommentDtoResponse.builder()
                .id(comment.getId())
                .created(comment.getCreated())
                .event(toEventShortDto(event))
                .user(toUserShortDto(user))
                .text(comment.getText())
                .build();
    }

    public static Comment toEntity(CommentDtoRequest dto,
                                   EventFullDto eventEntity,
                                   UserRequestDto userEntity,
                                   LocalDateTime createdEntity) {
        return Comment.builder()
                .text(dto.getText())
                .eventId(eventEntity.getId())
                .userId(userEntity.getId())
                .created(createdEntity)
                .build();
    }

    private static EventShortDto toEventShortDto(EventFullDto event) {
        return EventShortDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .build();
    }

    private static UserShortDto toUserShortDto(UserRequestDto user) {
        return new UserShortDto(user.getId(), user.getName());
    }
}
