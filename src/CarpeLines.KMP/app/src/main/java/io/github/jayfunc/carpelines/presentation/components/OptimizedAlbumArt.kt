package io.github.jayfunc.carpelines.presentation.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Dimension
import coil.size.Size
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.utils.LocalArtworkUri

internal const val MaxSafeAlbumArtDimensionPx = 2048
internal val SafeOriginalAlbumArtSize = Size(MaxSafeAlbumArtDimensionPx, MaxSafeAlbumArtDimensionPx)

internal fun safeAlbumArtTargetSize(targetSize: Size): Size {
    return if (targetSize == Size.ORIGINAL) {
        SafeOriginalAlbumArtSize
    } else {
        targetSize
    }
}

