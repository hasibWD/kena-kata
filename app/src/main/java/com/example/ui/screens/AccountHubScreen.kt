package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.i18n.AppLanguage
import com.example.i18n.Strings
import com.example.ui.components.BangladeshDistricts
import com.example.ui.components.ProductCard
import com.example.ui.components.StatusPill
import com.example.ui.components.orderStatusColors
import com.example.ui.state.StoreViewModel
import com.example.ui.theme.*

enum class AccountSubTab(val labelEn: String, val labelBn: String) {
    PROFILE("Profile", "প্রোফাইল"),
    WISHLIST("Wishlist", "পছন্দ"),
    LOYALTY("Loyalty & Refer", "পয়েন্ট ও রেফার"),
    NOTIFICATIONS("Alerts", "নোটিফিকেশন"),
    SUPPORT("Support", "সাপোর্ট")
}

@Composable
fun AccountHubScreen(
    vm: StoreViewModel,
    initialSubTab: AccountSubTab = AccountSubTab.PROFILE,
    onOpenProduct: (String) -> Unit,
    onOpenAdminDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang by vm.language.collectAsStateWithLifecycle()
    val currentUser by vm.currentUser.collectAsStateWithLifecycle()
    val addresses by vm.addresses.collectAsStateWithLifecycle()
    val products by vm.products.collectAsStateWithLifecycle()
    val wishlistIds by vm.wishlistProductIds.collectAsStateWithLifecycle()
    val compareList by vm.compareProducts.collectAsStateWithLifecycle()
    val loyaltyTx by vm.loyaltyTransactions.collectAsStateWithLifecycle()
    val referrals by vm.referrals.collectAsStateWithLifecycle()
    val notifications by vm.notifications.collectAsStateWithLifecycle()
    val supportMessages by vm.userSupportMessages.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val aiConsent by vm.aiConsent.collectAsStateWithLifecycle()
    val notifConsent by vm.notificationConsent.collectAsStateWithLifecycle()

    val pendingVerificationEmail by vm.pendingVerificationEmail.collectAsStateWithLifecycle()

    var activeTab by remember(initialSubTab) { mutableStateOf(initialSubTab) }
    var showAdminLoginDialog by remember { mutableStateOf(false) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val user = currentUser
        if (uri != null && user != null) {
            vm.uploadAndSaveAvatar(user.name, user.phone, uri.toString())
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(SoftGrayBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // User Header / Quick Admin Entry
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepBlue),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(VibrantOrange)
                                    .clickable(enabled = currentUser != null) {
                                        avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentUser?.avatarUri?.isNotBlank() == true) {
                                    AsyncImage(
                                        model = currentUser!!.avatarUri,
                                        contentDescription = "Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = PureWhite, modifier = Modifier.size(30.dp))
                                }
                            }
                            Column {
                                Text(
                                    text = currentUser?.name ?: Strings.tr(lang, "Guest Shopper", "গেস্ট ক্রেতা"),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = PureWhite
                                )
                                Text(
                                    text = currentUser?.email ?: Strings.tr(lang, "Sign in for loyalty & sync", "পয়েন্ট ও অর্ডারের জন্য লগইন করুন"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DeepBlueContainer
                                )
                            }
                        }
                        if (currentUser != null) {
                            IconButton(onClick = { vm.logout() }, modifier = Modifier.testTag("logout_button")) {
                                Icon(Icons.Filled.Logout, contentDescription = "Logout", tint = PureWhite)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (currentUser?.role == "ADMIN") {
                            Button(
                                onClick = onOpenAdminDashboard,
                                colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
                                modifier = Modifier.weight(1f).testTag("open_admin_dashboard_button")
                            ) {
                                Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Strings.adminPortal(lang))
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showAdminLoginDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                                border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f).testTag("owner_admin_login_trigger")
                            ) {
                                Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Strings.tr(lang, "Owner Admin Portal", "অ্যাডমিন লগইন"))
                            }
                        }
                    }
                }
            }
        }

        // Sub-navigation chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AccountSubTab.entries) { tab ->
                    FilterChip(
                        selected = activeTab == tab,
                        onClick = {
                            activeTab = tab
                            if (tab == AccountSubTab.NOTIFICATIONS) vm.markNotificationsRead()
                        },
                        label = { Text(if (lang == AppLanguage.BN) tab.labelBn else tab.labelEn) },
                        modifier = Modifier.testTag("account_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }

        when (activeTab) {
            AccountSubTab.PROFILE -> {
                if (currentUser == null) {
                    item { CustomerAuthCard(vm = vm, lang = lang) }
                } else {
                    item {
                        CustomerProfileCard(
                            user = currentUser!!,
                            lang = lang,
                            aiConsent = aiConsent,
                            notifConsent = notifConsent,
                            onVerifyEmail = { vm.verifyEmail() },
                            onSaveProfile = { n, p -> vm.updateProfile(n, p) },
                            onAiConsent = { vm.setAiConsent(it) },
                            onNotifConsent = { vm.setNotificationConsent(it) }
                        )
                    }
                    item {
                        AddressBookCard(
                            addresses = addresses,
                            lang = lang,
                            onSaveAddress = { l, fn, ph, d, a, st -> vm.saveAddress(l, fn, ph, d, a, st) },
                            onDeleteAddress = { vm.deleteAddress(it) }
                        )
                    }
                }
            }

            AccountSubTab.WISHLIST -> {
                val savedProducts = products.filter { it.id in wishlistIds }
                if (savedProducts.isEmpty()) {
                    item {
                        EmptySubSectionCard(
                            icon = Icons.Filled.FavoriteBorder,
                            title = Strings.tr(lang, "Your Wishlist is Empty", "পছন্দের তালিকা খালি"),
                            subtitle = Strings.tr(lang, "Tap the heart icon on products to save them for later.", "পণ্য সংরক্ষণ করতে হার্ট আইকনে ট্যাপ করুন।")
                        )
                    }
                } else {
                    items(savedProducts.chunked(2)) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { p ->
                                ProductCard(
                                    product = p,
                                    language = lang,
                                    isWishlisted = true,
                                    isCompared = compareList.any { it.id == p.id },
                                    onClick = { onOpenProduct(p.id) },
                                    onAddToCart = { vm.addToCart(p) },
                                    onToggleWishlist = { vm.toggleWishlist(p.id) },
                                    onToggleCompare = { vm.toggleCompare(p.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            AccountSubTab.LOYALTY -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(Strings.tr(lang, "Loyalty Points Balance", "লয়্যালটি পয়েন্ট ব্যালেন্স"), style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${currentUser?.loyaltyPoints ?: 0} Points",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = VibrantOrange
                            )
                            Text(
                                Strings.tr(
                                    lang,
                                    "Earn ${settings.loyaltyEarnPer100Bdt} pts per ৳100 verified spend. 1 pt = ৳${settings.loyaltyPointValueBdt.toInt()} discount.",
                                    "প্রতি ৳১০০ কেনাকাটায় ${settings.loyaltyEarnPer100Bdt} পয়েন্ট পান।"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            if (currentUser != null) {
                                HorizontalDivider()
                                Text(Strings.tr(lang, "Your Referral Code", "আপনার রেফারেল কোড"), style = MaterialTheme.typography.titleSmall)
                                Surface(color = DeepBlueContainer, shape = RoundedCornerShape(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(currentUser!!.referralCode, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeepBlue)
                                        Text("${settings.referralRewardPoints} pts / invite", style = MaterialTheme.typography.labelMedium, color = VibrantOrangeDark)
                                    }
                                }
                                Text(
                                    "Invite link: https://kenakata.bd/r/${currentUser!!.referralCode} (${referrals.size} friends joined)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
                items(loyaltyTx, key = { it.id }) { tx ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, CardBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tx.description, style = MaterialTheme.typography.bodyMedium)
                                Text(tx.type, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                            Text(
                                text = (if (tx.pointsDelta >= 0) "+" else "") + "${tx.pointsDelta} pts",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (tx.pointsDelta >= 0) SuccessGreen else ErrorRed
                            )
                        }
                    }
                }
            }

            AccountSubTab.NOTIFICATIONS -> {
                if (notifications.isEmpty()) {
                    item {
                        EmptySubSectionCard(
                            icon = Icons.Filled.NotificationsNone,
                            title = Strings.tr(lang, "No Notifications", "কোনো নোটিফিকেশন নেই"),
                            subtitle = Strings.tr(lang, "Order, payment, and flash sale updates will appear here.", "অর্ডার ও পেমেন্ট আপডেট এখানে দেখা যাবে।")
                        )
                    }
                } else {
                    items(notifications, key = { it.id }) { n ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (n.isRead) PureWhite else DeepBlueContainer.copy(alpha = 0.45f)),
                            border = BorderStroke(1.dp, CardBorder),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(if (lang == AppLanguage.BN) n.titleBn else n.titleEn, style = MaterialTheme.typography.titleSmall, color = DeepBlue)
                                    StatusPill(n.type, OrangeContainer, OnOrangeContainer)
                                }
                                Text(if (lang == AppLanguage.BN) n.messageBn else n.messageEn, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            AccountSubTab.SUPPORT -> {
                item {
                    SupportCenterCard(
                        lang = lang,
                        whatsappNumber = settings.whatsappNumber,
                        defaultName = currentUser?.name.orEmpty(),
                        defaultEmail = currentUser?.email.orEmpty(),
                        messages = supportMessages,
                        onOpenWhatsApp = {
                            val url = "https://wa.me/${settings.whatsappNumber.filter { it.isDigit() }}"
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                        },
                        onSendMessage = { name, email, subj, msg ->
                            vm.sendSupportMessage(name, email, subj, msg)
                        }
                    )
                }
            }
        }
    }

    if (showAdminLoginDialog) {
        var adminEmail by remember { mutableStateOf("owner@kenakata.bd") }
        var adminPass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdminLoginDialog = false },
            icon = { Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = DeepBlue) },
            title = { Text(Strings.tr(lang, "Owner Admin Authentication", "স্টোর ওনার অ্যাডমিন লগইন")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        Strings.tr(
                            lang,
                            "Single-vendor owner access only. Default demo credentials: owner@kenakata.bd / Admin@2026",
                            "শুধুমাত্র স্টোর মালিকের জন্য। ডেমো: owner@kenakata.bd / Admin@2026"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = adminEmail,
                        onValueChange = { adminEmail = it },
                        label = { Text("Owner Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_email_input")
                    )
                    OutlinedTextField(
                        value = adminPass,
                        onValueChange = { adminPass = it },
                        label = { Text("Owner Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_password_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.loginAdmin(adminEmail, adminPass) {
                            showAdminLoginDialog = false
                            onOpenAdminDashboard()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                    modifier = Modifier.testTag("admin_login_submit_button")
                ) { Text("Sign In") }
            },
            dismissButton = {
                TextButton(onClick = { showAdminLoginDialog = false }) { Text("Cancel") }
            }
        )
    }

    pendingVerificationEmail?.let { targetEmail ->
        var otpCode by remember(targetEmail) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { vm.dismissVerificationPrompt() },
            icon = { Icon(Icons.Filled.MarkEmailRead, contentDescription = null, tint = DeepBlue) },
            title = { Text(Strings.tr(lang, "Verify Your Email", "ইমেইল ভেরিফাই করুন")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        Strings.tr(
                            lang,
                            "Enter the 6-digit verification code sent to $targetEmail.",
                            "$targetEmail এ পাঠানো ৬-অঙ্কের ভেরিফিকেশন কোড লিখুন।"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { otpCode = it },
                        label = { Text("6-Digit Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("email_otp_input")
                    )
                    TextButton(onClick = { vm.resendVerificationCode(targetEmail) }) {
                        Text(Strings.tr(lang, "Resend Verification Code", "কোড পুনরায় পাঠান"), color = VibrantOrange)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { vm.verifyEmailCode(targetEmail, otpCode) },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                    modifier = Modifier.testTag("verify_otp_submit_button")
                ) { Text(Strings.tr(lang, "Verify", "যাচাই করুন")) }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissVerificationPrompt() }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CustomerAuthCard(vm: StoreViewModel, lang: AppLanguage) {
    var mode by remember { mutableStateOf("LOGIN") } // LOGIN, REGISTER, FORGOT
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("hasibulhasan2087@gmail.com") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("customer123") }
    var referralCode by remember { mutableStateOf("") }
    var resetCode by remember { mutableStateOf("") }
    var resetCodeSent by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, CardBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == "LOGIN", onClick = { mode = "LOGIN" }, label = { Text(Strings.tr(lang, "Login", "লগইন")) })
                FilterChip(selected = mode == "REGISTER", onClick = { mode = "REGISTER" }, label = { Text(Strings.tr(lang, "Register", "নিবন্ধন")) })
                FilterChip(selected = mode == "FORGOT", onClick = { mode = "FORGOT"; resetCodeSent = false }, label = { Text(Strings.tr(lang, "Reset", "রিসেট")) })
            }

            if (mode == "REGISTER") {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Mobile (01XXX-XXXXXX)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (mode == "FORGOT") {
                if (!resetCodeSent) {
                    Text(
                        Strings.tr(
                            lang,
                            "Step 1: Request a 6-digit password reset code via email.",
                            "ধাপ ১: আপনার ইমেইলে ৬-অঙ্কের রিসেট কোড পাঠান।"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                } else {
                    OutlinedTextField(
                        value = resetCode,
                        onValueChange = { resetCode = it },
                        label = { Text("6-Digit Reset Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (mode == "REGISTER") {
                OutlinedTextField(value = referralCode, onValueChange = { referralCode = it }, label = { Text("Referral Code (Optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            Button(
                onClick = {
                    when (mode) {
                        "LOGIN" -> vm.loginCustomer(email, password)
                        "REGISTER" -> vm.registerCustomer(name, email, phone, password, referralCode)
                        "FORGOT" -> {
                            if (!resetCodeSent) {
                                vm.sendPasswordResetCode(email) { resetCodeSent = true }
                            } else {
                                vm.resetPasswordWithCode(email, resetCode, password) {
                                    resetCodeSent = false
                                    resetCode = ""
                                    mode = "LOGIN"
                                }
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("customer_auth_submit_button")
            ) {
                Text(
                    when (mode) {
                        "LOGIN" -> Strings.tr(lang, "Sign In as Customer", "ক্রেতা হিসেবে লগইন করুন")
                        "REGISTER" -> Strings.tr(lang, "Create Customer Account", "নতুন অ্যাকাউন্ট তৈরি করুন")
                        else -> if (!resetCodeSent) Strings.tr(lang, "Send Reset Code", "রিসেট কোড পাঠান") else Strings.tr(lang, "Confirm Password Reset", "পাসওয়ার্ড পরিবর্তন নিশ্চিত করুন")
                    }
                )
            }
            if (mode == "FORGOT" && resetCodeSent) {
                TextButton(onClick = { vm.sendPasswordResetCode(email) }) {
                    Text(Strings.tr(lang, "Resend Reset Code", "কোড আবার পাঠান"), color = VibrantOrange)
                }
            }
            Text(
                Strings.tr(lang, "Demo customer pre-filled: hasibulhasan2087@gmail.com / customer123", "ডেমো ক্রেতা প্রি-ফিল করা আছে।"),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun CustomerProfileCard(
    user: com.example.data.local.entity.UserEntity,
    lang: AppLanguage,
    aiConsent: Boolean,
    notifConsent: Boolean,
    onVerifyEmail: () -> Unit,
    onSaveProfile: (String, String) -> Unit,
    onAiConsent: (Boolean) -> Unit,
    onNotifConsent: (Boolean) -> Unit
) {
    var name by remember(user.id, user.name) { mutableStateOf(user.name) }
    var phone by remember(user.id, user.phone) { mutableStateOf(user.phone) }

    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, CardBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(Strings.tr(lang, "Account Details", "অ্যাকাউন্ট তথ্য"), style = MaterialTheme.typography.titleMedium)
                if (user.emailVerified) {
                    StatusPill("Email Verified", SuccessContainer, SuccessGreen)
                } else {
                    TextButton(onClick = onVerifyEmail) { Text("Verify Email", color = VibrantOrange) }
                }
            }
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = { onSaveProfile(name, phone) },
                colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
            ) {
                Text(Strings.tr(lang, "Save Profile", "প্রোফাইল সংরক্ষণ"))
            }
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(Strings.tr(lang, "Personalized Recommendations Consent", "পার্সোনালাইজড সুপারিশ অনুমতি"), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Switch(checked = aiConsent, onCheckedChange = onAiConsent)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(Strings.tr(lang, "Order & Cart Reminder Alerts", "অর্ডার ও কার্ট রিমাইন্ডার অনুমতি"), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Switch(checked = notifConsent, onCheckedChange = onNotifConsent)
            }
        }
    }
}

@Composable
private fun AddressBookCard(
    addresses: List<com.example.data.local.entity.AddressEntity>,
    lang: AppLanguage,
    onSaveAddress: (String, String, String, String, String, String) -> Unit,
    onDeleteAddress: (String) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, CardBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(Strings.tr(lang, "Saved Addresses", "সংরক্ষিত ঠিকানা"), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { showAdd = !showAdd }) {
                    Text(if (showAdd) "Close" else "+ Add New", color = VibrantOrange)
                }
            }
            addresses.forEach { addr ->
                Surface(color = SurfaceElevated, shape = RoundedCornerShape(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${addr.label} • ${addr.district}", style = MaterialTheme.typography.labelLarge, color = DeepBlue)
                            Text("${addr.fullName} (${addr.phone})", style = MaterialTheme.typography.bodySmall)
                            Text(addr.streetAddress, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        IconButton(onClick = { onDeleteAddress(addr.id) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = ErrorRed)
                        }
                    }
                }
            }
            if (showAdd) {
                var label by remember { mutableStateOf("Home") }
                var fullName by remember { mutableStateOf("") }
                var phone by remember { mutableStateOf("") }
                var area by remember { mutableStateOf("") }
                var street by remember { mutableStateOf("") }
                var district by remember { mutableStateOf(BangladeshDistricts.all.first()) }
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label (Home/Office)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Recipient Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("Area (e.g. Mirpur 10)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = street, onValueChange = { street = it }, label = { Text("Street Address") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        onSaveAddress(label, fullName, phone, district, area, street)
                        showAdd = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save Address") }
            }
        }
    }
}

@Composable
private fun SupportCenterCard(
    lang: AppLanguage,
    whatsappNumber: String,
    defaultName: String,
    defaultEmail: String,
    messages: List<com.example.data.local.entity.SupportMessageEntity>,
    onOpenWhatsApp: () -> Unit,
    onSendMessage: (String, String, String, String) -> Unit
) {
    var name by remember(defaultName) { mutableStateOf(defaultName) }
    var email by remember(defaultEmail) { mutableStateOf(defaultEmail) }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            border = BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(Strings.tr(lang, "Kena Kata Customer Care", "কেনা কাটা কাস্টমার কেয়ার"), style = MaterialTheme.typography.titleMedium)
                Button(
                    onClick = onOpenWhatsApp,
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("whatsapp_support_button")
                ) {
                    Icon(Icons.Filled.Chat, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("WhatsApp Support ($whatsappNumber)")
                }
                HorizontalDivider()
                Text(Strings.tr(lang, "Send Message / Live Support Ticket", "সরাসরি মেসেজ পাঠান"), style = MaterialTheme.typography.titleSmall)
                if (name.isBlank()) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Message") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        onSendMessage(name, email, subject, body)
                        subject = ""
                        body = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) { Text("Send Support Message") }
            }
        }

        // FAQ
        Card(
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            border = BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("FAQ", style = MaterialTheme.typography.titleMedium)
                Text("• Delivery: 24-48h inside Dhaka (৳60, free over ৳1,500), 2-4 days nationwide (৳120).", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("• Payments: Manual bKash/Nagad TrxID verified by Owner Admin, plus Cash on Delivery.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("• Returns: 7-day return window for unopened/defective items.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        // Support History
        messages.forEach { m ->
            val (bg, fg) = orderStatusColors(m.status)
            Card(colors = CardDefaults.cardColors(containerColor = PureWhite), border = BorderStroke(1.dp, CardBorder), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(m.subject, style = MaterialTheme.typography.titleSmall)
                        StatusPill(m.status, bg, fg)
                    }
                    Text(m.message, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    if (m.adminReply.isNotBlank()) {
                        Surface(color = DeepBlueContainer, shape = RoundedCornerShape(8.dp)) {
                            Text("Owner Reply: ${m.adminReply}", style = MaterialTheme.typography.bodySmall, color = DeepBlue, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySubSectionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, CardBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = DeepBlue, modifier = Modifier.size(44.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}
