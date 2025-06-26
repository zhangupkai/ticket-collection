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
    val departureTime: String = "",
    val gate: String = "",
    val seatType: String = "",
    val createdAt: Long = System.currentTimeMillis()
) 