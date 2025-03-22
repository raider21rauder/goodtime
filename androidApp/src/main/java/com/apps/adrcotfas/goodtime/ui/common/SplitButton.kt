package com.apps.adrcotfas.goodtime.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

//TODO: explore this for time quick time profile changes
@Preview
@Composable
fun SplitButton() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Button(
            onClick = { /*TODO*/ },
            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
        ) {
            Text("Edit time profile")
        }
        FilledIconButton(
            onClick = { /*TODO*/ },
            shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        ) {
            Icon(Icons.Default.ArrowDropDown, "")
        }
    }
}
