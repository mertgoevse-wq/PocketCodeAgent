package com.pocketcodeagent.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pocketcodeagent.data.local.dao.ArtifactDao
import com.pocketcodeagent.data.local.dao.ChatMessageDao
import com.pocketcodeagent.data.local.dao.FilePatchDao
import com.pocketcodeagent.data.local.dao.LogDao
import com.pocketcodeagent.data.local.dao.ProviderDao
import com.pocketcodeagent.data.local.dao.SessionDao
import com.pocketcodeagent.data.local.dao.TerminalCommandDao
import com.pocketcodeagent.data.local.entity.ArtifactEntity
import com.pocketcodeagent.data.local.entity.ChatMessageEntity
import com.pocketcodeagent.data.local.entity.FilePatchEntity
import com.pocketcodeagent.data.local.entity.LogEntity
import com.pocketcodeagent.data.local.entity.ProviderEntity
import com.pocketcodeagent.data.local.entity.SessionEntity
import com.pocketcodeagent.data.local.entity.TerminalCommandEntity

@Database(
    entities = [
        ProviderEntity::class,
        LogEntity::class,
        SessionEntity::class,
        ChatMessageEntity::class,
        ArtifactEntity::class,
        FilePatchEntity::class,
        TerminalCommandEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun providerDao(): ProviderDao
    abstract fun logDao(): LogDao
    abstract fun sessionDao(): SessionDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun artifactDao(): ArtifactDao
    abstract fun filePatchDao(): FilePatchDao
    abstract fun terminalCommandDao(): TerminalCommandDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE providers ADD COLUMN providerType TEXT NOT NULL DEFAULT 'CUSTOM_OPENAI'")
                db.execSQL("ALTER TABLE providers ADD COLUMN enabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE providers ADD COLUMN lastTestStatus TEXT")
                db.execSQL("ALTER TABLE providers ADD COLUMN lastErrorSanitized TEXT")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pocket_code_agent_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
