package com.pocketcodeagent.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pocketcodeagent.data.local.entity.TerminalCommandEntity

@Dao
interface TerminalCommandDao {
    @Query("SELECT * FROM terminal_commands WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    suspend fun getCommandsForSession(sessionId: Int): List<TerminalCommandEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: TerminalCommandEntity)

    @Query("UPDATE terminal_commands SET status = :status WHERE id = :commandId")
    suspend fun updateCommandStatus(commandId: String, status: String)

    @Query("DELETE FROM terminal_commands WHERE sessionId = :sessionId")
    suspend fun deleteCommandsForSession(sessionId: Int)
}
