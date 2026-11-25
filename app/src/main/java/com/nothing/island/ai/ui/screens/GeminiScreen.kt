package com.nothing.island.ai.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.ui.components.DotMatrixGlyph
import com.nothing.island.ai.ui.components.DotMatrixPatterns
import com.nothing.island.ai.ui.components.IslandCard
import com.nothing.island.ai.ui.components.SectionHeader
import com.nothing.island.ai.ui.theme.NothingDivider
import com.nothing.island.ai.ui.theme.NothingElevatedSurface
import com.nothing.island.ai.ui.theme.NothingMutedText
import com.nothing.island.ai.ui.theme.NothingPrimaryAccent
import com.nothing.island.ai.ui.theme.NothingPrimaryBackground
import com.nothing.island.ai.ui.theme.NothingPrimaryText
import com.nothing.island.ai.ui.theme.NothingSecondaryBackground
import com.nothing.island.ai.ui.theme.NothingSecondaryText
import com.nothing.island.ai.ui.theme.NothingWarning

data class ChatMessage(
    val sender: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: String = "NOW"
)

@Composable
fun GeminiScreen(
    modifier: Modifier = Modifier
) {
    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                sender = "GEMINI 3.8",
                text = "Hello! I am the Dot Island AI companion interface. In Milestone 4.1, this is a clean visual foundation. Gemini API integration will be activated in Milestone 5.",
                isUser = false,
                timestamp = "INIT"
            )
        )
    }

    val inputQuery = remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingPrimaryBackground)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DotMatrixGlyph(
                        pattern = DotMatrixPatterns.SparkleAI,
                        dotSize = 3.dp,
                        spacing = 2.dp,
                        activeColor = NothingPrimaryAccent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GEMINI AI",
                        color = NothingPrimaryText,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NothingElevatedSurface)
                        .border(1.dp, NothingDivider, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "API PENDING",
                        color = NothingWarning,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = "ON-DEVICE ASSISTANT COMPANION",
                color = NothingMutedText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Suggested Action Chips
        SectionHeader(title = "SUGGESTED ACTIONS")

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionChip(
                text = "✦ Explain alerts",
                modifier = Modifier.weight(1f),
                onClick = {
                    messages.add(ChatMessage("USER", "Explain my recent notifications", true))
                    messages.add(ChatMessage("GEMINI 3.8", "Notification integration is scheduled for Milestone 4.2. Once connected, I will provide real-time notification summaries.", false))
                }
            )
            ActionChip(
                text = "✦ Island status",
                modifier = Modifier.weight(1f),
                onClick = {
                    messages.add(ChatMessage("USER", "What is the status of Dot Island?", true))
                    messages.add(ChatMessage("GEMINI 3.8", "Milestone 4.1 Design System is active. M3 Core Overlay Engine is calibrated for Nothing Phone (4a).", false))
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Conversation Area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(message = msg)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Honest Status Disclaimer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0D0D0D))
                .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "NOTE: Visual foundation only. No external API calls are made and no API keys are required in Milestone 4.1.",
                color = NothingMutedText,
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputQuery.value,
                onValueChange = { inputQuery.value = it },
                placeholder = {
                    Text(
                        text = "Ask Gemini or paste prompt...",
                        color = NothingMutedText,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NothingPrimaryAccent,
                    unfocusedBorderColor = NothingDivider,
                    focusedTextColor = NothingPrimaryText,
                    unfocusedTextColor = NothingPrimaryText,
                    focusedContainerColor = NothingElevatedSurface,
                    unfocusedContainerColor = NothingElevatedSurface
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send Button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (inputQuery.value.isNotBlank()) NothingPrimaryAccent else NothingElevatedSurface)
                    .border(1.dp, NothingDivider, RoundedCornerShape(14.dp))
                .clickable(enabled = inputQuery.value.isNotBlank()) {
                    val query = inputQuery.value.trim()
                    messages.add(ChatMessage("USER", query, true))
                    messages.add(ChatMessage("GEMINI 3.8", "Thank you for the prompt: \"$query\". Live Gemini API processing will connect in Milestone 5.", false))
                    inputQuery.value = ""
                },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SEND",
                    color = if (inputQuery.value.isNotBlank()) Color.White else NothingMutedText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun ActionChip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(NothingElevatedSurface)
            .border(1.dp, NothingDivider, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = NothingSecondaryText,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!message.isUser) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(NothingPrimaryAccent))
            }
            Text(
                text = message.sender,
                color = NothingMutedText,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 16.dp
                    )
                )
                .background(if (message.isUser) NothingElevatedSurface else Color(0xFF141414))
                .border(
                    1.dp,
                    if (message.isUser) NothingPrimaryAccent.copy(alpha = 0.5f) else NothingDivider,
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                color = NothingPrimaryText,
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
                lineHeight = 17.sp
            )
        }
    }
}
