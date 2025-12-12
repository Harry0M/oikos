package com.theblankstate.epmanager.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// FOUNDATION COLORS
// ==========================================
val Black = Color(0xFF0A0A0A)
val White = Color(0xFFFAFAFA)

// ==========================================
// GRAY SCALE
// ==========================================
val Gray50 = Color(0xFFF9FAFB)
val Gray100 = Color(0xFFF3F4F6)
val Gray200 = Color(0xFFE5E7EB)
val Gray300 = Color(0xFFD1D5DB)
val Gray400 = Color(0xFF9CA3AF)
val Gray500 = Color(0xFF6B7280)
val Gray600 = Color(0xFF4B5563)
val Gray700 = Color(0xFF374151)
val Gray800 = Color(0xFF1F2937)
val Gray900 = Color(0xFF111827)

// ==========================================
// PRIMARY ACCENT - ROSE (Brand Color)
// ==========================================
val Rose = Color(0xFFA85751)
val RoseLight = Color(0xFFF5E6E5)
val RoseDark = Color(0xFF8B3D38)
val RoseBg = Color(0xFF1A0F0E)

// ==========================================
// SECONDARY ACCENTS
// ==========================================
// Blue
val Blue = Color(0xFF3B82F6)
val BlueLight = Color(0xFFDBEAFE)
val BlueDark = Color(0xFF1D4ED8)

// Purple
val Purple = Color(0xFF8B5CF6)
val PurpleLight = Color(0xFFEDE9FE)
val PurpleDark = Color(0xFF6D28D9)

// Teal
val Teal = Color(0xFF14B8A6)
val TealLight = Color(0xFFCCFBF1)
val TealDark = Color(0xFF0D9488)

// Amber
val Amber = Color(0xFFF59E0B)
val AmberLight = Color(0xFFFEF3C7)
val AmberDark = Color(0xFFD97706)

// Green (for Income)
val Green = Color(0xFF22C55E)
val GreenLight = Color(0xFFDCFCE7)
val GreenDark = Color(0xFF16A34A)

// ==========================================
// SEMANTIC COLORS
// ==========================================
val Success = Green
val SuccessLight = GreenLight
val SuccessDark = GreenDark

val Warning = Amber
val WarningLight = AmberLight
val WarningDark = AmberDark

val Error = Color(0xFFEF4444)
val ErrorLight = Color(0xFFFEE2E2)
val ErrorDark = Color(0xFF991B1B)

val Info = Blue
val InfoLight = BlueLight
val InfoDark = BlueDark

// ==========================================
// EXPENSE CATEGORY COLORS
// ==========================================
object CategoryColors {
    val Food = Color(0xFFFF6B6B)
    val Transport = Color(0xFF4ECDC4)
    val Shopping = Color(0xFFFFE66D)
    val Entertainment = Color(0xFF95E1D3)
    val Bills = Color(0xFFF38181)
    val Healthcare = Color(0xFFAA96DA)
    val Education = Color(0xFF74B9FF)
    val Travel = Color(0xFFFD79A8)
    val Groceries = Color(0xFF55A3FF)
    val Subscriptions = Color(0xFFA29BFE)
    val Other = Gray400
}