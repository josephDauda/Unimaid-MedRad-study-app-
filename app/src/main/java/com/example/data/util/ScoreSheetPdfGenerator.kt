package com.example.data.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.ExamAttemptEntity
import com.example.data.local.entity.UserEntity
import com.example.data.util.LevelClassifier
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ScoreSheetPdfGenerator {

  private const val TAG = "ScoreSheetPdf"

  data class CandidateRow(
    val sn: Int,
    val name: String,
    val email: String,
    val level: String,
    val status: String,
    val timeSpentStr: String,
    val score: Int,
    val total: Int,
    val percentage: Int,
    val passed: Boolean,
    val dateStr: String
  )

  fun generateMockScoreSheetPdf(
    context: Context,
    courseCode: String,
    courseTitle: String,
    sessionName: String,
    deadlineStr: String,
    attempts: List<ExamAttemptEntity>,
    studentsMap: Map<Long, UserEntity>
  ): Result<File> {
    return try {
      val candidates = if (attempts.isNotEmpty()) {
        attempts.mapIndexed { index, att ->
          var studentName = ""
          var studentEmail = ""
          var studentLevel = ""

          // Try extracting metadata from answersJson if available
          try {
            if (att.answersJson.startsWith("{")) {
              val json = JSONObject(att.answersJson)
              val meta = json.optJSONObject("_meta")
              if (meta != null) {
                studentName = meta.optString("student_name", "")
                studentEmail = meta.optString("student_email", "")
                studentLevel = meta.optString("student_level", "")
              }
            }
          } catch (_: Exception) {}

          // Fallback to local studentsMap
          val student = studentsMap[att.userId]
          if (studentName.isBlank() && student != null) {
            studentName = student.fullName
          }
          if (studentEmail.isBlank() && student != null) {
            studentEmail = student.email
          }
          if (studentLevel.isBlank() && student != null) {
            studentLevel = student.level
          }
          if (studentName.isBlank()) {
            studentName = "Candidate #${att.userId}"
          }
          if (studentEmail.isBlank()) {
            studentEmail = "ID #${att.userId}"
          }

          val total = if (att.totalQuestions > 0) att.totalQuestions else 15
          val rawScore = if (att.score > total) (att.score * total) / 100 else att.score
          val pct = if (total > 0) (rawScore * 100) / total else 0
          val passed = pct >= 50

          val minutes = att.timeSpentSeconds / 60
          val seconds = att.timeSpentSeconds % 60
          val timeSpentFormatted = "${minutes}m ${seconds}s"

          val dateFormatted = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(att.timestamp))

          val examStatus = when {
            att.answersJson.contains("TERMINATED_APP_SWITCH") -> "Lockdown"
            att.answersJson.contains("TIME_EXPIRED") -> "Time Exp"
            att.totalQuestions > 0 -> "Completed"
            else -> "Submitted"
          }

          val normLevel = LevelClassifier.normalizeLevel(studentLevel.ifBlank { student?.level ?: "200 Level" })
          val shortLevel = normLevel.replace(" Level", "L")

          CandidateRow(
            sn = index + 1,
            name = studentName,
            email = studentEmail,
            level = shortLevel,
            status = examStatus,
            timeSpentStr = timeSpentFormatted,
            score = rawScore,
            total = total,
            percentage = pct,
            passed = passed,
            dateStr = dateFormatted
          )
        }
      } else if (studentsMap.isNotEmpty()) {
        studentsMap.values.mapIndexed { index, student ->
          val normLevel = LevelClassifier.normalizeLevel(student.level.ifBlank { "200 Level" })
          CandidateRow(
            sn = index + 1,
            name = student.fullName.ifBlank { "Candidate #${student.id}" },
            email = student.email.ifBlank { student.phoneNumber.ifBlank { "ID #${student.id}" } },
            level = normLevel.replace(" Level", "L"),
            status = "Awaiting",
            timeSpentStr = "—",
            score = 0,
            total = 0,
            percentage = 0,
            passed = false,
            dateStr = "Awaiting CBT"
          )
        }
      } else {
        emptyList()
      }

      val pdfDocument = PdfDocument()
      val pageWidth = 595 // A4 standard width (points)
      val pageHeight = 842 // A4 standard height (points)
      val rowsPerPage = 22
      val totalPages = if (candidates.isEmpty()) 1 else ((candidates.size + rowsPerPage - 1) / rowsPerPage)

      val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
      }

      val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(15, 34, 64) // MedRad Deep Navy
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }

      val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(71, 85, 105)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
      }

      val linePaint = Paint().apply {
        color = Color.rgb(203, 213, 225)
        strokeWidth = 1f
      }

      val fillPaint = Paint()

      // Calculate statistics safely
      val scoredCandidates = candidates.filter { it.total > 0 }
      val totalCandidates = candidates.size
      val avgPercentage = if (scoredCandidates.isNotEmpty()) scoredCandidates.map { it.percentage }.average().toInt() else 0
      val maxPercentage = if (scoredCandidates.isNotEmpty()) scoredCandidates.maxOf { it.percentage } else 0
      val passedCount = scoredCandidates.count { it.passed }
      val passRate = if (scoredCandidates.isNotEmpty()) (passedCount * 100) / scoredCandidates.size else 0

      for (pageNum in 0 until totalPages) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum + 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        var y = 40f

        // 1. Institution Header Banner
        headerPaint.textSize = 15f
        headerPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("UNIVERSITY OF MAIDUGURI", pageWidth / 2f, y, headerPaint)
        y += 18f

        subHeaderPaint.textSize = 11f
        subHeaderPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("COLLEGE OF MEDICAL SCIENCES", pageWidth / 2f, y, subHeaderPaint)
        y += 16f

        headerPaint.textSize = 13f
        headerPaint.color = Color.rgb(2, 132, 199) // MedRad Sky Blue
        canvas.drawText("DEPARTMENT OF MEDICAL RADIOGRAPHY", pageWidth / 2f, y, headerPaint)
        y += 18f

        headerPaint.textSize = 12f
        headerPaint.color = Color.rgb(15, 23, 42)
        canvas.drawText("OFFICIAL CBT MOCK EXAMINATION SCORE SHEET", pageWidth / 2f, y, headerPaint)
        y += 14f

        canvas.drawLine(36f, y, pageWidth - 36f, y, linePaint)
        y += 16f

        // 2. Exam Meta Information Box
        val leftX = 40f
        val rightX = pageWidth - 40f
        textPaint.textSize = 9.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = Color.rgb(15, 23, 42)
        val detectedLevel = LevelClassifier.getLevelFromCourseCode(courseCode) ?: "Departmental Level"
        canvas.drawText("Course: $courseCode — $courseTitle  •  $detectedLevel", leftX, y, textPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val dateGenerated = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Generated: $dateGenerated", rightX, y, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
        y += 14f

        val sessionLabel = if (sessionName.isNotBlank()) sessionName else "Departmental CBT Simulation"
        val deadlineLabel = if (deadlineStr.isNotBlank()) "Deadline: $deadlineStr" else "Session: Active Window"
        canvas.drawText("Session: $sessionLabel  •  $deadlineLabel", leftX, y, textPaint)

        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Candidates: $totalCandidates | Pass Rate: $passRate% | Class Avg: $avgPercentage%", rightX, y, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
        y += 16f

        // 3. Table Column Headers
        val tableTopY = y
        val rowHeight = 20f

        fillPaint.color = Color.rgb(15, 34, 64) // Deep Navy Table Header
        canvas.drawRect(36f, y, pageWidth - 36f, y + rowHeight, fillPaint)

        val colSN = 40f
        val colName = 65f
        val colEmail = 185f
        val colLevel = 290f
        val colStatus = 330f
        val colTime = 390f
        val colScore = 442f
        val colPct = 490f
        val colRemark = 525f

        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = Color.WHITE

        val textBaselineY = y + 14f
        canvas.drawText("S/N", colSN, textBaselineY, textPaint)
        canvas.drawText("CANDIDATE NAME", colName, textBaselineY, textPaint)
        canvas.drawText("EMAIL / ID", colEmail, textBaselineY, textPaint)
        canvas.drawText("LEVEL", colLevel, textBaselineY, textPaint)
        canvas.drawText("STATUS", colStatus, textBaselineY, textPaint)
        canvas.drawText("TIME", colTime, textBaselineY, textPaint)
        canvas.drawText("SCORE", colScore, textBaselineY, textPaint)
        canvas.drawText("%", colPct, textBaselineY, textPaint)
        canvas.drawText("REMARK", colRemark, textBaselineY, textPaint)

        y += rowHeight

        // 4. Candidates Data Rows
        val startIndex = pageNum * rowsPerPage
        val endIndex = minOf(startIndex + rowsPerPage, candidates.size)

        if (candidates.isEmpty()) {
          y += 24f
          textPaint.color = Color.GRAY
          textPaint.textAlign = Paint.Align.CENTER
          textPaint.textSize = 10f
          canvas.drawText("No candidates have submitted for this course exam session yet.", pageWidth / 2f, y, textPaint)
          textPaint.textAlign = Paint.Align.LEFT
        } else {
          for (i in startIndex until endIndex) {
            val cand = candidates[i]
            val isEven = (i % 2 == 0)

            // Zebra background
            fillPaint.color = if (isEven) Color.rgb(248, 250, 252) else Color.WHITE
            canvas.drawRect(36f, y, pageWidth - 36f, y + rowHeight, fillPaint)

            canvas.drawLine(36f, y + rowHeight, pageWidth - 36f, y + rowHeight, linePaint)

            val rowTextY = y + 13.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 8.5f
            textPaint.color = Color.rgb(30, 41, 59)

            // S/N
            canvas.drawText("${cand.sn}", colSN, rowTextY, textPaint)

            // Name (truncated if too long)
            val nameDisplay = if (cand.name.length > 20) cand.name.substring(0, 18) + ".." else cand.name
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(nameDisplay, colName, rowTextY, textPaint)

            // Email / ID
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.color = Color.rgb(71, 85, 105)
            val emailDisplay = if (cand.email.length > 18) cand.email.substring(0, 16) + ".." else cand.email
            canvas.drawText(emailDisplay, colEmail, rowTextY, textPaint)

            // Level
            textPaint.color = Color.rgb(15, 23, 42)
            canvas.drawText(cand.level, colLevel, rowTextY, textPaint)

            // Status
            val statusColor = when (cand.status) {
              "Lockdown" -> Color.rgb(185, 28, 28)
              "Time Exp" -> Color.rgb(194, 65, 12)
              "Completed" -> Color.rgb(22, 101, 52)
              else -> Color.rgb(71, 85, 105)
            }
            textPaint.color = statusColor
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(cand.status, colStatus, rowTextY, textPaint)

            // Time Spent
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.color = Color.rgb(71, 85, 105)
            canvas.drawText(cand.timeSpentStr, colTime, rowTextY, textPaint)

            // Score
            textPaint.color = Color.rgb(15, 23, 42)
            val scoreDisplay = if (cand.total > 0) "${cand.score}/${cand.total}" else "—"
            canvas.drawText(scoreDisplay, colScore, rowTextY, textPaint)

            // Percentage
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val pctDisplay = if (cand.total > 0) "${cand.percentage}%" else "—"
            textPaint.color = if (cand.total > 0) {
              if (cand.passed) Color.rgb(22, 101, 52) else Color.rgb(185, 28, 28)
            } else {
              Color.rgb(100, 116, 139)
            }
            canvas.drawText(pctDisplay, colPct, rowTextY, textPaint)

            // Remark
            val remarkDisplay = if (cand.total > 0) (if (cand.passed) "PASS" else "FAIL") else "AWAITING"
            canvas.drawText(remarkDisplay, colRemark, rowTextY, textPaint)

            y += rowHeight
          }
        }

        // 5. Final Page Summary & Certification Signature Block
        if (pageNum == totalPages - 1) {
          y += 24f

          // Summary Stats Pill Bar
          fillPaint.color = Color.rgb(241, 245, 249)
          canvas.drawRoundRect(36f, y, pageWidth - 36f, y + 42f, 8f, 8f, fillPaint)

          textPaint.color = Color.rgb(15, 23, 42)
          textPaint.textSize = 9f
          textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

          val statY = y + 18f
          canvas.drawText("COHORT PERFORMANCE SUMMARY:", 48f, statY, textPaint)

          textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
          canvas.drawText("Total Submissions: $totalCandidates candidates", 48f, statY + 14f, textPaint)
          canvas.drawText("Highest Score: $maxPercentage%    |    Average Score: $avgPercentage%    |    Pass Rate: $passRate%", 240f, statY + 14f, textPaint)

          y += 65f

          // Signatures
          textPaint.textSize = 8.5f
          textPaint.color = Color.rgb(51, 65, 85)
          canvas.drawLine(48f, y + 25f, 220f, y + 25f, linePaint)
          canvas.drawText("Departmental Examination Officer", 48f, y + 38f, textPaint)
          canvas.drawText("Date & Signature", 48f, y + 48f, textPaint)

          canvas.drawLine(pageWidth - 220f, y + 25f, pageWidth - 48f, y + 25f, linePaint)
          canvas.drawText("Head of Department / Chief Radiographer", pageWidth - 220f, y + 38f, textPaint)
          canvas.drawText("Department of Medical Radiography", pageWidth - 220f, y + 48f, textPaint)
        }

        // 6. Page Number Footer
        subHeaderPaint.textSize = 8f
        subHeaderPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Page ${pageNum + 1} of $totalPages  •  MEDRAD Study Squad Platform  •  Secure Academic Record", pageWidth / 2f, pageHeight - 20f, subHeaderPaint)

        pdfDocument.finishPage(page)
      }

      val sanitizedCourse = courseCode.replace(Regex("[^a-zA-Z0-9]"), "_")
      val sanitizedSession = sessionName.ifBlank { "Session1" }.replace(Regex("[^a-zA-Z0-9]"), "_")
      val fileName = "MedRad_ScoreSheet_${sanitizedCourse}_${sanitizedSession}_${System.currentTimeMillis() % 100000}.pdf"

      val outputDir = File(context.cacheDir, "score_sheets").apply { mkdirs() }
      val pdfFile = File(outputDir, fileName)

      FileOutputStream(pdfFile).use { out ->
        pdfDocument.writeTo(out)
      }
      pdfDocument.close()

      Log.i(TAG, "Generated PDF Score Sheet successfully at ${pdfFile.absolutePath} (${pdfFile.length()} bytes)")
      Result.success(pdfFile)
    } catch (e: Exception) {
      Log.e(TAG, "Error generating PDF score sheet: ${e.message}", e)
      Result.failure(e)
    }
  }

  fun openOrSharePdf(context: Context, file: File, title: String) {
    try {
      // Save copy to public Downloads folder on Android 10+ (Q+) for permanent device storage
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        try {
          val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/MedRad")
          }
          val resolver = context.contentResolver
          val downloadUri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
          if (downloadUri != null) {
            resolver.openOutputStream(downloadUri)?.use { outStream ->
              file.inputStream().use { inStream ->
                inStream.copyTo(outStream)
              }
            }
          }
        } catch (mediaEx: Exception) {
          Log.w(TAG, "Optional MediaStore download saving: ${mediaEx.message}")
        }
      }

      val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, "Official Examination Score Sheet: $title")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }

      val chooser = Intent.createChooser(shareIntent, "Save or Open Score Sheet PDF").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

      context.startActivity(chooser)
      Toast.makeText(context, "Score Sheet generated and ready to view/share!", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
      Log.e(TAG, "Error opening/sharing PDF: ${e.message}", e)
      Toast.makeText(context, "Score sheet generated: ${file.name}", Toast.LENGTH_LONG).show()
    }
  }
}
