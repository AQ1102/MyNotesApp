package com.example.notes

data class Note(
    val id: Long = System.currentTimeMillis(),
    var title: String,
    var content: String,
    val timestamp: Long = System.currentTimeMillis()
)
