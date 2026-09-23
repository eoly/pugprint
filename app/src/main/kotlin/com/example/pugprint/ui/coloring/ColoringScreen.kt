package com.example.pugprint.ui.coloring

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pugprint.R
import com.example.pugprint.design.components.KidScreen
import com.example.pugprint.design.theme.PugPrintTheme
import com.example.pugprint.design.theme.PugSpacing
import com.example.pugprint.imaging.ColoringPage
import com.example.pugprint.imaging.LabelShape
import com.example.pugprint.ui.imaging.drawRoundStickerGuide
import com.example.pugprint.ui.imaging.toImageBitmap

/** Every coloring page as a big tappable tile, [PER_ROW] to a row; tap one to open it on the draw sheet. */
@Composable
fun ColoringScreen(
    state: ColoringUiState,
    actions: ColoringActions = ColoringActions(),
    modifier: Modifier = Modifier,
) {
    KidScreen(
        modifier = modifier,
        title = stringResource(R.string.coloring_title),
        onBack = actions.onBack,
        onHome = actions.onHome,
        scrollable = true,
    ) {
        Text(
            text = stringResource(R.string.coloring_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(PugSpacing.medium))
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PugSpacing.small)) {
            state.pages.chunked(PER_ROW).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(PugSpacing.small)) {
                    row.forEach { page ->
                        PageTile(
                            page = page,
                            round = state.labelShape == LabelShape.CIRCLE,
                            onClick = { actions.onPick(page.id) },
                        )
                    }
                    repeat(PER_ROW - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** The page as it will print, with its name underneath; on a round roll the corners are faded. */
@Composable
private fun RowScope.PageTile(
    page: ColoringPage,
    round: Boolean,
    onClick: () -> Unit,
) {
    val bitmap = remember(page) { page.render().toImageBitmap() }
    val guideColour = MaterialTheme.colorScheme.outline
    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .weight(1f)
                .semantics { role = Role.Button },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(PugSpacing.small), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                bitmap = bitmap,
                contentDescription = null, // the name below is the label
                filterQuality = FilterQuality.High,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .drawWithContent {
                            drawContent()
                            if (round) drawRoundStickerGuide(guideColour)
                        },
            )
            Spacer(Modifier.height(PugSpacing.small))
            Text(text = page.displayName, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
    }
}

private const val PER_ROW = 2

@Preview(showBackground = true)
@Composable
private fun ColoringScreenPreview() {
    PugPrintTheme { ColoringScreen(state = ColoringUiState()) }
}
