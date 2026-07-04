# Code Editor Phase Report

**Phase:** Code Editor Upgrade Final
**Build:** ✅ `BUILD SUCCESSFUL`
**Commit:** Pending

---

## Modified Files

| File | Change |
|------|--------|
| `CodeEditorPanel.kt` | Complete rewrite: EditorHeader with type badge + status badge + relative path, conflict dialog, disabled preview/patch buttons with inline reason text, ToolbarTextButton composable, detectFileType helper, "Open Files" button in empty state |
| `WorkspaceViewModel.kt` | Added `openFileUri`, `openFileName`, `openFileRelativePath` tracking. Added `checkForConflicts()` (reads disk, compares with `openFileContent`), `reloadFromDisk()`. Updated `loadFileContent` to accept relativePath/fileName params |
| `PanelPlaceholder.kt` | Added optional `actionLabel` + `onAction` params for action button support |
| `MainShellScreen.kt` | Added `deriveRelativePath()` helper. Passes `filePath`, `onOpenFiles` to CodeEditorPanel |

---

## Editor UX

### Header
- **File name** — bold, primary color
- **Relative path** — monospace, gray, shown below filename when different from name
- **Type badge** — e.g. `KOTLIN`, `XML`, `HTML`, `JAVASCRIPT` in pill-style badge
- **Status badge** — `Saved` (gray), `Unsaved` (green), `Read-only (large)` (amber), `Read-only` (red)
- **File size** — KB display in monospace
- **Modified dot** — green circle indicator when unsaved changes exist

### Toolbar Buttons
- **Save** — enabled only when modified + not saving
- **Revert** — enabled only when modified (restores editorContent = originalFileContent)
- **Copy** — always enabled, copies full editor content to clipboard
- **Find** — toggles search bar, active state highlighted
- **Line numbers** — toggle, active state highlighted
- **Preview** — enabled only for .html/.htm files; disabled with reason text for non-HTML
- **Diff** — enabled when modified and < 500KB; disabled with "Patch blockiert (>500 KB)" for huge files

### Editor Body
- Monospace `BasicTextField` with SolidColor cursor
- Horizontal + vertical scrolling
- LineNumberGutter (toggleable)
- Placeholder text when editor is empty
- Status bar: `lines · chars` + `Modified` indicator

### Empty State
- Message: "Keine Datei offen"
- Subtitle: "Wähle im Files-Tab eine Datei aus."
- "Open Files" button navigates to Files tab

---

## Save/Revert/Copy

### Save Flow
1. User clicks Save
2. `checkForConflicts()` reads current disk content
3. If disk content differs from `openFileContent` → conflict dialog
4. If no conflict → `saveFileContent()` writes via Dispatchers.IO
5. After save: `originalFileContent = content`, `openFileContent = content`, `lastFileWriteTimestamp` updated

### Revert Flow
1. User clicks Revert
2. `editorContent = originalFileContent` (discards all edits)

### Copy Flow
1. User clicks Copy
2. Full `editorContent` copied to clipboard via `LocalClipboardManager`

---

## Large File Rules

| File Size | Behavior |
|-----------|----------|
| < 200 KB | Full editing, all buttons active |
| 200-500 KB | Opens in read-only mode with "Read-only: X KB" banner and "Edit trotzdem" button. Preview button works for HTML. Diff button enabled. |
| > 500 KB | "Datei zu groß" warning, no text field. Copy still available. Patch creation blocked with reason text. |

---

## Preview Integration

- HTML files (.html/.htm): "Preview" button active, calls `onPreviewFile(uri, name)` → sets `PreviewTarget.File` → switches to Preview tab
- Non-HTML files: "Preview" button shown but disabled, small gray text below: "Nur .html/.htm Dateien"
- Uses existing `StaticPreviewBundler` logic in PreviewPanel

---

## Diff Integration

- When editor has unsaved changes and file < 500KB: "Diff" button active
- Clicking Diff:
  1. Checks for disk conflicts
  2. If conflict → shows conflict dialog
  3. If no conflict → creates `FilePatch` with:
     - `path` = relative path from workspace
     - `action` = MODIFY
     - `oldText` = originalFileContent
     - `newText` = editorContent
     - `source` = USER
     - `replaceWholeFile` = true
  4. Patch added to `MainViewModel.pendingFileChanges` via `addPendingPatch()`
  5. Opens Diff tab
- No automatic writing — user must review in Diff tab and Apply

---

## Conflict Protection

### Detection
`WorkspaceViewModel.checkForConflicts()` reads the file from disk (via SAF) and compares with the stored `openFileContent`. If they differ, `openFileContent` is updated to the current disk content and `true` is returned.

### Dialog
AlertDialog with three options:
1. **"Von Disk neu laden"** — reloads file content, discards editor changes
2. **"Trotzdem speichern"** — saves editor content over the disk version
3. **"Abbrechen"** — dismisses dialog, keeps editor state unchanged

### Trigger Points
- Save button click
- Create Patch button click

---

## Build Result

```
BUILD SUCCESSFUL in 9s
```

No new compilation errors. Existing Compose/Room deprecation warnings unchanged.

---

## Limitations

- **Indentation preservation not implemented**: BasicTextField doesn't auto-indent on Enter. Would require custom InputConnection or key event handling
- **`detectFileType` duplicates `languageFromPath` in `WorkspaceContext.kt`**: Two functions map file extensions to language names — should be extracted to a shared utility
- **`deriveRelativePath` fragile for SAF URIs**: String replacement won't work for `content://` document URIs vs tree URIs. Falls back to `null` (shows filename only)
- **`checkForConflicts()` blocks calling thread**: Called from UI click handler, reads disk synchronously. For slow SAF reads this could cause jank. Should be made suspend
- **`hasConflict` property unused**: After refactoring to direct `checkForConflicts()` calls, the `val hasConflict` getter is dead code
