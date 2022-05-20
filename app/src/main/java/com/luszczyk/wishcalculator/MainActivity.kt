package com.luszczyk.wishcalculator

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.luszczyk.wishcalculator.ui.theme.WishCalculatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val wishViewModel: WishViewModel by viewModels()
        setContent {
            WishCalculatorTheme {
                MainScreen(wishViewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: WishViewModel){
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    val uiState = viewModel.uiState
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        val guarantee = uiState.guarantee
        val selectedSampleIndex = uiState.selectedSampleIndex
        val screenOpened = uiState.screenOpened
        val resultRunning = uiState.wishResultSimulationRunning
        val savingRunning = uiState.savingSimulationRunning

        Column(horizontalAlignment = Alignment.CenterHorizontally){
            ButtonGroup(value = screenOpened, onChange = {viewModel.openScreen(it)})
            Column(modifier= Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colors.primary,
                    shape = RoundedCornerShape(size = 16.dp)
                )
                .wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,) {
                PaddedRow(arrangement = Arrangement.SpaceBetween){
                    Text(text = "Parameters:", modifier = Modifier.padding(8.dp))
                    PaddedRow(arrangement = Arrangement.Center) {
                        NumberEntry(label = "Pity", value = uiState.pity, onChange = {viewModel.setPity(it)})
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Guarantee", modifier = Modifier.padding(4.dp))
                            Checkbox(checked = guarantee,
                                onCheckedChange = {viewModel.toggleGuarantee()},
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.primary))
                        }
                    }
                }

                PaddedRow(arrangement = Arrangement.SpaceBetween) {
                    Text(text = "Sample Size:", modifier = Modifier.padding(8.dp))
                    SampleSelectorGroup(value = selectedSampleIndex, onChange = {viewModel.setSampleIndex(it)})
                }
                if(screenOpened == Screen.RESULTS.index){
                    Text(text = "Wishes:")
                    NumberEntry(label = "Wishes", value = uiState.wishes, onChange = {viewModel.setWishes(it)})
                } else {
                    Text(text = "Event 5⭐ Pulls:")
                    NumberEntry(label = "5⭐", value = uiState.pulls, onChange = {viewModel.setPulls(it)})
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.weight(1f))

                    val cancelResults = {viewModel.cancelResultsSimulation()}
                    val cancelSaving = {viewModel.cancelSavingSimulation()}
                    val runResults = {viewModel.simulateResults()}
                    val runSaving = {viewModel.simulateSaving()}
                    val buttonFunction: () -> Unit

                    val cancelColors = ButtonDefaults.buttonColors(backgroundColor = Color.Red, contentColor = Color.White)
                    val simulateColors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary, contentColor = MaterialTheme.colors.background)
                    val buttonColors: ButtonColors

                    val simulateText = "SIMULATE!"
                    val cancelText = "CANCEL"
                    val buttonText: String

                    if(screenOpened == Screen.RESULTS.index){
                        if(resultRunning){
                            buttonColors = cancelColors
                            buttonText = cancelText
                            buttonFunction = cancelResults
                        } else {
                            buttonColors = simulateColors
                            buttonText = simulateText
                            buttonFunction = runResults
                        }
                    } else {
                        if(savingRunning){
                            buttonColors = cancelColors
                            buttonText = cancelText
                            buttonFunction = cancelSaving
                        } else {
                            buttonColors = simulateColors
                            buttonText = simulateText
                            buttonFunction = runSaving
                        }
                    }

                    OutlinedButton(colors = buttonColors,
                        modifier = Modifier.padding(vertical = 16.dp).weight(1f),
                        onClick = buttonFunction) {
                        Text(text = buttonText)
                    }
                    Box(Modifier.weight(1f)) {
                        if(( screenOpened == Screen.RESULTS.index && resultRunning) || (screenOpened == Screen.SAVING.index && savingRunning)){
                            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                        }
                    }
                }

            }
            if(screenOpened == Screen.RESULTS.index){
                val parameters = uiState.wishResultSimulationParameters
                val result = uiState.wishResultSimulationResult
                if(parameters.isNotEmpty()){
                    Text(text = "Wishes: ${parameters[0]} Pity: ${parameters[1]} Guarantee: ${parameters[2]} Samples: ${parameters[3]}K")
                }
                ResultsList(results = result)
            } else {
                val parameters = uiState.savingSimulationParameters
                if(parameters.isNotEmpty()){
                    Text(text = "Pulls: ${parameters[0]} Pity: ${parameters[1]} Guarantee: ${parameters[2]} Samples: ${parameters[3]}K")
                }
                val result = uiState.savingSimulationResult
                if(result.isNotEmpty()){
                    Row(modifier= Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colors.primary,
                            shape = RoundedCornerShape(size = 16.dp)
                        )
                        .wrapContentHeight()){
                        LazyColumn(modifier = Modifier
                            .weight(1.5f)
                            .wrapContentWidth()
                            .padding(vertical = 4.dp)) {
                            items(result) { item ->
                                SavingItem(item)
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.weight(2f)) {
                            val chanceToCheck = uiState.chanceToCheck
                            val pityToCheck = uiState.pityToCheck

                            PaddedRow(arrangement = Arrangement.SpaceBetween) {
                                NumberEntry(label = "Chance (%)", value = chanceToCheck, onChange = {viewModel.checkChance(it)})
                                Text(text = "${uiState.savingSimulationPityFromChance} Wishes")
                            }
                            PaddedRow(arrangement = Arrangement.SpaceBetween) {
                                NumberEntry(label = "Wishes", value = pityToCheck, onChange = {viewModel.checkPity(it)})
                                Text(text = "${uiState.savingSimulationChanceFromPity} Chance")
                            }
                        }
                    }
                }
            }
        }

    }
}

@Composable
fun ResultsList(results: List<Triple<String, String, String>>){
    LazyColumn(modifier= Modifier
        .fillMaxWidth()
        .padding(8.dp)
        .border(
            width = 6.dp,
            color = MaterialTheme.colors.primary,
            shape = RoundedCornerShape(size = 48.dp)
        )
        .wrapContentHeight()) {
        items(results) { result ->
            ResultItem(result)
        }
    }
}

@Composable
fun ResultItem(result: Triple<String, String, String>){
    Row(horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp, horizontal = 16.dp)) {
            Text(text = result.first, modifier = Modifier.weight(1f))
            Text(text = result.second, modifier = Modifier.weight(1f))
            Text(text = result.third, modifier = Modifier.weight(0.5f))
    }
}


@Composable
fun SavingItem(result: Pair<String, String>){
    Row(horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 4.dp, horizontal = 16.dp)
            .fillMaxWidth()) {
        Text(text = result.first)
        Text(text = result.second)
    }
}

@Composable
fun SampleSelectorGroup(value: Int, onChange: (Int) -> Unit){
    PaddedRow(arrangement = Arrangement.Center) {
        SampleSelectorButton(index = 0, selected = value,
            label = "1K", color = Color.Green, onChange = onChange)
        SampleSelectorButton(index = 1, selected = value,
            label = "10K", color = Color.Yellow, onChange = onChange)
        SampleSelectorButton(index = 2, selected = value,
            label = "100K", color = Color.Red, onChange = onChange)
    }
}

@Composable
fun SampleSelectorButton(index: Int, selected: Int,label: String, color: Color, onChange: (Int) -> Unit){
    OutlinedButton(onClick = { onChange(index) },
        colors = if(selected == index) {
            ButtonDefaults.buttonColors(backgroundColor = color,
                contentColor = MaterialTheme.colors.background)
        } else {
            ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.background,
                contentColor = color)
        }, border = BorderStroke(width = 1.dp, color = color), modifier = Modifier.padding(4.dp)
    ) {
        Text(text = label)
    }
}

@Composable
fun NumberEntry(label: String, value: String, onChange: (String) -> Unit) {

    OutlinedTextField(
        label = { Text(text = label)},
        value = value,
        singleLine = true,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .size(width = 80.dp, height = 60.dp)
            .wrapContentHeight(),
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        onValueChange = {
             onChange(it)
        },
        colors = TextFieldDefaults.outlinedTextFieldColors(unfocusedBorderColor = MaterialTheme.colors.primary)
    )

}

@Composable
fun ButtonGroup(value: Int, onChange: (Int) -> Unit){
    val cornerRadius = 8.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))

        val items = arrayListOf("Wish Results", "Wish Saving")
        items.forEachIndexed { index, _ ->
            OutlinedButton(
                onClick = {onChange(index)},
                shape = when (index) {
                    0 -> RoundedCornerShape(topStart = cornerRadius, topEnd = 0.dp, bottomStart = cornerRadius, bottomEnd = 0.dp)
                    items.size - 1 -> RoundedCornerShape(topStart = 0.dp, topEnd = cornerRadius, bottomStart = 0.dp, bottomEnd = cornerRadius)
                    else -> RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                },
                border = BorderStroke(1.dp, if(value == index) { MaterialTheme.colors.primary } else { Color.DarkGray.copy(alpha = 0.75f)}),
                colors = if(value == index) {
                    // selected colors
                    ButtonDefaults.outlinedButtonColors(backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colors.primary)
                } else {
                    // not selected colors
                    ButtonDefaults.outlinedButtonColors(backgroundColor = MaterialTheme.colors.surface, contentColor = MaterialTheme.colors.primary)
                },
                modifier = when (index){
                    0 -> {
                        if (value == index) {
                            Modifier
                                .offset(0.dp, 0.dp)
                                .zIndex(1f)
                        } else {
                            Modifier
                                .offset(0.dp, 0.dp)
                                .zIndex(0f)
                        }
                    }
                    else -> {
                        val offset = -1 * index
                        if (value == index) {
                            Modifier
                                .offset(offset.dp, 0.dp)
                                .zIndex(1f)
                        } else {
                            Modifier
                                .offset(offset.dp, 0.dp)
                                .zIndex(0f)
                        }
                    }
                },) {
                Text(
                    text = items[index],
                    color = if(value == index) { MaterialTheme.colors.primary } else { Color.DarkGray.copy(alpha = 0.9f) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun PaddedRow(arrangement: Arrangement.Horizontal, content: @Composable () -> Unit){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = arrangement,
        verticalAlignment = Alignment.CenterVertically) {
        content()
    }
}

@Composable
fun LockScreenOrientation(orientation: Int) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context.findActivity() ?: return@DisposableEffect onDispose {}
        val originalOrientation = activity.requestedOrientation
        activity.requestedOrientation = orientation
        onDispose {
            // restore original orientation when view disappears
            activity.requestedOrientation = originalOrientation
        }
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

enum class Screen(val index: Int) {
    RESULTS(0),
    SAVING(1),
}