package com.sudokucat.app.domain.engine

import com.sudokucat.app.domain.model.Difficulty
import kotlin.random.Random

/**
 * Motore Sudoku completamente locale.
 *
 * - genera griglie complete casuali
 * - rimuove indizi mantenendo una soluzione unica
 * - risolve con backtracking + MRV
 * - valida righe, colonne e quadranti
 */
object SudokuEngine {

    const val SIZE = 9
    const val CELLS = 81

    val rows: Array<IntArray> = Array(9) { r -> IntArray(9) { c -> r * 9 + c } }
    val cols: Array<IntArray> = Array(9) { c -> IntArray(9) { r -> r * 9 + c } }
    val boxes: Array<IntArray> = Array(9) { b ->
        val br = (b / 3) * 3
        val bc = (b % 3) * 3
        IntArray(9) { i -> (br + i / 3) * 9 + (bc + i % 3) }
    }
    val units: List<IntArray> = rows.toList() + cols.toList() + boxes.toList()
    val peers: Array<IntArray> = Array(81) { idx ->
        val r = idx / 9
        val c = idx % 9
        val b = (r / 3) * 3 + c / 3
        (rows[r].toSet() + cols[c].toSet() + boxes[b].toSet() - idx).toIntArray().sortedArray()
    }

    fun emptyBoard(): IntArray = IntArray(CELLS)

    fun parse(text: String): IntArray {
        val clean = text.trim().replace(Regex("[^0-9.]"), "")
        require(clean.length == CELLS) { "La griglia deve contenere 81 celle" }
        return IntArray(CELLS) { i ->
            val ch = clean[i]
            if (ch == '.' || ch == '0') 0 else ch.digitToInt()
        }
    }

    fun parseOrEmpty(text: String): IntArray = runCatching { parse(text) }.getOrElse { emptyBoard() }

    fun serialize(board: IntArray): String = buildString(CELLS) {
        board.forEach { append(if (it == 0) '.' else ('0' + it)) }
    }

    fun isValidPlacement(board: IntArray, index: Int, value: Int): Boolean {
        if (value !in 1..9) return false
        if (board[index] != 0) return board[index] == value
        return peers[index].none { board[it] == value }
    }

    fun conflictCells(board: IntArray): Set<Int> {
        val result = mutableSetOf<Int>()
        for (unit in units) {
            val groups = HashMap<Int, MutableList<Int>>()
            for (idx in unit) {
                val value = board[idx]
                if (value != 0) groups.getOrPut(value) { mutableListOf() }.add(idx)
            }
            groups.values.filter { it.size > 1 }.forEach { result.addAll(it) }
        }
        return result
    }

    fun isCoherent(board: IntArray): Boolean = conflictCells(board).isEmpty()

    fun isSolved(board: IntArray): Boolean =
        board.all { it in 1..9 } && conflictCells(board).isEmpty()

    fun candidateMask(board: IntArray, index: Int): Int {
        if (board[index] != 0) return 0
        var used = 0
        for (peer in peers[index]) used = used or (1 shl board[peer])
        var mask = 0
        for (value in 1..9) if (used and (1 shl value) == 0) mask = mask or (1 shl value)
        return mask
    }

    fun candidates(board: IntArray, index: Int): List<Int> =
        maskToDigits(candidateMask(board, index))

    fun maskToDigits(mask: Int): List<Int> = (1..9).filter { mask and (1 shl it) != 0 }

    fun digitsToMask(digits: Iterable<Int>): Int {
        var mask = 0
        digits.forEach { if (it in 1..9) mask = mask or (1 shl it) }
        return mask
    }

    /** Risolve la griglia. Se [randomize] è true esplora i candidati in ordine casuale. */
    fun solve(source: IntArray, randomize: Boolean = false, random: Random = Random.Default): IntArray? {
        val board = source.copyOf()
        return if (solveInPlace(board, randomize, random)) board else null
    }

    /** Conta le soluzioni fino a [limit]. Utile per garantire l'unicità. */
    fun countSolutions(source: IntArray, limit: Int = 2): Int {
        val board = source.copyOf()
        var found = 0
        fun recurse(): Boolean {
            val index = bestEmptyCell(board)
            if (index == -1) {
                found++
                return found >= limit
            }
            var mask = candidateMask(board, index)
            while (mask != 0) {
                val bit = mask and -mask
                val value = Integer.numberOfTrailingZeros(bit)
                board[index] = value
                val stop = recurse()
                board[index] = 0
                if (stop) return true
                mask = mask xor bit
            }
            return false
        }
        recurse()
        return found
    }

    private fun solveInPlace(
        board: IntArray,
        randomize: Boolean,
        random: Random
    ): Boolean {
        val index = bestEmptyCell(board)
        if (index == -1) return true
        var mask = candidateMask(board, index)
        if (mask == 0) return false
        val digits = maskToDigits(mask).toMutableList()
        if (randomize) digits.shuffle(random)
        for (value in digits) {
            board[index] = value
            if (solveInPlace(board, randomize, random)) return true
            board[index] = 0
        }
        return false
    }

    private fun bestEmptyCell(board: IntArray): Int {
        var best = -1
        var bestCount = 10
        for (i in 0 until CELLS) {
            if (board[i] == 0) {
                val count = Integer.bitCount(candidateMask(board, i))
                if (count == 0) return i
                if (count < bestCount) {
                    best = i
                    bestCount = count
                    if (count == 1) return i
                }
            }
        }
        return best
    }

    fun generate(difficulty: Difficulty, random: Random = Random.Default): GeneratedPuzzle {
        val solution = randomSolvedGrid(random)
        val puzzle = solution.copyOf()
        val indices = (0 until CELLS).shuffled(random)
        var clues = CELLS

        // Rimuoviamo in modo simmetrico per ottenere griglie eleganti.
        val seen = mutableSetOf<Int>()
        for (idx in indices) {
            if (clues <= difficulty.targetClues) break
            val mirrored = CELLS - 1 - idx
            if (idx in seen || mirrored in seen) continue
            seen.add(idx)
            seen.add(mirrored)

            val first = puzzle[idx]
            val second = puzzle[mirrored]
            if (first == 0) continue

            puzzle[idx] = 0
            var removed = 1
            if (mirrored != idx && puzzle[mirrored] != 0) {
                puzzle[mirrored] = 0
                removed++
            }

            if (countSolutions(puzzle, 2) != 1) {
                puzzle[idx] = first
                if (mirrored != idx) puzzle[mirrored] = second
            } else {
                clues -= removed
            }
        }

        // Se la rimozione simmetrica non basta, completiamo con rimozioni singole.
        if (clues > difficulty.targetClues) {
            for (idx in (0 until CELLS).shuffled(random)) {
                if (clues <= difficulty.targetClues) break
                if (puzzle[idx] == 0) continue
                val old = puzzle[idx]
                puzzle[idx] = 0
                if (countSolutions(puzzle, 2) == 1) clues-- else puzzle[idx] = old
            }
        }

        return GeneratedPuzzle(
            givens = puzzle,
            solution = solution,
            difficulty = difficulty,
            clues = puzzle.count { it != 0 }
        )
    }

    private fun randomSolvedGrid(random: Random): IntArray {
        val board = emptyBoard()
        fillRandom(board, random)
        return board
    }

    private fun fillRandom(board: IntArray, random: Random): Boolean {
        val index = bestEmptyCell(board)
        if (index == -1) return true
        val digits = maskToDigits(candidateMask(board, index)).shuffled(random)
        for (value in digits) {
            board[index] = value
            if (fillRandom(board, random)) return true
            board[index] = 0
        }
        return false
    }

    fun firstEmpty(board: IntArray): Int = board.indexOfFirst { it == 0 }

    fun solvedWithHint(board: IntArray): IntArray? = solve(board)

    fun coordinate(index: Int): String = "R${index / 9 + 1}C${index % 9 + 1}"

    fun boxOf(index: Int): Int = (index / 27) * 3 + (index % 9) / 3 + 1
}

data class GeneratedPuzzle(
    val givens: IntArray,
    val solution: IntArray,
    val difficulty: Difficulty,
    val clues: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GeneratedPuzzle) return false
        return difficulty == other.difficulty &&
            clues == other.clues &&
            givens.contentEquals(other.givens) &&
            solution.contentEquals(other.solution)
    }

    override fun hashCode(): Int {
        var result = difficulty.hashCode()
        result = 31 * result + clues
        result = 31 * result + givens.contentHashCode()
        result = 31 * result + solution.contentHashCode()
        return result
    }
}
