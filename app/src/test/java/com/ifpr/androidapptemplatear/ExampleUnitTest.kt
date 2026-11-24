package com.ifpr.androidapptemplatear

import com.ifpr.androidapptemplatear.ui.MapRound
import com.ifpr.androidapptemplatear.ui.PetType
import com.ifpr.androidapptemplatear.ui.ThrowResult
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun thirdMissEndsRoundAndANewRoundStartsWithThreeTreats() {
        val round = MapRound(-23.5505, -46.6333)
        val dog = round.pets.first { it.type == PetType.DOG }
        val cat = round.pets.first { it.type == PetType.CAT }
        assertTrue(round.openEncounter(cat.id))
        assertEquals(ThrowResult.CAPTURED, round.throwTreat(petApproached = true))
        assertTrue(round.openEncounter(dog.id))

        assertEquals(ThrowResult.MISSED, round.throwTreat(petApproached = false))
        assertEquals(2, round.treatsRemaining)
        assertEquals(ThrowResult.MISSED, round.throwTreat(petApproached = false))
        assertEquals(1, round.treatsRemaining)
        assertNull(round.escapedPet)
        assertEquals(ThrowResult.ESCAPED, round.throwTreat(petApproached = false))

        assertEquals(dog, round.escapedPet)
        assertEquals(100, round.score)
        assertFalse(round.pets.contains(dog))
        assertFalse(round.openEncounter(round.pets.first().id))
        assertNull(round.throwTreat(petApproached = true))
        assertEquals(100, round.score)

        val restarted = MapRound(round.latitude, round.longitude)
        assertEquals(0, restarted.score)
        assertNull(restarted.escapedPet)
        assertNull(restarted.encounter)
        assertEquals(15, restarted.pets.size)
        assertTrue(restarted.openEncounter(dog.id))
        assertEquals(3, restarted.treatsRemaining)
    }

    @Test
    fun bothDogTypesEarn100PointsAndCanBeCapturedOnTheLastTreat() {
        val round = MapRound(-23.5505, -46.6333)
        val dog = round.pets.first { it.type == PetType.DOG }
        val golden = round.pets.first { it.type == PetType.GOLDEN }
        assertTrue(round.openEncounter(dog.id))
        assertFalse(round.openEncounter(golden.id))
        repeat(2) { assertEquals(ThrowResult.MISSED, round.throwTreat(petApproached = false)) }
        assertEquals(0, round.score)

        assertEquals(ThrowResult.CAPTURED, round.throwTreat(petApproached = true))
        assertEquals(100, round.score)
        assertNull(round.escapedPet)
        assertFalse(round.openEncounter(dog.id))
        assertTrue(round.openEncounter(golden.id))
        assertEquals(3, round.treatsRemaining)
        assertEquals(ThrowResult.CAPTURED, round.throwTreat(petApproached = true))
        assertEquals(200, round.score)
    }
}
