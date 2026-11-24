package com.ifpr.androidapptemplatear.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.ifpr.androidapptemplatear.R
import java.util.UUID
import kotlin.random.Random

class SharedModelViewModel : ViewModel() {
    val selectedModel = MutableLiveData<String>()
    private val _pontuacao = MutableLiveData(0)
    val pontuacao: LiveData<Int> = _pontuacao
    private val pontuacaoPartida = PontuacaoPartida { _pontuacao.value = it }
    internal val visitaLocalizacao = EstadoVisita()
    internal var mapRound: MapRound? = null
        private set

    fun selectModel(modelPath: String) {
        selectedModel.value = modelPath
    }

    fun registrarEncontro(arquivo: String) {
        if (pontuacaoPartida.encerrada) return
        pontuacaoPartida.registrarModelo(arquivo)
        selectModel(arquivo)
    }

    internal fun iniciarMapa(latitude: Double, longitude: Double): MapRound {
        if (pontuacaoPartida.encerrada) {
            visitaLocalizacao.lugarAtual = null
            pontuacaoPartida.reiniciar()
        }
        return MapRound(latitude, longitude, pontuacaoPartida).also { mapRound = it }
    }
}

internal class EstadoVisita {
    var lugarAtual: Int? = null
}

internal class PontuacaoPartida(private val publicar: (Int) -> Unit = {}) {
    private val modelosEncontrados = mutableSetOf<String>()
    var total = 0
        private set
    var encerrada = false
        private set

    fun registrarModelo(arquivo: String) {
        if (!encerrada && modelosEncontrados.add(arquivo)) registrarCaptura()
    }

    fun registrarCaptura() {
        if (encerrada) return
        total += PONTOS_POR_ACAO
        publicar(total)
    }

    fun encerrar() {
        encerrada = true
    }

    fun reiniciar() {
        modelosEncontrados.clear()
        encerrada = false
        total = 0
        publicar(total)
    }

    companion object {
        const val PONTOS_POR_ACAO = 100
    }
}

internal class MapRound(
    val latitude: Double,
    val longitude: Double,
    private val pontuacao: PontuacaoPartida = PontuacaoPartida()
) {
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

    val score: Int get() = pontuacao.total
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
            pontuacao.registrarCaptura()
            availablePets.remove(pet.id)
            encounter = null
            return ThrowResult.CAPTURED
        }
        if (treatsRemaining == 0) {
            escapedPet = pet
            pontuacao.encerrar()
            availablePets.remove(pet.id)
            encounter = null
            return ThrowResult.ESCAPED
        }
        return ThrowResult.MISSED
    }

    companion object {
        const val MAX_TREATS = 3
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
