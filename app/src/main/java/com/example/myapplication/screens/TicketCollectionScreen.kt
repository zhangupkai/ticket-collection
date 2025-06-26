package com.example.myapplication.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.myapplication.data.Ticket
import com.example.myapplication.data.TicketRepository
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketCollectionScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { TicketRepository(context) }
    var tickets by remember { mutableStateOf<List<Ticket>>(emptyList()) }
    
    // 加载车票数据
    LaunchedEffect(Unit) {
        tickets = repository.getAllTickets()
    }
    
    val shareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "我的票夹",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (tickets.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无车票，快去添加吧！",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(tickets.sortedByDescending { it.createdAt }) { ticket ->
                    TicketCard(
                        ticket = ticket,
                        repository = repository,
                        onDelete = {
                            repository.deleteTicket(ticket.id)
                            tickets = repository.getAllTickets()
                            Toast.makeText(context, "车票已删除", Toast.LENGTH_SHORT).show()
                        },
                        onShare = { imagePath ->
                            shareTicket(context, imagePath, shareLauncher)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketCard(
    ticket: Ticket,
    repository: TicketRepository,
    onDelete: () -> Unit,
    onShare: (String) -> Unit
) {
    var showImage by remember { mutableStateOf(false) }
    var ticketImage by remember { mutableStateOf<Bitmap?>(null) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 车票基本信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${ticket.departure} → ${ticket.destination}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "车次: ${ticket.trainNumber}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "日期: ${ticket.date}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "乘客: ${ticket.passengerName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "座位: ${ticket.seatNumber} | 票价: ¥${ticket.price}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // 操作按钮
                Column {
                    Button(
                        onClick = {
                            if (ticket.imagePath != null) {
                                onShare(ticket.imagePath)
                            } else {
                                // 重新生成图片
                                ticketImage = repository.generateTicketImage(ticket)
                                if (ticketImage != null) {
                                    val newImagePath = repository.saveTicketImage(ticket)
                                    if (newImagePath != null) {
                                        onShare(newImagePath)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text("分享")
                    }
                    
                    Button(
                        onClick = onDelete
                    ) {
                        Text("删除")
                    }
                }
            }
            
            // 查看图片按钮
            Button(
                onClick = {
                    if (ticket.imagePath != null) {
                        val file = File(ticket.imagePath)
                        if (file.exists()) {
                            ticketImage = BitmapFactory.decodeFile(ticket.imagePath)
                        } else {
                            ticketImage = repository.generateTicketImage(ticket)
                        }
                    } else {
                        ticketImage = repository.generateTicketImage(ticket)
                    }
                    showImage = !showImage
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(if (showImage) "隐藏图片" else "查看图片")
            }
            
            // 显示车票图片
            if (showImage && ticketImage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .border(1.dp, Color.Gray, RoundedCornerShape(4.dp)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Image(
                        bitmap = ticketImage!!.asImageBitmap(),
                        contentDescription = "车票图片",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

private fun shareTicket(context: android.content.Context, imagePath: String, launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    try {
        val file = File(imagePath)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "我的火车票")
                putExtra(Intent.EXTRA_TEXT, "分享我的火车票")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            launcher.launch(Intent.createChooser(shareIntent, "分享车票"))
        } else {
            Toast.makeText(context, "图片文件不存在", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun TrainTicketView(ticket: Ticket, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(2.5f)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFEAF6FB), Color(0xFFD2E6F3))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(2.dp, Color(0xFFB0C4DE), RoundedCornerShape(16.dp))
    ) {
        // 斜线背景
        Canvas(modifier = Modifier.matchParentSize()) {
            val step = 24.dp.toPx()
            for (i in 0..(size.width / step).toInt() + (size.height / step).toInt()) {
                drawLine(
                    color = Color(0x22000000),
                    start = Offset(i * step, 0f),
                    end = Offset(0f, i * step),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // 票号
            Text(
                text = ticket.id.take(12).uppercase(),
                color = Color(0xFFD32F2F),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            // 站名、车次、到达站
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(Modifier.weight(1f)) {
                    Text(ticket.departure, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(getPinyin(ticket.departure), fontSize = 14.sp, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(ticket.trainNumber, fontSize = 24.sp, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(ticket.destination, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(getPinyin(ticket.destination), fontSize = 14.sp, color = Color.Gray)
                }
            }
            // 日期时间、车厢座位、票价
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${ticket.date} 18:57开", fontSize = 20.sp)
                Spacer(Modifier.weight(1f))
                Text("${ticket.seatNumber}", fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                Text("￥${ticket.price}", fontSize = 20.sp, color = Color(0xFF1976D2))
            }
            // 其他信息
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("二等座始发改签", fontSize = 16.sp, color = Color.DarkGray)
                Spacer(Modifier.weight(1f))
                Text("检票:18B", fontSize = 16.sp, color = Color.DarkGray)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("仅供报销使用", fontSize = 14.sp, color = Color.Gray)
                    Text("${ticket.idNumber.take(6)}****${ticket.idNumber.takeLast(4)} ${ticket.passengerName}", fontSize = 14.sp, color = Color.Gray)
                }
            }
            Spacer(Modifier.weight(1f))
            // 底部蓝色条
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(Color(0xFF6EC6FF), RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            ) {
                Text(
                    text = ticket.id.take(20),
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp)
                )
            }
        }
    }
}

fun getPinyin(chinese: String): String {
    return when (chinese) {
        "杭州东" -> "Hangzhoudong"
        "北京南" -> "Beijingnan"
        else -> chinese
    }
} 