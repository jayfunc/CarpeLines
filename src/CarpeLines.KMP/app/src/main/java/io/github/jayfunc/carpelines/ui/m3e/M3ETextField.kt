package io.github.jayfunc.carpelines.ui.m3e

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import io.github.jayfunc.carpelines.ui.theme.M3EShape

/**
 * The one text input in the app.
 *
 * Material's stock `OutlinedTextField` still ships the 4dp corner of the pre-Expressive scale,
 * a transparent container and a `outline` hairline, which reads flat and dated next to the rest of
 * this design system. M3 Expressive inputs instead sit in a **tonal container** on the `medium`
 * step of the shape scale, and the container itself carries the state: the hairline is invisible
 * at rest and turns `primary` when focused, `error` when invalid. Nothing is underlined, because a
 * straight indicator clipped by a rounded corner is exactly the "Material-looking but not
 * Material" artefact this file exists to remove.
 *
 * Every screen goes through this composable, so the following never drift apart again:
 * - shape — [M3EShape.medium] (12dp) for inputs, [M3EShape.full] for search
 * - container — `surfaceContainerHighest`, one rung above the page and above cards/sheets
 * - border — transparent at rest, `primary` on focus, `error` on error, 2dp on focus
 * - text colour — `onSurface` for value, `onSurfaceVariant` for label/placeholder/supporting
 *
 * The parameter list mirrors the Material one, so a call site can be migrated by renaming the
 * function and deleting the `colors =`/`shape =` overrides it used to carry.
 */
@Composable
fun M3ETextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    shape: Shape = M3EShape.medium,
    colors: TextFieldColors = m3eTextFieldColors(),
    interactionSource: MutableInteractionSource? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        shape = shape,
        colors = colors,
        interactionSource = interactionSource
    )
}

/**
 * [M3ETextField] for callers that keep a [TextFieldValue].
 *
 * Used where the caret is part of the state — a field that is re-entered after a navigation, or
 * one that selects its contents when it opens. The plain `String` overload would park the caret
 * at the start on restore, so this overload exists instead of forcing every caller into a String.
 */
@Composable
fun M3ETextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    shape: Shape = M3EShape.medium,
    colors: TextFieldColors = m3eTextFieldColors(),
    interactionSource: MutableInteractionSource? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        shape = shape,
        colors = colors,
        interactionSource = interactionSource
    )
}

/**
 * Search input.
 *
 * A search field is not a form field: it is a **query affordance**, so it takes the pill
 * silhouette and drops the outline entirely rather than borrowing the form-field treatment. It is
 * single-line by definition, submits from the keyboard's search action, and always leads with the
 * search glyph so the field is recognisable without its placeholder.
 *
 * Callers keep their own `trailingIcon` — the clear button differs in wording per screen and has
 * to stay localisable.
 */
@Composable
fun M3ESearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit) = { SearchGlyph() },
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    shape: Shape = M3EShape.full,
    colors: TextFieldColors = m3eSearchFieldColors(),
    interactionSource: MutableInteractionSource? = null,
) {
    M3ETextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = shape,
        colors = colors,
        interactionSource = interactionSource
    )
}

/** [M3ESearchField] for callers that keep a [TextFieldValue]. */
@Composable
fun M3ESearchField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit) = { SearchGlyph() },
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    shape: Shape = M3EShape.full,
    colors: TextFieldColors = m3eSearchFieldColors(),
    interactionSource: MutableInteractionSource? = null,
) {
    M3ETextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = shape,
        colors = colors,
        interactionSource = interactionSource
    )
}

@Composable
private fun SearchGlyph() {
    Icon(
        imageVector = Icons.Rounded.Search,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Colour scheme for [M3ETextField].
 *
 * Only the tokens that carry state are set; everything else keeps Material's own mapping so text,
 * cursor, selection and icon colours stay correct in light, dark and dynamic themes.
 */
@Composable
fun m3eTextFieldColors(): TextFieldColors {
    val scheme = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = scheme.surfaceContainerHighest,
        unfocusedContainerColor = scheme.surfaceContainerHighest,
        disabledContainerColor = scheme.surfaceContainerLow,
        errorContainerColor = scheme.surfaceContainerHighest,
        focusedBorderColor = scheme.primary,
        unfocusedBorderColor = Color.Transparent,
        disabledBorderColor = Color.Transparent,
        errorBorderColor = scheme.error
    )
}

/** Colour scheme for [M3ESearchField]: a filled pill that never draws an outline. */
@Composable
fun m3eSearchFieldColors(): TextFieldColors {
    val scheme = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = scheme.surfaceContainerHighest,
        unfocusedContainerColor = scheme.surfaceContainerHighest,
        disabledContainerColor = scheme.surfaceContainerLow,
        errorContainerColor = scheme.surfaceContainerHighest,
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        disabledBorderColor = Color.Transparent,
        errorBorderColor = Color.Transparent
    )
}
