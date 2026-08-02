package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val smtpHost by viewModel.smtpHost.collectAsStateWithLifecycle()
    val smtpPort by viewModel.smtpPort.collectAsStateWithLifecycle()
    val useSsl by viewModel.useSsl.collectAsStateWithLifecycle()
    val delaySeconds by viewModel.delaySeconds.collectAsStateWithLifecycle()
    val testResult by viewModel.smtpTestResult.collectAsStateWithLifecycle()

    var sentFileContent by remember { mutableStateOf("") }
    var errorLogContent by remember { mutableStateOf("") }
    var activeFileTab by remember { mutableStateOf("SENT") } // SENT, ERROR

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SMTP Advanced Config Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("إعدادات خادم SMTP وسرعة الإرسال", fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = smtpHost,
                    onValueChange = { viewModel.smtpHost.value = it },
                    label = { Text("عنوان خادم SMTP (Host)") },
                    placeholder = { Text("smtp.gmail.com") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = smtpPort,
                        onValueChange = { viewModel.smtpPort.value = it },
                        label = { Text("المنفذ (Port)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = delaySeconds,
                        onValueChange = { viewModel.delaySeconds.value = it },
                        label = { Text("التأخير بين الرسائل (ثوانٍ)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تفعيل تشفير SSL (Port 465)", fontSize = 14.sp)
                    Switch(
                        checked = useSsl,
                        onCheckedChange = { viewModel.useSsl.value = it }
                    )
                }

                Button(
                    onClick = { viewModel.testSmtpConnection() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_smtp_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.NetworkCheck, null)
                    Spacer(Modifier.width(6.dp))
                    Text("اختبار الاتصال بالخادم (Test SMTP Connection)")
                }

                if (testResult != null) {
                    Text(
                        text = testResult!!,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (testResult!!.contains("✅")) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
            }
        }

        // System Logs & File Persistence Inspector (sent.txt & error.log)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("إدارة ملفات النظام الحافظة (System Files)", fontWeight = FontWeight.Bold)

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                activeFileTab = "SENT"
                                sentFileContent = viewModel.readSentTxtContent()
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Description, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("sent.txt", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                activeFileTab = "ERROR"
                                errorLogContent = viewModel.readErrorLogContent()
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.BugReport, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("error.log", fontSize = 11.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (activeFileTab == "SENT") "محتوى sent.txt (يمنع التكرار)" else "محتوى error.log (سجل الأخطاء)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedButton(
                        onClick = {
                            if (activeFileTab == "SENT") {
                                viewModel.clearSentHistory()
                                sentFileContent = ""
                            } else {
                                viewModel.clearErrorLogHistory()
                                errorLogContent = ""
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFEF4444)
                        )
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("إفراغ الملف", fontSize = 11.sp)
                    }
                }

                val currentContent = if (activeFileTab == "SENT") {
                    sentFileContent.ifEmpty { viewModel.readSentTxtContent() }
                } else {
                    errorLogContent.ifEmpty { viewModel.readErrorLogContent() }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .padding(10.dp)
                ) {
                    Text(
                        text = currentContent.ifEmpty { "الملف فارغ حالياً." },
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
