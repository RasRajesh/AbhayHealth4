package com.example.abhayhealth.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.abhayhealth.R

/**
 * Official AbhayHealth Unified Brand Logo Component.
 * Ensures the official launcher logo (medical shield, cross, and vital pulse)
 * is maintained identically and consistently across the entire application:
 * Splash, Welcome, Auth, Headers, TopBars, and Profile.
 */
@Composable
fun AbhayHealthBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: Shape = RoundedCornerShape(percent = 26),
    elevation: Dp = 2.dp,
    showText: Boolean = false,
    showTagline: Boolean = false,
    textColor: Color = MaterialTheme.colorScheme.onBackground
) {
    if (showText) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LogoIconBox(size = size, shape = shape, elevation = elevation)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = textColor
            )
            if (showTagline) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.app_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.75f)
                )
            }
        }
    } else {
        LogoIconBox(
            modifier = modifier,
            size = size,
            shape = shape,
            elevation = elevation
        )
    }
}

@Composable
private fun LogoIconBox(
    size: Dp,
    shape: Shape,
    elevation: Dp,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = elevation,
                shape = shape,
                clip = false,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            ),
        shape = shape,
        color = Color(0xFF0C3848),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF2DD4BF).copy(alpha = 0.5f),
                    Color(0xFF0C3848).copy(alpha = 0.8f)
                )
            )
        )
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_abhay_logo),
            contentDescription = stringResource(id = R.string.app_name),
            modifier = Modifier
                .fillSize()
                .padding(size * 0.08f)
        )
    }
}

/**
 * Compact horizontal badge for TopAppBars and Section Headers.
 */
@Composable
fun AbhayHealthHeaderBadge(
    modifier: Modifier = Modifier,
    iconSize: Dp = 32.dp,
    title: String = stringResource(id = R.string.app_name),
    subtitle: String? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AbhayHealthBrandLogo(
            size = iconSize,
            shape = RoundedCornerShape(8.dp),
            elevation = 1.dp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun Modifier.fillSize(): Modifier = this.then(
    Modifier.padding(0.dp)
)
