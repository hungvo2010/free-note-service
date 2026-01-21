# Draft Persistence Flow Documentation

## Overview

This document explains how drafts are persisted to disk in the free-draw application, covering both new drafts and updates to existing drafts.

## File Structure

The persistence system uses multiple files to store draft data:

- **`draftId.idx`**: Maps draft IDs to their position index
- **`actions.offsets`**: Stores `[start, length]` pairs for each draft's actions
- **`actions`**: Contains the actual serialized action data (JSON strings)
- **`actions_index`**: Metadata for variable-length action storage

## Scenario 1: Persisting a Fully New Draft

### Example
```
Draft ID: "abc-123"
Actions: [action1, action2]
```

### Step-by-Step Flow

#### 1. Insert Draft ID
```java
draftPosition = searchDraftIx.insert("abc-123")
```
- Checks `valueIndexMap`: "abc-123" **NOT FOUND**
- Creates new index: `position = 0` (first draft)
- Writes "abc-123" to `draftId.idx` file at position 0
- **Returns: `draftPosition = 0`**

#### 2. Get Current Actions Vector Size
```java
currentActionsStart = actionsVector.getSize() // = 0 (empty file)
```

#### 3. Get or Create Offset Entry
```java
startLength = actionsStartLengthOffsets.getOrAppend(0, [0, 0])
```
- Position 0 **doesn't exist** in `actions.offsets` file
- Appends default value: `[0, 0]` at position 0
- **Returns: `[0, 0]`**

#### 4. Extract Start and Length
```java
start = 0   // where actions begin in actions vector
length = 0  // no actions stored yet
```

#### 5. Calculate New Actions
```java
newActionsCount = 2  // action1, action2
newActions = draft.actions[0:2] = [action1, action2]
```
Since `length = 0`, all actions are new.

#### 6. Append Actions to Vector
```java
// Append action1
actionsVector.append(JSON(action1))
  → Serializes action1 to JSON string
  → Writes to actions file at byte position 0
  → Updates actions_index with [0, lengthOfAction1]
  → Returns offset = 0

// Append action2
actionsVector.append(JSON(action2))
  → Serializes action2 to JSON string
  → Writes to actions file at byte position lengthOfAction1
  → Updates actions_index with [lengthOfAction1, lengthOfAction2]
  → Returns offset = 1
```

#### 7. Update Metadata
```java
actionsStartLengthOffsets.put(0, [0, 2])
```
- Updates position 0 in `actions.offsets` to `[0, 2]`
- **Meaning**: "Draft at position 0 has 2 actions starting at offset 0"

### Result Files

| File | Content | Description |
|------|---------|-------------|
| `draftId.idx` | `["abc-123"]` | Draft ID at position 0 |
| `actions.offsets` | `[[0, 2]]` | Start=0, Length=2 |
| `actions` | `[JSON(action1), JSON(action2)]` | Actual action data |
| `actions_index` | `[[0, len1], [len1, len2]]` | Byte offsets for each action |

---

## Scenario 2: Updating a Pre-existing Draft

### Example
```
Draft ID: "abc-123" (already exists)
Actions: [action1, action2, action3]  // action3 is new
```

### Step-by-Step Flow

#### 1. Insert Draft ID
```java
draftPosition = searchDraftIx.insert("abc-123")
```
- Checks `valueIndexMap`: "abc-123" **FOUND**
- Already exists at position 0
- **Returns: `draftPosition = 0`** (same as before)

#### 2. Get Current Actions Vector Size
```java
currentActionsStart = actionsVector.getSize() // = 2 (already has 2 actions)
```

#### 3. Get Existing Offset Entry
```java
startLength = actionsStartLengthOffsets.getOrAppend(0, [2, 0])
```
- Position 0 **EXISTS** in `actions.offsets` file
- Reads existing value: `[0, 2]`
- **Returns: `[0, 2]`**

#### 4. Extract Start and Length
```java
start = 0   // actions still begin at offset 0
length = 2  // 2 actions already stored
```

#### 5. Calculate New Actions
```java
newActionsCount = 3  // action1, action2, action3
newActions = draft.actions[2:3] = [action3]
```
**Only action3 is NEW!** Actions 1 and 2 already exist.

#### 6. Append ONLY New Actions
```java
// Append action3 only
actionsVector.append(JSON(action3))
  → Serializes action3 to JSON string
  → Writes to actions file at byte position (after action2)
  → Updates actions_index with [prevEnd, lengthOfAction3]
  → Returns offset = 2
```

#### 7. Update Metadata
```java
actionsStartLengthOffsets.put(0, [0, 3])
```
- Updates position 0 in `actions.offsets` to `[0, 3]`
- **Meaning**: "Draft at position 0 now has 3 actions starting at offset 0"

### Result Files

| File | Content | Description |
|------|---------|-------------|
| `draftId.idx` | `["abc-123"]` | Unchanged |
| `actions.offsets` | `[[0, 3]]` | Updated length to 3 |
| `actions` | `[JSON(action1), JSON(action2), JSON(action3)]` | New action appended |
| `actions_index` | `[[0, len1], [len1, len2], [len2, len3]]` | New offset added |

---

## Key Differences: New vs Existing Draft

| Aspect | New Draft | Existing Draft |
|--------|-----------|----------------|
| **`searchDraftIx.insert()`** | Creates new position | Returns existing position |
| **`getOrAppend()`** | Creates new entry `[currentSize, 0]` | Reads existing `[start, length]` |
| **Actions appended** | All actions | Only new actions (from `length` to end) |
| **Efficiency** | Writes all data | Incremental - only writes delta |

---

## Critical Design Points

### 1. Idempotency
If you call `persist()` with the same draft and same actions:
```java
if (newActionsCount == length) {
    log.info("No new actions to persist");
    return; // Early exit - no duplicate writes
}
```

### 2. Incremental Updates
Only new actions are appended, not the entire list:
```java
var newActions = draft.getActions().subList(length, newActionsCount);
```
This makes updates efficient for large drafts.

### 3. Index Integrity
The `draftPosition` links everything together:
- Position in `draftId.idx` → Draft ID
- Same position in `actions.offsets` → `[start, length]` for that draft's actions
- Actions stored in `actions` vector at offsets `[start, start+length)`

### 4. Data Serialization
Each `DraftAction` is serialized to JSON string before storage:
```java
actionsVector.append(JSONUtils.toJSONString(action));
```
The actual data stored is text (JSON), not binary objects.

### 5. Order of Operations (Fixed Bug)
**Critical**: Actions must be appended BEFORE updating metadata:
```java
// 1. Append actions first
for (var action : newActions) {
    actionsVector.append(JSONUtils.toJSONString(action));
}

// 2. Then update metadata
actionsStartLengthOffsets.put(draftPosition, new int[]{start, newActionsCount});
```
This ensures metadata always points to existing data.

---

## Reading Drafts Back

### Retrieval Flow
```java
1. Get all draft IDs from searchDraftIx.getAll()
2. For each draftId at position idx:
   a. Read [start, length] from actions.offsets at position idx
   b. Read actions from actions vector at offsets [start, start+length)
   c. Deserialize each JSON string back to DraftAction
   d. Build Draft object with draftId and actions list
```

### Example
```java
Draft draft = buildDraftById("abc-123", 0);
  → Read actions.offsets[0] = [0, 3]
  → Read actions[0], actions[1], actions[2]
  → Deserialize: JSONUtils.fromJSON(actionData, DraftAction.class)
  → Return Draft with 3 actions
```

---

## Error Handling

### Corrupted Data Detection
```java
if (start < 0 || length < 0 || length > 1000000) {
    log.error("Invalid start/length values. Reinitializing.");
    start = currentActionsStart;
    length = 0;
}
```

### Action Count Mismatch
```java
if (newActionsCount < length) {
    log.warn("Draft has fewer actions than previously stored. Resetting.");
    start = currentActionsStart;
    length = 0;
}
```

This handles cases where the draft state is inconsistent with persisted data.
