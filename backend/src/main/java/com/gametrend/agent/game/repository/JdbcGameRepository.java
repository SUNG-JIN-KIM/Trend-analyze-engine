package com.gametrend.agent.game.repository;

import com.gametrend.agent.game.domain.Game;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public class JdbcGameRepository implements GameRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcGameRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Game> findById(Long id) {
        String sql = "SELECT id, name, description FROM games WHERE id = ?";
        try {
            Game game = jdbcTemplate.queryForObject(sql, new Object[]{id}, (rs, rowNum) -> {
                Game game = new Game();
                game.setId(rs.getLong("id"));
                game.setName(rs.getString("name"));
                game.setDescription(rs.getString("description"));
                return game;
            });

            return Optional.ofNullable(game);

        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}