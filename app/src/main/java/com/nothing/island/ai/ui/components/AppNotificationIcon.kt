package com.nothing.island.ai.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.nothing.island.ai.NothingBorder
import com.nothing.island.ai.NothingDarkGray
import com.nothing.island.ai.NothingWhite

/**
 * AppNotificationIcon
 *
 * Renders an app icon safely with PackageManager lookup and fallback glyph badge.
 */
@Composable
fun AppNotificationIcon(
    packageName: String,
    fallbackChar: Char = 'N',
    size: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageBitmap = remember(packageName) {
        try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 72
            val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 72
            drawable.toBitmap(w, h, Bitmap.Config.ARGB_8888).asImageBitmap()
        } catch (_: Throwable) {
            null
        }
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = packageName,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(4.dp))
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(NothingDarkGray)
                .border(1.dp, NothingBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fallbackChar.toString(),
                color = NothingWhite,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
