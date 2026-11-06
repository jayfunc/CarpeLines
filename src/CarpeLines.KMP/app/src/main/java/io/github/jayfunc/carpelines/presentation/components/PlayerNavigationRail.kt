package io.github.jayfunc.carpelines.presentation.components

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuOpen
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.github.jayfunc.carpelines.BottomNavItem
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.presentation.navigation.navigateToTopLevelSafely
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

/**
 * Material 3 wide navigation rail used instead of the bottom bar on medium/expanded
 * window widths (tablets, unfolded foldables). The header button expands it to show
 * labels beside the icons. Mirrors the navigation semantics of
 * [PlayerInternalNavigationBar].
 */
@Composable
fun PlayerNavigationRail(
    navController: NavHostController,
    navItems: ImmutableList<BottomNavItem>,
    currentRoute: String?,
    modifier: Modifier = Modifier
) {
    val latestCurrentRoute by rememberUpdatedState(currentRoute)
    val latestNavigationEnabled by rememberUpdatedState(currentRoute != null)
    val scope = rememberCoroutineScope()
    val railState = rememberWideNavigationRailState()
    val isExpanded = railState.targetValue == WideNavigationRailValue.Expanded

    WideNavigationRail(
        modifier = modifier.fillMaxHeight(),
        state = railState,
        arrangement = Arrangement.Center,
        header = {
            IconButton(
                onClick = { scope.launch { railState.toggle() } },
                modifier = Modifier.padding(start = 24.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.AutoMirrored.Rounded.MenuOpen else Icons.Rounded.Menu,
                    contentDescription = stringResource(
                        if (isExpanded) R.string.navigation_rail_collapse else R.string.navigation_rail_expand
                    )
                )
            }
        }
    ) {
        navItems.forEach { item ->
            val isSelected = currentRoute != null && currentRoute == item.screen.route
            val label = stringResource(item.labelResId)
            val iconPainterResId = if (isSelected && item.selectedIconResId != null && item.selectedIconResId != 0) {
                item.selectedIconResId
            } else {
                item.iconResId
            }
            val onClickLambda: () -> Unit = remember(item.screen.route, navController, scope) {
                click@{
                    if (!latestNavigationEnabled) return@click
                    val itemRoute = item.screen.route
                    if (latestCurrentRoute != itemRoute) {
                        navController.navigateToTopLevelSafely(itemRoute)
                    }
                }
            }
            WideNavigationRailItem(
                selected = isSelected,
                onClick = onClickLambda,
                enabled = currentRoute != null,
                railExpanded = isExpanded,
                icon = {
                    Icon(
                        painter = painterResource(id = iconPainterResId),
                        contentDescription = label
                    )
                },
                label = { Text(label) }
            )
        }
    }
}
