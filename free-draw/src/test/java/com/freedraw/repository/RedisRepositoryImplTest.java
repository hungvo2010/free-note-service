package com.freedraw.repository;

import com.freedraw.entities.Draft;
import com.freedraw.exception.DraftNotFoundException;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RedisRepositoryImplTest {

    private static RedisRepositoryImpl repository;
    private static final String TEST_DRAFT_ID = "test-draft-123";
    private static final String TEST_DRAFT_NAME = "Test Draft";

    @BeforeAll
    static void setUp() {
        repository = new RedisRepositoryImpl();
    }

    @AfterEach
    void cleanup() {
        // Clean up test data after each test
        try {
            if (repository.exists(TEST_DRAFT_ID)) {
                repository.delete(TEST_DRAFT_ID);
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    @Order(1)
    void testSaveAndGetDraft() {
        // Create and save a draft
        Draft draft = new Draft(TEST_DRAFT_ID, TEST_DRAFT_NAME);
        repository.save(draft);

        // Retrieve the draft
        Draft retrieved = repository.getDraftById(TEST_DRAFT_ID);

        assertNotNull(retrieved);
        assertEquals(TEST_DRAFT_ID, retrieved.getDraftId());
        assertEquals(TEST_DRAFT_NAME, retrieved.getDraftName());
    }

    @Test
    @Order(2)
    void testGetNonExistentDraft() {
        assertThrows(DraftNotFoundException.class, () -> {
            repository.getDraftById("non-existent-id");
        });
    }

    @Test
    @Order(3)
    void testUpdateDraft() {
        // Save initial draft
        Draft draft = new Draft(TEST_DRAFT_ID, TEST_DRAFT_NAME);
        repository.save(draft);

        // Update the draft
        Draft updatedDraft = new Draft(TEST_DRAFT_ID, "Updated Name");
        repository.save(updatedDraft);

        // Verify update
        Draft retrieved = repository.getDraftById(TEST_DRAFT_ID);
        assertEquals("Updated Name", retrieved.getDraftName());
    }

    @Test
    @Order(4)
    void testDeleteDraft() {
        // Save a draft
        Draft draft = new Draft(TEST_DRAFT_ID, TEST_DRAFT_NAME);
        repository.save(draft);

        // Verify it exists
        assertTrue(repository.exists(TEST_DRAFT_ID));

        // Delete it
        repository.delete(TEST_DRAFT_ID);

        // Verify it's gone
        assertFalse(repository.exists(TEST_DRAFT_ID));
    }

    @Test
    @Order(5)
    void testDeleteNonExistentDraft() {
        assertThrows(DraftNotFoundException.class, () -> {
            repository.delete("non-existent-id");
        });
    }

    @Test
    @Order(6)
    void testExists() {
        assertFalse(repository.exists(TEST_DRAFT_ID));

        Draft draft = new Draft(TEST_DRAFT_ID, TEST_DRAFT_NAME);
        repository.save(draft);

        assertTrue(repository.exists(TEST_DRAFT_ID));
    }

    @Test
    @Order(7)
    void testGetAllDrafts() {
        // Save multiple drafts
        Draft draft1 = new Draft(TEST_DRAFT_ID, TEST_DRAFT_NAME);
        Draft draft2 = new Draft(TEST_DRAFT_ID + "-2", "Test Draft 2");
        
        repository.save(draft1);
        repository.save(draft2);

        List<Draft> allDrafts = repository.getAllDrafts();
        
        assertTrue(allDrafts.size() >= 2);
        
        // Cleanup second draft
        repository.delete(TEST_DRAFT_ID + "-2");
    }

    @Test
    @Order(8)
    void testGetAllDraftIds() {
        Draft draft = new Draft(TEST_DRAFT_ID, TEST_DRAFT_NAME);
        repository.save(draft);

        Set<String> ids = repository.getAllDraftIds();
        
        assertTrue(ids.contains(TEST_DRAFT_ID));
    }

    @Test
    @Order(9)
    void testCount() {
        long initialCount = repository.count();
        
        Draft draft = new Draft(TEST_DRAFT_ID, TEST_DRAFT_NAME);
        repository.save(draft);

        long newCount = repository.count();
        assertEquals(initialCount + 1, newCount);
    }

    @Test
    @Order(10)
    void testSaveNullDraft() {
        assertThrows(IllegalArgumentException.class, () -> {
            repository.save(null);
        });
    }

    @Test
    @Order(11)
    void testGetDraftWithNullId() {
        assertThrows(IllegalArgumentException.class, () -> {
            repository.getDraftById(null);
        });
    }

    @Test
    @Order(12)
    void testGetDraftWithEmptyId() {
        assertThrows(IllegalArgumentException.class, () -> {
            repository.getDraftById("");
        });
    }
}
