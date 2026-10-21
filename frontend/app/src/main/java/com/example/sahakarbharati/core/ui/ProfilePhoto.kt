package com.example.sahakarbharati.core.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sahakarbharati.data.DemoPhotos
import com.example.sahakarbharati.ui.theme.RathYatraColors
import java.io.File

@Composable
fun ProfilePhoto(
    photoPath: String?,
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    onClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val bitmap = remember(photoPath, photoPath?.let { File(it).lastModified() }) {
        loadProfileBitmap(context, photoPath)
    }
    val boxModifier = modifier
        .size(size)
        .clip(CircleShape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Profile photo",
            modifier = boxModifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = boxModifier.background(RathYatraColors.Navy),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.32f).sp,
            )
        }
    }
}

@Composable
fun DemoPhotoChoices(
    selectedPath: String,
    boyLabel: String,
    girlLabel: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.Top,
    ) {
        DemoPhotoChoice(
            photoPath = DemoPhotos.BOY,
            label = boyLabel,
            selected = selectedPath == DemoPhotos.BOY,
            size = size,
            onClick = { onSelect(DemoPhotos.BOY) },
        )
        DemoPhotoChoice(
            photoPath = DemoPhotos.GIRL,
            label = girlLabel,
            selected = selectedPath == DemoPhotos.GIRL,
            size = size,
            onClick = { onSelect(DemoPhotos.GIRL) },
        )
    }
}

@Composable
private fun DemoPhotoChoice(
    photoPath: String,
    label: String,
    selected: Boolean,
    size: Dp,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = if (selected) {
                Modifier.border(3.dp, RathYatraColors.Saffron, CircleShape)
            } else {
                Modifier.border(1.dp, RathYatraColors.Outline.copy(alpha = 0.4f), CircleShape)
            },
        ) {
            ProfilePhoto(
                photoPath = photoPath,
                initials = label.take(1),
                size = size,
                onClick = onClick,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) RathYatraColors.BurntSaffron else RathYatraColors.OnSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

fun loadProfileBitmap(context: Context, ref: String?) =
    runCatching {
        if (ref.isNullOrBlank()) return@runCatching null
        val drawableName = DemoPhotos.drawableName(ref)
        if (drawableName != null) {
            val id = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
            if (id == 0) return@runCatching null
            BitmapFactory.decodeResource(context.resources, id)?.asImageBitmap()
        } else if (ref.startsWith("content:") || ref.startsWith("file:")) {
            context.contentResolver.openInputStream(Uri.parse(ref))?.use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        } else {
            BitmapFactory.decodeFile(ref)?.asImageBitmap()
        }
    }.getOrNull()
