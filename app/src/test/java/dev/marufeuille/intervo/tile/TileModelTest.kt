package dev.marufeuille.intervo.tile

import dev.marufeuille.intervo.data.WorkoutWithCount
import org.junit.Assert.assertEquals
import org.junit.Test

class TileModelTest {

    private fun wc(id: String, name: String, count: Int) =
        WorkoutWithCount(id = id, name = name, sortOrder = 0, exerciseCount = count)

    @Test
    fun `先頭から最大3件を取りモデルへ写す`() {
        val all = (1..5).map { wc("id$it", "種目$it", it) }
        val result = tileWorkouts(all)
        assertEquals(3, result.size)
        assertEquals(listOf("種目1", "種目2", "種目3"), result.map { it.name })
        assertEquals(listOf("id1", "id2", "id3"), result.map { it.id })
        assertEquals(listOf(1, 2, 3), result.map { it.exerciseCount })
    }

    @Test
    fun `3件未満はそのまま`() {
        val all = listOf(wc("a", "A", 1), wc("b", "B", 2))
        assertEquals(2, tileWorkouts(all).size)
    }

    @Test
    fun `空なら空`() {
        assertEquals(emptyList<TileWorkout>(), tileWorkouts(emptyList()))
    }
}
