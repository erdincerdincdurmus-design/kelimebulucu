package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.domain.WordFilterEngine
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class ImportOutcome(
  val mergedWords: List<String>,
  val newlyAddedWords: List<String>,
  val importedCount: Int,
  val emptyFileNames: List<String>,
)

class DictionaryRepository(
  private val context: Context,
  private val dao: DictionaryDao,
) {
  private val dictDir: File by lazy {
    File(context.filesDir, "dictionaries").apply {
      if (!exists()) mkdirs()
    }
  }

  val sourcesFlow: Flow<List<DictionarySourceEntity>> = dao.observeSources()

  suspend fun getAllSourcesOnce(): List<DictionarySourceEntity> =
    withContext(Dispatchers.IO) { dao.getAllSourcesOnce() }

  suspend fun loadMergedWords(sources: List<DictionarySourceEntity>): List<String> =
    withContext(Dispatchers.IO) {
      if (sources.isEmpty()) return@withContext emptyList()
      val unique = HashSet<String>(sources.sumOf { it.wordCount }.coerceAtLeast(16))
      for (src in sources) {
        val file = File(dictDir, src.storageFileName)
        if (file.exists()) {
          file.useLines(StandardCharsets.UTF_8) { seq ->
            seq.forEach { line ->
              if (line.isNotEmpty()) unique.add(line)
            }
          }
        }
      }
      WordFilterEngine.sortTurkish(unique)
    }

  suspend fun importUris(
    uris: List<Uri>,
    existingMergedWords: List<String>,
  ): ImportOutcome =
    withContext(Dispatchers.IO) {
      val previousSet = existingMergedWords.toHashSet()
      val existingSources = dao.getAllSourcesOnce()
      val emptyFiles = mutableListOf<String>()
      var importedCount = 0

      for (uri in uris) {
        val fileName = resolveFileName(uri)
        val bytes =
          context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: continue
        val words = WordFilterEngine.decodeWords(bytes)
        if (words.isEmpty()) {
          emptyFiles.add(fileName)
          continue
        }

        // Replace any previously loaded file with the exact same name (matches HTML behavior)
        existingSources
          .filter { it.fileName == fileName }
          .forEach { old ->
            File(dictDir, old.storageFileName).delete()
            dao.deleteSourceById(old.id)
          }

        val id = UUID.randomUUID().toString()
        val storageName = "$id.txt"
        val destFile = File(dictDir, storageName)
        destFile.writeText(words.joinToString("\n"), StandardCharsets.UTF_8)

        dao.insertSource(
          DictionarySourceEntity(
            id = id,
            fileName = fileName,
            wordCount = words.size,
            storageFileName = storageName,
            addedAt = System.currentTimeMillis(),
          )
        )
        importedCount++
      }

      val updatedSources = dao.getAllSourcesOnce()
      val merged = loadMergedWords(updatedSources)
      val newlyAdded =
        if (previousSet.isNotEmpty() && importedCount > 0) {
          merged.filter { it !in previousSet }
        } else {
          emptyList()
        }

      ImportOutcome(
        mergedWords = merged,
        newlyAddedWords = newlyAdded,
        importedCount = importedCount,
        emptyFileNames = emptyFiles,
      )
    }

  suspend fun deleteSource(source: DictionarySourceEntity): List<String> =
    withContext(Dispatchers.IO) {
      File(dictDir, source.storageFileName).delete()
      dao.deleteSourceById(source.id)
      val remaining = dao.getAllSourcesOnce()
      loadMergedWords(remaining)
    }

  suspend fun deleteAllSources() =
    withContext(Dispatchers.IO) {
      dao.deleteAllSources()
      dictDir.listFiles()?.forEach { it.delete() }
    }

  suspend fun writeWordsToUri(uri: Uri, words: List<String>): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        context.contentResolver.openOutputStream(uri)?.use { out ->
          val writer = out.bufferedWriter(StandardCharsets.UTF_8)
          words.forEachIndexed { index, word ->
            if (index > 0) writer.write("\r\n")
            writer.write(word)
          }
          writer.flush()
        } ?: error("Dosya yazılamadı.")
      }
    }

  private fun resolveFileName(uri: Uri): String {
    var displayName: String? = null
    if (uri.scheme == "content") {
      context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
          if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) displayName = cursor.getString(idx)
          }
        }
    }
    return displayName?.takeIf { it.isNotBlank() }
      ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
      ?: "sozluk.txt"
  }
}
