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

        // Update the offset with new start and length
        actionsStartLengthOffsets.put(draftPosition, new int[]{newStart, newActionsCount});
        
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
        var start = startLength[0];
        var length = startLength[1];
        var actionsVector = getActionsVector();
        var result = new ArrayList<DraftAction>();
        for (int j = 0; j < length; j++) {
            var actionData = actionsVector.getData(start + j);
            if (!actionData.isEmpty()) {
//                log.info("Action data: {}", actionData);
            }
            result.add(JSONUtils.fromJSON(actionData, DraftAction.class));
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

