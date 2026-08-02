package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader

data class ParsedRecipient(
    val email: String,
    val name: String = "",
    val extraFields: Map<String, String> = emptyMap()
)

class ExcelParser(private val context: Context) {

    fun parseUri(uri: Uri): List<ParsedRecipient> {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return emptyList()
            parseInputStream(inputStream)
        } catch (e: Exception) {
            Log.e("ExcelParser", "Error parsing URI", e)
            emptyList()
        }
    }

    fun parseInputStream(inputStream: InputStream): List<ParsedRecipient> {
        val result = mutableListOf<ParsedRecipient>()
        try {
            val bytes = inputStream.readBytes()
            // Try Apache POI first
            try {
                val workbook = WorkbookFactory.create(bytes.inputStream())
                val sheet = workbook.getSheetAt(0)
                if (sheet != null && sheet.physicalNumberOfRows > 0) {
                    val headerRow = sheet.getRow(0)
                    var emailColIndex = -1
                    var nameColIndex = -1
                    val colNames = mutableMapOf<Int, String>()

                    if (headerRow != null) {
                        for (c in 0 until headerRow.lastCellNum) {
                            val cell = headerRow.getCell(c) ?: continue
                            val colVal = cell.toString().trim().lowercase()
                            colNames[c] = cell.toString().trim()
                            if (colVal.contains("email") || colVal.contains("إيميل") || colVal.contains("بريد")) {
                                if (emailColIndex == -1) emailColIndex = c
                            } else if (colVal.contains("name") || colVal.contains("اسم") || colVal.contains("الاسم")) {
                                if (nameColIndex == -1) nameColIndex = c
                            }
                        }
                    }

                    // Fallback to col 0 if email column wasn't detected by header name
                    if (emailColIndex == -1) emailColIndex = 0

                    val startRow = if (headerRow != null && emailColIndex != -1 &&
                        (headerRow.getCell(emailColIndex)?.toString()?.contains("email", true) == true ||
                         headerRow.getCell(emailColIndex)?.toString()?.contains("بريد", true) == true)
                    ) 1 else 0

                    for (r in startRow..sheet.lastRowNum) {
                        val row = sheet.getRow(r) ?: continue
                        val emailCell = row.getCell(emailColIndex) ?: continue
                        val emailStr = getCellValueAsString(emailCell).trim()

                        if (isValidEmail(emailStr)) {
                            val nameStr = if (nameColIndex != -1) {
                                row.getCell(nameColIndex)?.let { getCellValueAsString(it).trim() } ?: ""
                            } else ""

                            val extraMap = mutableMapOf<String, String>()
                            for (c in 0 until row.lastCellNum) {
                                if (c != emailColIndex && c != nameColIndex) {
                                    val headerName = colNames[c] ?: "col_$c"
                                    val cellVal = row.getCell(c)?.let { getCellValueAsString(it).trim() } ?: ""
                                    if (cellVal.isNotEmpty()) {
                                        extraMap[headerName] = cellVal
                                    }
                                }
                            }
                            result.add(ParsedRecipient(email = emailStr, name = nameStr, extraFields = extraMap))
                        }
                    }
                    workbook.close()
                    if (result.isNotEmpty()) return result
                }
            } catch (poiError: Exception) {
                Log.w("ExcelParser", "POI parse failed, falling back to CSV line parser", poiError)
            }

            // Fallback CSV / Plaintext parser
            val reader = BufferedReader(InputStreamReader(bytes.inputStream(), Charsets.UTF_8))
            var line: String? = reader.readLine()
            var isFirstLine = true
            while (line != null) {
                val cleanLine = line.trim()
                if (cleanLine.isNotEmpty()) {
                    val tokens = cleanLine.split(',', ';', '\t')
                    for (token in tokens) {
                        val possibleEmail = token.trim()
                        if (isValidEmail(possibleEmail)) {
                            val name = if (tokens.size > 1 && !tokens[0].contains("@")) tokens[0].trim() else ""
                            result.add(ParsedRecipient(email = possibleEmail, name = name))
                        }
                    }
                }
                isFirstLine = false
                line = reader.readLine()
            }
        } catch (e: Exception) {
            Log.e("ExcelParser", "Error parsing input stream", e)
        }
        return result
    }

    private fun getCellValueAsString(cell: org.apache.poi.ss.usermodel.Cell): String {
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> {
                val num = cell.numericCellValue
                if (num == num.toLong().toDouble()) num.toLong().toString() else num.toString()
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> {
                try { cell.stringCellValue } catch (_: Exception) { cell.numericCellValue.toString() }
            }
            else -> ""
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".") && email.length >= 5
    }

    /**
     * Creates a default sample emails.xlsx file in internal storage for easy 1-click testing
     */
    fun createSampleExcelFile(): File {
        val sampleFile = File(context.filesDir, "emails.xlsx")
        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Emails")

            val headerRow = sheet.createRow(0)
            headerRow.createCell(0).setCellValue("email")
            headerRow.createCell(1).setCellValue("name")
            headerRow.createCell(2).setCellValue("company")

            val sampleData = listOf(
                Triple("client1@example.com", "أحمد علي", "شركة التقنية"),
                Triple("sales@business.com", "سارة محمود", "حلول الأعمال"),
                Triple("info@demo.org", "محمد إبراهيم", "مؤسسة الأمل")
            )

            for ((i, data) in sampleData.withIndex()) {
                val row = sheet.createRow(i + 1)
                row.createCell(0).setCellValue(data.first)
                row.createCell(1).setCellValue(data.second)
                row.createCell(2).setCellValue(data.third)
            }

            FileOutputStream(sampleFile).use { out ->
                workbook.write(out)
            }
            workbook.close()
        } catch (e: Exception) {
            Log.e("ExcelParser", "Error creating sample emails.xlsx", e)
        }
        return sampleFile
    }
}
