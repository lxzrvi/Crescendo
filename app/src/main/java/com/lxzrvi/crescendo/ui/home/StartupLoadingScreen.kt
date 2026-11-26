package com.lxzrvi.crescendo.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lxzrvi.crescendo.ui.components.CrescendoLoader
import com.lxzrvi.crescendo.ui.theme.BorelFamily

@Composable
internal fun StartupLoadingScreen(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier.fillMaxSize().background(colors.background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
            .semantics { contentDescription = "Loading your music" },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CrescendoLoader(Modifier.size(70.dp), color = colors.primary)
            Spacer(Modifier.height(20.dp))
            Text("Crescendo", fontFamily = BorelFamily, color = colors.onBackground,
                fontSize = 30.sp, lineHeight = 42.sp, fontWeight = FontWeight.Normal)
            Text("Preparing your music", color = colors.onBackground.copy(alpha = 0.55f), fontSize = 13.sp)
        }
    }
}
