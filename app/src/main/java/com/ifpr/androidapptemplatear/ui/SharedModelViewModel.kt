package com.ifpr.androidapptemplatear.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ifpr.androidapptemplatear.R
import java.util.UUID
import kotlin.random.Random

class SharedModelViewModel : ViewModel() {
    val selectedModel = MutableLiveData<String>()
    internal var mapRound: MapRound? = null

    fun selectModel(modelPath: String) {
        selectedModel.value = modelPath
    }
}

internal class MapRound(val latitude: Double, val longitude: Double) {
    val id: String = UUID.randomUUID().toString()
    private val availablePets = List(15) { index ->
        val type = PetType.entries[index % PetType.entries.size]
        MapPet(
            index,
            type,
            latitude + Random.nextDouble(-0.004, 0.004),
            longitude + Random.nextDouble(-0.004, 0.004)
        )
    }.associateBy { it.id }.toMutableMap()

    val pets: Collection<MapPet> get() = availablePets.values

    var score = 0
        private set
    var encounter: MapPet? = null
        private set
    var escapedPet: MapPet? = null
        private set
    var treatsRemaining = MAX_TREATS
        private set

    fun openEncounter(petId: Int): Boolean {
        if (escapedPet != null || encounter != null) return false
        encounter = availablePets[petId] ?: return false
        treatsRemaining = MAX_TREATS
        return true
    }

    fun throwTreat(petApproached: Boolean): ThrowResult? {
        val pet = encounter ?: return null
        treatsRemaining--
        if (petApproached) {
            score += POINTS_PER_CAPTURE
            availablePets.remove(pet.id)
            encounter = null
            return ThrowResult.CAPTURED
        }
        if (treatsRemaining == 0) {
            escapedPet = pet
            availablePets.remove(pet.id)
            encounter = null
            return ThrowResult.ESCAPED
        }
        return ThrowResult.MISSED
    }

    companion object {
        const val MAX_TREATS = 3
        const val POINTS_PER_CAPTURE = 100
    }
}

internal enum class ThrowResult { CAPTURED, MISSED, ESCAPED }

internal enum class PetType(val emoji: String, val nameRes: Int, val color: String) {
    CAT("🐱", R.string.pet_cat, "#FF6B9D"),
    GOLDEN("🐕", R.string.pet_golden, "#FFB347"),
    DOG("🐶", R.string.pet_dog, "#87CEEB")
}

internal data class MapPet(
    val id: Int,
    val type: PetType,
    val latitude: Double,
    val longitude: Double
)
