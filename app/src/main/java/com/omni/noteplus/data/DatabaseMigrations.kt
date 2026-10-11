package com.omni.noteplus.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val all = arrayOf(upgrade(1), upgrade(2), upgrade(3))
    private fun upgrade(from: Int) = object : Migration(from, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Rebuild older note schemas with known defaults while retaining every existing row.
            if (from < 3) {
                val columns = mutableSetOf<String>()
                db.query("PRAGMA table_info(notes)").use { cursor ->
                    while (cursor.moveToNext()) columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                db.execSQL("""CREATE TABLE notes_migrated (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL, content TEXT NOT NULL, timestamp INTEGER NOT NULL,
                    isPinned INTEGER NOT NULL, colorHex TEXT, tags TEXT NOT NULL,
                    isArchived INTEGER NOT NULL, isTrashed INTEGER NOT NULL,
                    isLocked INTEGER NOT NULL, lockPin TEXT)""")
                val defaults = linkedMapOf("id" to "NULL", "title" to "''", "content" to "''", "timestamp" to "0",
                    "isPinned" to "0", "colorHex" to "NULL", "tags" to "''", "isArchived" to "0",
                    "isTrashed" to "0", "isLocked" to "0", "lockPin" to "NULL")
                val names = defaults.keys.joinToString(",")
                val values = defaults.map { (name, fallback) -> if (name in columns) "`$name`" else fallback }.joinToString(",")
                db.execSQL("INSERT INTO notes_migrated ($names) SELECT $values FROM notes")
                db.execSQL("DROP TABLE notes")
                db.execSQL("ALTER TABLE notes_migrated RENAME TO notes")
            }
            db.execSQL("""CREATE TABLE IF NOT EXISTS productivity_notes (historyId TEXT NOT NULL PRIMARY KEY,
                liveNoteId INTEGER, state TEXT NOT NULL, wordsAdded INTEGER NOT NULL,
                currentWords INTEGER NOT NULL, createdAt INTEGER, deletedAt INTEGER)""")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_productivity_notes_liveNoteId ON productivity_notes (liveNoteId)")
            db.execSQL("""CREATE TABLE IF NOT EXISTS productivity_activity (day TEXT NOT NULL,
                historyId TEXT NOT NULL, kind TEXT NOT NULL, wordsAdded INTEGER NOT NULL,
                occurredAt INTEGER NOT NULL, PRIMARY KEY(day, historyId, kind))""")
            db.execSQL("""CREATE TABLE IF NOT EXISTS productivity_tracking (id INTEGER NOT NULL PRIMARY KEY,
                startedAt INTEGER NOT NULL, baselineNotes INTEGER NOT NULL)""")
        }
    }
}
