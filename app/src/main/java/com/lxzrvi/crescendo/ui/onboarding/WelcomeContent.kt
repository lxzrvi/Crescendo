package com.lxzrvi.crescendo.ui.onboarding

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.lxzrvi.crescendo.ui.theme.Accent
import com.lxzrvi.crescendo.ui.theme.AmoledBlack
import com.lxzrvi.crescendo.ui.theme.TextPrimary
import com.lxzrvi.crescendo.ui.theme.TextSecondary

private const val REPO_URL = "https://github.com/lxzrvi/Crescendo"

/**
 * Post-permission welcome/info page: scrollable brand story,
 * privacy note, GitHub support nudge and the big get-started button.
 */
@Composable
fun WelcomeContent(onGetStarted: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "WELCOME TO CRESCENDO",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.5.sp,
            color = TextPrimary,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Section(
            heading = "1. IN SYNC WITH THE RHYTHM \uD83C\uDFB5",
            body = "Just like a crescendo rises from soft to powerful, Crescendo is here to lift your mood through the power of music.\n\nListen to songs from your own music library, discover your favorite rhythms, and let every beat bring a little more joy to your day.",
        )

        Divider()

        Section(
            heading = "2. YOUR PRIVACY MATTERS \uD83D\uDD12",
            body = "Your music stays yours. Crescendo accesses your local audio library to play your songs. Your personal music files are not uploaded to our servers.",
        )

        Divider()

        Section(
            heading = "3. SUPPORT THE PROJECT \u2B50",
            body = "Enjoying Crescendo? Show your support by giving us a star on GitHub. Your support motivates us to keep improving the app and making your music experience better.",
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL)))
                } catch (_: ActivityNotFoundException) {
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(
                text = "\u2B50 STAR ON GITHUB",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                letterSpacing = 1.sp,
                color = Accent,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

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
            Text(
                text = "LET'S GET STARTED \u2192",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                letterSpacing = 1.sp,
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "lxzrvi",
            fontSize = 12.sp,
            color = TextSecondary,
        )
    }
}

@Composable
private fun Section(heading: String, body: String) {
    Text(
        text = heading,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = TextPrimary,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = body,
        fontSize = 13.sp,
        lineHeight = 21.sp,
        color = TextSecondary,
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
