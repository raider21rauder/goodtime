/**
 *     Goodtime Productivity
 *     Copyright (C) 2025 Adrian Cotfas
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apps.adrcotfas.goodtime.billing

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.main.ProListItem
import com.apps.adrcotfas.goodtime.shared.R
import com.apps.adrcotfas.goodtime.ui.common.SubtleHorizontalDivider
import com.apps.adrcotfas.goodtime.ui.common.TopBar
import org.koin.compose.koinInject

@Composable
fun ProScreen(
    billing: GoogleBilling = koinInject<BillingAbstract>() as GoogleBilling,
    onNavigateBack: () -> Unit,
) {
    val activity = LocalActivity.current
    val productDetails by billing.productDetails.collectAsStateWithLifecycle()

    val isPro by billing.isPro.collectAsStateWithLifecycle(false)
    val isPending by billing.purchasePending.collectAsStateWithLifecycle(false)

    if (isPending || isPro) {
        onNavigateBack()
    }

    productDetails.let {
        val offerDetails = it?.oneTimePurchaseOfferDetails
        if (it == null || activity == null || offerDetails == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val priceString = offerDetails.formattedPrice
            ProScreenContent(onNavigateBack, priceString) { billing.buy(activity) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProScreenContent(onNavigateBack: () -> Unit, priceString: String, onClick: () -> Unit) {
    val listState = rememberScrollState()
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.unlock_premium),
                icon = Icons.Default.Close,
                onNavigateBack = { onNavigateBack() },
                showSeparator = listState.canScrollBackward,
            )
        },
        bottomBar = {
            if (listState.canScrollForward) {
                SubtleHorizontalDivider()
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.unlock_premium_tagline),
                    style = MaterialTheme.typography.labelSmall.copy(textAlign = TextAlign.Center),
                )
                ProListItem(subtitle = priceString, centered = true) {
                    onClick()
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(Modifier.verticalScroll(listState)) {
                val productName = stringResource(R.string.product_name_long)
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(R.string.unlock_premium_desc1, productName) + "\n" +
                        "\n" +
                        stringResource(R.string.unlock_premium_desc2) +
                        "\n" +
                        stringResource(R.string.unlock_premium_desc3),
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                )
            }
        }
        // TODO: features promo cards
    }
}

@Preview
@Composable
fun ProScreenPreview() {
    ProScreenContent(onNavigateBack = {}, onClick = {}, priceString = "42 USD")
}
