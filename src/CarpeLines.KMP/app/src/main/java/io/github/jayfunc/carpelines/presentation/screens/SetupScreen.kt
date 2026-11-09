
package io.github.jayfunc.carpelines.presentation.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator

import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.data.backup.model.BackupSection
import io.github.jayfunc.carpelines.data.backup.model.BackupTransferProgressUpdate
import io.github.jayfunc.carpelines.data.backup.model.RestorePlan
import io.github.jayfunc.carpelines.data.external.ExternalMediaMonitor
import io.github.jayfunc.carpelines.data.preferences.AppLanguage
import io.github.jayfunc.carpelines.data.preferences.AppThemeMode
import io.github.jayfunc.carpelines.presentation.components.PermissionIconCollage
import io.github.jayfunc.carpelines.presentation.components.BackupModuleSelectionDialog
import io.github.jayfunc.carpelines.presentation.components.BackupPasswordPromptDialog
import io.github.jayfunc.carpelines.presentation.components.LocalAppMessageHost
import io.github.jayfunc.carpelines.presentation.components.rememberAppMessage
import io.github.jayfunc.carpelines.presentation.components.rememberAppMessageHost
import io.github.jayfunc.carpelines.presentation.components.subcomps.MaterialYouVectorDrawable
import io.github.jayfunc.carpelines.presentation.components.subcomps.SineWaveLine
import io.github.jayfunc.carpelines.presentation.components.FileExplorerDialog
import io.github.jayfunc.carpelines.presentation.viewmodel.DirectoryEntry
import io.github.jayfunc.carpelines.presentation.viewmodel.SetupEvent
import io.github.jayfunc.carpelines.presentation.viewmodel.SetupUiState
import io.github.jayfunc.carpelines.presentation.viewmodel.SetupViewModel
import io.github.jayfunc.carpelines.ui.m3e.M3ESwitch
import io.github.jayfunc.carpelines.ui.theme.M3EShape
import io.github.jayfunc.carpelines.ui.theme.M3ESpacing
import io.github.jayfunc.carpelines.ui.theme.RoundedSans
import io.github.jayfunc.carpelines.ui.theme.emphasized
import io.github.jayfunc.carpelines.utils.AppLocaleManager
import io.github.jayfunc.carpelines.utils.StorageInfo
import io.github.jayfunc.carpelines.utils.isIgnoringBatteryOptimizations
import io.github.jayfunc.carpelines.utils.openBatteryOptimizationSettings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalPermissionsApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SetupScreen(
    setupViewModel: SetupViewModel = hiltViewModel(),
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val uiState by setupViewModel.uiState.collectAsStateWithLifecycle()
    val currentPath by setupViewModel.currentPath.collectAsStateWithLifecycle()
    val directoryChildren by setupViewModel.currentDirectoryChildren.collectAsStateWithLifecycle()
    val availableStorages by setupViewModel.availableStorages.collectAsStateWithLifecycle()
    val selectedStorageIndex by setupViewModel.selectedStorageIndex.collectAsStateWithLifecycle()
    val isExplorerPriming by setupViewModel.isExplorerPriming.collectAsStateWithLifecycle()
    val isExplorerReady by setupViewModel.isExplorerReady.collectAsStateWithLifecycle()
    val isCurrentDirectoryResolved by setupViewModel.isCurrentDirectoryResolved.collectAsStateWithLifecycle()
    val appMessageHost = rememberAppMessageHost()
    var selectedBackupUri by remember { mutableStateOf<Uri?>(null) }
    
    val backupPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedBackupUri = uri
        if (uri != null) {
            setupViewModel.inspectBackupFile(uri)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                setupViewModel.checkPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Folder exclusions only make sense when local scanning is enabled, so the page disappears
    // when media access is skipped.
    val pages = remember(uiState.mediaPermissionGranted) {
        buildSetupPages(includeDirectorySelection = uiState.mediaPermissionGranted)
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    // The list can shrink if media access is revoked mid-setup, so clamp before indexing.
    val currentPage = pages[pagerState.currentPage.coerceIn(0, pages.lastIndex)]
    val isNextButtonEnabled = isPermissionGateSatisfied(context, currentPage, uiState)
    var previousPageIndex by remember { mutableStateOf(0) }
    val navigateToPage: (Int) -> Unit = { targetPage ->
        scope.launch {
            val boundedPage = targetPage.coerceIn(0, pages.lastIndex)
            if (boundedPage != pagerState.currentPage) {
                pagerState.animateScrollToPage(boundedPage)
            }
        }
    }

    val directorySelectionPageIndex = remember(pages) { pages.indexOf(SetupPage.DirectorySelection) }
    val batteryOptimizationPageIndex = remember(pages) { pages.indexOf(SetupPage.BatteryOptimization) }
    val finishPageIndex = remember(pages) { pages.indexOf(SetupPage.Finish) }

    LaunchedEffect(setupViewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            setupViewModel.events.collectLatest { event ->
                when (event) {
                    is SetupEvent.Message -> {
                        appMessageHost.show(event.value)
                    }
                    is SetupEvent.RestoreCompleted -> {
                        appMessageHost.show(event.message)
                        val targetPageIndex = when {
                            batteryOptimizationPageIndex >= 0 && !isIgnoringBatteryOptimizations(context) ->
                                batteryOptimizationPageIndex
                            finishPageIndex >= 0 -> finishPageIndex
                            else -> null
                        }

                        if (targetPageIndex != null) {
                            pagerState.animateScrollToPage(targetPageIndex)
                        } else {
                            onSetupComplete()
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage > previousPageIndex) {
            val blockedPageIndex = firstBlockedForwardPageIndex(
                pages = pages,
                fromPageIndex = previousPageIndex,
                toPageIndex = pagerState.currentPage,
                context = context,
                uiState = uiState
            )
            if (blockedPageIndex != null) {
                setupViewModel.checkPermissions(context)
                pagerState.scrollToPage(blockedPageIndex)
                previousPageIndex = blockedPageIndex
                appMessageHost.show(context.getString(R.string.toast_grant_permission_first))
                return@LaunchedEffect
            }
        }
        previousPageIndex = pagerState.currentPage

        if (pagerState.currentPage == directorySelectionPageIndex) {
            setupViewModel.loadMusicDirectories()
        }
    }
    BackHandler {
        if (pagerState.currentPage > 0) {
            navigateToPage(pagerState.currentPage - 1)
        }
    }
    CompositionLocalProvider(LocalAppMessageHost provides appMessageHost) {
    Scaffold(
        snackbarHost = { SnackbarHost(appMessageHost.snackbarHostState) },
        bottomBar = {
            SetupBottomBar(
                pagerState = pagerState,
                animated = (pagerState.currentPage != 0),
                isNextButtonEnabled = isNextButtonEnabled,
                isFinishButtonEnabled = uiState.requiredPermissionsGranted,
                onNextClicked = {
                    val page = pages[pagerState.currentPage]
                    if (isPermissionGateSatisfied(context, page, uiState)) {
                        navigateToPage(pagerState.currentPage + 1)
                    } else {
                        setupViewModel.checkPermissions(context)
                        appMessageHost.show(context.getString(R.string.toast_grant_permission_first))
                    }
                },
                onFinishClicked = {
                    if (allRequiredPermissionsGrantedNow(context)) {
                        setupViewModel.setSetupComplete()
                        onSetupComplete()
                    } else {
                        setupViewModel.checkPermissions(context)
                        appMessageHost.show(context.getString(R.string.toast_grant_all_permissions))
                    }
                }
            )
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { pageIndex ->
            val page = pages[pageIndex]
            val pageOffset = pagerState.currentPageOffsetFraction

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 1f - pageOffset.coerceIn(0f, 1f)
                        translationX = size.width * pageOffset
                    },
                contentAlignment = Alignment.Center
            ) {
                when (page) {
                    SetupPage.Welcome -> WelcomePage()
                    SetupPage.NotificationAccess -> NotificationAccessPage(uiState = uiState)
                    SetupPage.MediaPermission -> MediaPermissionPage(
                        uiState = uiState,
                        onPermissionStateUpdated = { setupViewModel.checkPermissions(context) },
                        onSkip = {
                            navigateToPage(pagerState.currentPage + 1)
                        }
                    )
                    SetupPage.BackupRestore -> BackupRestorePage(
                        uiState = uiState,
                        onImportClicked = { backupPickerLauncher.launch("*/*") },
                        onSkip = {
                            setupViewModel.clearRestorePlan()
                            selectedBackupUri = null
                            navigateToPage(pagerState.currentPage + 1)
                        }
                    )
                    SetupPage.DirectorySelection -> DirectorySelectionPage(
                        uiState = uiState,
                        currentPath = currentPath,
                        directoryChildren = directoryChildren,
                        availableStorages = availableStorages,
                        selectedStorageIndex = selectedStorageIndex,
                        isExplorerPriming = isExplorerPriming,
                        isExplorerReady = isExplorerReady,
                        isCurrentDirectoryResolved = isCurrentDirectoryResolved,
                        isAtRoot = setupViewModel.isAtRoot(),
                        explorerRoot = setupViewModel.explorerRoot(),
                        onOpenExplorer = setupViewModel::openExplorer,
                        onNavigateTo = setupViewModel::loadDirectory,
                        onNavigateUp = setupViewModel::navigateUp,
                        onRefresh = setupViewModel::refreshCurrentDirectory,
                        onPrimeExplorer = setupViewModel::primeExplorer,
                        onSkip = {
                            navigateToPage(pagerState.currentPage + 1)
                        },
                        onToggleAllowed = setupViewModel::toggleDirectoryAllowed,
                        onSelectionFinished = setupViewModel::applyPendingDirectoryRuleChanges,
                        onStorageSelected = setupViewModel::selectStorage
                    )
                    SetupPage.BatteryOptimization -> BatteryOptimizationPage(
                        onSkip = {
                            navigateToPage(pagerState.currentPage + 1)
                        }
                    )
                    SetupPage.ThemeSelection -> ThemeSelectionPage(
                        uiState = uiState,
                        onModeSelected = setupViewModel::setAppThemeMode
                    )
                    SetupPage.ExternalServices -> ExternalServicesPage(
                        uiState = uiState,
                        onExternalArtistImagesChanged = setupViewModel::setExternalArtistImagesEnabled
                    )
                    SetupPage.Finish -> FinishPage()
                }
            }
        }
    }

    val restorePlan = uiState.restorePlan
    if (restorePlan != null && selectedBackupUri != null) {
        BackupModuleSelectionDialog(
            plan = restorePlan,
            inProgress = uiState.isRestoringBackup,
            onDismiss = {
                setupViewModel.clearRestorePlan()
                selectedBackupUri = null
            },
            onBack = {
                setupViewModel.clearRestorePlan()
                selectedBackupUri = null
            },
            onSelectionChanged = setupViewModel::updateRestorePlanSelection,
            onConfirm = {
                val uri = selectedBackupUri ?: return@BackupModuleSelectionDialog
                selectedBackupUri = null
                setupViewModel.restoreFromPlan(uri)
            }
        )
    }

    val pendingEncryptedUri = uiState.pendingEncryptedBackupUri
    if (pendingEncryptedUri != null) {
        BackupPasswordPromptDialog(
            wrongPassword = uiState.wrongBackupPassword,
            isInspecting = uiState.isInspectingBackup,
            onDismiss = setupViewModel::dismissEncryptedBackupPrompt,
            onConfirm = { password ->
                val uri = pendingEncryptedUri.toUri()
                selectedBackupUri = uri
                setupViewModel.inspectBackupFile(uri, password)
            }
        )
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectorySelectionPage(
    uiState: SetupUiState,
    currentPath: File,
    directoryChildren: ImmutableList<DirectoryEntry>,
    availableStorages: ImmutableList<StorageInfo>,
    selectedStorageIndex: Int,
    isExplorerPriming: Boolean,
    isExplorerReady: Boolean,
    isCurrentDirectoryResolved: Boolean,
    isAtRoot: Boolean,
    explorerRoot: File,
    onOpenExplorer: () -> Unit,
    onNavigateTo: (File) -> Unit,
    onNavigateUp: () -> Unit,
    onRefresh: () -> Unit,
    onPrimeExplorer: () -> Unit,
    onSkip: () -> Unit,
    onToggleAllowed: (File) -> Unit,
    onSelectionFinished: () -> Unit,
    onStorageSelected: (Int) -> Unit
) {
    var showDirectoryPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val showMessage = rememberAppMessage()

    val hasMediaPermission = uiState.mediaPermissionGranted
    val canOpenDirectoryPicker = hasMediaPermission

    LaunchedEffect(canOpenDirectoryPicker) {
        if (canOpenDirectoryPicker) {
            onPrimeExplorer()
        }
    }

    PermissionPageLayout(
        title = stringResource(R.string.setup_excluded_folders_title),
        description = stringResource(R.string.setup_excluded_folders_description),
        buttonText = stringResource(R.string.setup_choose_folders_ignore),
        buttonEnabled = canOpenDirectoryPicker,
        onGrantClicked = {
            if (canOpenDirectoryPicker) {
                showDirectoryPicker = true
                onOpenExplorer()
            } else {
                showMessage(context.getString(R.string.toast_grant_storage_first))
            }
        },
        icons = persistentListOf(
            R.drawable.rounded_folder_24,
            R.drawable.rounded_music_note_24,
            R.drawable.rounded_create_new_folder_24,
            R.drawable.rounded_folder_open_24,
            R.drawable.rounded_audio_file_24
        )
    ) {
        TextButton(onClick = onSkip) {
            Text(stringResource(R.string.skip_for_now), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }

    FileExplorerDialog(
        visible = showDirectoryPicker,
        currentPath = currentPath,
        directoryChildren = directoryChildren,
        availableStorages = availableStorages,
        selectedStorageIndex = selectedStorageIndex,
        isLoading = uiState.isLoadingDirectories,
        isPriming = isExplorerPriming,
        isReady = isExplorerReady,
        isCurrentDirectoryResolved = isCurrentDirectoryResolved,
        isAtRoot = isAtRoot,
        rootDirectory = explorerRoot,
        onNavigateTo = onNavigateTo,
        onNavigateUp = onNavigateUp,
        onNavigateHome = { onNavigateTo(explorerRoot) },
        onToggleAllowed = onToggleAllowed,
        onRefresh = onRefresh,
        onStorageSelected = onStorageSelected,
        onDone = {
            onSelectionFinished()
            showDirectoryPicker = false
        },
        onDismiss = {
            onSelectionFinished()
            showDirectoryPicker = false
        }
    )
}

sealed class SetupPage {
    object Welcome : SetupPage()
    object NotificationAccess : SetupPage()
    object MediaPermission : SetupPage()
    object BackupRestore : SetupPage()
    object DirectorySelection : SetupPage()
    object ExternalServices : SetupPage()
    object ThemeSelection : SetupPage()
    object BatteryOptimization : SetupPage()
    object Finish : SetupPage()
}

private fun buildSetupPages(includeDirectorySelection: Boolean): List<SetupPage> = buildList {
    add(SetupPage.Welcome)
    add(SetupPage.NotificationAccess)
    add(SetupPage.MediaPermission)
    add(SetupPage.BackupRestore)
    if (includeDirectorySelection) add(SetupPage.DirectorySelection)
    add(SetupPage.ExternalServices)
    add(SetupPage.ThemeSelection)
    add(SetupPage.BatteryOptimization)
    add(SetupPage.Finish)
}

private fun firstBlockedForwardPageIndex(
    pages: List<SetupPage>,
    fromPageIndex: Int,
    toPageIndex: Int,
    context: Context,
    uiState: SetupUiState
): Int? {
    if (toPageIndex <= fromPageIndex) return null
    return (fromPageIndex until toPageIndex).firstOrNull { pageIndex ->
        !isPermissionGateSatisfied(context, pages[pageIndex], uiState)
    }
}

private fun isPermissionGateSatisfied(
    context: Context,
    page: SetupPage,
    uiState: SetupUiState
): Boolean {
    return when (page) {
        // Media access only feeds local tag/.lrc scanning, so it can be skipped; notification
        // access is what makes the media-session listener work at all.
        SetupPage.MediaPermission -> true
        SetupPage.NotificationAccess -> {
            uiState.notificationAccessGranted || hasNotificationAccessNow(context)
        }
        else -> true
    }
}

private fun allRequiredPermissionsGrantedNow(context: Context): Boolean =
    hasNotificationAccessNow(context)

private fun hasNotificationAccessNow(context: Context): Boolean =
    ExternalMediaMonitor.hasNotificationAccess(context)

@Composable
fun WelcomePage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.setup_welcome_prefix),
                style = MaterialTheme.typography.emphasized.displayLarge.copy(
                    fontSize = 42.sp,
                    lineHeight = 1.1.em
                ),
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = RoundedSans,
                    fontSize = 46.sp,
                    color = MaterialTheme.colorScheme.primary,
                    lineHeight = 1.1.em
                ),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 2.dp,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.setup_beta_symbol),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = stringResource(R.string.setup_beta_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            SetupLanguagePill()
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(20.dp))
        ){
            MaterialYouVectorDrawable(
                modifier = Modifier.fillMaxSize(),
                drawableResId = R.drawable.welcome_art
            )
            SineWaveLine(
                modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(32.dp)
                .padding(horizontal = 8.dp)
                .padding(bottom = 4.dp),
                animate = true,
                color = MaterialTheme.colorScheme.surface,
                alpha = 0.95f,
                strokeWidth = 16.dp,
                amplitude = 4.dp,
                waves = 7.6f,
                phase = 0f
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(22.dp)
                    .background(color = MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 4.dp)
            ){

            }
            SineWaveLine(
                modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(32.dp)
                .padding(horizontal = 8.dp)
                .padding(bottom = 4.dp),
                animate = true,
                color = MaterialTheme.colorScheme.primary,
                alpha = 0.95f,
                strokeWidth = 4.dp,
                amplitude = 4.dp,
                waves = 7.6f,
                phase = 0f
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.setup_intro_body), style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * Language shortcut on the welcome page, so the rest of onboarding can be read in the chosen
 * language. It opens the same in-app picker as Settings > Appearance and stores the choice through
 * [AppLocaleManager], so both places agree. Recreating the activity applies the locale; the pager
 * state survives it, so the user stays on this page. AppLocaleManager also moves the application
 * context over, so text the retained SetupViewModel builds (restore progress, toasts) follows.
 */
@Composable
private fun SetupLanguagePill() {
    val context = LocalContext.current
    var showLanguageSheet by remember { mutableStateOf(false) }
    // recreate() rebuilds this composition after a change, so reading the tag once is enough.
    val currentLanguageTag = remember(context) { AppLocaleManager.currentLanguageTag(context) }
    val currentLanguage = remember(currentLanguageTag) {
        AppLanguage.entries.firstOrNull { it.tag == currentLanguageTag } ?: AppLanguage.SYSTEM
    }
    val languageLabel = stringResource(R.string.setcat_app_language_label)
    val currentLanguageName = stringResource(currentLanguage.labelRes)

    Surface(
        onClick = { showLanguageSheet = true },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.semantics {
            contentDescription = languageLabel
            stateDescription = currentLanguageName
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Language,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = currentLanguageName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (showLanguageSheet) {
        val languageOptions = remember(context) { AppLanguage.getLanguageOptions(context) }
        OptionSelectionSheet(
            title = languageLabel,
            options = languageOptions,
            selectedKey = currentLanguageTag,
            onSelect = { tag ->
                showLanguageSheet = false
                if (tag != currentLanguageTag) {
                    AppLocaleManager.applyLanguage(context, tag)
                    (context as? Activity)?.recreate()
                }
            },
            onDismiss = { showLanguageSheet = false }
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MediaPermissionPage(
    uiState: SetupUiState,
    onPermissionStateUpdated: () -> Unit,
    onSkip: () -> Unit
) {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    val permissionState = rememberMultiplePermissionsState(permissions = permissions)
    val mediaIcons = persistentListOf(
        R.drawable.rounded_music_note_24,
        R.drawable.rounded_album_24,
        R.drawable.rounded_library_music_24,
        R.drawable.rounded_artist_24,
        R.drawable.rounded_playlist_play_24
    )

    val isGranted = uiState.mediaPermissionGranted || permissionState.allPermissionsGranted

    LaunchedEffect(permissionState.allPermissionsGranted) {
        onPermissionStateUpdated()
    }

    PermissionPageLayout(
        title = stringResource(R.string.setup_permission_media_title),
        granted = isGranted,
        description = stringResource(R.string.setup_permission_media_description),
        buttonText = if (isGranted) stringResource(R.string.setup_permission_granted) else stringResource(R.string.setup_grant_media_permission),
        buttonEnabled = !isGranted,
        icons = mediaIcons,
        onGrantClicked = {
            if (!isGranted) {
                permissionState.launchMultiplePermissionRequest()
            }
        }
    ) {
        if (!isGranted) {
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.skip_for_now), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * Android only exposes other apps' media sessions to a notification listener, so this is the one
 * permission CarpeLines cannot work without. There is no runtime dialog: the user grants it on the
 * system page, and the page flips to granted when they come back.
 */
@Composable
fun NotificationAccessPage(uiState: SetupUiState) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasAccess by remember { mutableStateOf(ExternalMediaMonitor.hasNotificationAccess(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAccess = ExternalMediaMonitor.hasNotificationAccess(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isGranted = uiState.notificationAccessGranted || hasAccess
    val icons = persistentListOf(
        R.drawable.rounded_notifications_active_24,
        R.drawable.rounded_play_arrow_24,
        R.drawable.rounded_lyrics_24,
        R.drawable.rounded_music_note_24,
        R.drawable.rounded_album_24
    )

    PermissionPageLayout(
        title = stringResource(R.string.external_lyrics_permission_title),
        granted = isGranted,
        description = stringResource(R.string.external_lyrics_permission_body),
        buttonText = if (isGranted) stringResource(R.string.setup_permission_granted) else stringResource(R.string.external_lyrics_grant_access),
        buttonEnabled = !isGranted,
        icons = icons,
        onGrantClicked = {
            if (!isGranted) {
                ExternalMediaMonitor.openNotificationAccessSettings(context)
            }
        }
    )
}

@Composable
fun BatteryOptimizationPage(
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val showMessage = rememberAppMessage()
    val lifecycleOwner = LocalLifecycleOwner.current
    var isIgnoringOptimizations by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }

    // Re-check when the user comes back from the system dialog or settings.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isIgnoringOptimizations = isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val icons = persistentListOf(
        R.drawable.rounded_all_inclusive_24,
        R.drawable.rounded_headphones_24,
        R.drawable.rounded_play_arrow_24,
        R.drawable.rounded_timer_24,
        R.drawable.rounded_music_note_24
    )

    PermissionPageLayout(
        title = stringResource(R.string.setup_battery_optimization_title),
        granted = isIgnoringOptimizations,
        description = stringResource(R.string.setup_battery_optimization_description),
        buttonText = if (isIgnoringOptimizations) stringResource(R.string.setup_permission_granted) else stringResource(R.string.setup_disable_battery_optimization),
        buttonEnabled = !isIgnoringOptimizations,
        icons = icons,
        onGrantClicked = {
            if (!isIgnoringOptimizations && !openBatteryOptimizationSettings(context)) {
                showMessage(context.getString(R.string.setcat_toast_battery_settings_unavailable))
            }
        }
    ) {
        if (!isIgnoringOptimizations) {
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.skip_for_now), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BackupRestorePage(
    uiState: SetupUiState,
    onImportClicked: () -> Unit,
    onSkip: () -> Unit
) {
    val isBusy = uiState.isInspectingBackup || uiState.isRestoringBackup
    val progress = uiState.backupTransferProgress

    PermissionPageLayout(
        title = stringResource(R.string.setup_backup_have_title),
        description = stringResource(R.string.setup_backup_have_description),
        buttonText = when {
            uiState.isInspectingBackup -> stringResource(R.string.setup_inspecting_backup)
            uiState.isRestoringBackup -> stringResource(R.string.setup_restoring_backup)
            else -> stringResource(R.string.setup_import_backup)
        },
        buttonEnabled = !isBusy,
        icons = persistentListOf(
            R.drawable.rounded_upload_file_24,
            R.drawable.rounded_playlist_play_24,
            R.drawable.rounded_settings_24,
            R.drawable.rounded_lyrics_24,
            R.drawable.rounded_monitoring_24
        ),
        onGrantClicked = onImportClicked
    ) {
        AnimatedVisibility(
            visible = uiState.isInspectingBackup || progress != null
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (uiState.isInspectingBackup && progress == null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LoadingIndicator(modifier = Modifier.size(20.dp))
                            Text(
                                text = stringResource(R.string.setup_checking_backup),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (progress != null) {
                        Text(
                            text = progress.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = progress.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LinearWavyProgressIndicator(
                            progress = { progress.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        TextButton(
            onClick = onSkip,
            enabled = !uiState.isRestoringBackup
        ) {
            Text(stringResource(R.string.skip_not_now), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private data class ThemeOptionItem(
    val mode: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val recommended: Boolean = false
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionPage(
    uiState: SetupUiState,
    onModeSelected: (String) -> Unit
) {
    val themeOptions = listOf(
        ThemeOptionItem(
            mode = AppThemeMode.DARK,
            title = stringResource(R.string.setup_theme_dark_title),
            description = stringResource(R.string.setup_theme_dark_description),
            icon = Icons.Rounded.DarkMode,
            recommended = true
        ),
        ThemeOptionItem(
            mode = AppThemeMode.LIGHT,
            title = stringResource(R.string.setup_theme_light_title),
            description = stringResource(R.string.setup_theme_light_description),
            icon = Icons.Outlined.LightMode
        ),
        ThemeOptionItem(
            mode = AppThemeMode.FOLLOW_SYSTEM,
            title = stringResource(R.string.setup_theme_follow_title),
            description = stringResource(R.string.setup_theme_follow_description),
            icon = Icons.Rounded.PhoneAndroid
        )
    )

    // fillMaxSize before verticalScroll keeps the viewport as the minimum height, so SpaceBetween
    // still spreads the content on tall screens while short screens and large fonts can scroll.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.setup_theme_title),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = RoundedSans,
                    fontSize = 32.sp
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.setup_theme_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // The top padding only shows when the page overflows and SpaceBetween leaves no gap.
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        ) {
            themeOptions.forEach { option ->
                ThemeModeOptionCard(
                    option = option,
                    selected = uiState.appThemeMode == option.mode,
                    onClick = { onModeSelected(option.mode) }
                )
            }

            Text(
                text = stringResource(R.string.setup_theme_footer),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeOptionCard(
    option: ThemeOptionItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null,
                        tint = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (option.recommended) {
                    Surface(
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        contentColor = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.setup_recommended),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier.size(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExternalServicesPage(
    uiState: SetupUiState,
    onExternalArtistImagesChanged: (Boolean) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.setup_external_services_title),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = RoundedSans,
                    fontSize = 32.sp
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.setup_external_services_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        ) {
            ExternalServiceToggleCard(
                title = stringResource(R.string.setup_external_artist_images_title),
                description = stringResource(R.string.setup_external_artist_images_description),
                checked = uiState.externalArtistImagesEnabled,
                onCheckedChange = onExternalArtistImagesChanged,
                icon = { Icon(painterResource(R.drawable.rounded_artist_24), null) }
            )

            Text(
                text = stringResource(R.string.setup_external_services_footer),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun ExternalServiceToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: @Composable () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = M3EShape.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            // The whole card is the switch: `toggleable` reports the checked state to a screen
            // reader and the inner Switch is the visual affordance only.
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    role = Role.Switch,
                    onValueChange = onCheckedChange
                )
                .padding(horizontal = M3ESpacing.large, vertical = M3ESpacing.large),
            horizontalArrangement = Arrangement.spacedBy(M3ESpacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (checked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = if (checked) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
                shape = M3EShape.large,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    icon()
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(M3ESpacing.extraSmall)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            M3ESwitch(
                checked = checked,
                onCheckedChange = null
            )
        }
    }
}

@Composable
fun FinishPage() {
    val finishIcons = persistentListOf(
        R.drawable.rounded_check_circle_24,
        R.drawable.round_favorite_24,
        R.drawable.rounded_celebration_24,
        R.drawable.round_favorite_24,
        R.drawable.rounded_explosion_24
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = stringResource(R.string.setup_all_set_title), style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        PermissionIconCollage(
            modifier = Modifier.height(230.dp),
            icons = finishIcons
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.setup_all_set_body), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun PermissionPageLayout(
    title: String,
    granted: Boolean = false,
    description: String,
    buttonText: String,
    icons: ImmutableList<Int>,
    buttonEnabled: Boolean = true,
    onGrantClicked: () -> Unit,
    content: @Composable () -> Unit = {}
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompactHeight = maxHeight < 640.dp
        val horizontalPadding = if (maxWidth < 360.dp) 20.dp else 24.dp
        val verticalPadding = if (isCompactHeight) 18.dp else 24.dp
        val contentSpacing = if (isCompactHeight) 14.dp else 20.dp
        val titleSize = if (maxWidth < 360.dp || isCompactHeight) 29.sp else 32.sp
        val footerSpacing = if (isCompactHeight) 10.dp else 16.dp

        // Header and actions take their natural height; the icon collage gets what is left, so
        // it fills tall screens and shrinks (down to a readable minimum) on short ones instead of
        // pushing the button off screen. Only if even the minimum does not fit does the page
        // scroll. Keep lazy lists and nested vertical scrolls out of these slots: the header and
        // footer are measured with unbounded height.
        HeroFillLayout(
            viewportHeight = maxHeight - verticalPadding * 2,
            spacing = contentSpacing,
            minHeroHeight = if (isCompactHeight) 120.dp else 160.dp,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            header = {
                // fillMaxWidth: HeroFillLayout places slots at x = 0, so a narrower column would
                // pull short centered titles to the start.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!isCompactHeight) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = RoundedSans,
                            fontSize = titleSize
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(if (isCompactHeight) 10.dp else 16.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            hero = {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // The collage sizes its icons from `height`, so pass the space it really has.
                    PermissionIconCollage(
                        icons = icons,
                        height = minOf(maxHeight, 220.dp)
                    )
                }
            },
            footer = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    content()
                    Spacer(modifier = Modifier.height(footerSpacing))
                    Button(
                        onClick = onGrantClicked,
                        enabled = buttonEnabled,
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
                    ) {
                        // Long labels (and large font scales) wrap instead of being cut off; the
                        // footer is measured at its natural height, so the collage gives way.
                        AnimatedContent(targetState = granted, label = "ButtonAnim") { isGranted ->
                            if (isGranted) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Check, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(buttonText, textAlign = TextAlign.Center)
                                }
                            } else {
                                Text(
                                    text = buttonText,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(footerSpacing))
                }
            }
        )
    }
}

/**
 * Stacks [header], [hero] and [footer] vertically. Header and footer are measured at their
 * natural height; the hero receives the remaining part of [viewportHeight] (never less than
 * [minHeroHeight]). Works inside a vertical scroll, where a weighted Column cannot.
 */
@Composable
private fun HeroFillLayout(
    viewportHeight: Dp,
    spacing: Dp,
    minHeroHeight: Dp,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    hero: @Composable () -> Unit,
    footer: @Composable () -> Unit,
) {
    Layout(
        contents = listOf(header, hero, footer),
        modifier = modifier
    ) { (headerMeasurables, heroMeasurables, footerMeasurables), constraints ->
        val width = constraints.maxWidth
        val looseWidth = Constraints(maxWidth = width)
        val headerPlaceables = headerMeasurables.map { it.measure(looseWidth) }
        val footerPlaceables = footerMeasurables.map { it.measure(looseWidth) }
        val spacingPx = spacing.roundToPx()
        val headerHeight = headerPlaceables.sumOf { it.height }
        val footerHeight = footerPlaceables.sumOf { it.height }
        val heroHeight = (viewportHeight.roundToPx() - headerHeight - footerHeight - spacingPx * 2)
            .coerceAtLeast(minHeroHeight.roundToPx())
        val heroPlaceables = heroMeasurables.map {
            it.measure(Constraints.fixed(width, heroHeight))
        }
        val totalHeight = headerHeight + spacingPx + heroHeight + spacingPx + footerHeight
        layout(width, totalHeight) {
            var y = 0
            headerPlaceables.forEach { it.placeRelative(0, y); y += it.height }
            y += spacingPx
            heroPlaceables.forEach { it.placeRelative(0, y) }
            y += heroHeight + spacingPx
            footerPlaceables.forEach { it.placeRelative(0, y); y += it.height }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SetupRestoreDialog(
    plan: RestorePlan,
    inProgress: Boolean,
    progress: BackupTransferProgressUpdate?,
    onDismiss: () -> Unit,
    onSelectionChanged: (Set<BackupSection>) -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    val dateText = remember(plan.manifest.createdAt) {
        SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
            .format(Date(plan.manifest.createdAt))
    }
    val availableModules = remember(plan.availableModules) {
        plan.availableModules.toList().sortedBy { it.ordinal }
    }

    Dialog(
        onDismissRequest = {
            if (!inProgress) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !inProgress,
            dismissOnClickOutside = !inProgress,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                bottomBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = onDismiss,
                                enabled = !inProgress,
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text(stringResource(R.string.cancel), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Button(
                                onClick = onConfirm,
                                enabled = plan.selectedModules.isNotEmpty() && !inProgress,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                if (inProgress) {
                                    LoadingIndicator(modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.restoring))
                                } else {
                                    Icon(Icons.Rounded.Restore, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.restore_selected))
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.setup_restore_backup_title),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = RoundedSans
                        ),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.setup_restore_backup_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.setup_modules_selected_format,
                                    plan.selectedModules.size,
                                    plan.availableModules.size
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.setup_backup_created_format, dateText),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(
                                    R.string.setup_backup_from_version,
                                    plan.manifest.appVersion.ifEmpty { context.getString(R.string.unknown_version) }
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (progress != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = progress.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = progress.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                LinearWavyProgressIndicator(
                                    progress = { progress.progress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    if (plan.warnings.isNotEmpty()) {
                        plan.warnings.forEach { warning ->
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = warning,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(availableModules) { section ->
                            val detail = plan.moduleDetails[section]
                            val selected = section in plan.selectedModules
                            SetupRestoreSectionRow(
                                section = section,
                                detail = detail,
                                selected = selected,
                                enabled = !inProgress,
                                onClick = {
                                    val newSelection = if (selected) {
                                        plan.selectedModules - section
                                    } else {
                                        plan.selectedModules + section
                                    }
                                    onSelectionChanged(newSelection)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupRestoreSectionRow(
    section: BackupSection,
    detail: io.github.jayfunc.carpelines.data.backup.model.ModuleRestoreDetail?,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onClick() },
                enabled = enabled
            )

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(section.iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(section.labelRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(section.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = detail?.entryCount?.let { "$it" } ?: "-",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * A floating bottom bar with an expressive design inspired by Material 3,
 * including an animated sine wave along the top edge.
 *
 * @param modifier Modifier for the Composable.
 * @param pagerState The Pager state used to show the page indicator.
 * @param onNextClicked Lambda invoked when the "Next" button is pressed.
 * @param onFinishClicked Lambda invoked when the "Finish" button is pressed.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun SetupBottomBar(
    modifier: Modifier = Modifier,
    animated: Boolean = false,
    pagerState: PagerState,
    onNextClicked: () -> Unit,
    onFinishClicked: () -> Unit,
    isNextButtonEnabled: Boolean,
    isFinishButtonEnabled: Boolean
) {
    val morphAnimationSpec = tween<Float>(durationMillis = 600, easing = FastOutSlowInEasing)
    val rotationAnimationSpec = tween<Float>(durationMillis = 900, easing = FastOutSlowInEasing)

    val targetShapeValues = when (pagerState.currentPage % 3) {
        0 -> listOf(50f, 50f, 50f, 50f)
        1 -> listOf(26f, 26f, 26f, 26f)
        else -> listOf(18f, 50f, 18f, 50f)
    }

    val animatedTopStart by animateFloatAsState(targetShapeValues[0], morphAnimationSpec, label = "TopStart")
    val animatedTopEnd by animateFloatAsState(targetShapeValues[1], morphAnimationSpec, label = "TopEnd")
    val animatedBottomStart by animateFloatAsState(targetShapeValues[2], morphAnimationSpec, label = "BottomStart")
    val animatedBottomEnd by animateFloatAsState(targetShapeValues[3], morphAnimationSpec, label = "BottomEnd")

    val animatedRotation by animateFloatAsState(
        targetValue = pagerState.currentPage * 360f,
        animationSpec = rotationAnimationSpec,
        label = "Rotation"
    )

    val shape = RoundedCornerShape(
        topEnd = 38.dp,
        topStart = 38.dp,
        bottomEnd = 0.dp,
        bottomStart = 0.dp
    )

    Surface(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = shape, clip = true),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = AbsoluteSmoothCornerShape(
            cornerRadiusTR = 36.dp,
            smoothnessAsPercentTL = 60,
            cornerRadiusTL = 36.dp,
            smoothnessAsPercentBR = 60,
            cornerRadiusBR = 0.dp,
            smoothnessAsPercentBL = 60,
            cornerRadiusBL = 0.dp,
            smoothnessAsPercentTR = 60
        )
    ) {
        // The app draws edge-to-edge and Scaffold does not inset its bottomBar slot, so keep the
        // step text and FAB above the system navigation bar while the surface still runs under it.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState = pagerState.currentPage,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInVertically { height -> height } + fadeIn()).togetherWith(slideOutVertically { height -> -height } + fadeOut())
                        } else {
                            (slideInVertically { height -> -height } + fadeIn()).togetherWith(slideOutVertically { height -> height } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    },
                    label = "StepTextAnimation"
                ) { targetPage ->
                    if (targetPage == 0) {
                        Text(
                            text = stringResource(R.string.setup_lets_go),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Text(
                            text = stringResource(
                                R.string.setup_step_format,
                                targetPage,
                                pagerState.pageCount - 1
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                val isLastPage = pagerState.currentPage == pagerState.pageCount - 1
                val isPrimaryButtonEnabled = if (isLastPage) isFinishButtonEnabled else isNextButtonEnabled
                val containerColor = if (!isPrimaryButtonEnabled) {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
                val contentColor = if (!isPrimaryButtonEnabled) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }

                MediumExtendedFloatingActionButton(
                    onClick = if (isLastPage) onFinishClicked else onNextClicked,
                    shape = AbsoluteSmoothCornerShape(
                        cornerRadiusTL = animatedTopStart.toInt().dp,
                        smoothnessAsPercentTR = 60,
                        cornerRadiusTR = animatedTopEnd.toInt().dp,
                        smoothnessAsPercentTL = 60,
                        cornerRadiusBL = animatedBottomStart.toInt().dp,
                        smoothnessAsPercentBL = 60,
                        cornerRadiusBR = animatedBottomEnd.toInt().dp,
                        smoothnessAsPercentBR = 60,
                    ),
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    containerColor = containerColor,
                    contentColor = contentColor,
                    modifier = Modifier
                        .rotate(animatedRotation)
                        .padding(end = 0.dp)
                ) {
                    AnimatedContent(
                        modifier = Modifier.rotate(-animatedRotation),
                        targetState = pagerState.currentPage < pagerState.pageCount - 1,
                        transitionSpec = {
                            ContentTransform(
                                targetContentEnter = fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.9f, animationSpec = tween(220, delayMillis = 90)),
                                initialContentExit = fadeOut(animationSpec = tween(90)) + scaleOut(targetScale = 0.9f, animationSpec = tween(90))
                            ).using(SizeTransform(clip = false))
                        },
                        label = "AnimatedFabIcon"
                    ) { isNextPage ->
                        if (isNextPage) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = stringResource(R.string.cd_next_step))
                        } else {
                            if (isFinishButtonEnabled) {
                                Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.cd_finish))
                            } else {
                                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close))
                            }
                        }
                    }
                }
            }
        }
    }
}

