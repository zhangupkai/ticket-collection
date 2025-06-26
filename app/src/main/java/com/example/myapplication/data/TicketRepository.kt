package com.example.myapplication.data

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.example.myapplication.widget.TicketWidget
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
        updateWidget()
    }
    
    fun deleteTicket(ticketId: String) {
        tickets.removeAll { it.id == ticketId }
        saveTickets()
        updateWidget()
    }
    
    fun updateTicket(ticket: Ticket) {
        val index = tickets.indexOfFirst { it.id == ticket.id }
        if (index != -1) {
            tickets[index] = ticket
            saveTickets()
            updateWidget()
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
            updateWidget()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun updateWidget() {
        try {
            val intent = Intent(context, TicketWidget::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, TicketWidget::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            // 小组件更新失败，不影响主要功能
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
        val departureTimeText = if (ticket.departureTime.isNotBlank()) "${ticket.date} ${ticket.departureTime}开" else "${ticket.date} 18:57开"
        canvas.drawText(departureTimeText, 60f, 260f, infoPaint)
        canvas.drawText("${ticket.seatNumber}", width/2f-60f, 260f, infoPaint)
        canvas.drawText("￥${ticket.price}", width-320f, 260f, infoPaint)
        // 座席类型、检票口
        val subPaint = Paint().apply { color = Color.DKGRAY; textSize = 28f }
        val seatTypeText = if (ticket.seatType.isNotBlank()) ticket.seatType else "二等座"
        canvas.drawText(seatTypeText, 60f, 310f, subPaint)
        val gateText = if (ticket.gate.isNotBlank()) "检票:${ticket.gate}" else "检票:18B"
        canvas.drawText(gateText, width-320f, 310f, subPaint)
        // 仅供报销、身份证、乘客
        val smallPaint = Paint().apply { color = Color.GRAY; textSize = 24f }
        canvas.drawText("仅供报销使用", 60f, 350f, smallPaint)
        canvas.drawText("${ticket.idNumber.take(6)}****${ticket.idNumber.takeLast(4)} ${ticket.passengerName}", 60f, 380f, smallPaint)
        return bitmap
    }
    
    fun getPinyin(chinese: String): String {
        return when (chinese) {
            "杭州东" -> "Hangzhoudong"
            "北京南" -> "Beijingnan"
            else -> chinese
        }
    }
} 