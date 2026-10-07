package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MedicalTeal
import com.example.ui.theme.ProfessionalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

data class DrawPath(
  val path: List<Offset>,
  val color: Color,
  val strokeWidth: Float = 6f
)

@Composable
fun RadiographicDrawingCanvas(
  modifier: Modifier = Modifier,
  instructionText: String,
  isSubmitted: Boolean = false,
  onDrawingSubmitted: (strokeCount: Int) -> Unit
) {
  val paths = remember { mutableStateListOf<DrawPath>() }
  var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }
  val availableColors = listOf(Color(0xFF0F172A), ErrorRed, ProfessionalBlue, WarningAmber, SuccessGreen)
  var selectedColor by remember { mutableStateOf(availableColors[0]) }
  var hasSubmittedSketch by remember { mutableStateOf(isSubmitted) }

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = androidx.compose.foundation.BorderStroke(1.5.dp, if (hasSubmittedSketch) SuccessGreen else BorderSubtle)
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
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Outlined.Draw, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Radiographic Sketch / Annotation Canvas",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        if (hasSubmittedSketch) {
          Surface(
            color = SuccessGreen.copy(alpha = 0.15f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Sketch Saved", style = MaterialTheme.typography.labelSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = SuccessGreen)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = instructionText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Toolbar: Color selection, Undo, Clear
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Color swatches
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          availableColors.forEach { color ->
            val isSelected = selectedColor == color
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                  width = if (isSelected) 3.dp else 1.dp,
                  color = if (isSelected) WarningAmber else Color.White,
                  shape = CircleShape
                )
                .clickable { selectedColor = color }
            )
          }
        }

        // Undo & Clear buttons
        Row {
          IconButton(
            onClick = {
              if (paths.isNotEmpty()) {
                paths.removeAt(paths.lastIndex)
              }
            },
            enabled = paths.isNotEmpty()
          ) {
            Icon(Icons.Outlined.Undo, contentDescription = "Undo", tint = MaterialTheme.colorScheme.onSurface)
          }

          IconButton(
            onClick = {
              paths.clear()
              currentPath = emptyList()
              hasSubmittedSketch = false
            },
            enabled = paths.isNotEmpty()
          ) {
            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Clear", tint = ErrorRed)
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Interactive Drawing Canvas
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(260.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFFFAFAFA))
          .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
      ) {
        Canvas(
          modifier = Modifier
            .fillMaxSize()
            .pointerInput(selectedColor) {
              detectDragGestures(
                onDragStart = { offset ->
                  currentPath = listOf(offset)
                },
                onDrag = { change, _ ->
                  currentPath = currentPath + change.position
                },
                onDragEnd = {
                  if (currentPath.isNotEmpty()) {
                    paths.add(DrawPath(path = currentPath, color = selectedColor))
                    currentPath = emptyList()
                  }
                },
                onDragCancel = {
                  currentPath = emptyList()
                }
              )
            }
        ) {
          // Grid lines for radiographic alignment guide
          val step = 40.dp.toPx()
          for (x in 0..size.width.toInt() step step.toInt()) {
            drawLine(
              color = Color(0xFFF1F5F9),
              start = Offset(x.toFloat(), 0f),
              end = Offset(x.toFloat(), size.height),
              strokeWidth = 1f
            )
          }
          for (y in 0..size.height.toInt() step step.toInt()) {
            drawLine(
              color = Color(0xFFF1F5F9),
              start = Offset(0f, y.toFloat()),
              end = Offset(size.width, y.toFloat()),
              strokeWidth = 1f
            )
          }

          // Draw committed paths
          paths.forEach { drawPath ->
            if (drawPath.path.size > 1) {
              val p = Path().apply {
                moveTo(drawPath.path.first().x, drawPath.path.first().y)
                for (i in 1 until drawPath.path.size) {
                  lineTo(drawPath.path[i].x, drawPath.path[i].y)
                }
              }
              drawPath(
                path = p,
                color = drawPath.color,
                style = Stroke(width = drawPath.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
              )
            }
          }

          // Draw active path being drawn
          if (currentPath.size > 1) {
            val p = Path().apply {
              moveTo(currentPath.first().x, currentPath.first().y)
              for (i in 1 until currentPath.size) {
                lineTo(currentPath[i].x, currentPath[i].y)
              }
            }
            drawPath(
              path = p,
              color = selectedColor,
              style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
          }
        }

        if (paths.isEmpty() && currentPath.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Touch and drag to sketch or mark radiographic landmarks",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF94A3B8)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Save/Confirm Sketch Button
      Button(
        onClick = {
          hasSubmittedSketch = true
          onDrawingSubmitted(paths.size)
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(46.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (hasSubmittedSketch) SuccessGreen else MaterialTheme.colorScheme.primary,
          contentColor = if (hasSubmittedSketch) Color.White else MaterialTheme.colorScheme.onPrimary
        ),
        enabled = paths.isNotEmpty()
      ) {
        Icon(
          imageVector = if (hasSubmittedSketch) Icons.Outlined.Check else Icons.Outlined.Draw,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (hasSubmittedSketch) "Drawing Response Recorded (${paths.size} strokes)" else "Record & Confirm Drawing Answer",
          style = MaterialTheme.typography.labelLarge
        )
      }
    }
  }
}
