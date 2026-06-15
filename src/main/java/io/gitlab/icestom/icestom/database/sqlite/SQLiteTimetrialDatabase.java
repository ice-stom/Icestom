package io.gitlab.icestom.icestom.database.sqlite;

import io.gitlab.icestom.icestom.database.TimetrialDatabase;
import io.gitlab.icestom.icestom.timetrial.TimeTrialSerializer;
import io.gitlab.icestom.icestom.timetrial.lap.TimeTrialResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SQLiteTimetrialDatabase implements TimetrialDatabase, AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(SQLiteTimetrialDatabase.class.getName());

    private final Connection connection;

    public SQLiteTimetrialDatabase(@NotNull String databasePath) throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS attempts (
                        id TEXT PRIMARY KEY,
                        player TEXT NOT NULL,
                        track TEXT NOT NULL,
                        time INTEGER NOT NULL,
                        splits TEXT NOT NULL,
                        ticks TEXT NOT NULL
                    );
                    """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_track ON attempts (player, track, time);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_track_time ON attempts (track, time);");
        }
    }

    @Override
    public @NotNull UUID newAttempt(@NotNull TimeTrialResult result) {
        UUID id = UUID.randomUUID();

        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO attempts (id, player, track, time, splits, ticks)
                VALUES (?, ?, ?, ?, ?, ?);
                """)) {
            ps.setString(1, id.toString());
            ps.setString(2, result.player().toString());
            ps.setString(3, result.track());
            ps.setLong(4, result.getTime());
            ps.setString(5, TimeTrialSerializer.encodeSplits(result.splits()));
            ps.setString(6, TimeTrialSerializer.encodeTicks(result.ticks()));
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to insert attempt for player " + result.player(), e);
            throw new RuntimeException("Database error while saving attempt", e);
        }

        return id;
    }

    @Override
    public @Nullable TimeTrialResult getAttempt(@NotNull UUID attempt) {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT id, player, track, time, splits, ticks
                FROM attempts
                WHERE id = ?;
                """)) {
            ps.setString(1, attempt.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to fetch attempt " + attempt, e);
        }
        return null;
    }

    @Override
    public @Nullable TimeTrialResult getBestAttempt(@NotNull UUID player, @NotNull String track_id) {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT id, player, track, time, splits, ticks
                FROM attempts
                WHERE player = ? AND track = ?
                ORDER BY time ASC
                LIMIT 1;
                """)) {
            ps.setString(1, player.toString());
            ps.setString(2, track_id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to fetch best attempt for player " + player + " on track " + track_id, e);
        }
        return null;
    }

    @Override
    public @NotNull List<TimeTrialResult> getBestAttempts(@NotNull String track_id, int number) {
        List<TimeTrialResult> results = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT id, player, track, time, splits, ticks
                FROM (
                    SELECT id, player, track, time, splits, ticks,
                           ROW_NUMBER() OVER (
                               PARTITION BY player
                               ORDER BY time ASC, id ASC
                           ) AS rn
                    FROM attempts
                    WHERE track = ?
                )
                WHERE rn = 1
                ORDER BY time ASC
                LIMIT ?;
                """)) {
            ps.setString(1, track_id);
            ps.setInt(2, number);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to fetch best attempts for track " + track_id, e);
        }

        return Collections.unmodifiableList(results);
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to close SQLite connection", e);
        }
    }

    private @NotNull TimeTrialResult mapRow(@NotNull ResultSet rs) throws SQLException {
        UUID player = UUID.fromString(rs.getString("player"));
        String track = rs.getString("track");

        return new TimeTrialResult(
                player,
                track,
                TimeTrialSerializer.decodeSplits(rs.getString("splits")),
                TimeTrialSerializer.decodeTicks(rs.getString("ticks"))
        );
    }
}