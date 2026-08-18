package com.freedraw.repository;

import com.freedraw.entities.Draft;
import com.freedraw.entities.DraftAction;
import com.freedraw.resources.PostgresClient;
import com.freenote.app.server.util.JSONUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL-backed draft repository.
 *
 * Storage model (append-only action log):
 *   draft        (draft_id PK, draft_name, action_count)
 *   draft_action (draft_id FK, seq PK, shapes_json JSONB)
 *
 * Writes are append-only: save() upserts the draft, reads the stored
 * action_count, appends only the actions beyond it, and bumps the count.
 * Simple SQL, no concurrency control (assume no concurrent writers).
 */
public class PostgresDraftRepositoryImpl implements DraftRepository {
    private static final Logger log = LogManager.getLogger(PostgresDraftRepositoryImpl.class);

    private static final String SELECT_DRAFT =
            "SELECT draft_id, draft_name FROM draft WHERE draft_id = ?";
    private static final String SELECT_ACTIONS =
            "SELECT shapes_json FROM draft_action WHERE draft_id = ? ORDER BY seq";
    private static final String UPSERT_DRAFT =
            "INSERT INTO draft (draft_id, draft_name) VALUES (?, ?) " +
            "ON CONFLICT (draft_id) DO UPDATE SET draft_name = EXCLUDED.draft_name";
    private static final String SELECT_ACTION_COUNT =
            "SELECT action_count FROM draft WHERE draft_id = ?";
    private static final String INSERT_ACTION =
            "INSERT INTO draft_action (draft_id, seq, shapes_json) VALUES (?, ?, ?) " +
            "ON CONFLICT (draft_id, seq) DO NOTHING";
    private static final String UPDATE_ACTION_COUNT =
            "UPDATE draft SET action_count = ?, updated_at = now() WHERE draft_id = ?";

    public PostgresDraftRepositoryImpl() {
        PostgresClient.getDataSource();
        log.info("PostgresDraftRepositoryImpl initialized");
    }

    @Override
    public Draft getDraftById(String draftId) {
        if (draftId == null || draftId.isEmpty()) {
            throw new IllegalArgumentException("Draft ID cannot be null or empty");
        }

        try (Connection connection = PostgresClient.getConnection()) {
            Draft draft = loadDraft(connection, draftId);
            if (draft == null) {
                return null;
            }
            draft.setActions(loadActions(connection, draftId));
            return draft;
        } catch (SQLException e) {
            log.error("Error loading draft {}: {}", draftId, e.getMessage(), e);
            throw new RuntimeException("Failed to load draft: " + draftId, e);
        }
    }

    @Override
    public void save(Draft draft) {
        if (draft == null || draft.getDraftId() == null || draft.getDraftId().isEmpty()) {
            throw new IllegalArgumentException("Draft and draftId cannot be null or empty");
        }

        try (Connection connection = PostgresClient.getConnection()) {
            connection.setAutoCommit(false);
            try {
                upsertDraft(connection, draft);
                long existingCount = getActionCount(connection, draft.getDraftId());
                appendNewActions(connection, draft, existingCount);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            log.error("Error saving draft {}: {}", draft.getDraftId(), e.getMessage(), e);
            throw new RuntimeException("Failed to save draft: " + draft.getDraftId(), e);
        }
    }

    private Draft loadDraft(Connection connection, String draftId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_DRAFT)) {
            ps.setString(1, draftId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Draft(rs.getString("draft_id"), rs.getString("draft_name"));
                }
                return null;
            }
        }
    }

    private List<DraftAction> loadActions(Connection connection, String draftId) throws SQLException {
        List<DraftAction> actions = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(SELECT_ACTIONS)) {
            ps.setString(1, draftId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DraftAction action = JSONUtils.fromJSON(rs.getString("shapes_json"), DraftAction.class);
                    if (action != null) {
                        actions.add(action);
                    }
                }
            }
        }
        return actions;
    }

    private void upsertDraft(Connection connection, Draft draft) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(UPSERT_DRAFT)) {
            ps.setString(1, draft.getDraftId());
            ps.setString(2, draft.getDraftName());
            ps.executeUpdate();
        }
    }

    private long getActionCount(Connection connection, String draftId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_ACTION_COUNT)) {
            ps.setString(1, draftId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("action_count");
                }
            }
        }
        throw new SQLException("Draft row vanished after upsert: " + draftId);
    }

    private void appendNewActions(Connection connection, Draft draft, long existingCount) throws SQLException {
        List<DraftAction> actions = draft.getActions();
        long actionCount = actions.size();
        for (long seq = existingCount; seq < actionCount; seq++) {
            DraftAction action = actions.get((int) seq);
            if (action == null) {
                continue;
            }
            try (PreparedStatement ps = connection.prepareStatement(INSERT_ACTION)) {
                ps.setString(1, draft.getDraftId());
                ps.setLong(2, seq);
                ps.setString(3, JSONUtils.toJSONString(action));
                ps.executeUpdate();
            }
        }
        try (PreparedStatement ps = connection.prepareStatement(UPDATE_ACTION_COUNT)) {
            ps.setLong(1, actionCount);
            ps.setString(2, draft.getDraftId());
            ps.executeUpdate();
        }
    }
}
