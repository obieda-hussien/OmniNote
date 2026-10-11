package com.omni.noteplus.data

enum class NoteSection { ACTIVE, ARCHIVED, TRASHED }
enum class NoteLibraryAction { ARCHIVE, UNARCHIVE, TRASH, RESTORE, DELETE }
data class LibraryState(val archived: Boolean, val trashed: Boolean) {
    val section get() = if (trashed) NoteSection.TRASHED else if (archived) NoteSection.ARCHIVED else NoteSection.ACTIVE
}
fun NoteEntity.libraryState() = LibraryState(isArchived, isTrashed)
fun swipeAction(note: NoteEntity, right: Boolean): NoteLibraryAction =
    if (!right) { if (note.isTrashed) NoteLibraryAction.DELETE else NoteLibraryAction.TRASH }
    else when (note.libraryState().section) {
        NoteSection.ACTIVE -> NoteLibraryAction.ARCHIVE
        NoteSection.ARCHIVED -> NoteLibraryAction.UNARCHIVE
        NoteSection.TRASHED -> NoteLibraryAction.RESTORE
    }

/** Undo stores state only; restoring an old snapshot must never overwrite more recent writing. */
data class NoteActionReceipt(val noteId: Int, val title: String, val action: NoteLibraryAction,
    val before: LibraryState, val after: LibraryState?, val undoToken: String?, val authorizedPin: String?)

class NoteActionConflictException : IllegalStateException("This note changed. Refresh and try again.")
class NoteActionLockedException : IllegalStateException("Unlock this note to continue.")
