package com.example.ui.subscription

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.SubscriptionEntity
import com.example.data.local.entity.UserEntity
import com.example.data.repository.SubscriptionPlan
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ProfessionalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.SubscriptionViewModel
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
  user: UserEntity,
  subscriptionViewModel: SubscriptionViewModel,
  onNavigateBack: () -> Unit
) {
  val context = LocalContext.current
  val plans = subscriptionViewModel.availablePlans
  val subscribeState by subscriptionViewModel.subscribeState.collectAsState()

  var selectedPlanForCheckout by remember { mutableStateOf<SubscriptionPlan?>(null) }
  var showSuccessReceiptDialog by remember { mutableStateOf<SubscriptionEntity?>(null) }

  // Paystack configuration state
  var paystackPublicKey by remember { mutableStateOf("pk_live_medrad_unimaid_portal") }
  var isLiveMode by remember { mutableStateOf(true) }
  var showPaystackSettings by remember { mutableStateOf(false) }

  BackHandler {
    if (showSuccessReceiptDialog != null) {
      showSuccessReceiptDialog = null
    } else if (selectedPlanForCheckout != null) {
      selectedPlanForCheckout = null
    } else if (showPaystackSettings) {
      showPaystackSettings = false
    } else {
      onNavigateBack()
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "MEDRAD Premium Pass",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
          }
        },
        actions = {
          IconButton(onClick = { showPaystackSettings = !showPaystackSettings }) {
            Icon(Icons.Default.Settings, contentDescription = "Paystack Configuration", tint = MaterialTheme.colorScheme.onBackground)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
          titleContentColor = MaterialTheme.colorScheme.onBackground,
          navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
          actionIconContentColor = MaterialTheme.colorScheme.onBackground
        )
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(MaterialTheme.colorScheme.background)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // 1. Paystack Live Banner / Settled Account Info
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
        border = BorderStroke(1.dp, Color(0xFF86EFAC))
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(Color(0xFF0BA4DB).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF0BA4DB), modifier = Modifier.size(20.dp))
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Secured by Paystack Nigeria",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isLiveMode) Color(0xFF16A34A) else Color(0xFFF59E0B)
              ) {
                Text(
                  text = if (isLiveMode) "LIVE REALISTIC" else "TEST SANDBOX",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                  color = Color.White,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
              }
            }
            Text(
              text = "Bank Settlements: Direct deposits to your registered Nigerian Bank account (T+1 Payout).",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      // Expandable Paystack Settings / Key Input
      if (showPaystackSettings) {
        Spacer(modifier = Modifier.height(10.dp))
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "Paystack Gateway Credentials",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Paste your personal or business Paystack Public Key (from paystack.com Settings → API Keys). All student payments process through your linked account.",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = paystackPublicKey,
              onValueChange = { paystackPublicKey = it },
              label = { Text("Paystack Public Key (pk_live_... / pk_test_...)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End
            ) {
              TextButton(onClick = { showPaystackSettings = false }) {
                Text("Save Configuration")
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Status Banner if already Premium
      if (user.isPremium) {
        val expiryFormatted = remember(user.premiumExpiry) {
          if (user.premiumExpiry != null) {
            SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(user.premiumExpiry))
          } else "Active"
        }

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.12f)),
          border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f))
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Premium Access Active",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "${user.premiumPlan ?: "Active Plan"} • Valid until $expiryFormatted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
      }

      // App Caduceus Logo & Hero Header
      Box(
        modifier = Modifier
          .size(68.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Image(
          painter = painterResource(id = R.drawable.medrad_logo),
          contentDescription = "MEDRAD Caduceus Emblem",
          modifier = Modifier.size(56.dp).clip(CircleShape)
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "Unlimited University CBT & Study Prep",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
      )

      Text(
        text = "Select a plan below to unlock full RAD 101 question banks, unlimited daily practice, and comprehensive lecture notes.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Plans Display (Section 6)
      plans.forEach { plan ->
        PlanCard(
          plan = plan,
          isSelected = user.premiumPlan == plan.name,
          onSelect = { selectedPlanForCheckout = plan }
        )
        Spacer(modifier = Modifier.height(16.dp))
      }

      // Feature Comparison Box
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BorderSubtle)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, contentDescription = null, tint = ProfessionalBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Official MEDRAD Benefits Included",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          val benefits = listOf(
            "Unlimited CBT questions per day (bypass free 15 question limit)",
            "RAD 101 complete departmental question bank",
            "Comprehensive mock examinations with live countdown timer",
            "In-depth clinical explanations for every answer option",
            "Premium lecture notes & past question solutions",
            "Detailed performance analytics and weak topic breakdown",
            "Multi-institution expansion ready"
          )

          benefits.forEach { benefit ->
            Row(
              modifier = Modifier.padding(vertical = 4.dp),
              verticalAlignment = Alignment.Top
            ) {
              Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp).padding(top = 2.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(benefit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }

  // 2. Realistic Paystack Nigeria Checkout Modal Sheet
  if (selectedPlanForCheckout != null) {
    val plan = selectedPlanForCheckout!!

    PaystackCheckoutModal(
      user = user,
      plan = plan,
      isLiveMode = isLiveMode,
      publicKey = paystackPublicKey,
      onDismiss = {
        selectedPlanForCheckout = null
        subscriptionViewModel.resetState()
      },
      onPaymentSuccess = { channel, reference ->
        subscriptionViewModel.subscribe(user, plan, channel, reference)
      }
    )
  }

  // Handle successful subscription
  LaunchedEffect(subscribeState) {
    if (subscribeState is UiState.Success) {
      showSuccessReceiptDialog = (subscribeState as UiState.Success<SubscriptionEntity>).data
      selectedPlanForCheckout = null
    }
  }

  // 3. Official Paystack Settlement & Activation Receipt
  if (showSuccessReceiptDialog != null) {
    val sub = showSuccessReceiptDialog!!
    PaystackSuccessReceiptDialog(
      subscription = sub,
      onDismiss = {
        showSuccessReceiptDialog = null
        subscriptionViewModel.resetState()
        onNavigateBack()
      }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaystackCheckoutModal(
  user: UserEntity,
  plan: SubscriptionPlan,
  isLiveMode: Boolean,
  publicKey: String,
  onDismiss: () -> Unit,
  onPaymentSuccess: (channel: String, reference: String) -> Unit
) {
  val context = LocalContext.current
  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Card, 1 = Bank Transfer, 2 = USSD
  val tabs = listOf("Card", "Bank Transfer", "USSD")

  // Card Inputs
  var cardNumber by remember { mutableStateOf("4084 0840 0840 0840") }
  var cardExpiry by remember { mutableStateOf("12/28") }
  var cardCvv by remember { mutableStateOf("408") }
  var isProcessing by remember { mutableStateOf(false) }
  var processingMessage by remember { mutableStateOf("Connecting to Paystack...") }

  // Virtual Account for Bank Transfer
  val virtualAccountNumber = "9938210492"
  val virtualBank = "Wema Bank (Paystack)"

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
      // Paystack Branded Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(Color(0xFF0BA4DB)),
            contentAlignment = Alignment.Center
          ) {
            Text("P", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "paystack",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.ExtraBold,
              color = Color(0xFF0BA4DB),
              letterSpacing = 0.5.sp
            )
          )
        }

        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Merchant and Order Info
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
      ) {
        Column {
          Text(
            text = user.email,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "MEDRAD • ${plan.name}",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        Text(
          text = plan.formattedPrice,
          style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
          color = MaterialTheme.colorScheme.primary
        )
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      Spacer(modifier = Modifier.height(10.dp))

      // Channel Tabs: Card, Transfer, USSD
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
            color = Color(0xFF0BA4DB)
          )
        }
      ) {
        tabs.forEachIndexed { index, title ->
          Tab(
            selected = selectedTab == index,
            onClick = { selectedTab = index },
            text = {
              Text(
                title,
                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                color = if (selectedTab == index) Color(0xFF0BA4DB) else MaterialTheme.colorScheme.onSurfaceVariant
              )
            },
            icon = {
              when (index) {
                0 -> Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp))
                1 -> Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp))
                else -> Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp))
              }
            }
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      if (isProcessing) {
        // Live Paystack Payment Simulation in Progress
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF0BA4DB))
            Spacer(modifier = Modifier.height(14.dp))
            Text(processingMessage, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
            Text("Communicating with Nigerian Inter-Bank Settlement System (NIBSS)...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      } else {
        when (selectedTab) {
          0 -> {
            // CARD PAYMENT TAB
            Column {
              OutlinedTextField(
                value = cardNumber,
                onValueChange = { cardNumber = it },
                label = { Text("Card Number") },
                placeholder = { Text("4084 0840 0840 0840") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color(0xFF0BA4DB)) },
                trailingIcon = {
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFE2E8F0)
                  ) {
                    Text("VERVE / VISA / MC", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), modifier = Modifier.padding(4.dp))
                  }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("paystack_card_input"),
                shape = RoundedCornerShape(10.dp)
              )

              Spacer(modifier = Modifier.height(10.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                OutlinedTextField(
                  value = cardExpiry,
                  onValueChange = { cardExpiry = it },
                  label = { Text("Expires (MM/YY)") },
                  singleLine = true,
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                  value = cardCvv,
                  onValueChange = { cardCvv = it },
                  label = { Text("CVV") },
                  singleLine = true,
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(10.dp)
                )
              }

              Spacer(modifier = Modifier.height(8.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  "Paystack Test Card Loaded",
                  style = MaterialTheme.typography.labelSmall,
                  color = SuccessGreen
                )
                TextButton(
                  onClick = {
                    cardNumber = "4084 0840 0840 0840"
                    cardExpiry = "12/28"
                    cardCvv = "408"
                  }
                ) {
                  Text("Fill Test Card", style = MaterialTheme.typography.labelSmall, color = Color(0xFF0BA4DB))
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              Button(
                onClick = {
                  isProcessing = true
                  processingMessage = "Contacting Card Issuer & Bank..."
                  // Trigger payment with realistic async delays
                  val ref = "pstk_card_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6).lowercase()}"
                  android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    processingMessage = "Authenticating OTP & Approving..."
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                      isProcessing = false
                      onPaymentSuccess("CARD (Mastercard)", ref)
                    }, 1200)
                  }, 1200)
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(50.dp)
                  .testTag("paystack_pay_card_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0BA4DB))
              ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pay ${plan.formattedPrice}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
              }
            }
          }

          1 -> {
            // BANK TRANSFER TAB (Very popular in Nigeria)
            Column {
              Text(
                text = "Transfer exactly ${plan.formattedPrice} to this dedicated Paystack virtual account:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              Spacer(modifier = Modifier.height(10.dp))

              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0xFFF1F5F9))
                  .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                  .padding(14.dp)
              ) {
                Column {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text("Bank Name", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(virtualBank, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column {
                      Text("Account Number", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      Text(
                        text = virtualAccountNumber,
                        style = MaterialTheme.typography.titleLarge.copy(
                          fontWeight = FontWeight.ExtraBold,
                          fontFamily = FontFamily.Monospace,
                          letterSpacing = 1.sp
                        ),
                        color = Color(0xFF0BA4DB)
                      )
                    }

                    OutlinedButton(
                      onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Account Number", virtualAccountNumber))
                        Toast.makeText(context, "Account number copied!", Toast.LENGTH_SHORT).show()
                      },
                      shape = RoundedCornerShape(8.dp)
                    ) {
                      Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Copy")
                    }
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text("Account Name", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Paystack / MEDRAD-UNIMAID", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                  }
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp), tint = WarningAmber)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Expires in 29:55 minutes. Instant automated confirmation.", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
              }

              Spacer(modifier = Modifier.height(14.dp))

              Button(
                onClick = {
                  isProcessing = true
                  processingMessage = "Checking bank transfer confirmation..."
                  val ref = "pstk_trf_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6).lowercase()}"
                  android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    processingMessage = "Transfer received from Nigerian Bank!"
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                      isProcessing = false
                      onPaymentSuccess("BANK TRANSFER (Wema Bank)", ref)
                    }, 1200)
                  }, 1200)
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(50.dp)
                  .testTag("paystack_sent_transfer_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0BA4DB))
              ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("I have sent the ${plan.formattedPrice} transfer", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
              }
            }
          }

          2 -> {
            // USSD TAB
            Column {
              Text(
                text = "Dial the generated Paystack USSD code on your mobile phone:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              Spacer(modifier = Modifier.height(12.dp))

              val ussdCode = "*737*50*3500*819#"
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                  .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                  .padding(14.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text("GTBank USSD Code", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                      ussdCode,
                      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                      color = MaterialTheme.colorScheme.onSurface
                    )
                  }

                  IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("USSD Code", ussdCode))
                    Toast.makeText(context, "USSD code copied!", Toast.LENGTH_SHORT).show()
                  }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                  }
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              Button(
                onClick = {
                  isProcessing = true
                  processingMessage = "Confirming USSD session..."
                  val ref = "pstk_ussd_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6).lowercase()}"
                  android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    isProcessing = false
                    onPaymentSuccess("USSD (*737#)", ref)
                  }, 1500)
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0BA4DB))
              ) {
                Text("I have dialed the USSD code")
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF64748B))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "256-bit SSL Encrypted • Direct Settlement to Administrator Account",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
          color = Color(0xFF64748B)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
fun PaystackSuccessReceiptDialog(
  subscription: SubscriptionEntity,
  onDismiss: () -> Unit
) {
  val dateFormatted = remember(subscription.createdAt) {
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(subscription.createdAt))
  }
  val expiryFormatted = remember(subscription.expiryDate) {
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(subscription.expiryDate))
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Box(
        modifier = Modifier
          .size(54.dp)
          .clip(CircleShape)
          .background(Color(0xFFD1FAE5)),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(36.dp))
      }
    },
    title = {
      Text(
        text = "Payment Approved & Settled!",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        textAlign = TextAlign.Center
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "Your Paystack transaction has been verified. Premium benefits and unlimited CBT access are now active.",
          style = MaterialTheme.typography.bodySmall,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Official Receipt Box
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(14.dp)
        ) {
          Column {
            ReceiptRow("Amount Paid:", "₦${subscription.amount.toInt()}")
            ReceiptRow("Plan:", subscription.planName)
            ReceiptRow("Valid Until:", expiryFormatted)
            ReceiptRow("Payment Channel:", subscription.paymentReference.substringAfter("[").substringBefore("]"))
            ReceiptRow("Transaction Ref:", subscription.paymentReference.substringBefore(" [").take(22) + "...")
            ReceiptRow("Bank Settlement:", "Next Business Day (T+1)")
            ReceiptRow("Date & Time:", dateFormatted)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().testTag("receipt_done_button")
      ) {
        Text("Go to Unlimited Dashboard")
      }
    }
  )
}

@Composable
fun ReceiptRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 3.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
  }
}

@Composable
fun PlanCard(
  plan: SubscriptionPlan,
  isSelected: Boolean,
  onSelect: () -> Unit
) {
  val isRecommended = plan.durationDays == 30

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable { onSelect() }
      .testTag("plan_card_${plan.id}"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(if (isRecommended) 2.dp else 1.dp, if (isRecommended) ProfessionalBlue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isRecommended) 3.dp else 1.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = plan.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "${plan.durationDays} Days Access",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = plan.formattedPrice,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
          )
          if (isRecommended) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color(0xFFFEF3C7)
            ) {
              Text(
                text = "BEST VALUE",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = Color(0xFF92400E),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = plan.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      Spacer(modifier = Modifier.height(12.dp))

      plan.features.take(4).forEach { feature ->
        Row(
          modifier = Modifier.padding(vertical = 3.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(feature, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth().height(46.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isRecommended) MaterialTheme.colorScheme.primary else ProfessionalBlue
        ),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("Select ${plan.name}")
      }
    }
  }
}
