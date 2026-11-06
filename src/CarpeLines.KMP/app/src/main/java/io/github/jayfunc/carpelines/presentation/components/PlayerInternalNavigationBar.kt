package io.github.jayfunc.carpelines.presentation.components

import io.github.jayfunc.carpelines.presentation.navigation.navigateToTopLevelSafely

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.github.jayfunc.carpelines.BottomNavItem
import io.github.jayfunc.carpelines.ui.m3e.M3ENavItem
import kotlinx.collections.immutable.ImmutableList

internal val NavBarContentHeight = 68.dp
internal val NavBarCompactContentHeight = 68.dp
internal val NavBarContentHeightFullWidth = NavBarContentHeight
internal val NavBarOuterTopPadding = 8.dp

// The pill floats above the gesture bar instead of sitting on it. 16 dp read as "attached to the
// system bar"; one more spacing step keeps it visually inside the page while still reachable.
internal val NavBarOuterBottomPadding = 24.dp
internal val NavBarOuterHorizontalPadding = 16.dp
private val MainScreenBottomGradientExtraHeight = 8.dp
internal val MaxNavigationBarBottomInset = 96.dp

internal fun sanitizeNavigationBarBottomInset(systemNavBarInset: Dp): Dp {
    if (!systemNavBarInset.value.isFinite()) return 0.dp
    return systemNavBarInset.coerceIn(0.dp, MaxNavigationBarBottomInset)
}

internal fun calculatePlayerSheetCollapsedTargetY(
    containerHeightPx: Float,
    collapsedContentHeightPx: Float,
    bottomMarginPx: Float,
    bottomSpacerPx: Float
): Float {
    val safeContainerHeightPx = containerHeightPx.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    val safeCollapsedContentHeightPx = collapsedContentHeightPx.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    val safeBottomMarginPx = bottomMarginPx.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    val safeBottomSpacerPx = bottomSpacerPx.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    val maxTargetY = (safeContainerHeightPx - safeCollapsedContentHeightPx).coerceAtLeast(0f)

    return (safeContainerHeightPx - safeCollapsedContentHeightPx - safeBottomMarginPx - safeBottomSpacerPx)
        .coerceIn(0f, maxTargetY)
}

internal fun resolveNavBarContentHeight(compactMode: Boolean): Dp = NavBarContentHeight

internal fun resolveMainScreenBottomGradientHeight(compactMode: Boolean): Dp =
    NavBarContentHeight + NavBarOuterTopPadding + NavBarOuterBottomPadding + MainScreenBottomGradientExtraHeight

/** Height occupied by the fixed floating bar, including its outer margins and inset. */
internal fun resolveNavBarOccupiedHeight(
    systemNavBarInset: Dp,
    compactMode: Boolean
): Dp = NavBarContentHeight + NavBarOuterTopPadding + NavBarOuterBottomPadding + systemNavBarInset

/**
 * The app's top-level navigation bar.
 *
 * A floating pill toolbar: the active destination grows an icon and slides its label in beside
 * it, while inactive destinations show only icons. The compact pill re-measures as the selection
 * changes, so the whole bar animates instead of swapping states abruptly.
 *
 * Navigation semantics live here because the bar is the only place that knows about the
 * top-level destinations.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerInternalNavigationBar(
    navController: NavHostController,
    navItems: ImmutableList<BottomNavItem>,
    currentRoute: String?,
    modifier: Modifier = Modifier,
    compactMode: Boolean,
    bottomBarPadding: Dp = 0.dp
) {
    val navBarInsetPadding = sanitizeNavigationBarBottomInset(
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    )
    val innerRowPadding = (navBarInsetPadding - bottomBarPadding).coerceAtLeast(0.dp)

    val latestCurrentRoute by rememberUpdatedState(currentRoute)
    val latestNavigationEnabled by rememberUpdatedState(currentRoute != null)
    val haptics = LocalHapticFeedback.current

    val items = navItems.map { item ->
        val label = stringResource(item.labelResId)
        val selectedIconRes = item.selectedIconResId
            ?.takeIf { it != 0 }
            ?: item.iconResId
        M3ENavItem(
            label = label,
            icon = painterResource(item.iconResId),
            selectedIcon = painterResource(selectedIconRes)
        )
    }

    val selectedIndex = navItems.indexOfFirst { it.screen.route == currentRoute }

    fun handleSelect(index: Int) {
        if (!latestNavigationEnabled) return
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        val itemRoute = navItems[index].screen.route
        if (latestCurrentRoute != itemRoute) {
            navController.navigateToTopLevelSafely(itemRoute)
        }
    }

    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier
            .animateContentSize()
            .height(NavBarContentHeight)
            .padding(bottom = innerRowPadding),
        shape = RoundedCornerShape(100),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
            toolbarContainerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            ShortNavigationBarItem(
                selected = selected,
                onClick = { handleSelect(index) },
                enabled = currentRoute != null,
                icon = {
                    val extraHeight by animateDpAsState(
                        targetValue = if (selected) 16.dp else 0.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "navIconHeight"
                    )
                    Box(
                        modifier = Modifier.height(26.dp + extraHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = if (selected) item.selectedIcon else item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                },
                label = {
                    AnimatedVisibility(
                        visible = selected,
                        enter = fadeIn() + slideInHorizontally(
                            initialOffsetX = { -15 },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) + expandHorizontally(expandFrom = Alignment.Start),
                        exit = fadeOut() + slideOutHorizontally(
                            targetOffsetX = { -15 }
                        ) + shrinkHorizontally(shrinkTowards = Alignment.Start)
                    ) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            modifier = Modifier.padding(start = 2.dp, end = 4.dp)
                        )
                    }
                },
                iconPosition = NavigationItemIconPosition.Start,
                colors = ShortNavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColorStartIconPosition = MaterialTheme.colorScheme.onPrimary,
                    selectedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unselectedTextColor = Color.Transparent
                ),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .fillMaxHeight()
            )
        }
    }
}
