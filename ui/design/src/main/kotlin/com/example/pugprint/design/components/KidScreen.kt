package com.example.pugprint.design.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.pugprint.design.R
import com.example.pugprint.design.theme.PugLayout
import com.example.pugprint.design.theme.PugSpacing
import com.example.pugprint.design.theme.PugTouch

/**
 * The frame every screen sits in: theme background, safe-area padding, content capped at
 * [PugLayout.maxContentWidth] and centred (so a tablet still looks hand-sized), and an optional
 * header with a big back button, a [title] and, deep in a flow, a Home button ([onHome]) so a
 * kid never has to count back-presses.
 */
@Composable
fun KidScreen(
    modifier: Modifier = Modifier,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    onHome: (() -> Unit)? = null,
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(modifier = modifier.fillMaxSize(), snackbarHost = snackbarHost) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .widthIn(max = PugLayout.maxContentWidth)
                        .padding(horizontal = PugLayout.screenPadding, vertical = PugSpacing.small),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (title != null || onBack != null || onHome != null) Header(title, onBack, onHome)
                content()
            }
        }
    }
}

@Composable
private fun Header(
    title: String?,
    onBack: (() -> Unit)?,
    onHome: (() -> Unit)?,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(PugTouch.secondary)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.design_back),
                    modifier = Modifier.size(PugSpacing.huge),
                )
            }
            Spacer(Modifier.width(PugSpacing.small))
        }
        if (title != null) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        } else {
            Spacer(Modifier.weight(1f))
        }
        if (onHome != null) {
            IconButton(onClick = onHome, modifier = Modifier.size(PugTouch.secondary)) {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = stringResource(R.string.design_home),
                    modifier = Modifier.size(PugSpacing.huge),
                )
            }
        }
    }
}

/** The app's name (or another big friendly word) at the top of a screen without a header. */
@Composable
fun HeroTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}
