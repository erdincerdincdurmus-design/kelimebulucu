package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
  uiState: MainUiState,
  onStartsWithChange: (String) -> Unit,
  onEndsWithChange: (String) -> Unit,
  onContainsChange: (String) -> Unit,
  onUnorderedLettersChange: (String) -> Unit,
  onPatternChange: (String) -> Unit,
  onToggleLength: (Int) -> Unit,
  onShowHelp: (FilterHelpTopic) -> Unit,
  onSearch: () -> Unit,
  onClear: () -> Unit,
  onCopyAll: () -> Unit,
  onCopyWord: (String) -> Unit,
  onExportResults: () -> Unit,
  onOpenFilePicker: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val keyboardController = LocalSoftwareKeyboardController.current
  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()

  val triggerSearch = {
    keyboardController?.hide()
    onSearch()
    if (uiState.totalWordCount > 0) {
      coroutineScope.launch {
        // Gently scroll towards results header on compact screens if needed
        if (listState.firstVisibleItemIndex == 0) {
          listState.animateScrollToItem(1)
        }
      }
    }
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isExpanded = maxWidth >= 840.dp
    val gridColumns =
      when {
        maxWidth >= 600.dp -> 3
        else -> 2
      }
    val displayWords =
      remember(uiState.results.words) {
        uiState.results.words.take(MainViewModel.MAX_DISPLAY_WORDS)
      }

    if (isExpanded) {
      // Two-pane layout for tablets & foldables
      Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        Column(
          modifier =
            Modifier.width(410.dp)
              .fillMaxHeight()
              .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          if (uiState.totalWordCount == 0 && !uiState.isInitialLoading) {
            EmptyDictionaryPromptCard(onOpenFilePicker = onOpenFilePicker)
          }
          FilterFormCard(
            uiState = uiState,
            compactPairs = true,
            onStartsWithChange = onStartsWithChange,
            onEndsWithChange = onEndsWithChange,
            onContainsChange = onContainsChange,
            onUnorderedLettersChange = onUnorderedLettersChange,
            onPatternChange = onPatternChange,
            onToggleLength = onToggleLength,
            onShowHelp = onShowHelp,
            onSearch = {
              keyboardController?.hide()
              onSearch()
            },
            onClear = onClear,
          )
        }

        Column(
          modifier = Modifier.weight(1f).fillMaxHeight(),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          ResultsHeaderCard(
            results = uiState.results,
            copiedAllFeedback = uiState.copiedAllFeedback,
            onCopyAll = onCopyAll,
            onExportResults = onExportResults,
          )

          if (uiState.results.hasSearched && uiState.results.words.isEmpty() && uiState.totalWordCount > 0) {
            NoMatchesCard(isNewWords = uiState.results.isNewWordsResult)
          } else if (displayWords.isNotEmpty()) {
            LazyVerticalGrid(
              columns = GridCells.Adaptive(minSize = 150.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              contentPadding = PaddingValues(bottom = 24.dp),
              modifier = Modifier.fillMaxSize(),
            ) {
              items(displayWords, key = { it }) { word ->
                WordResultTile(word = word, onClick = { onCopyWord(word) })
              }
            }
          }
        }
      }
    } else {
      // Single scrollable LazyColumn for phones (no nested scroll conflicts)
      val chunkedRows =
        remember(displayWords, gridColumns) { displayWords.chunked(gridColumns) }

      LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
      ) {
        if (uiState.totalWordCount == 0 && !uiState.isInitialLoading) {
          item(key = "empty_dict_prompt") {
            EmptyDictionaryPromptCard(onOpenFilePicker = onOpenFilePicker)
          }
        }

        item(key = "filter_card") {
          FilterFormCard(
            uiState = uiState,
            compactPairs = maxWidth >= 340.dp,
            onStartsWithChange = onStartsWithChange,
            onEndsWithChange = onEndsWithChange,
            onContainsChange = onContainsChange,
            onUnorderedLettersChange = onUnorderedLettersChange,
            onPatternChange = onPatternChange,
            onToggleLength = onToggleLength,
            onShowHelp = onShowHelp,
            onSearch = triggerSearch,
            onClear = onClear,
          )
        }

        item(key = "results_header") {
          ResultsHeaderCard(
            results = uiState.results,
            copiedAllFeedback = uiState.copiedAllFeedback,
            onCopyAll = onCopyAll,
            onExportResults = onExportResults,
          )
        }

        if (uiState.results.hasSearched && uiState.results.words.isEmpty() && uiState.totalWordCount > 0) {
          item(key = "no_matches") {
            NoMatchesCard(isNewWords = uiState.results.isNewWordsResult)
          }
        } else {
          items(chunkedRows) { rowWords ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              for (word in rowWords) {
                WordResultTile(
                  word = word,
                  onClick = { onCopyWord(word) },
                  modifier = Modifier.weight(1f),
                )
              }
              repeat(gridColumns - rowWords.size) {
                Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
        }

        item(key = "bottom_spacer") {
          Spacer(modifier = Modifier.height(12.dp))
        }
      }
    }
  }
}

@Composable
private fun EmptyDictionaryPromptCard(
  onOpenFilePicker: () -> Unit,
) {
  Surface(
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.size(40.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.MenuBook,
              contentDescription = null,
              modifier = Modifier.size(22.dp),
            )
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Sözlük dosyası yükle",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          Text(
            text = "Her satırda bir kelime içeren .txt dosyanı seçerek hemen aramaya başla.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
          )
        }
      }
      Button(
        onClick = onOpenFilePicker,
        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("home_upload_button"),
        shape = MaterialTheme.shapes.small,
      ) {
        Icon(
          imageVector = Icons.Rounded.FileUpload,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("TXT Sözlük Dosyası Seç")
      }
    }
  }
}

@Composable
private fun FilterFormCard(
  uiState: MainUiState,
  compactPairs: Boolean,
  onStartsWithChange: (String) -> Unit,
  onEndsWithChange: (String) -> Unit,
  onContainsChange: (String) -> Unit,
  onUnorderedLettersChange: (String) -> Unit,
  onPatternChange: (String) -> Unit,
  onToggleLength: (Int) -> Unit,
  onShowHelp: (FilterHelpTopic) -> Unit,
  onSearch: () -> Unit,
  onClear: () -> Unit,
) {
  Surface(
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    tonalElevation = 1.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (compactPairs) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          FilterInputField(
            label = "Şununla başlar",
            value = uiState.criteria.startsWith,
            clearGeneration = uiState.clearGeneration,
            onValueChange = onStartsWithChange,
            placeholder = "ör. ay",
            onHelpClick = { onShowHelp(FilterHelpTopic.STARTS_WITH) },
            onSearchTriggered = onSearch,
            testTag = "input_starts_with",
            modifier = Modifier.weight(1f),
          )
          FilterInputField(
            label = "Şununla biter",
            value = uiState.criteria.endsWith,
            clearGeneration = uiState.clearGeneration,
            onValueChange = onEndsWithChange,
            placeholder = "ör. lı",
            onHelpClick = { onShowHelp(FilterHelpTopic.ENDS_WITH) },
            onSearchTriggered = onSearch,
            testTag = "input_ends_with",
            modifier = Modifier.weight(1f),
          )
        }
      } else {
        FilterInputField(
          label = "Şununla başlar",
          value = uiState.criteria.startsWith,
          clearGeneration = uiState.clearGeneration,
          onValueChange = onStartsWithChange,
          placeholder = "ör. ay",
          onHelpClick = { onShowHelp(FilterHelpTopic.STARTS_WITH) },
          onSearchTriggered = onSearch,
          testTag = "input_starts_with",
        )
        FilterInputField(
          label = "Şununla biter",
          value = uiState.criteria.endsWith,
          clearGeneration = uiState.clearGeneration,
          onValueChange = onEndsWithChange,
          placeholder = "ör. lı",
          onHelpClick = { onShowHelp(FilterHelpTopic.ENDS_WITH) },
          onSearchTriggered = onSearch,
          testTag = "input_ends_with",
        )
      }

      FilterInputField(
        label = "İçinde geçer (peş peşe)",
        value = uiState.criteria.contains,
        clearGeneration = uiState.clearGeneration,
        onValueChange = onContainsChange,
        placeholder = "ör. ar",
        onHelpClick = { onShowHelp(FilterHelpTopic.CONTAINS) },
        onSearchTriggered = onSearch,
        testTag = "input_contains",
      )

      FilterInputField(
        label = "Şu harfleri içersin (sırasız)",
        value = uiState.criteria.unorderedLetters,
        clearGeneration = uiState.clearGeneration,
        onValueChange = onUnorderedLettersChange,
        placeholder = "ör. l k m",
        onHelpClick = { onShowHelp(FilterHelpTopic.UNORDERED_LETTERS) },
        onSearchTriggered = onSearch,
        testTag = "input_unordered",
      )

      FilterInputField(
        label = "Aralıklı kalıp",
        value = uiState.criteria.pattern,
        clearGeneration = uiState.clearGeneration,
        onValueChange = onPatternChange,
        placeholder = "ör. k*l*m",
        useMonospace = true,
        showWildcardButton = true,
        onHelpClick = { onShowHelp(FilterHelpTopic.PATTERN) },
        onSearchTriggered = onSearch,
        testTag = "input_pattern",
      )

      LengthChipsSection(
        selectedLengths = uiState.criteria.selectedLengths,
        onToggleLength = onToggleLength,
      )

      Spacer(modifier = Modifier.height(2.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Button(
          onClick = onSearch,
          shape = MaterialTheme.shapes.small,
          contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
          modifier = Modifier.weight(1.4f).minimumInteractiveComponentSize().testTag("search_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Ara",
            style = MaterialTheme.typography.titleMedium,
          )
        }

        OutlinedButton(
          onClick = onClear,
          shape = MaterialTheme.shapes.small,
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
          modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("clear_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.DeleteSweep,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Temizle",
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }
}

@Composable
private fun ResultsHeaderCard(
  results: ResultsState,
  copiedAllFeedback: Boolean,
  onCopyAll: () -> Unit,
  onExportResults: () -> Unit,
) {
  Surface(
    shape = MaterialTheme.shapes.medium,
    color =
      if (results.isNewWordsResult) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
      else MaterialTheme.colorScheme.surface,
    border =
      BorderStroke(
        1.dp,
        if (results.isNewWordsResult) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.outlineVariant,
      ),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (results.isNewWordsResult) {
          Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(18.dp),
          )
        }
        Text(
          text = results.headingText,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }

      AnimatedVisibility(visible = results.words.isNotEmpty()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilledTonalButton(
            onClick = onCopyAll,
            shape = MaterialTheme.shapes.small,
            colors =
              if (copiedAllFeedback) {
                ButtonDefaults.filledTonalButtonColors(
                  containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                  contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
              } else {
                ButtonDefaults.filledTonalButtonColors()
              },
            modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("copy_results_button"),
          ) {
            Icon(
              imageVector = if (copiedAllFeedback) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (copiedAllFeedback) "Kopyalandı ✓" else "Listeyi kopyala",
              maxLines = 1,
            )
          }

          OutlinedButton(
            onClick = onExportResults,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("export_results_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Download,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "TXT kaydet",
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun NoMatchesCard(isNewWords: Boolean) {
  Surface(
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.padding(24.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.SearchOff,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(36.dp),
      )
      Text(
        text =
          if (isNewWords) "Eklenen dosyadaki tüm kelimeler zaten mevcut sözlükte vardı."
          else "Bu kriterlere uyan kelime bulunamadı.",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (!isNewWords) {
        Text(
          text = "Harf sayısını veya kalıptaki karakterleri kontrol ederek tekrar dene.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
