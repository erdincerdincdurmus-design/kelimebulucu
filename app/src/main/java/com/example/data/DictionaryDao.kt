package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DictionaryDao {
  @Query("SELECT * FROM dictionary_sources ORDER BY addedAt ASC")
  fun observeSources(): Flow<List<DictionarySourceEntity>>

  @Query("SELECT * FROM dictionary_sources ORDER BY addedAt ASC")
  suspend fun getAllSourcesOnce(): List<DictionarySourceEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSource(source: DictionarySourceEntity)

  @Query("DELETE FROM dictionary_sources WHERE id = :id")
  suspend fun deleteSourceById(id: String)

  @Query("DELETE FROM dictionary_sources WHERE fileName = :fileName")
  suspend fun deleteByFileName(fileName: String)

  @Query("DELETE FROM dictionary_sources")
  suspend fun deleteAllSources()
}
