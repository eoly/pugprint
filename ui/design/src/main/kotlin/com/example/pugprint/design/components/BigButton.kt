package com.example.pugprint.design.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.pugprint.design.theme.PugTouch

/** A full-width button a kid can hit without aiming. */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasis: ButtonEmphasis = ButtonEmphasis.Primary,
) {
    val shape = MaterialTheme.shapes.large
    val label: @Composable () -> Unit = {
        Text(
            text = text,
            style =
                if (emphasis ==
                    ButtonEmphasis.Primary
                ) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.titleMedium
                },
            textAlign = TextAlign.Center,
        )
    }
    when (emphasis) {
        ButtonEmphasis.Primary ->
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = shape,
                modifier = modifier.fillMaxWidth().heightIn(min = PugTouch.primary),
            ) { label() }
        ButtonEmphasis.Secondary ->
            FilledTonalButton(
                onClick = onClick,
                enabled = enabled,
                shape = shape,
                modifier = modifier.fillMaxWidth().heightIn(min = PugTouch.secondary),
            ) { label() }
        ButtonEmphasis.Quiet ->
            TextButton(
                onClick = onClick,
                enabled = enabled,
                shape = shape,
                modifier = modifier.heightIn(min = PugTouch.minimum),
            ) { label() }
    }
}
