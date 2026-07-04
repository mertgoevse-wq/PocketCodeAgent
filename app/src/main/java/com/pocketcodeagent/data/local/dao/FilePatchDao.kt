package com.pocketcodeagent.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pocketcodeagent.data.local.entity.FilePatchEntity

@Dao
interface FilePatchDao {
    @Query("SELECT * FROM file_patches WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    suspend fun getPatchesForSession(sessionId: Int): List<FilePatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatch(patch: FilePatchEntity)

    @Query("UPDATE file_patches SET status = :status, errorMessage = :errorMessage WHERE id = :patchId")
    suspend fun updatePatchStatus(patchId: String, status: String, errorMessage: String? = null)

    @Query("DELETE FROM file_patches WHERE sessionId = :sessionId")
    suspend fun deletePatchesForSession(sessionId: Int)
}
