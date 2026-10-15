package com.devflow.mapper;

import com.devflow.dto.comment.CommentResponse;
import com.devflow.entity.Comment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentMapper {

    private final UserMapper userMapper;

    public CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getText(),
                userMapper.toSummary(comment.getAuthor()),
                comment.getTask().getId(),
                comment.getCreatedAt());
    }
}
