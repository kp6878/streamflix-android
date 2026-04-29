package com.example.streamfilx_androidtv.features.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import androidx.compose.material3.TextButton
import com.example.streamfilx_androidtv.navigation.Screen

data class NavTab(
    val label: String,
    val route: String,
)

val TOP_NAV_TABS = listOf(
    NavTab("Home", Screen.Home.route),
    NavTab("Browse", Screen.Browse.route),
    NavTab("Search", Screen.Search.route),
    NavTab("My List", Screen.Library.route),
    NavTab("Downloads", Screen.Downloads.route),
    NavTab("Settings", Screen.Settings.route),
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopNavBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color.Black.copy(alpha = 0.95f)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // App wordmark
            Text(
                text = "STREAMFLIX",
                color = Color(0xFFE50914),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(end = 24.dp),
            )

            TOP_NAV_TABS.forEach { tab ->
                NavTabItem(
                    label = tab.label,
                    isSelected = currentRoute == tab.route,
                    onClick = { onTabSelected(tab.route) },
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavTabItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    val textColor by animateColorAsState(
        targetValue = when {
            isSelected -> Color.White
            isFocused -> Color.White
            else -> Color.White.copy(alpha = 0.6f)
        },
        animationSpec = tween(150),
        label = "tabColor",
    )

    val underlineColor = Color(0xFFE50914)

    Box(
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .drawBehind {
                if (isSelected) {
                    // Red underline for selected tab
                    drawLine(
                        color = underlineColor,
                        start = Offset(0f, size.height - 2.dp.toPx()),
                        end = Offset(size.width, size.height - 2.dp.toPx()),
                        strokeWidth = 2.dp.toPx(),
                    )
                }
                if (isFocused && !isSelected) {
                    // Subtle underline when focused but not selected
                    drawLine(
                        color = Color.White.copy(alpha = 0.4f),
                        start = Offset(0f, size.height - 2.dp.toPx()),
                        end = Offset(size.width, size.height - 2.dp.toPx()),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            },
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}
