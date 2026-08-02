package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CampaignEntity
import com.example.data.LogEntity
import com.example.data.RecipientEntity
import com.example.util.ExcelParser
import com.example.util.FileLogManager
import com.example.util.HtmlTemplates
import com.example.util.NetlifyDeployResult
import com.example.util.NetlifyDeployer
import com.example.util.ParsedRecipient
import com.example.util.SendResult
import com.example.util.SmtpConfig
import com.example.util.SmtpEmailSender
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiSendingState(
    val isSending: Boolean = false,
    val isPaused: Boolean = false,
    val currentIndex: Int = 0,
    val totalCount: Int = 0,
    val sentCount: Int = 0,
    val failedCount: Int = 0,
    val skippedCount: Int = 0,
    val currentEmail: String = "",
    val statusMessage: String = ""
)

data class DeployedSite(
    val siteName: String,
    val siteUrl: String,
    val adminUrl: String,
    val deployedAt: String
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.appDao()
    private val fileLogManager = FileLogManager(application)
    private val excelParser = ExcelParser(application)
    private val smtpEmailSender = SmtpEmailSender()
    private val netlifyDeployer = NetlifyDeployer(application)

    // Configuration state
    val senderEmail = MutableStateFlow("")
    val appPassword = MutableStateFlow("")
    val smtpHost = MutableStateFlow("smtp.gmail.com")
    val smtpPort = MutableStateFlow("465")
    val useSsl = MutableStateFlow(true)
    val subject = MutableStateFlow("موضوع الرسالة الترويجية")
    val htmlContent = MutableStateFlow(HtmlTemplates.templates[0].htmlContent)
    val delaySeconds = MutableStateFlow("3")

    // Netlify State
    val netlifySiteName = MutableStateFlow("aredine")
    val netlifyAccessToken = MutableStateFlow("")
    val isNetlifyDeploying = MutableStateFlow(false)
    val netlifyDeployResult = MutableStateFlow<NetlifyDeployResult?>(null)
    val deployedSitesList = MutableStateFlow<List<DeployedSite>>(emptyList())

    // Database reactive streams
    val recipients: StateFlow<List<RecipientEntity>> = dao.getAllRecipients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<LogEntity>> = dao.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val campaigns: StateFlow<List<CampaignEntity>> = dao.getAllCampaigns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sending state
    private val _sendingState = MutableStateFlow(UiSendingState())
    val sendingState: StateFlow<UiSendingState> = _sendingState.asStateFlow()

    // Test connection outcome message
    private val _smtpTestResult = MutableStateFlow<String?>(null)
    val smtpTestResult: StateFlow<String?> = _smtpTestResult.asStateFlow()

    private var sendJob: Job? = null

    init {
        // Log startup info
        viewModelScope.launch {
            val sentSet = fileLogManager.getSentEmails()
            logInfo("تم تشغيل التطبيق بنجاح. عدد الإيميلات المسجلة سابقاً في sent.txt: ${sentSet.size}")
        }
    }

    fun loadExcelFromUri(uri: Uri) {
        viewModelScope.launch {
            logInfo("جاري قراءة الملف المحدد...")
            val parsedList = excelParser.parseUri(uri)
            if (parsedList.isNotEmpty()) {
                val entities = parsedList.map { p ->
                    val isAlreadySent = fileLogManager.isAlreadySent(p.email)
                    RecipientEntity(
                        email = p.email,
                        name = p.name,
                        status = if (isAlreadySent) "SKIPPED" else "PENDING",
                        errorMessage = if (isAlreadySent) "موجود في sent.txt سابقاً" else null
                    )
                }
                dao.deleteAllRecipients()
                dao.insertRecipients(entities)
                val skippedCount = entities.count { it.status == "SKIPPED" }
                logInfo("تم تحميل ${entities.size} إيميل من الملف ($skippedCount مسجل سابقاً في sent.txt وتم تخطيه تلقائياً)")
            } else {
                logError("لم يتم العثور على أية إيميلات صالحة داخل الملف المحدد", null)
            }
        }
    }

    fun loadSampleExcel() {
        viewModelScope.launch {
            val sampleFile = excelParser.createSampleExcelFile()
            val uri = Uri.fromFile(sampleFile)
            loadExcelFromUri(uri)
        }
    }

    fun addSingleRecipient(emailStr: String, nameStr: String) {
        if (emailStr.isBlank() || !emailStr.contains("@")) return
        viewModelScope.launch {
            val isAlreadySent = fileLogManager.isAlreadySent(emailStr)
            val recipient = RecipientEntity(
                email = emailStr.trim(),
                name = nameStr.trim(),
                status = if (isAlreadySent) "SKIPPED" else "PENDING",
                errorMessage = if (isAlreadySent) "موجود في sent.txt" else null
            )
            dao.insertRecipient(recipient)
            logInfo("تم إضافة الإيميل: $emailStr")
        }
    }

    fun clearRecipients() {
        viewModelScope.launch {
            dao.deleteAllRecipients()
            logInfo("تم مسح قائمة الإيميلات الحالية")
        }
    }

    fun deleteRecipient(recipient: RecipientEntity) {
        viewModelScope.launch {
            dao.updateRecipient(recipient.copy(status = "DELETED"))
        }
    }

    fun startSending() {
        if (_sendingState.value.isSending) return
        val currentRecipients = recipients.value.filter { it.status == "PENDING" }
        if (currentRecipients.isEmpty()) {
            logError("لا توجد إيميلات جديدة قيد الانتظار للإرسال!", null)
            return
        }
        if (senderEmail.value.isBlank() || appPassword.value.isBlank()) {
            logError("يرجى إدخال بريد المرسل وكلمة مرور التطبيق أولاً", null)
            return
        }

        val parsedDelay = delaySeconds.value.toIntOrNull() ?: 3
        val portNum = smtpPort.value.toIntOrNull() ?: 465
        val config = SmtpConfig(
            senderEmail = senderEmail.value.trim(),
            appPassword = appPassword.value.trim(),
            host = smtpHost.value.trim(),
            port = portNum,
            useSsl = useSsl.value
        )

        _sendingState.value = UiSendingState(
            isSending = true,
            totalCount = currentRecipients.size,
            statusMessage = "جاري بدء عملية الإرسال..."
        )

        sendJob = viewModelScope.launch {
            logInfo("بدء حملة الإرسال لـ ${currentRecipients.size} مستلم بتأخير $parsedDelay ثوانٍ بين كل إيميل...")

            var sentCount = 0
            var failedCount = 0
            var skippedCount = 0

            for ((index, item) in currentRecipients.withIndex()) {
                if (!_sendingState.value.isSending) break

                // Double check sent.txt
                if (fileLogManager.isAlreadySent(item.email)) {
                    skippedCount++
                    dao.updateRecipient(item.copy(status = "SKIPPED", errorMessage = "موجود في sent.txt"))
                    logInfo("تخطي الإيميل (${item.email}) مكرر في sent.txt")
                    _sendingState.value = _sendingState.value.copy(
                        currentIndex = index + 1,
                        skippedCount = skippedCount
                    )
                    continue
                }

                _sendingState.value = _sendingState.value.copy(
                    currentIndex = index + 1,
                    currentEmail = item.email,
                    statusMessage = "جاري الإرسال إلى ${item.email} (${index + 1}/${currentRecipients.size})"
                )

                val result = smtpEmailSender.sendHtmlEmail(
                    config = config,
                    recipientEmail = item.email,
                    recipientName = item.name,
                    subjectTemplate = subject.value,
                    htmlTemplate = htmlContent.value
                )

                when (result) {
                    is SendResult.Success -> {
                        sentCount++
                        fileLogManager.recordSentEmail(item.email)
                        dao.updateRecipient(
                            item.copy(
                                status = "SENT",
                                sentAt = System.currentTimeMillis()
                            )
                        )
                        logSuccess("تم إرسال الإيميل بنجاح إلى: ${item.email}", item.email)
                    }
                    is SendResult.Failure -> {
                        failedCount++
                        fileLogManager.recordError(item.email, result.errorMessage)
                        dao.updateRecipient(
                            item.copy(
                                status = "FAILED",
                                errorMessage = result.errorMessage
                            )
                        )
                        logError("فشل الإرسال إلى ${item.email}: ${result.errorMessage}", item.email)
                    }
                    is SendResult.Skipped -> {
                        skippedCount++
                    }
                }

                _sendingState.value = _sendingState.value.copy(
                    sentCount = sentCount,
                    failedCount = failedCount,
                    skippedCount = skippedCount
                )

                // Delay between emails (User specified requirement: 3 seconds delay default)
                if (index < currentRecipients.size - 1) {
                    delay(parsedDelay * 1000L)
                }
            }

            logInfo("انتهت عملية الإرسال! الناجحة: $sentCount | الفاشلة: $failedCount | المتخطاة: $skippedCount")
            _sendingState.value = _sendingState.value.copy(
                isSending = false,
                statusMessage = "اكتملت الحملة بنجاح"
            )
        }
    }

    fun stopSending() {
        sendJob?.cancel()
        _sendingState.value = _sendingState.value.copy(
            isSending = false,
            statusMessage = "تم إيقاف عملية الإرسال من قبل المستخدم"
        )
        logInfo("تم إيقاف عملية الإرسال بناءً على طلب المستخدم")
    }

    fun testSmtpConnection() {
        viewModelScope.launch {
            _smtpTestResult.value = "جاري اختبار الاتصال بخادم SMTP..."
            val portNum = smtpPort.value.toIntOrNull() ?: 465
            val config = SmtpConfig(
                senderEmail = senderEmail.value.trim(),
                appPassword = appPassword.value.trim(),
                host = smtpHost.value.trim(),
                port = portNum,
                useSsl = useSsl.value
            )
            val res = smtpEmailSender.testSmtpConnection(config)
            if (res.isSuccess) {
                _smtpTestResult.value = "✅ نجح الاتصال بالبريد والموثقية الصحيحة!"
                logSuccess("نجح اختبار الاتصال بـ SMTP", senderEmail.value)
            } else {
                val err = res.exceptionOrNull()?.localizedMessage ?: "فشل الاتصال"
                _smtpTestResult.value = "❌ فشل الاتصال: $err"
                logError("فشل اختبار الاتصال بـ SMTP: $err", senderEmail.value)
            }
        }
    }

    fun applyTemplate(templateIndex: Int) {
        if (templateIndex in HtmlTemplates.templates.indices) {
            val tpl = HtmlTemplates.templates[templateIndex]
            subject.value = tpl.defaultSubject
            htmlContent.value = tpl.htmlContent
            logInfo("تم تطبيق القالب: ${tpl.title}")
        }
    }

    fun insertPlaceholder(tag: String) {
        htmlContent.value = htmlContent.value + " " + tag
    }

    fun saveCampaign(title: String) {
        viewModelScope.launch {
            val campaign = CampaignEntity(
                title = title.ifBlank { "حملة جديدة" },
                subject = subject.value,
                senderEmail = senderEmail.value,
                appPassword = appPassword.value,
                smtpHost = smtpHost.value,
                smtpPort = smtpPort.value.toIntOrNull() ?: 465,
                useSsl = useSsl.value,
                htmlContent = htmlContent.value,
                delaySeconds = delaySeconds.value.toIntOrNull() ?: 3
            )
            dao.insertCampaign(campaign)
            logInfo("تم حفظ الحملة بنجاح: ${campaign.title}")
        }
    }

    fun loadCampaign(campaign: CampaignEntity) {
        subject.value = campaign.subject
        senderEmail.value = campaign.senderEmail
        appPassword.value = campaign.appPassword
        smtpHost.value = campaign.smtpHost
        smtpPort.value = campaign.smtpPort.toString()
        useSsl.value = campaign.useSsl
        htmlContent.value = campaign.htmlContent
        delaySeconds.value = campaign.delaySeconds.toString()
        logInfo("تم تحميل تفاصيل الحملة: ${campaign.title}")
    }

    fun clearSentHistory() {
        fileLogManager.clearSentFile()
        logInfo("تم إفراغ ملف sent.txt بنجاح")
    }

    fun clearErrorLogHistory() {
        fileLogManager.clearErrorLog()
        logInfo("تم إفراغ ملف error.log بنجاح")
    }

    fun clearDbLogs() {
        viewModelScope.launch {
            dao.clearLogs()
        }
    }

    fun readSentTxtContent(): String = fileLogManager.readSentFileContent()
    fun readErrorLogContent(): String = fileLogManager.readErrorLogContent()

    fun deployToNetlify(requestedSiteName: String, customToken: String? = null) {
        val targetName = requestedSiteName.ifBlank { netlifySiteName.value }.ifBlank { "aredine" }
        netlifySiteName.value = targetName

        viewModelScope.launch {
            isNetlifyDeploying.value = true
            logInfo("جاري إنشاء ونشر موقع Netlify بالاسم المحدد: $targetName...")

            val result = netlifyDeployer.deployHtmlSite(
                siteName = targetName,
                htmlContent = htmlContent.value,
                personalAccessToken = customToken ?: netlifyAccessToken.value
            )

            isNetlifyDeploying.value = false
            netlifyDeployResult.value = result

            if (result.success) {
                logSuccess("تم إنشاء موقع Netlify بنجاح: ${result.siteUrl}")
                val now = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                val newSite = DeployedSite(
                    siteName = result.siteName,
                    siteUrl = result.siteUrl,
                    adminUrl = result.adminUrl,
                    deployedAt = now
                )
                deployedSitesList.value = listOf(newSite) + deployedSitesList.value
            } else {
                logError("فشل نشر الموقع على Netlify: ${result.errorMessage}", null)
            }
        }
    }

    private fun logInfo(msg: String) {
        viewModelScope.launch {
            dao.insertLog(LogEntity(type = "INFO", message = msg))
        }
    }

    private fun logSuccess(msg: String, email: String? = null) {
        viewModelScope.launch {
            dao.insertLog(LogEntity(type = "SUCCESS", message = msg, recipientEmail = email))
        }
    }

    private fun logError(msg: String, email: String? = null) {
        viewModelScope.launch {
            dao.insertLog(LogEntity(type = "ERROR", message = msg, recipientEmail = email))
        }
    }
}
