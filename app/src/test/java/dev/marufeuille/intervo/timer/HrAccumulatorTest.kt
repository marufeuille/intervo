package dev.marufeuille.intervo.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HrAccumulatorTest {

    @Test
    fun `empty accumulator reports null stats`() {
        val acc = HrAccumulator()
        assertNull(acc.startHr())
        assertNull(acc.avgHr())
        assertNull(acc.maxHr())
        assertEquals(emptyList<Any>(), acc.exerciseRecords())
    }

    @Test
    fun `tracks start avg and max`() {
        val acc = HrAccumulator()
        acc.record(60, 0, "腕立て")
        acc.record(80, 0, "腕立て")
        acc.record(100, 0, "腕立て")
        assertEquals(60, acc.startHr())
        assertEquals(80, acc.avgHr())
        assertEquals(100, acc.maxHr())
    }

    @Test
    fun `ignores non positive samples`() {
        val acc = HrAccumulator()
        acc.record(0, 0, "x")
        acc.record(-5, 0, "x")
        assertNull(acc.startHr())
        assertNull(acc.avgHr())
    }

    @Test
    fun `per exercise captures first and last hr`() {
        val acc = HrAccumulator()
        acc.record(70, 0, "腕立て")
        acc.record(90, 0, "腕立て")
        acc.record(110, 1, "スクワット")
        acc.record(120, 1, "スクワット")
        val records = acc.exerciseRecords()
        assertEquals(2, records.size)
        assertEquals(0, records[0].exerciseIndex)
        assertEquals(70, records[0].startHr)
        assertEquals(90, records[0].endHr)
        assertEquals(0, records[0].sortOrder)
        assertEquals(1, records[1].exerciseIndex)
        assertEquals(110, records[1].startHr)
        assertEquals(120, records[1].endHr)
        assertEquals(1, records[1].sortOrder)
    }

    @Test
    fun `null exercise index excluded from per exercise but counted in totals`() {
        val acc = HrAccumulator()
        acc.record(60, null, "")
        acc.record(80, 0, "腕立て")
        assertEquals(70, acc.avgHr())
        assertEquals(1, acc.exerciseRecords().size)
        assertEquals(80, acc.exerciseRecords()[0].startHr)
    }

    @Test
    fun `reset clears everything`() {
        val acc = HrAccumulator()
        acc.record(60, 0, "x")
        acc.reset()
        assertNull(acc.startHr())
        assertEquals(emptyList<Any>(), acc.exerciseRecords())
    }
}
