package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Official Emblem for the Department of Medical Radiography,
 * College of Medical Sciences, University of Maiduguri.
 * Displays the distinctive golden circular radiation trefoil seal with caduceus.
 */
@Composable
fun MedicalRadiographyEmblem(
  modifier: Modifier = Modifier,
  size: Dp = 48.dp,
  showBorder: Boolean = true,
  borderColor: Color = Color(0xFFF59E0B) // Amber gold
) {
  Box(
    modifier = modifier
      .size(size)
      .clip(CircleShape)
      .then(
        if (showBorder) Modifier.border(2.dp, borderColor, CircleShape)
        else Modifier
      )
      .background(Color.White),
    contentAlignment = Alignment.Center
  ) {
    Image(
      painter = painterResource(id = R.drawable.medrad_official_logo),
      contentDescription = "Department of Medical Radiography Emblem",
      modifier = Modifier
        .size(size)
        .clip(CircleShape),
      contentScale = ContentScale.Crop
    )
  }
}
