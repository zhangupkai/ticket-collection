package com.example.myapplication.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.compose.ui.graphics.toArgb
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class TicketRepository(private val context: Context) {
    private val gson = Gson()
    private val sharedPreferences = context.getSharedPreferences("tickets", Context.MODE_PRIVATE)
    private val tickets = mutableListOf<Ticket>()
    
    init {
        loadTickets()
    }
    
    fun getAllTickets(): List<Ticket> = tickets.toList()
    
    fun addTicket(ticket: Ticket) {
        tickets.add(ticket)
        saveTickets()
    }
    
    fun deleteTicket(ticketId: String) {
        tickets.removeAll { it.id == ticketId }
        saveTickets()
    }
    
    fun updateTicket(ticket: Ticket) {
        val index = tickets.indexOfFirst { it.id == ticket.id }
        if (index != -1) {
            tickets[index] = ticket
            saveTickets()
        }
    }
    
    private fun saveTickets() {
        val json = gson.toJson(tickets)
        sharedPreferences.edit().putString("tickets_data", json).apply()
    }
    
    private fun loadTickets() {
        val json = sharedPreferences.getString("tickets_data", "[]")
        val type = object : TypeToken<List<Ticket>>() {}.type
        val loadedTickets = gson.fromJson<List<Ticket>>(json, type) ?: emptyList()
        tickets.clear()
        tickets.addAll(loadedTickets)
    }
    
    fun exportTickets(): String {
        return gson.toJson(tickets)
    }
    
    fun importTickets(json: String): Boolean {
        return try {
            val type = object : TypeToken<List<Ticket>>() {}.type
            val importedTickets = gson.fromJson<List<Ticket>>(json, type) ?: emptyList()
            tickets.clear()
            tickets.addAll(importedTickets)
            saveTickets()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun generateTicketImage(ticket: Ticket): Bitmap {
        val width = 1000
        val height = 420
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // 斜线背景
        val bgPaint = Paint().apply { color = Color.parseColor("#EAF6FB"); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        val linePaint = Paint().apply { color = Color.parseColor("#22000000"); strokeWidth = 2f }
        val step = 32
        for (i in 0..(width/step+height/step)) {
            canvas.drawLine((i*step).toFloat(), 0f, 0f, (i*step).toFloat(), linePaint)
        }
        // 边框
        val borderPaint = Paint().apply { color = Color.parseColor("#B0C4DE"); style = Paint.Style.STROKE; strokeWidth = 8f }
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), 40f, 40f, borderPaint)
        // 票号
        val idPaint = Paint().apply { color = Color.parseColor("#D32F2F"); textSize = 48f; isFakeBoldText = true }
        canvas.drawText(ticket.id.take(12).uppercase(), 40f, 70f, idPaint)
        // 站名、车次、到达站
        val stationPaint = Paint().apply { color = Color.BLACK; textSize = 64f; isFakeBoldText = true }
        canvas.drawText(ticket.departure, 60f, 160f, stationPaint)
        canvas.drawText(ticket.destination, width-320f, 160f, stationPaint)
        val pinyinPaint = Paint().apply { color = Color.GRAY; textSize = 28f }
        canvas.drawText(getPinyin(ticket.departure), 60f, 200f, pinyinPaint)
        canvas.drawText(getPinyin(ticket.destination), width-320f, 200f, pinyinPaint)
        val trainPaint = Paint().apply { color = Color.BLACK; textSize = 48f; isFakeBoldText = true; isUnderlineText = true }
        canvas.drawText(ticket.trainNumber, width/2f-60f, 170f, trainPaint)
        // 日期时间、车厢座位、票价
        val infoPaint = Paint().apply { color = Color.BLACK; textSize = 36f }
        canvas.drawText("${ticket.date} 18:57开", 60f, 260f, infoPaint)
        canvas.drawText("${ticket.seatNumber}", width/2f-60f, 260f, infoPaint)
        canvas.drawText("￥${ticket.price}", width-320f, 260f, infoPaint)
        // 二等座、检票口
        val subPaint = Paint().apply { color = Color.DKGRAY; textSize = 28f }
        canvas.drawText("二等座始发改签", 60f, 310f, subPaint)
        canvas.drawText("检票:18B", width-320f, 310f, subPaint)
        // 仅供报销、身份证、乘客
        val smallPaint = Paint().apply { color = Color.GRAY; textSize = 24f }
        canvas.drawText("仅供报销使用", 60f, 350f, smallPaint)
        canvas.drawText("${ticket.idNumber.take(6)}****${ticket.idNumber.takeLast(4)} ${ticket.passengerName}", 60f, 380f, smallPaint)
        // 底部蓝色条
        val bluePaint = Paint().apply { color = Color.parseColor("#6EC6FF"); style = Paint.Style.FILL }
        canvas.drawRoundRect(0f, height-48f, width.toFloat(), height.toFloat(), 0f, 0f, bluePaint)
        val whitePaint = Paint().apply { color = Color.WHITE; textSize = 28f }
        canvas.drawText(ticket.id.take(20), 40f, height-16f, whitePaint)
        return bitmap
    }
    
    fun saveTicketImage(ticket: Ticket): String? {
        return try {
            val bitmap = generateTicketImage(ticket)
            val fileName = "ticket_${ticket.id}.png"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName)
            
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
    
    fun getPinyin(chinese: String): String {
        return when (chinese) {
            "杭州东" -> "Hangzhoudong"
            "北京南" -> "Beijingnan"
            else -> chinese
        }
    }
} 