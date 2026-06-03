package com.example.rgbcontrller.presentation.screens.effects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rgbcontrller.data.mock.AppContainer
import com.example.rgbcontrller.domain.model.EffectCategory
import com.example.rgbcontrller.domain.model.LightEffect
import com.example.rgbcontrller.domain.repository.EffectRepository
import com.example.rgbcontrller.domain.repository.LightRepository
import com.example.rgbcontrller.presentation.ui.components.AuroraBackground
import com.example.rgbcontrller.presentation.ui.components.EffectMarketCard
import com.example.rgbcontrller.presentation.ui.components.LedMatrixPreview
import com.example.rgbcontrller.presentation.ui.components.MiniEffectPreview
import com.example.rgbcontrller.presentation.ui.components.PageTitle

class EffectsViewModel(
    private val effectRepository: EffectRepository = AppContainer.effectRepository,
    private val lightRepository: LightRepository = AppContainer.lightRepository,
) : ViewModel() {
    var selectedCategory by mutableStateOf<EffectCategory?>(null)
        private set

    val session = lightRepository.session

    fun effects(): List<LightEffect> {
        return selectedCategory?.let { category ->
            effectRepository.effects.filter { it.category == category }
        } ?: effectRepository.effects
    }

    fun selectCategory(category: EffectCategory?) {
        selectedCategory = category
    }

    fun effectById(id: String): LightEffect? = effectRepository.effects.firstOrNull { it.id == id }

    fun apply(effect: LightEffect) {
        lightRepository.applyEffect(effect)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EffectsScreen(
    onOpenEffect: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EffectsViewModel = viewModel(),
) {
    AuroraBackground(modifier.fillMaxSize()) {
        Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    PageTitle("Effects", "Discover light scenes and expressive animations")
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = viewModel.selectedCategory == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text("全部") },
                        )
                        EffectCategory.entries.forEach { category ->
                            FilterChip(
                                selected = viewModel.selectedCategory == category,
                                onClick = { viewModel.selectCategory(category) },
                                label = { Text(category.label) },
                            )
                        }
                    }
                }
                items(viewModel.effects(), key = { it.id }) { effect ->
                    EffectMarketCard(effect = effect, onClick = { onOpenEffect(effect.id) })
                }
            }
        }
    }
}

@Composable
fun EffectDetailScreen(
    effectId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EffectsViewModel = viewModel(),
) {
    val state by viewModel.session.collectAsState()
    val effect = viewModel.effectById(effectId)
    AuroraBackground(modifier.fillMaxSize()) {
        Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                PageTitle(effect?.name ?: "Effect", effect?.description)
                effect?.let {
                    MiniEffectPreview(it.palette, Modifier.fillMaxWidth().height(180.dp))
                    LedMatrixPreview(state.matrix, title = "Parameter preview")
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.apply(it)
                            onBack()
                        },
                    ) {
                        Text("Apply effect")
                    }
                }
            }
        }
    }
}
