package dev.yilliee.iotventure.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.yilliee.iotventure.ui.viewmodels.MapViewModel
import dev.yilliee.iotventure.ui.components.MapView
import dev.yilliee.iotventure.ui.components.ChallengeInfoCard
import dev.yilliee.iotventure.data.model.Challenge

@Composable
fun MapScreen(
    onNavigateToChallenge: (Int) -> Unit,
    viewModel: MapViewModel = viewModel()
) {
    val challenges by viewModel.challenges.collectAsState()
    val selectedChallenge by viewModel.selectedChallenge.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadChallenges()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapView(
            challenges = challenges,
            onChallengeSelected = { challenge ->
                viewModel.selectChallenge(challenge)
            }
        )

        selectedChallenge?.let { challenge ->
            ChallengeInfoCard(
                challenge = challenge,
                onNavigateToChallenge = {
                    onNavigateToChallenge(challenge.id)
                },
                onDismiss = {
                    viewModel.clearSelectedChallenge()
                }
            )
        }
    }
} 