package com.example.pugprint.design.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pugprint.design.theme.PugSpacing
import com.example.pugprint.design.theme.PugTheme
import com.example.pugprint.design.theme.PugTouch

/**
 * "Pick a look": one tile per theme, each painted in *its own* colours (a swatch of the big
 * button on the theme's background) so a kid sees what she is choosing before she taps it.
 */
@Composable
fun ThemePicker(
    themes: List<PugTheme>,
    selectedId: String,
    onSelect: (PugTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(PugSpacing.small),
    ) {
        themes.forEach { theme ->
            ThemeTile(theme = theme, selected = theme.id == selectedId, onClick = { onSelect(theme) })
        }
    }
}

@Composable
private fun RowScope.ThemeTile(
    theme: PugTheme,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val current = MaterialTheme.colorScheme
    val palette = theme.palette
    Surface(
        modifier =
            Modifier
                .weight(1f)
                .heightIn(min = PugTouch.primary)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = palette.background,
        contentColor = palette.text,
        border = BorderStroke(if (selected) 3.dp else 1.dp, if (selected) current.primary else palette.outline),
    ) {
        Column(
            modifier = Modifier.padding(vertical = PugSpacing.small, horizontal = PugSpacing.tiny),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PugSpacing.tiny),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(SWATCH)
                        .background(palette.primarySoft, CircleShape)
                        .border(SWATCH_RING, palette.primary, CircleShape)
                        .padding(SWATCH_RING * 2)
                        .background(palette.primary, CircleShape),
            )
            Text(
                text = theme.displayName,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 1, // one line: autoSize shrinks the label instead of breaking a word
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(minFontSize = MIN_LABEL_SIZE, maxFontSize = MAX_LABEL_SIZE),
            )
        }
    }
}

private val SWATCH = 28.dp
private val MIN_LABEL_SIZE = 11.sp
private val MAX_LABEL_SIZE = 14.sp
private val SWATCH_RING = 3.dp
