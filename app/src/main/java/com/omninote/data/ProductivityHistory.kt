package com.omninote.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** Aggregate metadata only. No note text, titles, filenames or PINs are retained. */
@Entity(tableName = "productivity_notes", indices = [Index(value = ["liveNoteId"], unique = true)])
data class ProductivityNote(
    @PrimaryKey val historyId: String,
    val liveNoteId: Int?,
    val state: String,
    val wordsAdded: Long,
    val currentWords: Int,
    val createdAt: Long?,
    val deletedAt: Long?
)

@Entity(tableName = "productivity_activity", primaryKeys = ["day", "historyId", "kind"])
data class ProductivityActivity(
    val day: String,
    val historyId: String,
    val kind: String,
    val wordsAdded: Long,
    val occurredAt: Long
)

@Entity(tableName = "productivity_tracking")
data class ProductivityTracking(@PrimaryKey val id: Int = 1, val startedAt: Long, val baselineNotes: Int)

data class ProductivityDay(
    val day: String,
    val wordsAdded: Long,
    val workedOn: Int,
    val created: Int,
    val archived: Int,
    val trashed: Int,
    val restored: Int,
    val deleted: Int
)

@Dao
interface ProductivityDao {
    @Query("SELECT * FROM notes") suspend fun currentNotes(): List<NoteEntity>
    @Query("SELECT * FROM productivity_tracking WHERE id = 1") suspend fun tracking(): ProductivityTracking?
    @Query("SELECT * FROM productivity_tracking WHERE id = 1") fun observeTracking(): Flow<ProductivityTracking?>
    @Insert suspend fun insertTracking(value: ProductivityTracking)
    @Query("SELECT * FROM productivity_notes WHERE liveNoteId = :id") suspend fun note(id: Int): ProductivityNote?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putNote(value: ProductivityNote)
    @Query("SELECT * FROM productivity_notes") fun observeNotes(): Flow<List<ProductivityNote>>
    @Query("SELECT * FROM productivity_activity WHERE day = :day AND historyId = :id AND kind = :kind")
    suspend fun activity(day: String, id: String, kind: String): ProductivityActivity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putActivity(value: ProductivityActivity)
    @Query("""SELECT day, SUM(wordsAdded) AS wordsAdded,
        COUNT(DISTINCT CASE WHEN kind IN ('CREATED','EDITED') THEN historyId END) AS workedOn,
        SUM(CASE WHEN kind = 'CREATED' THEN 1 ELSE 0 END) AS created,
        SUM(CASE WHEN kind = 'ARCHIVED' THEN 1 ELSE 0 END) AS archived,
        SUM(CASE WHEN kind = 'TRASHED' THEN 1 ELSE 0 END) AS trashed,
        COUNT(DISTINCT CASE WHEN kind IN ('RESTORED','UNARCHIVED') THEN historyId END) AS restored,
        SUM(CASE WHEN kind = 'DELETED' THEN 1 ELSE 0 END) AS deleted
        FROM productivity_activity GROUP BY day ORDER BY day DESC""")
    fun observeDays(): Flow<List<ProductivityDay>>
}
