package com.example.readsync2

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform