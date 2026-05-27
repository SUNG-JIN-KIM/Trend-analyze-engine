package com.gametrend.agent.youtube.repository;

import com.gametrend.agent.youtube.entity.YoutubeComment;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface YoutubeCommentRepository extends CrudRepository<YoutubeComment, Long> {

    Optional<YoutubeComment> findByCommentId(String commentId);

    @Query("""
            SELECT *
            FROM youtube_comments
            WHERE LOWER(game_keyword) = LOWER(:keyword)
            ORDER BY like_count DESC, published_at DESC
            """)
    List<YoutubeComment> findByGameKeyword(@Param("keyword") String keyword);
}
