package com.example.myapplication.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.data.Ticket
import com.example.myapplication.data.TicketRepository
import java.text.SimpleDateFormat
import java.util.*

class TicketWidget : AppWidgetProvider() {
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d("TicketWidget", "onUpdate called with ${appWidgetIds.size} widgets")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                val repository = TicketRepository(context)
                val tickets = repository.getAllTickets()
                
                Log.d("TicketWidget", "Found ${tickets.size} tickets")
                
                // 获取离当前时间最近的车票
                val nearestTicket = getNearestTicket(tickets)
                
                val views = RemoteViews(context.packageName, R.layout.ticket_widget).apply {
                    if (nearestTicket != null) {
                        Log.d("TicketWidget", "Setting nearest ticket: ${nearestTicket.departure} → ${nearestTicket.destination} on ${nearestTicket.date}")
                        
                        // 显示最近的车票信息
                        setTextViewText(R.id.widget_departure, nearestTicket.departure)
                        setTextViewText(R.id.widget_destination, nearestTicket.destination)
                        setTextViewText(R.id.widget_train_number, nearestTicket.trainNumber)
                        setTextViewText(R.id.widget_date, nearestTicket.date)
                        
                        val timeText = if (nearestTicket.departureTime.isNotBlank()) nearestTicket.departureTime else "18:57"
                        setTextViewText(R.id.widget_time, timeText)
                        Log.d("TicketWidget", "Setting time: $timeText")
                        
                        setTextViewText(R.id.widget_seat, nearestTicket.seatNumber)
                        setTextViewText(R.id.widget_price, "¥${nearestTicket.price}")
                        setTextViewText(R.id.widget_passenger, nearestTicket.passengerName)
                        
                        val seatTypeText = if (nearestTicket.seatType.isNotBlank()) nearestTicket.seatType else "二等座"
                        setTextViewText(R.id.widget_seat_type, seatTypeText)
                        Log.d("TicketWidget", "Setting seat type: $seatTypeText")
                        
                        val gateText = if (nearestTicket.gate.isNotBlank()) nearestTicket.gate else "18B"
                        setTextViewText(R.id.widget_gate, gateText)
                        Log.d("TicketWidget", "Setting gate: $gateText")
                        
                    } else {
                        Log.d("TicketWidget", "No tickets found, showing placeholder")
                        
                        // 没有车票时显示提示
                        setTextViewText(R.id.widget_departure, "暂无")
                        setTextViewText(R.id.widget_destination, "车票")
                        setTextViewText(R.id.widget_train_number, "点击添加")
                        setTextViewText(R.id.widget_date, "")
                        setTextViewText(R.id.widget_time, "")
                        setTextViewText(R.id.widget_seat, "")
                        setTextViewText(R.id.widget_price, "")
                        setTextViewText(R.id.widget_passenger, "")
                        setTextViewText(R.id.widget_seat_type, "")
                        setTextViewText(R.id.widget_gate, "")
                    }
                }
                
                // 设置点击事件，打开应用
                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent)
                
                appWidgetManager.updateAppWidget(appWidgetId, views)
                Log.d("TicketWidget", "Widget updated successfully")
                
            } catch (e: Exception) {
                Log.e("TicketWidget", "Error updating widget: ${e.message}")
                e.printStackTrace()
            }
        }
        
        /**
         * 获取离当前时间最近的车票
         * 优先显示未来的车票，如果没有未来车票则显示最近过去的车票
         */
        private fun getNearestTicket(tickets: List<Ticket>): Ticket? {
            if (tickets.isEmpty()) return null
            
            val currentDate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            // 将车票按时间排序
            val sortedTickets = tickets.sortedBy { ticket ->
                try {
                    val ticketDate = dateFormat.parse(ticket.date)
                    ticketDate?.time ?: Long.MAX_VALUE
                } catch (e: Exception) {
                    Log.e("TicketWidget", "Error parsing date: ${ticket.date}")
                    Long.MAX_VALUE
                }
            }
            
            Log.d("TicketWidget", "Current date: ${dateFormat.format(currentDate)}")
            sortedTickets.forEach { ticket ->
                Log.d("TicketWidget", "Ticket: ${ticket.departure} → ${ticket.destination} on ${ticket.date}")
            }
            
            // 找到第一个未来的车票
            val futureTickets = sortedTickets.filter { ticket ->
                try {
                    val ticketDate = dateFormat.parse(ticket.date)
                    val isFuture = ticketDate?.time ?: 0 >= currentDate.time
                    Log.d("TicketWidget", "Ticket ${ticket.date} is future: $isFuture")
                    isFuture
                } catch (e: Exception) {
                    Log.e("TicketWidget", "Error checking future date: ${ticket.date}")
                    false
                }
            }
            
            val result = if (futureTickets.isNotEmpty()) {
                // 如果有未来车票，返回最近的一个
                Log.d("TicketWidget", "Found ${futureTickets.size} future tickets, showing nearest one")
                futureTickets.first()
            } else {
                // 如果没有未来车票，返回最近过去的一个
                Log.d("TicketWidget", "No future tickets, showing most recent past ticket")
                sortedTickets.last()
            }
            
            Log.d("TicketWidget", "Selected ticket: ${result.departure} → ${result.destination} on ${result.date}")
            return result
        }
    }
} 