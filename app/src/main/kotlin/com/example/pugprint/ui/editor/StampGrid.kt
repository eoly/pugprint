package com.example.pugprint.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.pugprint.design.theme.PugSpacing
import com.example.pugprint.design.theme.PugTouch
import com.example.pugprint.imaging.Stamp
import com.example.pugprint.imaging.StampCatalog
import com.example.pugprint.imaging.StampRasterizer
import com.example.pugprint.imaging.StampSize
import com.example.pugprint.ui.imaging.toImageBitmap

/** Every stamp in [StampCatalog], as big tappable tiles, [PER_ROW] to a row. */
@Composable
fun StampGrid(
    onPick: (stampId: String) -> Unit,
    modifier: Modifier = Modifier,
    stamps: List<Stamp> = StampCatalog.all,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PugSpacing.small)) {
        stamps.chunked(PER_ROW).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(PugSpacing.small)) {
                row.forEach { stamp -> StampTile(stamp, onClick = { onPick(stamp.id) }) }
                repeat(PER_ROW - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun RowScope.StampTile(
    stamp: Stamp,
    onClick: () -> Unit,
) {
    val bitmap = remember(stamp) { StampRasterizer.render(stamp, StampSize.SMALL).toImageBitmap() }
    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .weight(1f)
                .heightIn(min = PugTouch.secondary)
                .semantics { role = Role.Button },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(PugSpacing.small)) {
            Image(
                bitmap = bitmap,
                contentDescription = stamp.displayName,
                filterQuality = FilterQuality.None,
                modifier = Modifier.size(TILE_ART),
            )
        }
    }
}

private const val PER_ROW = 6
private val TILE_ART = 40.dp
