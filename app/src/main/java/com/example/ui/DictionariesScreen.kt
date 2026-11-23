package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DictionarySourceEntity
import com.example.domain.WordFilterEngine

@Composable
fun DictionariesScreen(
  uiState: MainUiState,
  onOpenFilePicker: () -> Unit,
  onExportAll: () -> Unit,
  onDeleteSource: (DictionarySourceEntity) -> Unit,
  onRequestDeleteAll: () -> Unit,
  onBackToSearch: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler { onBackToSearch() }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
    ) {
      item(key = "upload_card") {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          tonalElevation = 1.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "Sözlük dosyası yükle / ekle",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text =
                "Her satırda bir kelime bulunan .txt dosyalarını yükle. Birden fazla dosya seçebilirsin; kelimeler otomatik birleştirilir ve Türkçe alfabeye göre sıralanır. İkinci bir dosya eklediğinde sözlükte olmayan yeni kelimeler otomatik listelenir.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "UTF-8 ve Türkçe Windows-1254 kodlamaları otomatik algılanır. Yüklenen sözlükler cihazda kalıcı olarak saklanır.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Button(
              onClick = onOpenFilePicker,
              shape = MaterialTheme.shapes.small,
              contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
              modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("upload_dict_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.FileUpload,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "TXT Dosyası Seç ve Ekle",
                style = MaterialTheme.typography.titleMedium,
              )
            }
          }
        }
      }

      item(key = "status_card") {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = uiState.statusText,
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurface,
            )

            if (uiState.sources.isNotEmpty()) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedButton(
                  onClick = onExportAll,
                  shape = MaterialTheme.shapes.small,
                  modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("export_all_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Tümünü kaydet", maxLines = 1)
                }

                OutlinedButton(
                  onClick = onRequestDeleteAll,
                  shape = MaterialTheme.shapes.small,
                  colors =
                    ButtonDefaults.outlinedButtonColors(
                      contentColor = MaterialTheme.colorScheme.error,
                    ),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                  modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("delete_all_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Tümünü sil", maxLines = 1)
                }
              }
            }
          }
        }
      }

      if (uiState.sources.isEmpty()) {
        item(key = "empty_sources") {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.padding(28.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp),
              )
              Text(
                text = "Henüz kayıtlı sözlük yok",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "Yukarıdaki butona dokunarak telefonundan .txt uzantılı kelime listesi yükleyebilirsin.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        item(key = "sources_header") {
          Text(
            text = "Yüklü Kaynaklar (${uiState.sources.size})",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp),
          )
        }

        items(uiState.sources, key = { it.id }) { source ->
          SourceFileRow(
            source = source,
            onDelete = { onDeleteSource(source) },
          )
        }
      }
    }
  }
}

@Composable
private fun SourceFileRow(
  source: DictionarySourceEntity,
  onDelete: () -> Unit,
) {
  Surface(
    shape = MaterialTheme.shapes.small,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(38.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Rounded.Description,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = source.fileName,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${WordFilterEngine.formatNumber(source.wordCount)} kelime",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      IconButton(
        onClick = onDelete,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("delete_source_${source.fileName}"),
      ) {
        Icon(
          imageVector = Icons.Rounded.DeleteOutline,
          contentDescription = "${source.fileName} sil",
          tint = MaterialTheme.colorScheme.error,
        )
      }
    }
  }
}
