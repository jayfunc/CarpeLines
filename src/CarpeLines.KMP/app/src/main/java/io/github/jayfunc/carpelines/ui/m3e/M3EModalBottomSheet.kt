package io.github.jayfunc.carpelines.ui.m3e

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.ui.theme.M3EShape

/**
 * The app's modal bottom sheet.
 *
 * Material's sheet ships a rectangular drag handle: the press state layer covers the whole 32×48dp
 * hit area with square corners, and the long-press hint is a 4dp-corner bubble, which reads as a
 * plain rectangle on a modern rounded surface. Both are drawn by the library's internal
 * `DragHandleWithTooltip`, so the only way to round them is to take the handle over: this wrapper
 * asks Material for no handle and draws [M3EDragHandle] at the top of the content instead.
 *
 * Everything else is forwarded unchanged, so a caller can switch from [ModalBottomSheet] to this
 * wrapper without thinking about the difference.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3EModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    sheetGesturesEnabled: Boolean = true,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    contentWindowInsets: @Composable () -> WindowInsets = {
        BottomSheetDefaults.modalWindowInsets
    },
    properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        sheetMaxWidth = sheetMaxWidth,
        sheetGesturesEnabled = sheetGesturesEnabled,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        scrimColor = scrimColor,
        // The handle is drawn below, inside the content: Material's slot would wrap it in the
        // rectangular state layer and the square-cornered hint this component exists to replace.
        dragHandle = null,
        contentWindowInsets = contentWindowInsets,
        properties = properties,
    ) {
        M3EDragHandle()
        content()
    }
}

/**
 * The sheet's drag handle, drawn as a pill.
 *
 * The 32×4dp bar takes the stadium shape, and the long-press hint is the app's `large` shape rather
 * than Material's 4dp corner. It carries no click gesture of its own: the sheet is already
 * draggable, the scrim and back dismiss it, and a handle that also toggled the sheet would have to
 * second-guess every caller's guarded `onDismissRequest`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3EDragHandle(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.cd_drag_handle)
    val tooltipState = rememberTooltipState()
    val barColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = HandleBarAlpha)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                TooltipAnchorPosition.Above
            ),
            tooltip = {
                PlainTooltip(shape = M3EShape.large) {
                    Text(text = label)
                }
            },
            state = tooltipState,
            modifier = Modifier.semantics { contentDescription = label },
        ) {
            Box(
                modifier = Modifier.size(width = HandleTouchWidth, height = HandleTouchHeight),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = HandleBarWidth, height = HandleBarHeight)
                        .background(color = barColor, shape = CircleShape)
                )
            }
        }
    }
}

/** Sheet handle bar: wide and thin, exactly the pill Material draws. */
private val HandleBarWidth = 32.dp
private val HandleBarHeight = 4.dp

/**
 * Hit area around the bar.
 *
 * 48dp tall like any other interactive target, so a long press is as easy to land on the pill as
 * it was on Material's handle, and no taller than that: the sheet's top band keeps the height the
 * stock handle occupied, so swapping it in does not shift a sheet's content.
 */
private val HandleTouchWidth = 64.dp
private val HandleTouchHeight = 48.dp

/** The bar recedes on both light and dark surfaces without disappearing. */
private const val HandleBarAlpha = 0.4f
