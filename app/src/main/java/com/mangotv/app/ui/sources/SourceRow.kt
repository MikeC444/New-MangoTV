package com.mangotv.app.ui.sources

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mangotv.app.data.model.ResolutionTier
import com.mangotv.app.data.model.Stream
import com.mangotv.app.ui.components.TvFocusSurface
import com.mangotv.app.ui.theme.MangoAmber
import com.mangotv.app.ui.theme.MangoCoral
import com.mangotv.app.ui.theme.MangoDimens
import com.mangotv.app.ui.theme.MangoSurfaceHigh
import com.mangotv.app.ui.theme.MangoTangerine
import com.mangotv.app.ui.theme.TextPrimary
import com.mangotv.app.ui.theme.TextSecondary
import com.mangotv.app.ui.theme.TextTertiary

@Composable
fun SourceRow(
    stream: Stream,
    isRecommended: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(MangoDimens.CardCornerRadius)

    Box(modifier = modifier.fillMaxWidth()) {
        TvFocusSurface(
            onClick = onClick,
            shape = shape,
            backgroundColor = MangoSurfaceHigh,
            modifier = Modifier
                .fillMaxWidth()
                .let { if (isRecommended) it.border(BorderStroke(2.dp, MangoAmber), shape) else it },
            bringIntoViewOnFocus = false
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QualityBadge(stream)
                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stream.releaseTitle,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = listOfNotNull(stream.codec, stream.sourceTag, stream.audioTag)
                        .joinToString("  •  ")
                    if (subtitle.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (stream.seeders != null || stream.qualityLabel != null) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            stream.seeders?.let {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.width(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "$it seeders",
                                    color = TextTertiary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Spacer(Modifier.width(14.dp))
                            }
                            stream.qualityLabel?.let {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = MangoAmber,
                                    modifier = Modifier.width(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = it,
                                    color = TextTertiary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(horizontalAlignment = Alignment.End) {
                    stream.sizeLabel?.let {
                        Text(
                            text = it,
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Wifi,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.width(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stream.providerLabel,
                            color = TextTertiary,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(20.dp))

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MangoAmber.copy(alpha = 0.18f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play this source",
                        tint = MangoAmber,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(20.dp)
                    )
                }
            }
        }

        if (isRecommended) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-16).dp, y = (-10).dp)
                    .background(MangoAmber, RoundedCornerShape(percent = 50))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.width(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Recommended",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun QualityBadge(stream: Stream) {
    val color = when (stream.resolutionTier) {
        ResolutionTier.UHD_4K -> MangoAmber
        ResolutionTier.FHD_1080P -> MangoTangerine
        ResolutionTier.HD_720P -> MangoCoral
        ResolutionTier.OTHER -> TextTertiary
    }
    Column(
        modifier = Modifier
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stream.qualityBadge,
            color = color,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        stream.sourceTag?.let {
            Text(
                text = it,
                color = color,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
