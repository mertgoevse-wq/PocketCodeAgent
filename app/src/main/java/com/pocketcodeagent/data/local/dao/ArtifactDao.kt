package com.pocketcodeagent.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pocketcodeagent.data.local.entity.ArtifactEntity

@Dao
interface ArtifactDao {
    @Query("SELECT * FROM artifacts WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    suspend fun getArtifactsForSession(sessionId: Int): List<ArtifactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtifact(artifact: ArtifactEntity)

    @Query("DELETE FROM artifacts WHERE sessionId = :sessionId")
    suspend fun deleteArtifactsForSession(sessionId: Int)
}
