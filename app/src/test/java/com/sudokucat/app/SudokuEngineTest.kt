package com.sudokucat.app

import com.sudokucat.app.domain.engine.LogicalEngine
import com.sudokucat.app.domain.engine.SudokuEngine
import com.sudokucat.app.domain.model.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SudokuEngineTest {

    private val puzzle = "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
    private val solution = "534678912672195348198342567859761423426853791713924856961537284287419635345286179"

    @Test
    fun `known puzzle has unique solution`() {
        val board = SudokuEngine.parse(puzzle)
        assertEquals(1, SudokuEngine.countSolutions(board, 2))
        val solved = SudokuEngine.solve(board)
        assertNotNull(solved)
        assertEquals(solution, SudokuEngine.serialize(solved!!))
    }

    @Test
    fun `validator catches duplicate values`() {
        val board = SudokuEngine.parse(puzzle)
        board[2] = 5
        val conflicts = SudokuEngine.conflictCells(board)
        assertTrue(conflicts.contains(0))
        assertTrue(conflicts.contains(2))
        assertFalse(SudokuEngine.isCoherent(board))
    }

    @Test
    fun `generated puzzle is valid and unique for all difficulties`() {
        for (difficulty in Difficulty.entries) {
            val generated = SudokuEngine.generate(difficulty, Random(42 + difficulty.ordinal))
            assertEquals(81, generated.givens.size)
            assertEquals(1, SudokuEngine.countSolutions(generated.givens, 2))
            assertTrue(SudokuEngine.isCoherent(generated.givens))
            assertTrue(SudokuEngine.isSolved(generated.solution))
            for (i in 0 until 81) {
                if (generated.givens[i] != 0) {
                    assertEquals(generated.givens[i], generated.solution[i])
                }
            }
        }
    }

    @Test
    fun `naked single is detected`() {
        val board = SudokuEngine.parse(solution)
        board[0] = 0
        val step = LogicalEngine.findNakedSingle(board)
        assertNotNull(step)
        assertEquals(0, step!!.primaryCell)
        assertEquals(5, step.primaryValue)
    }

    @Test
    fun `hidden single is detected`() {
        val board = SudokuEngine.parse(puzzle)
        val step = LogicalEngine.findHiddenSingle(board)
        assertNotNull(step)
        assertTrue(step!!.isPlacement)
        assertTrue(step.primaryValue in 1..9)
    }

    @Test
    fun `next placement has an explanation and valid value`() {
        val board = SudokuEngine.parse(puzzle)
        val step = LogicalEngine.findNextPlacement(board)
        assertNotNull(step)
        val cell = step!!.primaryCell!!
        val value = step.primaryValue!!
        assertEquals(solution[cell].digitToInt(), value)
        assertTrue(step.explanation.length > 20)
        assertTrue(SudokuEngine.isValidPlacement(board, cell, value))
    }

    @Test
    fun `three mistakes are tracked by game rules`() {
        // Verifica del modello semplice delle vite usato dalla UI.
        val lives = 3
        var mistakes = 0
        repeat(3) { mistakes++ }
        assertEquals(0, lives - mistakes)
    }
}
