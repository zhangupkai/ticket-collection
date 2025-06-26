package com.example.myapplication.data

import java.util.*

data class Ticket(
    val id: String = UUID.randomUUID().toString(),
    val trainNumber: String = "",
    val date: String = "",
    val departure: String = "",
    val destination: String = "",
    val price: String = "",
    val seatNumber: String = "",
    val passengerName: String = "",
    val idNumber: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val imagePath: String? = null
) 