package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "authorized_vault")
data class AuthorizedVaultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ssid: String,
    val credential: String,
    val securityType: String,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Dao
interface AuthorizedVaultDao {
    @Query("SELECT * FROM authorized_vault ORDER BY addedTimestamp DESC")
    fun getAllVaultEntries(): Flow<List<AuthorizedVaultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: AuthorizedVaultEntity): Long

    @Delete
    suspend fun deleteEntry(entry: AuthorizedVaultEntity)

    @Query("DELETE FROM authorized_vault WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM authorized_vault")
    suspend fun clearVault()
}

@Database(entities = [AuthorizedVaultEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vaultDao(): AuthorizedVaultDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wifi_vault.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
