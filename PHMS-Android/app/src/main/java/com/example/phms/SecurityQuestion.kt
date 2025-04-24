package com.example.phms

data class SecurityQuestion(
    val id: Int,
    val question: String
)

object SecurityQuestions {
    val questions = listOf(
        SecurityQuestion(1, "What was the name of your first pet?"),
        SecurityQuestion(2, "In which city were you born?"),
        SecurityQuestion(3, "What was your childhood nickname?"),
        SecurityQuestion(4, "What is your mother's maiden name?"),
        SecurityQuestion(5, "What was the make of your first car?")
    )
}