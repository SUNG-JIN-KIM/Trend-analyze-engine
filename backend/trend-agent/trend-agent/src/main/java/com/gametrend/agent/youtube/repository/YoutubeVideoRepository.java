package com.gametrend.agent.youtube.repository;

import com.gametrend.agent.youtube.entity.YoutubeVideo;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface YoutubeVideoRepository extends CrudRepository<YoutubeVideo, Long> {

    Optional<YoutubeVideo> findByVideoId(String videoId);

    @Query("""
            SELECT *
            FROM youtube_videos
            WHERE LOWER(game_keyword) = LOWER(:keyword)
            ORDER BY view_count DESC
            """)
    List<YoutubeVideo> findByKeywordOrderByViewCountDesc(@Param("keyword") String keyword);
}
