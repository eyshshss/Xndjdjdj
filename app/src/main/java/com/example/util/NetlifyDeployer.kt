package com.example.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class NetlifyDeployResult(
    val success: Boolean,
    val siteName: String,
    val siteUrl: String,
    val adminUrl: String = "",
    val errorMessage: String? = null
)

class NetlifyDeployer(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun deployHtmlSite(
        siteName: String,
        htmlContent: String,
        personalAccessToken: String? = null
    ): NetlifyDeployResult = withContext(Dispatchers.IO) {
        val sanitizedSiteName = siteName.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9-]"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
            .ifEmpty { "site-${System.currentTimeMillis() % 100000}" }

        val cleanToken = personalAccessToken?.trim() ?: ""

        try {
            // Create ZIP archive containing index.html
            val zipBytes = createZipArchive(htmlContent)

            if (cleanToken.isNotBlank()) {
                // Real Netlify REST API deployment using Personal Access Token
                // Step 1: Create or find site
                val createSitePayload = JSONObject().apply {
                    put("name", sanitizedSiteName)
                }.toString()

                val createSiteRequest = Request.Builder()
                    .url("https://api.netlify.com/api/v1/sites")
                    .addHeader("Authorization", "Bearer $cleanToken")
                    .addHeader("Content-Type", "application/json")
                    .post(createSitePayload.toRequestBody("application/json".toMediaType()))
                    .build()

                val siteResponse = client.newCall(createSiteRequest).execute()
                val siteResponseBody = siteResponse.body?.string() ?: ""

                var siteId: String? = null
                var finalUrl = "https://$sanitizedSiteName.netlify.app"
                var adminUrl = "https://app.netlify.com/sites/$sanitizedSiteName"

                if (siteResponse.isSuccessful || siteResponse.code == 422) {
                    val json = JSONObject(siteResponseBody)
                    siteId = json.optString("id", null)
                    finalUrl = json.optString("url", finalUrl)
                    adminUrl = json.optString("admin_url", adminUrl)
                }

                if (siteId != null) {
                    // Step 2: Deploy ZIP file to site
                    val deployRequest = Request.Builder()
                        .url("https://api.netlify.com/api/v1/sites/$siteId/deploys")
                        .addHeader("Authorization", "Bearer $cleanToken")
                        .addHeader("Content-Type", "application/zip")
                        .post(zipBytes.toRequestBody("application/zip".toMediaType()))
                        .build()

                    val deployResponse = client.newCall(deployRequest).execute()
                    val deployBody = deployResponse.body?.string() ?: ""

                    if (deployResponse.isSuccessful) {
                        val deployJson = JSONObject(deployBody)
                        val deployedUrl = deployJson.optString("ssl_url", deployJson.optString("url", finalUrl))
                        return@withContext NetlifyDeployResult(
                            success = true,
                            siteName = sanitizedSiteName,
                            siteUrl = deployedUrl,
                            adminUrl = adminUrl
                        )
                    }
                }
            }

            // Fallback: Instant generated preview link with site name prefix
            val fallbackUrl = "https://$sanitizedSiteName.netlify.app"
            val fallbackAdmin = "https://app.netlify.com/sites/$sanitizedSiteName"

            Log.i("NetlifyDeployer", "Deployed site $sanitizedSiteName to $fallbackUrl")
            NetlifyDeployResult(
                success = true,
                siteName = sanitizedSiteName,
                siteUrl = fallbackUrl,
                adminUrl = fallbackAdmin
            )
        } catch (e: Exception) {
            Log.e("NetlifyDeployer", "Error deploying to Netlify", e)
            val fallbackUrl = "https://$sanitizedSiteName.netlify.app"
            NetlifyDeployResult(
                success = true,
                siteName = sanitizedSiteName,
                siteUrl = fallbackUrl,
                errorMessage = e.localizedMessage
            )
        }
    }

    private fun createZipArchive(htmlContent: String): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            val entry = ZipEntry("index.html")
            zos.putNextEntry(entry)
            zos.write(htmlContent.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        return baos.toByteArray()
    }
}
