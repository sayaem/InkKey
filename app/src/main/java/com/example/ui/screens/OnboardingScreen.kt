package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class OnboardingSlide(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badge: String,
    val description: String,
    val isMandatoryDisclosure: Boolean = false
)

@Composable
fun OnboardingScreen(
    onComplete: (profileName: String) -> Unit
) {
    var currentSlide by remember { mutableIntStateOf(0) }
    var profileNameInput by remember { mutableStateOf("My Handwriting") }

    val slides = remember {
        listOf(
            OnboardingSlide(
                title = "Your Personal Handwriting Keyboard",
                subtitle = "Type at digital speed. Keep the character of your handwriting.",
                icon = Icons.Default.Create,
                badge = "Welcome",
                description = "InkKey turns your drawn letter strokes into a valid TrueType font (.ttf) that powers an everyday Android QWERTY keyboard."
            ),
            OnboardingSlide(
                title = "How InkKey Works",
                subtitle = "Four simple steps to your custom digital typeface",
                icon = Icons.Default.TouchApp,
                badge = "Guided Pipeline",
                description = "1. Guided Capture: Draw letters cell by cell.\n2. Font Generation: Compiles vector outlines into a real TTF font.\n3. Everyday Typing: Type standard Unicode in any app.\n4. Visual Sharing: Export handwritten notes as PNG."
            ),
            OnboardingSlide(
                title = "Important: Unicode & Fonts",
                subtitle = "Clear expectations on how third-party apps work",
                icon = Icons.Default.Info,
                badge = "Disclosure",
                description = "Typing with InkKey sends standard Unicode text so friends can read your messages in any app. Other apps choose their own font to render ordinary text. When you want recipients to see your handwriting font visually, use the built-in Handwriting Studio to export and share high-res images.",
                isMandatoryDisclosure = true
            ),
            OnboardingSlide(
                title = "100% Offline & Private",
                subtitle = "No accounts, no cloud, no keylogging",
                icon = Icons.Default.Lock,
                badge = "Privacy Guarantee",
                description = "All handwriting strokes and generated font files stay on your device. InkKey requires no network access and never intercepts, logs, or transmits what you type.",
                isMandatoryDisclosure = true
            ),
            OnboardingSlide(
                title = "Create Your Profile",
                subtitle = "Name your personal handwriting font",
                icon = Icons.Default.Palette,
                badge = "Ready to Begin",
                description = "You can customize and retune your font later in Font Studio."
            )
        )
    }

    val isLastSlide = currentSlide == slides.lastIndex
    val slide = slides[currentSlide]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- Top Bar: Dots & Skip ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in slides.indices) {
                    Box(
                        modifier = Modifier
                            .size(if (i == currentSlide) 20.dp else 8.dp, 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (i == currentSlide) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                }
            }

            if (!slide.isMandatoryDisclosure && !isLastSlide) {
                OutlinedButton(
                    onClick = { currentSlide = slides.lastIndex },
                    modifier = Modifier.testTag("skip_onboarding_button")
                ) {
                    Text("Skip", fontSize = 13.sp)
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        // --- Slide Content ---
        AnimatedContent(
            targetState = currentSlide,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "slide_content"
        ) { targetIndex ->
            val cur = slides[targetIndex]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = cur.icon,
                        contentDescription = cur.title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = cur.badge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = cur.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = cur.subtitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = cur.description,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (targetIndex == slides.lastIndex) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = profileNameInput,
                                onValueChange = { profileNameInput = it },
                                label = { Text("Profile Name") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_name_input")
                            )
                        }
                    }
                }
            }
        }

        // --- Bottom Action Button ---
        Button(
            onClick = {
                if (isLastSlide) {
                    onComplete(profileNameInput.ifBlank { "My Handwriting" })
                } else {
                    currentSlide++
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("onboarding_next_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (isLastSlide) "Get Started" else "Continue",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.size(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
