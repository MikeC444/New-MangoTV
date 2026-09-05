package com.mangotv.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mangotv.app.ui.theme.MangoBackground
import com.mangotv.app.ui.theme.MangoBrandGradient
import com.mangotv.app.ui.theme.MangoDimens
import com.mangotv.app.ui.theme.TextPrimary

enum class MangoButtonStyle { FILLED, GLASS }

@Composable
fun MangoButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: MangoButtonStyle = MangoButtonStyle.GLASS,
    focusRequester: FocusRequester? = null,
    focusUp: FocusRequester? = null,
    focusDown: FocusRequester? = null,
    bringIntoViewOnFocus: Boolean = true
) {
    val contentColor = if (style == MangoButtonStyle.FILLED) MangoBackground else TextPrimary

    TvFocusSurface(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(MangoDimens.ButtonCornerRadius),
        backgroundColor = if (style == MangoButtonStyle.GLASS) Color.White.copy(alpha = 0.12f) else Color.Transparent,
        backgroundBrush = if (style == MangoButtonStyle.FILLED) MangoBrandGradient else null,
        focusRequester = focusRequester,
        focusUp = focusUp,
        focusDown = focusDown,
        bringIntoViewOnFocus = bringIntoViewOnFocus
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 26.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.height(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
