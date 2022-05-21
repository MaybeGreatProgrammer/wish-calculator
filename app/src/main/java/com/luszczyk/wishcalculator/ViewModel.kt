package com.luszczyk.wishcalculator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import java.lang.Integer.max
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random


data class UiState(
    val wishes: String = "0",
    val pity: String = "0",
    val guarantee: Boolean = false,
    val selectedSampleIndex: Int = 0,
    val pulls: String = "0",
    val chanceToCheck: String = "0",
    val pityToCheck: String = "0",

    val wishResultSimulationResult: List<Triple<String, String, String>> = arrayListOf(),
    val wishResultSimulationJob: Job? = null,
    val wishResultSimulationParameters: List<String> = listOf(),
    val wishResultSimulationRunning: Boolean = false,

    val savingPityToChanceMap : MutableMap<Int, Int> = mutableMapOf(),
    val savingChanceToPityMap : MutableMap<Int, Int> = mutableMapOf(),
    val chances: List<Int> = listOf(),
    val pities: List<Int> = listOf(),

    val savingSimulationResult: List<Pair<String, String>> = listOf(),
    val savingSimulationJob: Job? = null,
    val savingSimulationParameters: List<String> = listOf(),
    val savingSimulationRunning: Boolean = false,

    val savingSimulationPityFromChance: String = "",
    val savingSimulationChanceFromPity: String = "",


    val screenOpened: Int = 0
)

class WishViewModel : ViewModel() {
    private val sampleMap = mapOf(0 to 1000, 1 to 10000, 2 to 100000)

    var uiState by mutableStateOf(UiState())

    private set

    fun toggleGuarantee(){
        uiState = uiState.copy(guarantee = !uiState.guarantee)
    }

    fun setSampleIndex(sampleIndex: Int){
        uiState = uiState.copy(selectedSampleIndex = sampleIndex)
    }

    fun setPity(pity : String){
        if (pity.length <= 2) {
            uiState = uiState.copy(pity = pity.filter { it.isDigit() })
        }
    }

    fun setWishes(wishes : String){
        if (wishes.length <= 3) {
            uiState = uiState.copy(wishes = wishes.filter { it.isDigit() })
        }
    }

    fun setPulls(pulls : String){
        if (pulls.length <= 1) {
            uiState = uiState.copy(pulls = pulls.filter { it.isDigit() })
        }
    }

    fun openScreen(screen: Int){
        uiState = uiState.copy(screenOpened = screen)
    }

    fun checkChance(chance : String){
        if (chance.length <= 5) {
            val filteredChance = chance.filter{ it.isDigit() || it == '.' }
            uiState = if(filteredChance.toDoubleOrNull() != null){
                val lookupChance = filteredChance.toBigDecimal().multiply(1000.toBigDecimal()).toInt()
                val closestChance = if(lookupChance != 0) uiState.chances.closestValue(lookupChance)
                else 0
                var result = uiState.savingChanceToPityMap[closestChance]
                if(result == null) result = 0
                uiState.copy(chanceToCheck = filteredChance, savingSimulationPityFromChance = result.toString() )
            } else {
                uiState.copy(chanceToCheck = filteredChance)
            }


        }
    }

    fun checkPity(pity : String){
        if (pity.length <= 4) {
            val filteredPity = pity.filter{ it.isDigit()}
            val lookupPity = filteredPity.toIntOrNull()
            var result = "0%"
            if(lookupPity != null){
                val closestPity = if(lookupPity != 0) uiState.pities.closestValue(lookupPity)
                else 0
                if(closestPity!=null){
                    if(uiState.savingPityToChanceMap.containsKey(closestPity)){
                        result = uiState.savingPityToChanceMap[closestPity]!!.toBigDecimal().divide(1000.toBigDecimal()).toString() + "%"
                    }
                }
            }
            uiState = uiState.copy(pityToCheck = filteredPity, savingSimulationChanceFromPity = result)
        }
    }

    fun simulateResults(){
        cancelResultsSimulation()
        val job =  viewModelScope.launch {
            withContext(Dispatchers.Default){
                val sampleSize = sampleMap[uiState.selectedSampleIndex]!!
                val increment: BigDecimal = (1-0.006).toBigDecimal().divide(17.toBigDecimal(), 8, RoundingMode.HALF_UP)
                val baseChance = 0.006.toBigDecimal()

                val pityParameter: Int = if(uiState.pity== ""){
                    0
                } else {
                    uiState.pity.toInt()
                }

                val wishParameter: Int = if(uiState.wishes== ""){
                    0
                } else {
                    uiState.wishes.toInt()
                }
                val guaranteeParameter =  uiState.guarantee

                val copiesPulled = mutableMapOf<Int, Int>()

                var samples = 0
                while(samples < sampleSize && isActive) {
                    var pity = pityParameter
                    var guarantee = guaranteeParameter
                    var wishesSpent = 0
                    var pulled = 0

                    while (wishesSpent < wishParameter){
                        wishesSpent += 1
                        pity += 1
                        val roll = Random.nextFloat()
                        var chance = baseChance

                        if(pity > 73){
                            val aboveSoftPity = pity - 73
                            chance = chance.add(aboveSoftPity.toBigDecimal().multiply(increment))
                        }

                        if(roll < chance.toDouble()){
                            if(guarantee){
                                guarantee = false
                                pulled += 1
                            } else {
                                if(Random.nextBoolean()){
                                    pulled += 1
                                } else {
                                    guarantee = true
                                }
                            }
                            pity = 0
                            continue
                        }
                    }
                    samples += 1
                    copiesPulled[(pulled)] = copiesPulled.getOrDefault(pulled, 0)+1
                }
                val mostPulled = copiesPulled.keys.maxOrNull() ?: 0
                val leastPulled = copiesPulled.keys.minOrNull() ?: 0
                var totalPulled = leastPulled

                val output : MutableList<Triple<String, String, String>> = arrayListOf(Triple("Copies Pulled", "Chance","Total Chance"))

                var previousChance = 100.toBigDecimal()

                while(totalPulled <= mostPulled && isActive) {
                    val instances = copiesPulled.getOrDefault(totalPulled, 0)
                    val chance = instances.toBigDecimal().divide(sampleSize.toBigDecimal()).times(100.toBigDecimal())
                    output.add(Triple(totalPulled.toString(), "${chance.setScale(max(chance.scale()-2,0), RoundingMode.HALF_UP)}%", "${previousChance.setScale(max(chance.scale()-2,0), RoundingMode.HALF_UP)}%"))
                    previousChance = previousChance.subtract(chance)
                    totalPulled += 1
                }
                if(isActive) {
                    uiState = uiState.copy(wishResultSimulationResult = output, wishResultSimulationRunning = false,
                        wishResultSimulationParameters = listOf(
                    wishParameter.toString(), pityParameter.toString(),
                    if(guaranteeParameter) "Yes" else "No", (sampleSize/1000).toString())) }
            }
        }
        uiState = uiState.copy(wishResultSimulationJob = job, wishResultSimulationRunning = true)
    }

    fun cancelResultsSimulation(){
        uiState.wishResultSimulationJob?.cancel()
        uiState = uiState.copy(wishResultSimulationJob = null, wishResultSimulationRunning = false)
    }

    fun simulateSaving(){
        cancelSavingSimulation()
        val job =  viewModelScope.launch {
            withContext(Dispatchers.Default){
                val sampleSize = sampleMap[uiState.selectedSampleIndex]!!
                val increment: BigDecimal = (1-0.006).toBigDecimal().divide(17.toBigDecimal(), 8, RoundingMode.HALF_UP)
                val baseChance = 0.006.toBigDecimal()

                val pityParameter: Int = if(uiState.pity== ""){
                    0
                } else {
                    uiState.pity.toInt()
                }

                val pullParameter: Int = if(uiState.pulls== ""){
                    0
                } else {
                    uiState.pulls.toInt()
                }
                val guaranteeParameter =  uiState.guarantee

                val totalsToPull = mutableMapOf<Int, Int>()

                var samples = 0
                while(samples < sampleSize && isActive) {
                    var pity = pityParameter
                    var guarantee = guaranteeParameter
                    var totalPulls = 0
                    var totalPity = -pity


                    while (totalPulls < pullParameter){
                        pity += 1
                        val roll = Random.nextFloat()
                        var chance = baseChance

                        if(pity > 73){
                            val aboveSoftPity = pity - 73
                            chance = chance.add(aboveSoftPity.toBigDecimal().multiply(increment))
                        }

                        if(roll < chance.toDouble()){
                            if(guarantee){
                                guarantee = false
                                totalPulls += 1
                            } else {
                                if(Random.nextBoolean()){
                                    totalPulls += 1
                                } else {
                                    guarantee = true
                                }
                            }
                            totalPity += pity
                            pity = 0
                            continue
                        }
                    }
                    samples += 1
                    if(totalPity<0) totalPity = 0
                    totalsToPull[(totalPity)] = totalsToPull.getOrDefault(totalPity, 0)+1
                }

                val sortedMap = totalsToPull.toSortedMap()
                val pityToChanceMap : MutableMap<Int, Int> = mutableMapOf()
                val chanceToPityMap: MutableMap<Int, Int> = mutableMapOf()
                var thisOrLessPityInstances = 0

                for(key in sortedMap.keys){
                    thisOrLessPityInstances += sortedMap.getOrDefault(key, 0)
                    val chance = thisOrLessPityInstances.toBigDecimal().divide(sampleSize.toBigDecimal()).times((100*1000).toBigDecimal())
                    pityToChanceMap[key] = chance.toInt()
                    chanceToPityMap[chance.toInt()] = key
                    if(!isActive) break
                }

                val chances: List<Int> = ArrayList<Int>(chanceToPityMap.keys)
                val pities: List<Int> = ArrayList<Int>(pityToChanceMap.keys)

                val result = listOf(
                    Pair("Chance", "Wishes"),
                    Pair("50.0%", chanceToPityMap[chances.closestValue(50*1000)].toString()),
                    Pair("66.6%", chanceToPityMap[chances.closestValue(66666)].toString()),
                    Pair("75.0%", chanceToPityMap[chances.closestValue(75*1000)].toString()),
                    Pair("90.0%", chanceToPityMap[chances.closestValue(90*1000)].toString()),
                    Pair("95.0%", chanceToPityMap[chances.closestValue(95*1000)].toString()),
                    Pair("99.0%", chanceToPityMap[chances.closestValue(99*1000)].toString()),
                    Pair("99.9%", chanceToPityMap[chances.closestValue(999*100)].toString()))

                if(isActive) {
                    uiState = uiState.copy(
                        savingSimulationResult = result, savingSimulationRunning = false,
                        savingSimulationParameters = listOf(
                            pullParameter.toString(), pityParameter.toString(),
                            if (guaranteeParameter) "Yes" else "No", (sampleSize / 1000).toString()
                        ),
                        chances = chances, savingChanceToPityMap = chanceToPityMap,
                        pities = pities, savingPityToChanceMap = pityToChanceMap
                    )
                    checkChance(uiState.chanceToCheck)
                    checkPity(uiState.pityToCheck)
                }

            }
        }
        uiState = uiState.copy(savingSimulationJob = job, savingSimulationRunning = true)
    }

    fun cancelSavingSimulation(){
        uiState.savingSimulationJob?.cancel()
        uiState = uiState.copy(savingSimulationJob = null, savingSimulationRunning = false)
    }

    private fun List<Int>.closestValue(value: Int) = minByOrNull{ kotlin.math.abs(value - it) }

}
