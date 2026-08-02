package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.HtmlWebViewPreview
import com.example.util.HtmlTemplates

@Composable
fun HtmlEditorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val htmlContent by viewModel.htmlContent.collectAsStateWithLifecycle()
    val netlifySiteName by viewModel.netlifySiteName.collectAsStateWithLifecycle()
    val isNetlifyDeploying by viewModel.isNetlifyDeploying.collectAsStateWithLifecycle()
    val netlifyDeployResult by viewModel.netlifyDeployResult.collectAsStateWithLifecycle()
    val deployedSitesList by viewModel.deployedSitesList.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Editor, 1 = Full Preview
    var campaignTitle by remember { mutableStateOf("") }
    var showNetlifyDialog by remember { mutableStateOf(false) }

    var inputSiteName by remember { mutableStateOf(netlifySiteName) }
    var customToken by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Toolbar & Template Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "القوالب الجاهزة (Ready HTML Templates)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Netlify Deploy Action Button
                    Button(
                        onClick = {
                            inputSiteName = netlifySiteName
                            showNetlifyDialog = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00C7B7) // Netlify Cyan
                        ),
                        modifier = Modifier.testTag("open_netlify_deploy_dialog_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("نشر رابط Netlify", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(HtmlTemplates.templates) { index, template ->
                        AssistChip(
                            onClick = { viewModel.applyTemplate(index) },
                            label = { Text(template.title, fontSize = 12.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                // Variable insertion chips
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("إدراج متغيّر:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AssistChip(
                        onClick = { viewModel.insertPlaceholder("{name}") },
                        label = { Text("{name} - الاسم", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp)) }
                    )
                    AssistChip(
                        onClick = { viewModel.insertPlaceholder("{email}") },
                        label = { Text("{email} - الإيميل", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp)) }
                    )
                    AssistChip(
                        onClick = { viewModel.insertPlaceholder("{date}") },
                        label = { Text("{date} - التاريخ", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp)) }
                    )
                }
            }
        }

        // Deployed Netlify Sites Quick Bar
        if (deployedSitesList.isNotEmpty()) {
            val latestSite = deployedSitesList.first()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("رابط Netlify النشط حالياً:", fontSize = 11.sp, color = Color(0xFF065F46))
                            Text(
                                text = latestSite.siteUrl,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Netlify URL", latestSite.siteUrl)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "تم نسخ الرابط بنجاح!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, null, tint = Color(0xFF047857), modifier = Modifier.size(18.dp))
                        }

                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(latestSite.siteUrl))
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.OpenInNew, null, tint = Color(0xFF047857), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Switcher Tab: Editor vs Preview
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("محرر الكود HTML", fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Preview, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("المعاينة الحية الكاملة", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Editor or Preview Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (selectedTab == 0) {
                OutlinedTextField(
                    value = htmlContent,
                    onValueChange = { viewModel.htmlContent.value = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("html_code_editor"),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("اكتب محتوى HTML هنا...") }
                )
            } else {
                HtmlWebViewPreview(htmlContent = htmlContent)
            }
        }

        // Save Campaign Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = campaignTitle,
                onValueChange = { campaignTitle = it },
                placeholder = { Text("اسم القالب / الحملة") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )

            Button(
                onClick = {
                    viewModel.saveCampaign(campaignTitle)
                    campaignTitle = ""
                    Toast.makeText(context, "تم حفظ القالب بنجاح", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_campaign_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("حفظ القالب")
            }
        }
    }

    // Netlify Deploy Dialog (Ask User for Desired Site Name & Custom Domain Subdomain)
    if (showNetlifyDialog) {
        val cleanNamePreview = inputSiteName.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9-]"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
            .ifEmpty { "aredine" }

        val livePreviewUrl = "https://$cleanNamePreview.netlify.app"

        AlertDialog(
            onDismissRequest = { if (!isNetlifyDeploying) showNetlifyDialog = false },
            icon = {
                Icon(Icons.Default.Language, null, tint = Color(0xFF00C7B7), modifier = Modifier.size(32.dp))
            },
            title = {
                Text(
                    text = "إنشاء رابط موقع على Netlify",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "اختر اسم الموقع والنطاق الذي تريده للموقع المرفوع بلغة HTML:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = inputSiteName,
                        onValueChange = { inputSiteName = it },
                        label = { Text("اسم الموقع / Subdomain") },
                        placeholder = { Text("مثل: aredine") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("netlify_sitename_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Live preview card of the generated URL
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("معاينة الرابط الناتج:", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = livePreviewUrl,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customToken,
                        onValueChange = { customToken = it },
                        label = { Text("Netlify Access Token (اختياري)") },
                        placeholder = { Text("للنشر المباشر عبر حسابك الخاص") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    if (isNetlifyDeploying) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF00C7B7))
                            Spacer(Modifier.width(10.dp))
                            Text("جاري إنشاء الموقع وتفعيل الرابط...", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deployToNetlify(inputSiteName, customToken)
                        showNetlifyDialog = false
                    },
                    enabled = !isNetlifyDeploying && inputSiteName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C7B7)),
                    modifier = Modifier.testTag("confirm_netlify_deploy_button")
                ) {
                    Text("أنشئ وافتح الرابط الآن", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNetlifyDialog = false },
                    enabled = !isNetlifyDeploying
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}
