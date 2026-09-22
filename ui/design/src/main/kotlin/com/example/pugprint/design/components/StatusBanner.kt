package com.example.pugprint.design.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.pugprint.design.theme.PugSpacing

/**
 * A full-width card with an icon, one line of [text] and an optional [hint] saying what to do.
 * The one place the app talks to the kid about the printer.
 */
@Composable
fun StatusBanner(
    kind: BannerKind,
    text: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    /** 0.0–1.0 for a [BannerKind.Working] banner that knows how far along it is. */
    progress: Float? = null,
    /** True for a banner that appears in response to something (a print finished, an error): TalkBack reads it out. */
    announce: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val (container, accent) =
        when (kind) {
            BannerKind.Info -> colors.surfaceVariant to colors.onSurfaceVariant
            BannerKind.Working -> colors.surfaceVariant to colors.primary
            BannerKind.Problem -> colors.errorContainer to colors.error
            BannerKind.Success -> colors.primaryContainer to colors.primary
        }
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { if (announce) liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = colors.onSurface,
    ) {
        Column(modifier = Modifier.padding(PugSpacing.medium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BannerIcon(kind, accent)
                Spacer(Modifier.size(PugSpacing.medium))
                Column {
                    Text(text = text, style = MaterialTheme.typography.titleMedium)
                    if (hint != null) {
                        Text(text = hint, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }
            }
            if (progress != null) {
                Spacer(Modifier.height(PugSpacing.small))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = accent,
                    trackColor = colors.background,
                )
            }
        }
    }
}

@Composable
private fun BannerIcon(
    kind: BannerKind,
    tint: Color,
) {
    val size = 28.dp
    when (kind) {
        BannerKind.Info ->
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(size),
            )
        BannerKind.Working ->
            CircularProgressIndicator(
                modifier = Modifier.size(size),
                color = tint,
                strokeWidth = 3.dp,
            )
        BannerKind.Problem ->
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(size),
            )
        BannerKind.Success ->
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(size),
            )
    }
}
