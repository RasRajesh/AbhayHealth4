package com.example.abhayhealth.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Modern, smooth rounded shapes adhering to M3 and high-end healthcare aesthetics
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),      // Smooth standard cards & dialogs
    large = RoundedCornerShape(22.dp),       // Smooth hero cards & modal bottom sheets
    extraLarge = RoundedCornerShape(30.dp)   // Smooth pills & elevated panels
)
