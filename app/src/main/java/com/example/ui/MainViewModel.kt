package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DictionaryRepository
import com.example.data.DictionarySourceEntity
import com.example.domain.FilterCriteria
import com.example.domain.WordFilterEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppTab {
  SEARCH,
  DICTIONARIES,
}

enum class FilterHelpTopic(
  val title: String,
  val description: String,
  val examples: List<String>,
) {
  STARTS_WITH(
    title = "Şununla başlar",
    description = "Kelimenin başlangıç harflerini filtreler.",
    examples = listOf("ay → ayak, ayva, aylık"),
  ),
  ENDS_WITH(
    title = "Şununla biter",
    description =
      "Kelimenin bitiş harflerini filtreler. Başlangıçla birlikte yazarsan arada en az bir harf bulunan kelimeler gelir.",
    examples = listOf("lı → akıllı, bağlı, yağlı", "ak + lı → akıllı"),
  ),
  CONTAINS(
    title = "İçinde geçer (peş peşe)",
    description =
      "Yazdığın harfler yan yana ve yazdığın sırayla kelimenin herhangi bir yerinde geçer.",
    examples = listOf("ar → kara, mart, arı"),
  ),
  UNORDERED_LETTERS(
    title = "Şu harfleri içersin (sırasız)",
    description =
      "Yazdığın harflerin hepsi kelimede bulunur; sırası ve yeri önemli değildir, başka harfler de olabilir. Aynı harfi iki kere yazarsan o harften en az iki tane arar.",
    examples = listOf("l k m → kalem, kemal, malik", "a a m → araba, masa değil; arama, mamak"),
  ),
  PATTERN(
    title = "Aralıklı kalıp",
    description =
      "Çengel bulmaca için: bilinen harfleri yaz, bilinmeyen her harf yerine * koy. Her * tam bir harftir; kelimenin uzunluğu işaret ve harf sayısı kadar olur. ?, _ ve . işaretleri de * gibi çalışır.",
    examples = listOf("k*l*m → kalem, kelam, kalım (5 harfli)", "a*** → a ile başlayan 4 harfli kelimeler"),
  ),
}

data class ResultsState(
  val words: List<String> = emptyList(),
  val headingText: String = "Sonuç yok",
  val hasSearched: Boolean = false,
  val isNewWordsResult: Boolean = false,
)

data class MainUiState(
  val activeTab: AppTab = AppTab.SEARCH,
  val isInitialLoading: Boolean = true,
  val isBusy: Boolean = false,
  val sources: List<DictionarySourceEntity> = emptyList(),
  val totalWordCount: Int = 0,
  val statusText: String = "Henüz dosya yüklenmedi.",
  val criteria: FilterCriteria = FilterCriteria(),
  val clearGeneration: Int = 0,
  val results: ResultsState = ResultsState(),
  val activeHelpTopic: FilterHelpTopic? = null,
  val copiedAllFeedback: Boolean = false,
  val showDeleteAllDialog: Boolean = false,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
  private val repository: DictionaryRepository by lazy {
    val db = AppDatabase.getInstance(application)
    DictionaryRepository(application, db.dictionaryDao())
  }

  private val _uiState = MutableStateFlow(MainUiState())
  val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

  private val _snackbarEvents = MutableSharedFlow<String>()
  val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

  // Cached sorted unique words kept in memory for instant crossword filtering
  @Volatile private var mergedWords: List<String> = emptyList()
  private var copyResetJob: Job? = null

  init {
    observeAndRestoreDictionaries()
  }

  private fun observeAndRestoreDictionaries() {
    viewModelScope.launch {
      val initialSources = repository.getAllSourcesOnce()
      if (initialSources.isNotEmpty()) {
        val loaded = repository.loadMergedWords(initialSources)
        mergedWords = loaded
        val formatted = WordFilterEngine.formatNumber(loaded.size)
        _uiState.update {
          it.copy(
            isInitialLoading = false,
            sources = initialSources,
            totalWordCount = loaded.size,
            statusText = "Toplam $formatted kelime hazır (kayıtlı sözlükler otomatik yüklendi).",
          )
        }
      } else {
        _uiState.update {
          it.copy(
            isInitialLoading = false,
            sources = emptyList(),
            totalWordCount = 0,
            statusText = "Henüz dosya yüklenmedi.",
          )
        }
      }

      repository.sourcesFlow.collect { latestSources ->
        _uiState.update { it.copy(sources = latestSources) }
      }
    }
  }

  fun selectTab(tab: AppTab) {
    _uiState.update { it.copy(activeTab = tab) }
  }

  fun updateStartsWith(value: String) {
    _uiState.update { it.copy(criteria = it.criteria.copy(startsWith = value)) }
  }

  fun updateEndsWith(value: String) {
    _uiState.update { it.copy(criteria = it.criteria.copy(endsWith = value)) }
  }

  fun updateContains(value: String) {
    _uiState.update { it.copy(criteria = it.criteria.copy(contains = value)) }
  }

  fun updateUnorderedLetters(value: String) {
    _uiState.update { it.copy(criteria = it.criteria.copy(unorderedLetters = value)) }
  }

  fun updatePattern(value: String) {
    _uiState.update { it.copy(criteria = it.criteria.copy(pattern = value)) }
  }

  fun toggleLength(length: Int) {
    _uiState.update { state ->
      val current = state.criteria.selectedLengths
      val updated = if (length in current) current - length else current + length
      state.copy(criteria = state.criteria.copy(selectedLengths = updated))
    }
  }

  fun showHelp(topic: FilterHelpTopic?) {
    _uiState.update { it.copy(activeHelpTopic = topic) }
  }

  fun search() {
    val currentWords = mergedWords
    if (currentWords.isEmpty()) {
      _uiState.update {
        it.copy(
          results = ResultsState(
            words = emptyList(),
            headingText = "Önce sözlük dosyasını yükle.",
            hasSearched = true,
            isNewWordsResult = false,
          )
        )
      }
      return
    }

    val criteria = _uiState.value.criteria
    viewModelScope.launch {
      _uiState.update { it.copy(isBusy = true) }
      val matches =
        withContext(Dispatchers.Default) {
          WordFilterEngine.filterWords(currentWords, criteria)
        }
      val heading = formatResultsHeading(matches.size, "kelime bulundu")
      _uiState.update {
        it.copy(
          isBusy = false,
          results = ResultsState(
            words = matches,
            headingText = heading,
            hasSearched = true,
            isNewWordsResult = false,
          ),
        )
      }
    }
  }

  fun clearFilters() {
    _uiState.update {
      it.copy(
        criteria = FilterCriteria(),
        clearGeneration = it.clearGeneration + 1,
        results = ResultsState(),
      )
    }
  }

  fun importFiles(uris: List<Uri>) {
    if (uris.isEmpty()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isBusy = true) }
      val previousWords = mergedWords
      val outcome = repository.importUris(uris, previousWords)
      mergedWords = outcome.mergedWords

      val totalFormatted = WordFilterEngine.formatNumber(outcome.mergedWords.size)
      val status =
        if (outcome.mergedWords.isNotEmpty()) {
          "Toplam $totalFormatted kelime hazır."
        } else {
          "Henüz dosya yüklenmedi."
        }

      val newResults =
        if (previousWords.isNotEmpty() && outcome.importedCount > 0) {
          ResultsState(
            words = outcome.newlyAddedWords,
            headingText = formatResultsHeading(outcome.newlyAddedWords.size, "yeni kelime eklendi"),
            hasSearched = true,
            isNewWordsResult = true,
          )
        } else {
          _uiState.value.results
        }

      _uiState.update {
        it.copy(
          isBusy = false,
          totalWordCount = outcome.mergedWords.size,
          statusText = status,
          results = newResults,
          activeTab =
            if (previousWords.isNotEmpty() && outcome.importedCount > 0) AppTab.SEARCH
            else it.activeTab,
        )
      }

      if (outcome.emptyFileNames.isNotEmpty()) {
        _snackbarEvents.emit(
          "Boş veya okunamayan dosya atlandı: ${outcome.emptyFileNames.joinToString(", ")}"
        )
      } else if (outcome.importedCount > 0) {
        if (previousWords.isNotEmpty()) {
          val addedStr = WordFilterEngine.formatNumber(outcome.newlyAddedWords.size)
          _snackbarEvents.emit("Sözlük birleştirildi: $addedStr yeni kelime eklendi.")
        } else {
          _snackbarEvents.emit("Sözlük yüklendi: $totalFormatted kelime hazır.")
        }
      }
    }
  }

  fun deleteSource(source: DictionarySourceEntity) {
    viewModelScope.launch {
      _uiState.update { it.copy(isBusy = true) }
      val remaining = repository.deleteSource(source)
      mergedWords = remaining
      val formatted = WordFilterEngine.formatNumber(remaining.size)
      val status =
        if (remaining.isNotEmpty()) {
          "${source.fileName} silindi. Kalan $formatted kelime."
        } else {
          "${source.fileName} silindi. Sözlük boş."
        }
      _uiState.update {
        it.copy(
          isBusy = false,
          totalWordCount = remaining.size,
          statusText = status,
          results = ResultsState(),
        )
      }
    }
  }

  fun requestDeleteAllDialog(show: Boolean) {
    _uiState.update { it.copy(showDeleteAllDialog = show) }
  }

  fun confirmDeleteAllSources() {
    viewModelScope.launch {
      _uiState.update { it.copy(isBusy = true, showDeleteAllDialog = false) }
      repository.deleteAllSources()
      mergedWords = emptyList()
      _uiState.update {
        it.copy(
          isBusy = false,
          totalWordCount = 0,
          statusText = "Tüm sözlük silindi. Yeni dosya yükleyebilirsin.",
          results = ResultsState(),
        )
      }
      _snackbarEvents.emit("Tüm sözlükler silindi.")
    }
  }

  fun exportResultsToUri(uri: Uri) {
    val wordsToExport = _uiState.value.results.words
    if (wordsToExport.isEmpty()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isBusy = true) }
      val result = repository.writeWordsToUri(uri, wordsToExport)
      _uiState.update { it.copy(isBusy = false) }
      if (result.isSuccess) {
        val countStr = WordFilterEngine.formatNumber(wordsToExport.size)
        _snackbarEvents.emit("$countStr sonuç TXT olarak kaydedildi.")
      } else {
        _snackbarEvents.emit("Dosya kaydedilirken hata oluştu.")
      }
    }
  }

  fun exportAllWordsToUri(uri: Uri) {
    val allWords = mergedWords
    if (allWords.isEmpty()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isBusy = true) }
      val result = repository.writeWordsToUri(uri, allWords)
      _uiState.update { it.copy(isBusy = false) }
      if (result.isSuccess) {
        val countStr = WordFilterEngine.formatNumber(allWords.size)
        _snackbarEvents.emit("Tüm sözlük ($countStr kelime) TXT olarak kaydedildi.")
      } else {
        _snackbarEvents.emit("Sözlük kaydedilirken hata oluştu.")
      }
    }
  }

  fun notifyCopiedAll() {
    copyResetJob?.cancel()
    _uiState.update { it.copy(copiedAllFeedback = true) }
    copyResetJob =
      viewModelScope.launch {
        delay(1500)
        _uiState.update { it.copy(copiedAllFeedback = false) }
      }
  }

  fun notifySingleWordCopied(word: String) {
    viewModelScope.launch {
      _snackbarEvents.emit("\"$word\" panoya kopyalandı")
    }
  }

  private fun formatResultsHeading(count: Int, suffix: String): String {
    val formattedCount = WordFilterEngine.formatNumber(count)
    return if (count > MAX_DISPLAY_WORDS) {
      "$formattedCount $suffix (ekranda ilk 3.000, kopyala ve kaydet hepsini alır)"
    } else {
      "$formattedCount $suffix"
    }
  }

  companion object {
    const val MAX_DISPLAY_WORDS = 3000
  }
}
