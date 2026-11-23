package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.WordFilterEngine
import com.example.ui.AppTab
import com.example.ui.ConfirmDeleteAllDialog
import com.example.ui.DictionariesScreen
import com.example.ui.FilterHelpDialog
import com.example.ui.MainViewModel
import com.example.ui.SearchScreen
import com.example.ui.theme.SozlukFiltreTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      SozlukFiltreTheme {
        SozlukFiltreApp()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SozlukFiltreApp(
  viewModel: MainViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(Unit) {
    viewModel.snackbarEvents.collect { message ->
      snackbarHostState.currentSnackbarData?.dismiss()
      snackbarHostState.showSnackbar(message)
    }
  }

  val openTxtLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
      if (uris.isNotEmpty()) {
        viewModel.importFiles(uris)
      }
    }

  val exportResultsLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
      if (uri != null) {
        viewModel.exportResultsToUri(uri)
      }
    }

  val exportAllLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
      if (uri != null) {
        viewModel.exportAllWordsToUri(uri)
      }
    }

  val launchFilePicker = {
    openTxtLauncher.launch(arrayOf("text/plain", "text/*"))
  }

  val copyTextToClipboard: (String) -> Unit = { text ->
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Sözlük Filtre", text))
  }

  uiState.activeHelpTopic?.let { topic ->
    FilterHelpDialog(
      topic = topic,
      onDismiss = { viewModel.showHelp(null) },
    )
  }

  if (uiState.showDeleteAllDialog) {
    ConfirmDeleteAllDialog(
      wordCountText = WordFilterEngine.formatNumber(uiState.totalWordCount),
      onConfirm = { viewModel.confirmDeleteAllSources() },
      onDismiss = { viewModel.requestDeleteAllDialog(false) },
    )
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 840.dp

    Scaffold(
      contentWindowInsets = WindowInsets.safeDrawing,
      containerColor = MaterialTheme.colorScheme.background,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = {
        Column {
          CenterAlignedTopAppBar(
            colors =
              TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
              ),
            title = {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Text(
                  text = "Sözlük Filtre",
                  style = MaterialTheme.typography.headlineMedium,
                )
                if (uiState.totalWordCount > 0) {
                  Surface(
                    onClick = { viewModel.selectTab(AppTab.DICTIONARIES) },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                  ) {
                    Text(
                      text = "${WordFilterEngine.formatNumber(uiState.totalWordCount)} kelime",
                      style = MaterialTheme.typography.labelMedium,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                  }
                }
              }
            },
          )
          AnimatedVisibility(visible = uiState.isInitialLoading || uiState.isBusy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
          }
        }
      },
      bottomBar = {
        if (!useNavigationRail) {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            NavigationBarItem(
              selected = uiState.activeTab == AppTab.SEARCH,
              onClick = { viewModel.selectTab(AppTab.SEARCH) },
              icon = {
                Icon(
                  imageVector =
                    if (uiState.activeTab == AppTab.SEARCH) Icons.Rounded.Search
                    else Icons.Outlined.Search,
                  contentDescription = "Kelime Bul",
                )
              },
              label = { Text("Kelime Bul") },
              modifier = Modifier.testTag("nav_search"),
            )
            NavigationBarItem(
              selected = uiState.activeTab == AppTab.DICTIONARIES,
              onClick = { viewModel.selectTab(AppTab.DICTIONARIES) },
              icon = {
                Icon(
                  imageVector =
                    if (uiState.activeTab == AppTab.DICTIONARIES) Icons.Rounded.LibraryBooks
                    else Icons.Outlined.LibraryBooks,
                  contentDescription = "Sözlükler",
                )
              },
              label = {
                val countSuffix =
                  if (uiState.sources.isNotEmpty()) " (${uiState.sources.size})" else ""
                Text("Sözlükler$countSuffix")
              },
              modifier = Modifier.testTag("nav_dictionaries"),
            )
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
      ) {
        if (useNavigationRail) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxHeight(),
          ) {
            NavigationRailItem(
              selected = uiState.activeTab == AppTab.SEARCH,
              onClick = { viewModel.selectTab(AppTab.SEARCH) },
              icon = {
                Icon(
                  imageVector =
                    if (uiState.activeTab == AppTab.SEARCH) Icons.Rounded.Search
                    else Icons.Outlined.Search,
                  contentDescription = "Kelime Bul",
                  modifier = Modifier.size(24.dp),
                )
              },
              label = { Text("Kelime Bul") },
              modifier = Modifier.testTag("nav_search"),
            )
            NavigationRailItem(
              selected = uiState.activeTab == AppTab.DICTIONARIES,
              onClick = { viewModel.selectTab(AppTab.DICTIONARIES) },
              icon = {
                Icon(
                  imageVector =
                    if (uiState.activeTab == AppTab.DICTIONARIES) Icons.Rounded.LibraryBooks
                    else Icons.Outlined.LibraryBooks,
                  contentDescription = "Sözlükler",
                  modifier = Modifier.size(24.dp),
                )
              },
              label = { Text("Sözlükler") },
              modifier = Modifier.testTag("nav_dictionaries"),
            )
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
          when (uiState.activeTab) {
            AppTab.SEARCH ->
              SearchScreen(
                uiState = uiState,
                onStartsWithChange = viewModel::updateStartsWith,
                onEndsWithChange = viewModel::updateEndsWith,
                onContainsChange = viewModel::updateContains,
                onUnorderedLettersChange = viewModel::updateUnorderedLetters,
                onPatternChange = viewModel::updatePattern,
                onToggleLength = viewModel::toggleLength,
                onShowHelp = viewModel::showHelp,
                onSearch = viewModel::search,
                onClear = viewModel::clearFilters,
                onCopyAll = {
                  val list = uiState.results.words
                  if (list.isNotEmpty()) {
                    copyTextToClipboard(list.joinToString("\n"))
                    viewModel.notifyCopiedAll()
                  }
                },
                onCopyWord = { word ->
                  copyTextToClipboard(word)
                  viewModel.notifySingleWordCopied(word)
                },
                onExportResults = {
                  if (uiState.results.words.isNotEmpty()) {
                    exportResultsLauncher.launch("sonuc.txt")
                  }
                },
                onOpenFilePicker = launchFilePicker,
              )

            AppTab.DICTIONARIES ->
              DictionariesScreen(
                uiState = uiState,
                onOpenFilePicker = launchFilePicker,
                onExportAll = {
                  if (uiState.totalWordCount > 0) {
                    exportAllLauncher.launch("tum-sozluk.txt")
                  }
                },
                onDeleteSource = viewModel::deleteSource,
                onRequestDeleteAll = { viewModel.requestDeleteAllDialog(true) },
                onBackToSearch = { viewModel.selectTab(AppTab.SEARCH) },
              )
          }
        }
      }
    }
  }
}
