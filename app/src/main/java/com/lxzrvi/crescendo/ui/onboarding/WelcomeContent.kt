package com.lxzrvi.crescendo.ui.onboarding

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lxzrvi.crescendo.ui.components.ArrowRightIcon
import com.lxzrvi.crescendo.ui.components.CrescendoMark
import com.lxzrvi.crescendo.ui.components.LockIcon
import com.lxzrvi.crescendo.ui.components.MusicNoteIcon
import com.lxzrvi.crescendo.ui.components.StarIcon
import com.lxzrvi.crescendo.ui.theme.Accent
import com.lxzrvi.crescendo.ui.theme.AmoledBlack
import com.lxzrvi.crescendo.ui.theme.BorelFamily
import com.lxzrvi.crescendo.ui.theme.TextPrimary
import com.lxzrvi.crescendo.ui.theme.TextSecondary

private const val REPO_URL = "https://github.com/lxzrvi/Crescendo"

@Composable
fun WelcomeContent(
    onGetStarted: () -> Unit,
    onMarkPositioned: (Offset) -> Unit = {},
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // WELCOME bada, TO ka color bhi same (TextPrimary), baseline aligned
            Row {
                Text(
                    text = "WELCOME",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    color = TextPrimary,
                    modifier = Modifier.alignByBaseline(),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "TO",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Crescendo left, icon bilkul right me, text ke vertical center me
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Crescendo",
                    fontFamily = BorelFamily,
                    fontSize = 44.sp,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.weight(1f))
                CrescendoMark(
                    modifier = Modifier
                        .size(38.dp)
                        .onGloballyPositioned { onMarkPositioned(it.boundsInRoot().center) },
                    tint = TextPrimary,
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Section(
                icon = { MusicNoteIcon(modifier = Modifier.size(16.dp), tint = Accent) },
                heading = "IN SYNC WITH THE RHYTHM",
                body = "Just like a crescendo rises from soft to powerful, Crescendo is here to lift your mood through the power of music.\n\nListen to songs from your own music library, discover your favorite rhythms, and let every beat bring a little more joy to your day.",
            )

            Divider()

            Section(
                icon = { LockIcon(modifier = Modifier.size(16.dp), tint = Accent) },
                heading = "YOUR PRIVACY MATTERS",
                body = "Your music stays yours. Crescendo accesses your local audio library to play your songs. Your personal music files are not uploaded to our servers.",
            )

            Divider()

            Section(
                icon = { StarIcon(modifier = Modifier.size(16.dp), tint = Accent) },
                heading = "SUPPORT THE PROJECT",
                body = "Enjoying Crescendo? Show your support by giving us a star on GitHub. Your support motivates us to keep improving the app and making your music experience better.",
            )

            Spacer(modifier = Modifier.height(20.dp))

            GithubStarButton(
                onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL)))
                    } catch (_: ActivityNotFoundException) {
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = AmoledBlack,
                ),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "LET'S GET STARTED",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp,
                    )
                    ArrowRightIcon(modifier = Modifier.size(16.dp), tint = AmoledBlack)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Footer: dono texts ki baseline same line pe
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Crescendo",
                    fontFamily = BorelFamily,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    text = "lxzrvi \u2218 2026",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
    }
}

/** Gradient border + tinted fill wala full-width GitHub star button. */
@Composable
private fun GithubStarButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(Accent.copy(alpha = 0.20f), Accent.copy(alpha = 0.05f))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(Accent.copy(alpha = 0.85f), Accent.copy(alpha = 0.20f))
                ),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            StarIcon(modifier = Modifier.size(17.dp), tint = Accent)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Star on GitHub",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Text(
                text = "lxzrvi/Crescendo",
                fontSize = 11.sp,
                color = TextSecondary,
            )
        }
        ArrowRightIcon(modifier = Modifier.size(16.dp), tint = TextPrimary)
    }
}

@Composable
private fun Section(
    icon: @Composable () -> Unit,
    heading: String,
    body: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = heading,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = TextPrimary,
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = body,
        fontSize = 13.sp,
        lineHeight = 21.sp,
        color = TextSecondary,
        modifier = Modifier.padding(start = 24.dp),
    )
}

@Composable
private fun Divider() {
    Spacer(modifier = Modifier.height(18.dp))
    HorizontalDivider(
        thickness = 1.dp,
        color = TextPrimary.copy(alpha = 0.12f),
    )
    Spacer(modifier = Modifier.height(18.dp))
}
