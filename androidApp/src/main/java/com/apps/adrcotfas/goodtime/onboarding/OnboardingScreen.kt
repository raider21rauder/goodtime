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
package com.apps.adrcotfas.goodtime.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.adrcotfas.goodtime.R
import com.apps.adrcotfas.goodtime.common.isPortrait
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private val lightGray = Color(0xFFDEDEDE)
private val darkGray = Color(0xFF4C4546)

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = koinViewModel()) {
    val pages = OnboardingPage.pages
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    BackHandler(pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            HorizontalPager(
                state = pagerState,
            ) { page ->
                OnboardingPage(
                    title = stringResource(pages[page].title),
                    description = stringResource(pages[page].description),
                    image = {
                        Image(
                            painter = painterResource(id = pages[page].image),
                            contentDescription = stringResource(pages[page].title),
                        )
                    },
                )
            }
        }

        val isLastPage = pagerState.currentPage == pages.lastIndex

        FloatingActionButton(
            modifier = Modifier.align(Alignment.BottomEnd).padding(32.dp).size(72.dp),
            containerColor = lightGray,
            contentColor = darkGray,
            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
            shape = CircleShape,
            onClick = {
                if (isLastPage) {
                    viewModel.setOnboardingFinished(true)
                } else {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            },
        ) {
            Crossfade(isLastPage, label = "onboarding button") {
                if (it) {
                    Icon(Icons.Filled.Check, contentDescription = "Finish")
                } else {
                    Icon(
                        Icons.AutoMirrored.Default.ArrowForward,
                        contentDescription = "Finish",
                    )
                }
            }
        }

        OnboardingPageIndicator(
            modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
            pageCount = pages.size,
            currentPage = pagerState.currentPage,
        )
    }
}

@Composable
fun OnboardingPage(
    title: String,
    description: String,
    image: @Composable () -> Unit,
) {
    val isPortrait = LocalConfiguration.current.isPortrait
    if (isPortrait) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            image()
            Spacer(modifier = Modifier.padding(16.dp))
            OnboardingPageTextSection(title, description)
        }
    } else {
        Row(
            modifier = Modifier
                .wrapContentSize()
                .padding(64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            image()
            Spacer(modifier = Modifier.padding(8.dp))
            OnboardingPageTextSection(title, description)
        }
    }
}

@Composable
fun OnboardingPageTextSection(title: String, description: String) {
    Column(
        modifier = Modifier.widthIn(max = 320.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val textColor = Color(0xFF333333)
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = textColor)
        Spacer(modifier = Modifier.padding(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
            color = textColor,
        )
    }
}

@Composable
fun OnboardingPageIndicator(modifier: Modifier = Modifier, pageCount: Int, currentPage: Int) {
    Row(
        modifier
            .height(18.dp)
            .widthIn(max = 128.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(pageCount) { iteration ->
            val color = if (currentPage == iteration) darkGray else lightGray
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(color)
                    .size(10.dp),

            )
        }
    }
}

@Preview
@Composable
fun OnboardingPagePreview() {
    OnboardingPage(
        title = stringResource(R.string.intro_title_1),
        description = stringResource(R.string.intro_description_1),
        image = {
            Image(
                painter = painterResource(id = R.drawable.intro1),
                contentDescription = "Title",
            )
        },
    )
}
