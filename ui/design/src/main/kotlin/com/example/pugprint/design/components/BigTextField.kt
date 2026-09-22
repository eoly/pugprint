package com.example.pugprint.design.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.pugprint.design.theme.PugTouch

/** One line of big, easy-to-read typing; the keyboard's Done key calls [onDone]. */
@Composable
fun BigTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    maxLength: Int = DEFAULT_MAX_LENGTH,
    onDone: () -> Unit = {},
) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= maxLength) onValueChange(it) },
        placeholder = { Text(placeholder, style = MaterialTheme.typography.titleLarge) },
        textStyle = MaterialTheme.typography.titleLarge,
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        keyboardOptions =
            KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
        keyboardActions =
            KeyboardActions(
                onDone = {
                    keyboard?.hide()
                    onDone()
                },
            ),
        modifier = modifier.fillMaxWidth().heightIn(min = PugTouch.primary),
    )
}

private const val DEFAULT_MAX_LENGTH = 60
