package com.tether.app.data.local

import androidx.room.*
import com.tether.app.domain.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun getCurrentUser(): Flow<User?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUserSync(): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}

@Dao
interface PairingDao {
    @Query("SELECT * FROM pairings WHERE status = :status LIMIT 1")
    fun getActivePairing(status: PairingStatus = PairingStatus.ACTIVE): Flow<Pairing?>

    @Query("SELECT * FROM pairings WHERE status = :status LIMIT 1")
    suspend fun getActivePairingSync(status: PairingStatus = PairingStatus.ACTIVE): Pairing?

    @Query("SELECT * FROM pairings WHERE id = :pairingId")
    suspend fun getPairingById(pairingId: String): Pairing?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPairing(pairing: Pairing)

    @Update
    suspend fun updatePairing(pairing: Pairing)

    @Delete
    suspend fun deletePairing(pairing: Pairing)

    @Query("DELETE FROM pairings")
    suspend fun deleteAllPairings()
}

@Dao
interface CanvasDao {
    @Query("SELECT * FROM canvas_states WHERE pairing_id = :pairingId LIMIT 1")
    fun getCanvasState(pairingId: String): Flow<CanvasState?>

    @Query("SELECT * FROM canvas_states WHERE pairing_id = :pairingId LIMIT 1")
    suspend fun getCanvasStateSync(pairingId: String): CanvasState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCanvasState(canvasState: CanvasState)

    @Update
    suspend fun updateCanvasState(canvasState: CanvasState)

    @Query("DELETE FROM canvas_states WHERE pairing_id = :pairingId")
    suspend fun deleteCanvasState(pairingId: String)

    // Strokes
    @Query("SELECT * FROM strokes WHERE user_id = :userId AND canvas_state_id = :canvasId ORDER BY sequence_number ASC")
    fun getStrokesForLayer(canvasId: String, userId: String): Flow<List<Stroke>>

    @Query("SELECT * FROM strokes WHERE user_id = :userId AND canvas_state_id = :canvasId ORDER BY sequence_number ASC")
    suspend fun getStrokesForLayerSync(canvasId: String, userId: String): List<Stroke>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStroke(stroke: Stroke): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStrokes(strokes: List<Stroke>)

    @Query("DELETE FROM strokes WHERE canvas_state_id = :canvasId AND user_id = :userId")
    suspend fun clearLayer(canvasId: String, userId: String)

    // Pending strokes (not yet synced)
    @Query("SELECT * FROM pending_strokes ORDER BY created_at ASC")
    suspend fun getPendingStrokes(): List<PendingStroke>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingStroke(pendingStroke: PendingStroke)

    @Delete
    suspend fun deletePendingStroke(pendingStroke: PendingStroke)

    @Query("DELETE FROM pending_strokes WHERE stroke_id = :strokeId")
    suspend fun deletePendingStrokeByStrokeId(strokeId: String)

    // Stickers
    @Query("SELECT * FROM stickers WHERE user_id = :userId AND canvas_state_id = :canvasId ORDER BY sequence_number ASC")
    suspend fun getStickersForLayer(canvasId: String, userId: String): List<Sticker>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSticker(sticker: Sticker)

    // Text items
    @Query("SELECT * FROM text_items WHERE user_id = :userId AND canvas_state_id = :canvasId ORDER BY sequence_number ASC")
    suspend fun getTextItemsForLayer(canvasId: String, userId: String): List<TextItem>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTextItem(textItem: TextItem)
}

@Dao
interface ArchiveDao {
    @Query("SELECT * FROM canvas_archive WHERE pairing_id = :pairingId ORDER BY archived_at DESC")
    fun getArchiveForPairing(pairingId: String): Flow<List<CanvasArchiveEntry>>

    @Query("SELECT * FROM canvas_archive WHERE pairing_id = :pairingId ORDER BY archived_at DESC")
    suspend fun getArchiveForPairingSync(pairingId: String): List<CanvasArchiveEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArchiveEntry(entry: CanvasArchiveEntry)

    @Delete
    suspend fun deleteArchiveEntry(entry: CanvasArchiveEntry)
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val phoneNumberHash: String,
    val displayName: String?,
    val accentColor: Long,
    val createdAt: Long,
    val isPaired: Boolean,
    val pairedWithUserId: String?
) {
    fun toUser() = User(
        id = id,
        phoneNumberHash = phoneNumberHash,
        displayName = displayName,
        accentColor = accentColor,
        createdAt = createdAt,
        isPaired = isPaired,
        pairedWithUserId = pairedWithUserId
    )

    companion object {
        fun fromUser(user: User) = UserEntity(
            id = user.id,
            phoneNumberHash = user.phoneNumberHash,
            displayName = user.displayName,
            accentColor = user.accentColor,
            createdAt = user.createdAt,
            isPaired = user.isPaired,
            pairedWithUserId = user.pairedWithUserId
        )
    }
}

@Entity(tableName = "pairings")
data class PairingEntity(
    @PrimaryKey val id: String,
    val localUserId: String,
    val partnerUserId: String,
    val partnerPhoneNumberHash: String,
    val partnerDisplayName: String?,
    val pairingCode: String,
    val status: PairingStatus,
    val createdAt: Long,
    val pairedAt: Long?
) {
    fun toPairing() = Pairing(
        id = id,
        localUserId = localUserId,
        partnerUserId = partnerUserId,
        partnerPhoneNumberHash = partnerPhoneNumberHash,
        partnerDisplayName = partnerDisplayName,
        pairingCode = pairingCode,
        status = status,
        createdAt = createdAt,
        pairedAt = pairedAt
    )

    companion object {
        fun fromPairing(pairing: Pairing) = PairingEntity(
            id = pairing.id,
            localUserId = pairing.localUserId,
            partnerUserId = pairing.partnerUserId,
            partnerPhoneNumberHash = pairing.partnerPhoneNumberHash,
            partnerDisplayName = pairing.partnerDisplayName,
            pairingCode = pairing.pairingCode,
            status = pairing.status,
            createdAt = pairing.createdAt,
            pairedAt = pairing.pairedAt
        )
    }
}

@Entity(tableName = "canvas_states")
data class CanvasStateEntity(
    @PrimaryKey val canvasId: String,
    val pairingId: String,
    val backgroundJson: String,
    val layerAJson: String,
    val layerBJson: String,
    val lastResetTime: Long,
    val resetSchedule: ResetSchedule,
    val snapshotSequenceNumber: Long
) {
    fun toCanvasState() = CanvasState(
        canvasId = canvasId,
        pairingId = pairingId,
        background = kotlinx.serialization.json.Json.decodeFromString(backgroundJson),
        layerA = kotlinx.serialization.json.Json.decodeFromString(layerAJson),
        layerB = kotlinx.serialization.json.Json.decodeFromString(layerBJson),
        lastResetTime = lastResetTime,
        resetSchedule = resetSchedule,
        snapshotSequenceNumber = snapshotSequenceNumber
    )

    companion object {
        fun fromCanvasState(state: CanvasState) = CanvasStateEntity(
            canvasId = state.canvasId,
            pairingId = state.pairingId,
            backgroundJson = kotlinx.serialization.json.Json.encodeToString(state.background),
            layerAJson = kotlinx.serialization.json.Json.encodeToString(state.layerA),
            layerBJson = kotlinx.serialization.json.Json.encodeToString(state.layerB),
            lastResetTime = state.lastResetTime,
            resetSchedule = state.resetSchedule,
            snapshotSequenceNumber = state.snapshotSequenceNumber
        )
    }
}

@Entity(tableName = "strokes")
data class StrokeEntity(
    @PrimaryKey val id: String,
    val canvasStateId: String,
    val userId: String,
    val pointsJson: String,
    val color: Long,
    val strokeWidth: Float,
    val sequenceNumber: Long,
    val timestamp: Long
) {
    fun toStroke() = Stroke(
        id = id,
        userId = userId,
        points = kotlinx.serialization.json.Json.decodeFromString(pointsJson),
        color = color,
        strokeWidth = strokeWidth,
        sequenceNumber = sequenceNumber,
        timestamp = timestamp
    )

    companion object {
        fun fromStroke(stroke: Stroke, canvasId: String) = StrokeEntity(
            id = stroke.id,
            canvasStateId = canvasId,
            userId = stroke.userId,
            pointsJson = kotlinx.serialization.json.Json.encodeToString(stroke.points),
            color = stroke.color,
            strokeWidth = stroke.strokeWidth,
            sequenceNumber = stroke.sequenceNumber,
            timestamp = stroke.timestamp
        )
    }
}

@Entity(tableName = "pending_strokes")
data class PendingStroke(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val strokeJson: String,
    val canvasId: String,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toStroke(): Stroke = kotlinx.serialization.json.Json.decodeFromString(strokeJson)
}

@Entity(tableName = "stickers")
data class StickerEntity(
    @PrimaryKey val id: String,
    val canvasStateId: String,
    val userId: String,
    val stickerPackId: String,
    val stickerIndex: Int,
    val x: Float,
    val y: Float,
    val rotation: Float,
    val scale: Float,
    val sequenceNumber: Long,
    val timestamp: Long
) {
    fun toSticker() = Sticker(
        id = id,
        userId = userId,
        stickerPackId = stickerPackId,
        stickerIndex = stickerIndex,
        x = x,
        y = y,
        rotation = rotation,
        scale = scale,
        sequenceNumber = sequenceNumber,
        timestamp = timestamp
    )

    companion object {
        fun fromSticker(sticker: Sticker, canvasId: String) = StickerEntity(
            id = sticker.id,
            canvasStateId = canvasId,
            userId = sticker.userId,
            stickerPackId = sticker.stickerPackId,
            stickerIndex = sticker.stickerIndex,
            x = sticker.x,
            y = sticker.y,
            rotation = sticker.rotation,
            scale = sticker.scale,
            sequenceNumber = sticker.sequenceNumber,
            timestamp = sticker.timestamp
        )
    }
}

@Entity(tableName = "text_items")
data class TextItemEntity(
    @PrimaryKey val id: String,
    val canvasStateId: String,
    val userId: String,
    val content: String,
    val fontId: Int,
    val size: TextSize,
    val color: Long,
    val x: Float,
    val y: Float,
    val rotation: Float,
    val sequenceNumber: Long,
    val timestamp: Long
) {
    fun toTextItem() = TextItem(
        id = id,
        userId = userId,
        content = content,
        fontId = fontId,
        size = size,
        color = color,
        x = x,
        y = y,
        rotation = rotation,
        sequenceNumber = sequenceNumber,
        timestamp = timestamp
    )

    companion object {
        fun fromTextItem(item: TextItem, canvasId: String) = TextItemEntity(
            id = item.id,
            canvasStateId = canvasId,
            userId = item.userId,
            content = item.content,
            fontId = item.fontId,
            size = item.size,
            color = item.color,
            x = item.x,
            y = item.y,
            rotation = item.rotation,
            sequenceNumber = item.sequenceNumber,
            timestamp = item.timestamp
        )
    }
}

@Entity(tableName = "canvas_archive")
data class CanvasArchiveEntryEntity(
    @PrimaryKey val id: String,
    val canvasId: String,
    val pairingId: String,
    val archivedAt: Long,
    val thumbnailData: String?,
    val eventCount: Int
) {
    fun toArchiveEntry() = CanvasArchiveEntry(
        id = id,
        canvasId = canvasId,
        pairingId = pairingId,
        archivedAt = archivedAt,
        thumbnailData = thumbnailData,
        eventCount = eventCount
    )

    companion object {
        fun fromArchiveEntry(entry: CanvasArchiveEntry) = CanvasArchiveEntryEntity(
            id = entry.id,
            canvasId = entry.canvasId,
            pairingId = entry.pairingId,
            archivedAt = entry.archivedAt,
            thumbnailData = entry.thumbnailData,
            eventCount = entry.eventCount
        )
    }
}

@Database(
    entities = [
        UserEntity::class,
        PairingEntity::class,
        CanvasStateEntity::class,
        StrokeEntity::class,
        PendingStroke::class,
        StickerEntity::class,
        TextItemEntity::class,
        CanvasArchiveEntryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TetherDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun pairingDao(): PairingDao
    abstract fun canvasDao(): CanvasDao
    abstract fun archiveDao(): ArchiveDao

    companion object {
        const val DATABASE_NAME = "tether_db"
    }
}

class Converters {
    @TypeConverter
    fun fromPairingStatus(value: PairingStatus): String = value.name

    @TypeConverter
    fun toPairingStatus(value: String): PairingStatus = PairingStatus.valueOf(value)

    @TypeConverter
    fun fromResetSchedule(value: ResetSchedule): String = value.name

    @TypeConverter
    fun toResetSchedule(value: String): ResetSchedule = ResetSchedule.valueOf(value)

    @TypeConverter
    fun fromTextSize(value: TextSize): String = value.name

    @TypeConverter
    fun toTextSize(value: String): TextSize = TextSize.valueOf(value)

    @TypeConverter
    fun fromBackgroundType(value: BackgroundType): String = value.name

    @TypeConverter
    fun toBackgroundType(value: String): BackgroundType = BackgroundType.valueOf(value)

    @TypeConverter
    fun fromGestureType(value: GestureType): String = value.name

    @TypeConverter
    fun toGestureType(value: String): GestureType = GestureType.valueOf(value)
}
