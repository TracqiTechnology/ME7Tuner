package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A small Info icon that shows a rich tooltip on hover/long-press.
 * Drop this next to any label, chart title, table header, or section
 * heading where an inline explanation would help the user.
 *
 * @param title  Bold heading shown inside the tooltip (usually the parameter name).
 * @param text   Body text explaining what the parameter means / how to use it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoTooltip(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 16.dp
) {
    @Suppress("DEPRECATION")
    TooltipBox(
        positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
        tooltip = {
            RichTooltip(title = { Text(title) }) {
                Text(text, style = MaterialTheme.typography.bodySmall)
            }
        },
        state = rememberTooltipState(isPersistent = true),
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = "$title info",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * An [OutlinedTextField] with a trailing [InfoTooltip] icon.
 * Use this everywhere a bare OutlinedTextField appears so every editable
 * parameter carries an inline explanation without cluttering the layout.
 *
 * @param label    Field label displayed inside the text field border.
 * @param tooltip  Explanation shown in the rich tooltip popup on hover.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParameterField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tooltip: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
    placeholder: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        readOnly = readOnly,
        singleLine = singleLine,
        textStyle = textStyle,
        placeholder = placeholder,
        keyboardOptions = keyboardOptions,
        trailingIcon = {
            @Suppress("DEPRECATION")
            TooltipBox(
                positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
                tooltip = {
                    RichTooltip(title = { Text(label) }) {
                        Text(tooltip, style = MaterialTheme.typography.bodySmall)
                    }
                },
                state = rememberTooltipState(isPersistent = true)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "$label info",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    )
}

/**
 * A label + [OutlinedTextField] + unit row with an [InfoTooltip] on the label.
 * Used in KRKTE, PLSOL, and KFMIOP where fields follow the pattern:
 *   "Label:  [field]  unit"
 */
@Composable
fun LabeledParameterRow(
    label: String,
    value: String,
    unit: String,
    tooltip: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    fieldWidth: Dp = 120.dp
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label + info icon take the remaining space
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.width(4.dp))
            InfoTooltip(title = label, text = tooltip)
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(fieldWidth).height(48.dp)
        )
        if (unit.isNotEmpty()) {
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp).width(60.dp)
            )
        } else {
            Spacer(Modifier.width(68.dp))
        }
    }
}

/**
 * A compact label + [OutlinedTextField] + unit column layout (stacked).
 * Used in PLSOL's constants panel where fields are displayed side-by-side
 * in a horizontal Row and space is limited.
 */
@Composable
fun ColumnParameterField(
    label: String,
    value: String,
    unit: String,
    tooltip: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Spacer(Modifier.width(4.dp))
            InfoTooltip(title = label, text = tooltip, iconSize = 14.dp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f).height(48.dp)
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

