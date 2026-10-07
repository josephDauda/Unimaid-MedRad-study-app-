package com.example.ui.welcome

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import kotlinx.coroutines.isActive
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.MedicalRadiographyEmblem
import com.example.ui.theme.AcademicNavy
import com.example.ui.theme.MedicalTeal
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class WelcomeSlideData(
  val imageRes: Int,
  val categoryTag: String,
  val title: String,
  val subtitle: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(
  onNavigateToRegister: () -> Unit,
  onNavigateToLogin: () -> Unit,
  onNavigateToAbout: () -> Unit = {}
) {
  val slides = listOf(
    WelcomeSlideData(
      imageRes = R.drawable.medrad_slide_1,
      categoryTag = "CLINICAL RADIOGRAPHY & ONCOLOGY",
      title = "Medical Radiography Academic Platform",
      subtitle = "University of Maiduguri • College of Medical Sciences. Access course materials, curriculum past questions, and diagnostic learning activities in one place."
    ),
    WelcomeSlideData(
      imageRes = R.drawable.medrad_slide_2,
      categoryTag = "EXAM SIMULATIONS & CBT",
      title = "Course-Specific Quizzes & CBT Tests",
      subtitle = "Practice with curriculum-aligned questions, radiographic diagram annotations, and timed mock examinations curated by department faculty."
    ),
    WelcomeSlideData(
      imageRes = R.drawable.medrad_slide_3,
      categoryTag = "ACADEMIC COMMUNITY & EXCELLENCE",
      title = "MedRad Study Squad",
      subtitle = "Stay connected with instant departmental announcements, performance tracking, and verified revision packs for undergraduate radiographers."
    )
  )

  val pagerState = rememberPagerState(pageCount = { slides.size })
  val coroutineScope = rememberCoroutineScope()

  // Auto-advance timer (every 6 seconds) with smooth non-blocking execution
  LaunchedEffect(pagerState) {
    while (isActive) {
      delay(6000)
      if (!pagerState.isScrollInProgress) {
        val nextPage = (pagerState.currentPage + 1) % slides.size
        pagerState.animateScrollToPage(nextPage)
      }
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isShortScreen = maxHeight < 680.dp
    val isTablet = maxWidth >= 600.dp

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
    ) {
      // 1. Full-bleed Image Transition / Slider
      HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
      ) { page ->
        val slide = slides[page]
        Box(modifier = Modifier.fillMaxSize()) {
          Image(
            painter = painterResource(id = slide.imageRes),
            contentDescription = slide.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )

          // Multi-stop contrast scrim to ensure text and buttons remain 100% readable
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(
                    AcademicNavy.copy(alpha = 0.88f),
                    AcademicNavy.copy(alpha = 0.35f),
                    AcademicNavy.copy(alpha = 0.70f),
                    AcademicNavy.copy(alpha = 0.98f)
                  ),
                  startY = 0f,
                  endY = Float.POSITIVE_INFINITY
                )
              )
          )
        }
      }

      // 2. Foreground Adaptive UI (Respecting window breakpoint)
      Column(
        modifier = Modifier
          .fillMaxHeight()
          .then(if (isTablet) Modifier.widthIn(max = 640.dp).fillMaxWidth() else Modifier.fillMaxWidth())
          .align(Alignment.Center)
          .statusBarsPadding()
          .navigationBarsPadding()
          .then(if (isShortScreen) Modifier.verticalScroll(rememberScrollState()) else Modifier)
          .padding(horizontal = 24.dp, vertical = if (isShortScreen) 12.dp else 20.dp),
        verticalArrangement = if (isShortScreen) Arrangement.spacedBy(16.dp) else Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // TOP: Department Logo, Institutional Title & About Link
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            MedicalRadiographyEmblem(
              size = if (isShortScreen) 44.dp else 52.dp,
              showBorder = true,
              borderColor = WarningAmber
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
              Text(
                text = "DEPARTMENT OF MEDICAL RADIOGRAPHY",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.1.sp
                ),
                color = WarningAmber
              )
              Text(
                text = "College of Medical Sciences • UNIMAID",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
              )
            }
          }

          // Subtle About App button
          Surface(
            onClick = onNavigateToAbout,
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.15f),
            modifier = Modifier.size(38.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                Icons.Outlined.Info,
                contentDescription = "About App",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }

        if (!isShortScreen) {
          Spacer(modifier = Modifier.weight(1f))
        } else {
          Spacer(modifier = Modifier.height(24.dp))
        }

        // CENTER / LOWER: Slide Content & Dynamic Intro
        AnimatedContent(
          targetState = pagerState.currentPage,
          transitionSpec = {
            (fadeIn(animationSpec = tween(320)) + slideInVertically(animationSpec = tween(320)) { it / 6 }) togetherWith (
              fadeOut(animationSpec = tween(180)) + slideOutVertically(animationSpec = tween(180)) { -it / 8 }
            )
          },
          label = "WelcomeSlideContentTransition"
        ) { page ->
          val currentSlide = slides[page]
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.Start
          ) {
          // Pill Category Tag
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = MedicalTeal.copy(alpha = 0.25f),
            border = BorderStroke(1.dp, MedicalTeal.copy(alpha = 0.5f))
          ) {
            Text(
              text = currentSlide.categoryTag,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = Color(0xFF5EEAD4),
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = "MedRad Study App",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp
            ),
            color = WarningAmber
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = currentSlide.title,
            style = MaterialTheme.typography.headlineMedium.copy(
              fontWeight = FontWeight.Bold,
              lineHeight = 32.sp
            ),
            color = Color.White
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = currentSlide.subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(
              lineHeight = 21.sp,
              fontWeight = FontWeight.Normal
            ),
            color = Color(0xFFE2E8F0)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Page Indicator Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            slides.indices.forEach { index ->
              val isSelected = pagerState.currentPage == index
              Box(
                modifier = Modifier
                  .height(8.dp)
                  .width(if (isSelected) 28.dp else 8.dp)
                  .clip(CircleShape)
                  .background(
                    if (isSelected) WarningAmber else Color.White.copy(alpha = 0.35f)
                  )
                  .clickable {
                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                  }
              )
            }
          }
        }

        // BOTTOM: Clear Call-to-Action Buttons
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Button(
            onClick = onNavigateToRegister,
            modifier = Modifier
              .fillMaxWidth()
              .height(54.dp)
              .testTag("welcome_create_account_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = WarningAmber,
              contentColor = Color.Black
            )
          ) {
            Text(
              text = "Create Account",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
          }

          OutlinedButton(
            onClick = onNavigateToLogin,
            modifier = Modifier
              .fillMaxWidth()
              .height(54.dp)
              .testTag("welcome_login_button"),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.85f)),
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = Color.White
            )
          ) {
            Text(
              text = "Sign In",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
          }
        }
      }
    }
  }
}
