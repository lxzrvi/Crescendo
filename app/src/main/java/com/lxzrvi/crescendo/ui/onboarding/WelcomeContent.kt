package com.lxzrvi.crescendo.ui.onboarding

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * Post-permission welcome/info page: brand story with vector icons,
 * privacy note, GitHub support pill and a pinned get-started button.
 */
@Composable
fun WelcomeContent(onGetStarted: () -> Unit) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {

        // Scrollable brand content (upar, status bar ke neeche)
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

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "WELCOME",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TO",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Crescendo",
                    fontFamily = BorelFamily,
                    fontSize = 40.sp,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.width(10.dp))
                CrescendoMark(
                    modifier = Modifier.size(26.dp),
                    tint = TextPrimary, // off-white
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

            OutlinedButton(
                onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL)))
                    } catch (_: ActivityNotFoundException) {
                    }
                },
                modifier = Modifier.height(46.dp),
                shape = RoundedCornerShape(50), // compact pill
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StarIcon(modifier = Modifier.size(14.dp), tint = Accent)
                    Text(
                        text = "STAR ON GITHUB",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        color = Accent,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Pinned bottom: get started + footer (screen ke sabse niche)
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

            // Crescendo aur lxzrvi ek hi line pe aligned
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "Crescendo",
                    fontFamily = BorelFamily,
                    fontSize = 13.sp,
                    color = TextSecondary,
                )
                Text(
                    text = "lxzrvi \u2218 2026",
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
            }
        }
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
