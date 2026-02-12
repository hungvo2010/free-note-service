package com.freedraw.example;

import com.freedraw.entities.Draft;
import com.freedraw.entities.DraftAction;
import com.freedraw.repository.RedisRepositoryImpl;
import com.freedraw.resources.RedisClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Example demonstrating Redis repository usage for Draft management
 */
public class RedisRepositoryExample {
    private static final Logger log = LogManager.getLogger(RedisRepositoryExample.class);

    public static void main(String[] args) {
        RedisRepositoryImpl repository = new RedisRepositoryImpl();

        try {
            // Example 1: Create and save a new draft
            log.info("=== Example 1: Creating and saving a draft ===");
            Draft newDraft = new Draft();
            DraftAction action = new DraftAction();
            newDraft.addAction(action);
            
            repository.save(newDraft);
            log.info("Saved draft with ID: {}", newDraft.getDraftId());

            // Example 2: Retrieve a draft by ID
            log.info("\n=== Example 2: Retrieving draft by ID ===");
            Draft retrievedDraft = repository.getDraftById(newDraft.getDraftId());
            log.info("Retrieved draft: ID={}, Name={}", 
                    retrievedDraft.getDraftId(), 
                    retrievedDraft.getDraftName());

            // Example 3: Update a draft
            log.info("\n=== Example 3: Updating a draft ===");
            Draft updatedDraft = new Draft(newDraft.getDraftId(), "My Updated Draft");
            repository.save(updatedDraft);
            log.info("Updated draft name to: {}", updatedDraft.getDraftName());

            // Example 4: Check if draft exists
            log.info("\n=== Example 4: Checking draft existence ===");
            boolean exists = repository.exists(newDraft.getDraftId());
            log.info("Draft exists: {}", exists);

            // Example 5: Get all drafts
            log.info("\n=== Example 5: Getting all drafts ===");
            List<Draft> allDrafts = repository.getAllDrafts();
            log.info("Total drafts in Redis: {}", allDrafts.size());
            allDrafts.forEach(draft -> 
                log.info("  - Draft ID: {}, Name: {}", draft.getDraftId(), draft.getDraftName())
            );

            // Example 6: Get draft count
            log.info("\n=== Example 6: Getting draft count ===");
            long count = repository.count();
            log.info("Total draft count: {}", count);

            // Example 7: Delete a draft
            log.info("\n=== Example 7: Deleting a draft ===");
            repository.delete(newDraft.getDraftId());
            log.info("Deleted draft with ID: {}", newDraft.getDraftId());

            // Verify deletion
            boolean stillExists = repository.exists(newDraft.getDraftId());
            log.info("Draft still exists after deletion: {}", stillExists);

        } catch (Exception e) {
            log.error("Error in Redis repository example: {}", e.getMessage(), e);
        } finally {
            // Cleanup: Shutdown Redis connection
            RedisClient.shutdown();
            log.info("\n=== Redis connection closed ===");
        }
    }
}
