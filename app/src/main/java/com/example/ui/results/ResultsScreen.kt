package com.example.ui.results

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.QuestionEntity
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ProfessionalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.ResultsViewModel
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
  attemptId: Long,
  resultsViewModel: ResultsViewModel,
  onNavigateToDashboard: () -> Unit,
  onPracticeAgain: () -> Unit
) {
  LaunchedEffect(attemptId) {
    resultsViewModel.loadAttempt(attemptId)
  }

  val attempt by resultsViewModel.attempt.collectAsState()
  val questions by resultsViewModel.questions.collectAsState()

  BackHandler {
    onNavigateToDashboard()
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Examination Results",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )
        },
        navigationIcon = {
          IconButton(onClick = onNavigateToDashboard) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
          titleContentColor = MaterialTheme.colorScheme.onBackground,
          navigationIconContentColor = MaterialTheme.colorScheme.onBackground
        )
      )
    }
  ) { innerPadding ->
    if (attempt == null) {
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
      }
    } else {
      val att = attempt!!
      val answersJson = try {
        JSONObject(att.answersJson)
      } catch (e: Exception) {
        JSONObject()
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        item {
          Spacer(modifier = Modifier.height(8.dp))
          // 1. Overall Score Hero Card
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = att.examTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = att.courseTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              Spacer(modifier = Modifier.height(16.dp))

              // Circular Percentage Badge
              val scoreColor = when {
                att.score >= 70 -> SuccessGreen
                att.score >= 50 -> WarningAmber
                else -> ErrorRed
              }

              Box(
                modifier = Modifier
                  .size(110.dp)
                  .clip(CircleShape)
                  .background(scoreColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = "${att.score}%",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = scoreColor
                  )
                  Text(
                    text = if (att.score >= 70) "DISTINCTION" else if (att.score >= 50) "PASSED" else "NEEDS REVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                    color = scoreColor
                  )
                }
              }

              Spacer(modifier = Modifier.height(16.dp))

              Text(
                text = "${att.correctAnswers} / ${att.totalQuestions} Correct",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
              )

              Spacer(modifier = Modifier.height(20.dp))

              // Breakdown Row
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
              ) {
                StatItem("Correct", "${att.correctAnswers}", SuccessGreen)
                StatItem("Incorrect", "${att.incorrectAnswers}", ErrorRed)
                StatItem("Unanswered", "${att.unansweredQuestions}", Color(0xFF64748B))
                val minutesUsed = att.timeSpentSeconds / 60
                val secondsUsed = att.timeSpentSeconds % 60
                StatItem("Time", "%02d:%02d".format(minutesUsed, secondsUsed), ProfessionalBlue)
              }
            }
          }
        }

        // Action Buttons
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            OutlinedButton(
              onClick = onPracticeAgain,
              modifier = Modifier.weight(1f).height(48.dp),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Practice Again")
            }

            Button(
              onClick = onNavigateToDashboard,
              modifier = Modifier.weight(1f).height(48.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              ),
              shape = RoundedCornerShape(10.dp)
            ) {
              Text("Dashboard")
            }
          }
        }

        // Review Header
        item {
          Text(
            text = "Detailed Question Review & Explanations",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 8.dp)
          )
        }

        // List of question reviews
        itemsIndexed(questions) { index, question ->
          val answerInfo = if (answersJson.has(question.id.toString())) {
            answersJson.getJSONObject(question.id.toString())
          } else {
            null
          }

          val chosen = answerInfo?.optString("chosen", "") ?: ""
          val isCorrect = answerInfo?.optBoolean("isCorrect", false) ?: false

          QuestionReviewCard(
            questionNumber = index + 1,
            question = question,
            chosenOption = chosen,
            isCorrect = isCorrect
          )
        }

        item {
          Spacer(modifier = Modifier.height(24.dp))
        }
      }
    }
  }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = color
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
fun QuestionReviewCard(
  questionNumber: Int,
  question: QuestionEntity,
  chosenOption: String,
  isCorrect: Boolean
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(
      1.dp,
      if (chosenOption.isEmpty()) Color(0xFFCBD5E1) else if (isCorrect) Color(0xFF86EFAC) else Color(0xFFFCA5A5)
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
          Text(
            text = "Question $questionNumber",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (chosenOption.isEmpty()) {
            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Unanswered", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
          } else if (isCorrect) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Correct (+1)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = SuccessGreen)
          } else {
            Icon(Icons.Default.Cancel, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Incorrect (0)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ErrorRed)
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = question.questionText,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Answer Comparison
      Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Your Answer:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            text = if (chosenOption.isNotEmpty()) "Option $chosenOption" else "None Selected",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = if (chosenOption.isEmpty()) Color(0xFF94A3B8) else if (isCorrect) SuccessGreen else ErrorRed
          )
        }

        Column(modifier = Modifier.weight(1f)) {
          Text("Correct Answer:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            text = "Option ${question.correctAnswer}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = SuccessGreen
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Explanation Box
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
          .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
          .padding(12.dp)
      ) {
        Column {
          Text(
            text = "Academic Explanation & Clinical Rationale:",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = question.explanation,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}
