package com.freedraw.repository.persistence.disk;

import com.freedraw.entities.Draft;
import com.freedraw.entities.DraftAction;
import com.freedraw.repository.persistence.disk.service.SearchFieldByOffset;
import com.freedraw.repository.persistence.disk.service.SearchIx;
import com.freedraw.repository.persistence.disk.service.SearchOffset;
import com.freedraw.repository.persistence.disk.service.impl.SearchDraftActionsRangeByOffset;
import com.freedraw.repository.persistence.disk.service.impl.generic.FixedLengthFieldSearchByOffset;
import com.freedraw.repository.persistence.disk.service.impl.generic.SearchIxImpl;
import com.freedraw.repository.persistence.disk.service.impl.generic.VariableLengthFieldSearchByOffset;
import com.freenote.app.server.util.JSONUtils;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

@Getter
public class PersistenceContext implements PersistenceWriter {
    private static final Logger log = LogManager.getLogger(PersistenceContext.class);
    private SearchIx<String> searchDraftIx;
    private SearchOffset searchOffset;
    private final Map<String, SearchFieldByOffset> searchOffsetMap = new HashMap<>();
    
    // In-memory store only
    private InMemoryDraftStore inMemoryStore;

    public void initData() {
        try {
            initOrLoadFileIndex();
            initOrLoadFileOffsets();
            initOrLoadSingleFieldData();
            initInMemoryStore();
        } catch (IOException e) {
            log.error("Error initializing PersistenceContext: {}", e.getMessage());
        }
    }
    
    /**
     * Initialize in-memory store and load all data from disk
     */
    private void initInMemoryStore() {
        log.info("Initializing in-memory draft store");
        inMemoryStore = new InMemoryDraftStore();
        
        // Load all existing drafts from disk
        List<Draft> draftsFromDisk = getAllDraftsFromDisk();
        inMemoryStore.loadFromDisk(draftsFromDisk);
        
        log.info("In-memory store initialized with {} drafts", draftsFromDisk.size());
    }

    /**
     * Persist a draft to disk (called by scheduler)
     * Always rewrites all actions at the end of the vector to avoid conflicts
     */
    @Override
    public void persist(Draft draft) {
        var draftPosition = this.searchDraftIx.insert(draft.getDraftId());
        log.info("Persisting draftId: {}, position: {}", draft.getDraftId(), draftPosition);

        var actionsVector = getActionsVector();
        var actionsStartLengthOffsets = getActionsStartLengthOffsets();

        int newActionsCount = draft.getActions().size();
        
        if (newActionsCount == 0) {
            log.info("No actions to persist for draftId: {}", draft.getDraftId());
            // Still need to ensure offset entry exists with [0, 0]
            Integer[] existing = actionsStartLengthOffsets.getData(draftPosition);
            if (existing == null) {
                actionsStartLengthOffsets.append(new Integer[]{0, 0});
            }
            return;
        }

        // ALWAYS rewrite all actions at the end of the vector
        // This solves the insertion/append conflict problem
        int newStart = actionsVector.getSize();
        
        log.info("Rewriting {} actions for draftId: {} at position {}", 
            newActionsCount, draft.getDraftId(), newStart);

        for (var action : draft.getActions().stream().filter(Objects::nonNull).toList()) {
            actionsVector.append(JSONUtils.toJSONString(action));
        }

        // Check if position exists, then either update or append
        Integer[] existing = actionsStartLengthOffsets.getData(draftPosition);
        if (existing != null) {
            // Position exists, update it
            actionsStartLengthOffsets.put(draftPosition, new int[]{newStart, newActionsCount});
        } else {
            // Position doesn't exist, append it
            actionsStartLengthOffsets.append(new Integer[]{newStart, newActionsCount});
        }
        
        log.info("Successfully persisted draft {} with {} actions", draft.getDraftId(), newActionsCount);
    }

    private VariableLengthFieldSearchByOffset<String> getActionsVector() {
        return (VariableLengthFieldSearchByOffset<String>) this.searchOffsetMap.get("actions");
    }

    /**
     * Get all drafts from in-memory store (fast)
     */
    public List<Draft> getAllDrafts() {
        if (inMemoryStore != null) {
            return inMemoryStore.getAllDrafts();
        }
        // Fallback to disk if in-memory store not initialized
        return getAllDraftsFromDisk();
    }
    
    /**
     * Get all drafts directly from disk (used during initialization)
     * CRITICAL: LinkedHashMap in SearchIxImpl preserves insertion order,
     * so list index matches the stored position in actions.offsets
     */
    private List<Draft> getAllDraftsFromDisk() {
        var allDraftIds = this.searchDraftIx.getAll();
        var result = new ArrayList<Draft>();
        for (int draftIdIdx = 0; draftIdIdx < allDraftIds.size(); draftIdIdx++) {
            var draftId = allDraftIds.get(draftIdIdx);
            result.add(buildDraftById(draftId, draftIdIdx));
        }
        return result;
    }
    
    /**
     * Get a specific draft from in-memory store
     */
    public Optional<Draft> getDraft(String draftId) {
        if (inMemoryStore != null) {
            return inMemoryStore.getDraft(draftId);
        }
        return Optional.empty();
    }
    
    /**
     * Save draft to in-memory store (will be persisted later)
     */
    public void saveDraft(Draft draft) {
        if (inMemoryStore != null) {
            inMemoryStore.saveDraft(draft);
        } else {
            log.warn("In-memory store not initialized, cannot save draft");
        }
    }
    
    /**
     * Add action to draft in memory
     */
    public void addAction(String draftId, DraftAction action) {
        if (inMemoryStore != null) {
            inMemoryStore.addAction(draftId, action);
        } else {
            log.warn("In-memory store not initialized, cannot add action");
        }
    }
    
    /**
     * Insert action at specific position in memory
     */
    public void insertAction(String draftId, int position, DraftAction action) {
        if (inMemoryStore != null) {
            inMemoryStore.insertAction(draftId, position, action);
        } else {
            log.warn("In-memory store not initialized, cannot insert action");
        }
    }

    private Draft buildDraftById(String draftId, int idx) {
        var draft = new Draft(draftId);
        draft.setActions(getDraftActionsStartLength(idx));
        return draft;
    }

    private List<DraftAction> getDraftActionsStartLength(int draftPosition) {
        var startLength = getStartLength(getActionsStartLengthOffsets(), draftPosition);
        
        // Handle null or empty case (draft exists but has no actions yet)
        if (startLength == null || startLength.length < 2) {
            log.debug("No actions found for draft at position: {}", draftPosition);
            return new ArrayList<>();
        }
        
        var start = startLength[0];
        var length = startLength[1];
        
        // Validate start and length
        if (start < 0 || length < 0) {
            log.warn("Invalid start/length [{}, {}] for draft at position: {}", start, length, draftPosition);
            return new ArrayList<>();
        }
        
        if (length == 0) {
            return new ArrayList<>();
        }
        
        var actionsVector = getActionsVector();
        var result = new ArrayList<DraftAction>();
        for (int j = 0; j < length; j++) {
            var actionData = actionsVector.getData(start + j);
            if (actionData != null && !actionData.isEmpty()) {
//                log.info("Action data: {}", actionData);
                result.add(JSONUtils.fromJSON(actionData, DraftAction.class));
            }
        }
        return result;
    }

    private FixedLengthFieldSearchByOffset<Integer[]> getActionsStartLengthOffsets() {
        return (FixedLengthFieldSearchByOffset<Integer[]>) this.searchOffsetMap.get("actions.offsets");
    }

    private Integer[] getStartLength(FixedLengthFieldSearchByOffset<Integer[]> actionsStartLengthOffsets, int draftPosition) {
        return actionsStartLengthOffsets.getData(draftPosition);

    }

    private void initOrLoadFileOffsets() throws IOException {
        var fileOffsets = FileUtility.findFile("offsets");
        this.searchOffset = new SearchDraftActionsRangeByOffset(fileOffsets.getPath());
    }

    private void initOrLoadFileIndex() throws IOException {
        var draftIdIdx = FileUtility.findFile("draftId.idx");
        this.searchDraftIx = new SearchIxImpl<>(draftIdIdx.getPath(), String.class);
    }

    private void initOrLoadSingleFieldData() throws IOException {
        var fileActionOffsets = FileUtility.findFile("actions.offsets");
        var fileActions = FileUtility.findFile("actions");
        this.searchOffsetMap.put("actions.offsets", new FixedLengthFieldSearchByOffset<>(fileActionOffsets.getPath(), Integer[].class));
        this.searchOffsetMap.put("actions", new VariableLengthFieldSearchByOffset<>(fileActions.getPath(), String.class));
    }

}

