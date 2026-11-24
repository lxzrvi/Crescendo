package com.lxzrvi.crescendo.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lxzrvi.crescendo.ui.components.CrescendoLoader
import com.lxzrvi.crescendo.ui.theme.AmoledBlack

/** Loader screen — real library UI comes next. */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack),
        contentAlignment = Alignment.Center,
    ) {
        CrescendoLoader(modifier = Modifier.size(88.dp))
    }
}
