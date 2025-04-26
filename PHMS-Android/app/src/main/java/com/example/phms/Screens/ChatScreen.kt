package com.example.phms.Screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phms.ChatApiService
import com.example.phms.ChatMessage
import com.example.phms.R
import com.example.phms.ui.theme.PokemonClassicFontFamily
import com.example.phms.ui.theme.pokeChatAssistantBg
import com.example.phms.ui.theme.pokeChatBorderColor
import com.example.phms.ui.theme.pokeChatTextColor
import com.example.phms.ui.theme.pokeChatUserBg
import com.example.phms.ui.theme.pokeYellow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val chatService = remember { ChatApiService() }
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // Get the height of the bottom navigation bar
    val density = LocalDensity.current
    val bottomNavHeight = with(density) { 80.dp.toPx() }
    val bottomNavHeightDp = with(density) { bottomNavHeight.toDp() }
    val settingsLabel = stringResource(R.string.settings)

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top app bar
        TopAppBar(
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.health_chat))
                }
                    },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
            actions = {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = settingsLabel
                    )
                }

            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary, // e.g., pokeBlue
                titleContentColor = MaterialTheme.colorScheme.onPrimary, // e.g., Color.White
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        // Messages area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.chat_welcome_title),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.chat_welcome_message),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { message ->
                        PokemonChatMessageItem(message = message)
                    }

                    if (isLoading) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "AI",
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            shadowElevation = 0.dp,
            shape = RoundedCornerShape(0.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, pokeChatBorderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    placeholder = { Text(stringResource(R.string.type_health_question)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (messageText.isNotBlank() && !isLoading) {
                            sendMessage(
                                chatService = chatService,
                                messageText = messageText,
                                messages = messages,
                                updateMessages = { messages = it },
                                updateIsLoading = { isLoading = it },
                                updateMessageText = { messageText = it },
                                scope = coroutineScope
                            )
                            focusManager.clearFocus()
                        }
                    }),
                    maxLines = 4,
                    shape = RoundedCornerShape(0.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = pokeChatTextColor,
                        unfocusedTextColor = pokeChatTextColor,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = pokeChatBorderColor,
                        unfocusedBorderColor = pokeChatBorderColor.copy(alpha = 0.7f),
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = PokemonClassicFontFamily)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send button
                FloatingActionButton(
                    onClick = {
                        if (messageText.isNotBlank() && !isLoading) {
                            sendMessage(
                                chatService = chatService,
                                messageText = messageText,
                                messages = messages,
                                updateMessages = { messages = it },
                                updateIsLoading = { isLoading = it },
                                updateMessageText = { messageText = it },
                                scope = coroutineScope
                            )
                            focusManager.clearFocus()
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = pokeYellow,
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(0.dp),
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = stringResource(R.string.send_message)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(bottomNavHeightDp))
    }
}

@Composable
fun PokemonChatMessageItem(message: ChatMessage) {
    val isUserMessage = message.role == "user"
    val bubbleColor = if (isUserMessage) pokeChatUserBg else pokeChatAssistantBg
    val alignment = if (isUserMessage) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleShape = RoundedCornerShape(0.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isUserMessage) 48.dp else 8.dp,
                end = if (isUserMessage) 8.dp else 48.dp
            ),
        contentAlignment = alignment
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            border = BorderStroke(2.dp, pokeChatBorderColor),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                fontFamily = PokemonClassicFontFamily,
                fontSize = 14.sp,
                color = pokeChatTextColor
            )
            // Note: The speech bubble tail from the image requires a custom drawable background (9-patch).
            // This implementation uses simple rounded rectangles.
        }
    }
}

private fun sendMessage(
    chatService: ChatApiService,
    messageText: String,
    messages: List<ChatMessage>,
    updateMessages: (List<ChatMessage>) -> Unit,
    updateIsLoading: (Boolean) -> Unit,
    updateMessageText: (String) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    // Add user message
    val userMessage = ChatMessage(role = "user", content = messageText)
    val updatedMessages = messages + listOf(userMessage)
    updateMessages(updatedMessages)
    updateMessageText("")

    // Show loading and send request
    updateIsLoading(true)

    scope.launch {
        chatService.sendMessage(
            messages = updatedMessages,
            onResponse = { response ->
                val assistantMessage = ChatMessage(role = "assistant", content = response)
                updateMessages(updatedMessages + listOf(assistantMessage))
                updateIsLoading(false)
            },
            onError = { error ->
                val errorMessage = ChatMessage(role = "assistant", content = error)
                updateMessages(updatedMessages + listOf(errorMessage))
                updateIsLoading(false)
            }
        )
    }
}

@Composable
fun FormattedText(
    text: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    // Simple formatting for numbered lists, bullet points, bold and italic text
    val formattedText = buildAnnotatedString {
        val lines = text.lines()

        lines.forEach { line ->
            // Check for bold text (** or __ in markdown)
            val boldPattern = "\\*\\*(.+?)\\*\\*|__(.+?)__".toRegex()
            val italicPattern = "\\*(.+?)\\*|_(.+?)_".toRegex()

            var currentIndex = 0

            val isNumberedList = line.trim().matches("^\\d+\\.\\s+.*$".toRegex())

            val isBulletList = line.trim().matches("^[•*-]\\s+.*$".toRegex())

            var workingLine = line

            val boldMatches = boldPattern.findAll(workingLine)
            for (match in boldMatches) {
                val startIndex = match.range.first
                val endIndex = match.range.last + 1
                val content = match.groupValues[1].ifEmpty { match.groupValues[2] }

                append(workingLine.substring(currentIndex, startIndex))
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(content)
                pop()

                currentIndex = endIndex
            }

            if (currentIndex < workingLine.length) {
                append(workingLine.substring(currentIndex))
            }

            if (line != lines.last()) {
                append("\n")
            }
        }
    }

    Text(
        text = formattedText,
        modifier = modifier,
        color = color
    )
}