package com.example.myapplication.screens

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapplication.data.Ticket
import com.example.myapplication.data.TicketRepository
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTicketScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { TicketRepository(context) }
    
    var trainNumber by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var departure by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var seatNumber by remember { mutableStateOf("") }
    var passengerName by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }
    var generatedImage by remember { mutableStateOf<Bitmap?>(null) }
    var showImage by remember { mutableStateOf(false) }
    
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    generatedImage?.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                Toast.makeText(context, "车票图片已保存", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "添加火车票",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // 表单输入
        OutlinedTextField(
            value = trainNumber,
            onValueChange = { trainNumber = it },
            label = { Text("车次") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true
        )
        
        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("日期 (YYYY-MM-DD)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true
        )
        
        OutlinedTextField(
            value = departure,
            onValueChange = { departure = it },
            label = { Text("出发地") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true
        )
        
        OutlinedTextField(
            value = destination,
            onValueChange = { destination = it },
            label = { Text("目的地") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true
        )
        
        OutlinedTextField(
            value = price,
            onValueChange = { price = it },
            label = { Text("票价") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        
        OutlinedTextField(
            value = seatNumber,
            onValueChange = { seatNumber = it },
            label = { Text("座位号") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true
        )
        
        OutlinedTextField(
            value = passengerName,
            onValueChange = { passengerName = it },
            label = { Text("乘客姓名") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true
        )
        
        OutlinedTextField(
            value = idNumber,
            onValueChange = { idNumber = it },
            label = { Text("身份证号") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            singleLine = true
        )
        
        // 按钮区域
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    if (trainNumber.isNotBlank() && date.isNotBlank() && 
                        departure.isNotBlank() && destination.isNotBlank() &&
                        price.isNotBlank() && seatNumber.isNotBlank() &&
                        passengerName.isNotBlank() && idNumber.isNotBlank()) {
                        
                        val ticket = Ticket(
                            trainNumber = trainNumber,
                            date = date,
                            departure = departure,
                            destination = destination,
                            price = price,
                            seatNumber = seatNumber,
                            passengerName = passengerName,
                            idNumber = idNumber
                        )
                        
                        generatedImage = repository.generateTicketImage(ticket)
                        showImage = true
                        
                        Toast.makeText(context, "车票图片已生成", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "请填写所有必填字段", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                Text("生成车票")
            }
            
            Button(
                onClick = {
                    if (generatedImage != null) {
                        saveLauncher.launch("ticket_${System.currentTimeMillis()}.png")
                    } else {
                        Toast.makeText(context, "请先生成车票图片", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            ) {
                Text("保存图片")
            }
        }
        
        // 保存到票夹按钮
        Button(
            onClick = {
                if (generatedImage != null) {
                    val ticket = Ticket(
                        trainNumber = trainNumber,
                        date = date,
                        departure = departure,
                        destination = destination,
                        price = price,
                        seatNumber = seatNumber,
                        passengerName = passengerName,
                        idNumber = idNumber
                    )
                    
                    val imagePath = repository.saveTicketImage(ticket)
                    val savedTicket = ticket.copy(imagePath = imagePath)
                    repository.addTicket(savedTicket)
                    
                    Toast.makeText(context, "车票已保存到票夹", Toast.LENGTH_SHORT).show()
                    
                    // 清空表单
                    trainNumber = ""
                    date = ""
                    departure = ""
                    destination = ""
                    price = ""
                    seatNumber = ""
                    passengerName = ""
                    idNumber = ""
                    generatedImage = null
                    showImage = false
                } else {
                    Toast.makeText(context, "请先生成车票图片", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("保存到票夹")
        }
        
        // 显示生成的车票图片
        if (showImage && generatedImage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .border(2.dp, Color.Gray, RoundedCornerShape(8.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Image(
                    bitmap = generatedImage!!.asImageBitmap(),
                    contentDescription = "生成的车票",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(8.dp)
                )
            }
        }
    }
} 