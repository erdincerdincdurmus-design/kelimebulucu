package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dictionary_sources")
data class DictionarySourceEntity(
  @PrimaryKey val id: String,
  val fileName: String,
  val wordCount: Int,
  val storageFileName: String,
  val addedAt: Long = System.currentTimeMillis(),
)
