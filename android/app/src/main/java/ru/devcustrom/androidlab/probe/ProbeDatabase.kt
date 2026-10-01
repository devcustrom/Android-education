package ru.devcustrom.androidlab.probe

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "probe")
data class ProbeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
)

@Dao
interface ProbeDao {
    @Query("SELECT * FROM probe")
    fun observeAll(): Flow<List<ProbeEntity>>

    @Insert
    suspend fun insert(entity: ProbeEntity): Long
}

@Database(entities = [ProbeEntity::class], version = 1, exportSchema = false)
abstract class ProbeDatabase : RoomDatabase() {
    abstract fun probeDao(): ProbeDao
}