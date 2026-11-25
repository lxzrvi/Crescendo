package com.lxzrvi.crescendo.ui.onboarding

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lxzrvi.crescendo.ui.components.ArrowRightIcon
import com.lxzrvi.crescendo.ui.components.CrescendoMark
import com.lxzrvi.crescendo.ui.components.GithubIcon
import com.lxzrvi.crescendo.ui.components.LockIcon
import com.lxzrvi.crescendo.ui.components.StarIcon
import com.lxzrvi.crescendo.ui.components.WaveformIcon
import com.lxzrvi.crescendo.ui.theme.Accent
import com.lxzrvi.crescendo.ui.theme.AmoledBlack
import com.lxzrvi.crescendo.ui.theme.BorelFamily
import com.lxzrvi.crescendo.ui.theme.TextPrimary
import com.lxzrvi.crescendo.ui.theme.TextSecondary

private const val REPO_URL = "https://github.com/lxzrvi/Crescendo"
internal const val WELCOME_MARK_SIZE_DP = 68f

@Composable
fun WelcomeContent(
    onGetStarted: () -> Unit,
    onMarkPositioned: (Offset) -> Unit = {},
) {
    val context = LocalContext.current
    // Borel has a very large descent; keep the footer compact without clipping larger text.
    val footerHeight = (18f * LocalDensity.current.fontScale).dp

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

            // The larger mark sits to the right, centered against both lines as one group.
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val textWidth = (maxWidth.value - WELCOME_MARK_SIZE_DP - 22f).coerceAtLeast(1f)
                val fontScale = LocalDensity.current.fontScale
                val headerScale = (textWidth / (242f * fontScale)).coerceIn(0.35f, 1f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row {
                            Text(
                                text = "WELCOME",
                                fontSize = 38.sp * headerScale,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 2.sp * headerScale,
                                color = TextPrimary,
                                modifier = Modifier.alignByBaseline(),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "TO",
                                fontSize = 20.sp * headerScale,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                modifier = Modifier.alignByBaseline(),
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Crescendo",
                            fontFamily = BorelFamily,
                            fontSize = 44.sp * headerScale,
                            color = TextPrimary,
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    CrescendoMark(
                        modifier = Modifier
                            .offset(y = (-12).dp)
                            .size(WELCOME_MARK_SIZE_DP.dp)
                            .onGloballyPositioned { coordinates ->
                                val origin = coordinates.positionInRoot()
                                onMarkPositioned(
                                    origin + Offset(
                                        coordinates.size.width / 2f,
                                        coordinates.size.height / 2f,
                                    )
                                )
                            },
                        tint = TextPrimary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Section(
                icon = { WaveformIcon(modifier = Modifier.size(16.dp), tint = Accent) },
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
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 5.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(ONBOARDING_ACTION_RADIUS_DP.dp),
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
                    ArrowRightIcon(modifier = Modifier.size(16.dp), tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Same bottom safe-area position as the permission indicator; baselines stay aligned.
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(footerHeight),
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

/** Subtle tinted fill, without a border. */
@Composable
private fun GithubStarButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(AmoledBlack.copy(alpha = 0.32f))
            .background(
                Brush.horizontalGradient(
                    listOf(Accent.copy(alpha = 0.25f), Accent.copy(alpha = 0.10f))
                )
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
            GithubIcon(modifier = Modifier.size(20.dp), tint = Color.White)
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
        color = TextPrimary,
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
