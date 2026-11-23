package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun FilterInputField(
  label: String,
  value: String,
  clearGeneration: Int,
  onValueChange: (String) -> Unit,
  placeholder: String,
  onHelpClick: () -> Unit,
  onSearchTriggered: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
  useMonospace: Boolean = false,
  showWildcardButton: Boolean = false,
) {
  var tfv by remember {
    mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
  }
  var isFocused by remember { mutableStateOf(false) }
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(clearGeneration) {
    if (clearGeneration > 0) {
      tfv = TextFieldValue(text = "", selection = TextRange(0))
    }
  }

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.weight(1f),
      )
      IconButton(
        onClick = onHelpClick,
        modifier = Modifier.minimumInteractiveComponentSize().size(40.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.HelpOutline,
          contentDescription = "$label hakkında bilgi",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
      }
    }

    OutlinedTextField(
      value = tfv,
      onValueChange = { newTfv ->
        tfv = newTfv
        onValueChange(newTfv.text)
      },
      placeholder = {
        Text(
          text = placeholder,
          style =
            if (useMonospace) {
              MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMonoFontFamily)
            } else {
              MaterialTheme.typography.bodyLarge
            },
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
      },
      textStyle =
        if (useMonospace) {
          MaterialTheme.typography.titleMedium.copy(
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            textDirection = TextDirection.Ltr,
          )
        } else {
          MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Medium,
            textDirection = TextDirection.Ltr,
          )
        },
      singleLine = true,
      keyboardOptions =
        KeyboardOptions(
          capitalization = KeyboardCapitalization.None,
          autoCorrectEnabled = false,
          imeAction = ImeAction.Search,
        ),
      keyboardActions = KeyboardActions(onSearch = { onSearchTriggered() }),
      trailingIcon = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (showWildcardButton) {
            Surface(
              onClick = {
                val currentText = tfv.text
                val insertPos =
                  if (isFocused) tfv.selection.end.coerceIn(0, currentText.length)
                  else currentText.length
                val selStart =
                  if (isFocused) tfv.selection.min.coerceIn(0, currentText.length)
                  else currentText.length
                val selEnd =
                  if (isFocused) tfv.selection.max.coerceIn(0, currentText.length)
                  else currentText.length
                val newText =
                  currentText.substring(0, selStart) + "*" + currentText.substring(selEnd)
                val nextCursor = (selStart + 1).coerceAtLeast(if (!isFocused) newText.length else insertPos + 1)
                tfv =
                  TextFieldValue(
                    text = newText,
                    selection = TextRange(nextCursor.coerceIn(0, newText.length)),
                  )
                onValueChange(newText)
                if (!isFocused) {
                  focusRequester.requestFocus()
                }
              },
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.secondaryContainer,
              contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(end = 4.dp).minimumInteractiveComponentSize().testTag("wildcard_star_button"),
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              ) {
                Text(
                  text = "*",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                  ),
                )
              }
            }
          }
          if (tfv.text.isNotEmpty()) {
            IconButton(
              onClick = {
                tfv = TextFieldValue(text = "", selection = TextRange(0))
                onValueChange("")
              },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Temizle",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      },
      shape = MaterialTheme.shapes.small,
      colors =
        OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
      modifier =
        Modifier.fillMaxWidth()
          .focusRequester(focusRequester)
          .onFocusChanged { isFocused = it.isFocused }
          .testTag(testTag),
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LengthChipsSection(
  selectedLengths: Set<Int>,
  onToggleLength: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = "Harf sayısı",
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = "Birden fazla seçilebilir; hiçbiri seçili değilse tüm uzunluklar aranır.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(10.dp))
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      for (len in 2..16) {
        val selected = len in selectedLengths
        val containerColor by
          animateColorAsState(
            targetValue =
              if (selected) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            label = "chipContainer",
          )
        val contentColor by
          animateColorAsState(
            targetValue =
              if (selected) MaterialTheme.colorScheme.onPrimary
              else MaterialTheme.colorScheme.onSurface,
            label = "chipContent",
          )
        val borderColor =
          if (selected) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.outline

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = containerColor,
          contentColor = contentColor,
          border = BorderStroke(1.dp, borderColor),
          modifier =
            Modifier.heightIn(min = 44.dp)
              .clip(RoundedCornerShape(10.dp))
              .clickable { onToggleLength(len) }
              .testTag("length_chip_$len"),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
          ) {
            Text(
              text = if (len == 16) "16+" else len.toString(),
              style = MaterialTheme.typography.labelLarge,
              fontFamily = JetBrainsMonoFontFamily,
            )
          }
        }
      }
    }
  }
}

@Composable
fun WordResultTile(
  word: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier =
      modifier
        .heightIn(min = 48.dp)
        .clip(RoundedCornerShape(10.dp))
        .clickable(onClick = onClick),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
      Text(
        text = word,
        style = MaterialTheme.typography.bodyLarge.copy(
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Medium,
        ),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Icon(
        imageVector = Icons.Rounded.ContentCopy,
        contentDescription = "$word kopyala",
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
        modifier = Modifier.size(15.dp),
      )
    }
  }
}

@Composable
fun FilterHelpDialog(
  topic: FilterHelpTopic,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Rounded.Lightbulb,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.secondary,
      )
    },
    title = {
      Text(
        text = topic.title,
        style = MaterialTheme.typography.headlineSmall,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = topic.description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.surfaceVariant,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = "Örnekler",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
            topic.examples.forEach { example ->
              Text(
                text = "• $example",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontFamily = JetBrainsMonoFontFamily,
                ),
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Anladım")
      }
    },
  )
}

@Composable
fun ConfirmDeleteAllDialog(
  wordCountText: String,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Rounded.DeleteForever,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
      )
    },
    title = { Text("Tüm sözlüğü sil?") },
    text = {
      Text(
        "Yüklü olan tüm sözlük dosyaları ($wordCountText kelime) cihazdan kaldırılacak. İstediğinde yeniden .txt dosyası yükleyebilirsin."
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
          ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
      ) {
        Text("Tümünü Sil")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Vazgeç")
      }
    },
  )
}
