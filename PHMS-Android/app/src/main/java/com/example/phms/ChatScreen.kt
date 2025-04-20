package com.example.phms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBackClick: () -> Unit
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
    val bottomNavHeight = with(density) { 56.dp.toPx() }
    val bottomNavHeightDp = with(density) { bottomNavHeight.toDp() }

    // Automatically scroll to the bottom when messages change
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top app bar
        TopAppBar(
            title = { Text(stringResource(R.string.health_chat)) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }
        )

        // Messages area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                // Welcome message when there are no messages
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.chat_welcome_title),
                        style = MaterialTheme.typography.headlineSmall
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
                // Chat messages
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                        ChatMessageItem(message = message)
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

        // Input area
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            shadowElevation = 4.dp,
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Text input field
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
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
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = stringResource(R.string.send_message)
                    )
                }
            }
        }

        // Space for the bottom navigation
        Spacer(modifier = Modifier.height(bottomNavHeightDp))
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
fun ChatMessageItem(message: ChatMessage) {
    val isUserMessage = message.role == "user"
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUserMessage) Arrangement.End else Arrangement.Start
    ) {
        if (!isUserMessage) {
            // AI avatar for assistant messages
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
        }

        Column(
            horizontalAlignment = if (isUserMessage) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isUserMessage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (isUserMessage) {
                    // For user messages, just display the text
                    Text(
                        text = message.content,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    // For assistant messages, apply some basic formatting
                    FormattedText(
                        text = message.content,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = dateFormat.format(Date(message.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }

        if (isUserMessage) {
            Spacer(modifier = Modifier.width(8.dp))
            // User avatar for user messages
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "You",
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
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

            // Check for numbered list
            val isNumberedList = line.trim().matches("^\\d+\\.\\s+.*$".toRegex())

            // Check for bullet list
            val isBulletList = line.trim().matches("^[•*-]\\s+.*$".toRegex())

            var workingLine = line

            // Handle bold matches
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

            // Append the rest of the line
            if (currentIndex < workingLine.length) {
                append(workingLine.substring(currentIndex))
            }

            // Add newline except for the last line
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