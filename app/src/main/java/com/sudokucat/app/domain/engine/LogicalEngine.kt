package com.sudokucat.app.domain.engine

/**
 * Motore logico locale: trova mosse umane e spiega il ragionamento.
 * Non usa alcuna API e funziona completamente offline.
 */
object LogicalEngine {

    enum class Technique(
        val title: String,
        val level: Int,
        val summary: String
    ) {
        NAKED_SINGLE(
            "Naked Single",
            1,
            "Una cella ha un solo candidato possibile."
        ),
        HIDDEN_SINGLE(
            "Hidden Single",
            2,
            "Un numero può stare in una sola cella della riga, colonna o quadrante."
        ),
        NAKED_PAIR(
            "Naked Pair",
            3,
            "Due celle condividono due soli candidati: quei numeri vanno eliminati dal resto dell'unità."
        ),
        HIDDEN_PAIR(
            "Hidden Pair",
            4,
            "Due numeri compaiono solo nelle stesse due celle di un'unità."
        ),
        POINTING(
            "Pointing Pair/Triple",
            4,
            "Un candidato in un quadrante è confinato su una riga o colonna."
        ),
        BOX_LINE(
            "Box-Line Reduction",
            4,
            "Un candidato in una riga o colonna è confinato in un quadrante."
        ),
        X_WING(
            "X-Wing",
            5,
            "Un candidato forma un rettangolo su due righe e due colonne."
        ),
        SWORDFISH(
            "Swordfish",
            6,
            "Tre righe o colonne formano uno schema chiuso con un candidato."
        ),
        AI_VERIFIED(
            "Mossa AI verificata",
            1,
            "Mossa proposta da Gemini con Structured Output e controllata dal risolutore locale."
        );

        val label: String get() = title
    }

    data class Elimination(val cell: Int, val digits: List<Int>)

    data class LogicalStep(
        val technique: Technique,
        val placements: Map<Int, Int> = emptyMap(),
        val eliminations: List<Elimination> = emptyList(),
        val highlights: List<Int> = emptyList(),
        val explanation: String
    ) {
        val primaryCell: Int? get() = placements.keys.firstOrNull()
        val primaryValue: Int? get() = placements.values.firstOrNull()
        val isPlacement: Boolean get() = placements.isNotEmpty()
        val eliminatedDigits: List<Int> get() = eliminations.flatMap { it.digits }.distinct()
    }

    fun candidates(board: IntArray): IntArray =
        IntArray(81) { if (board[it] == 0) SudokuEngine.candidateMask(board, it) else 0 }

    private fun maskDigits(mask: Int): List<Int> = SudokuEngine.maskToDigits(mask)

    private fun applyMasksElimination(masks: IntArray, step: LogicalStep) {
        for (elimination in step.eliminations) {
            var mask = masks[elimination.cell]
            for (digit in elimination.digits) mask = mask and (1 shl digit).inv()
            masks[elimination.cell] = mask
        }
    }

    fun findNakedSingle(board: IntArray): LogicalStep? {
        for (i in 0 until 81) {
            if (board[i] != 0) continue
            val mask = SudokuEngine.candidateMask(board, i)
            if (Integer.bitCount(mask) == 1) {
                val value = Integer.numberOfTrailingZeros(mask)
                return LogicalStep(
                    technique = Technique.NAKED_SINGLE,
                    placements = mapOf(i to value),
                    highlights = listOf(i),
                    explanation = "In ${SudokuEngine.coordinate(i)} resta un solo candidato: $value. " +
                        "Tutti gli altri numeri sono già presenti nella riga, nella colonna o nel quadrante."
                )
            }
        }
        return null
    }

    fun findHiddenSingle(board: IntArray): LogicalStep? =
        findHiddenSingleWithMasks(board, candidates(board))

    private fun findHiddenSingleWithMasks(board: IntArray, masks: IntArray): LogicalStep? {
        for ((unitIndex, unit) in SudokuEngine.units.withIndex()) {
            for (digit in 1..9) {
                val cells = unit.filter { idx ->
                    board[idx] == 0 && masks[idx] and (1 shl digit) != 0
                }
                if (cells.size == 1) {
                    val cell = cells.first()
                    val unitName = unitName(unitIndex)
                    return LogicalStep(
                        technique = Technique.HIDDEN_SINGLE,
                        placements = mapOf(cell to digit),
                        highlights = unit.toList(),
                        explanation = "Nella $unitName il numero $digit può stare solo in " +
                            "${SudokuEngine.coordinate(cell)}. Le altre celle sono escluse da numeri già presenti."
                    )
                }
            }
        }
        return null
    }

    fun findNakedPair(board: IntArray): LogicalStep? {
        for ((unitIndex, unit) in SudokuEngine.units.withIndex()) {
            val byMask = HashMap<Int, MutableList<Int>>()
            for (idx in unit) {
                if (board[idx] != 0) continue
                val mask = SudokuEngine.candidateMask(board, idx)
                if (Integer.bitCount(mask) == 2) {
                    byMask.getOrPut(mask) { mutableListOf() }.add(idx)
                }
            }
            for ((mask, cells) in byMask) {
                if (cells.size != 2) continue
                val digits = maskDigits(mask)
                val eliminations = mutableListOf<Elimination>()
                for (other in unit) {
                    if (other in cells || board[other] != 0) continue
                    val otherMask = SudokuEngine.candidateMask(board, other)
                    val remove = digits.filter { otherMask and (1 shl it) != 0 }
                    if (remove.isNotEmpty()) eliminations.add(Elimination(other, remove))
                }
                if (eliminations.isNotEmpty()) {
                    return LogicalStep(
                        technique = Technique.NAKED_PAIR,
                        eliminations = eliminations,
                        highlights = cells + eliminations.map { it.cell },
                        explanation = "Nella ${unitName(unitIndex)} le due celle " +
                            "${cells.joinToString(" e ") { SudokuEngine.coordinate(it) }} " +
                            "possono contenere solo ${digits.joinToString(" e ")}. " +
                            "Quindi ${digits.joinToString(" e ")} non possono comparire nelle altre celle dell'unità."
                    )
                }
            }
        }
        return null
    }

    fun findHiddenPair(board: IntArray): LogicalStep? {
        for ((unitIndex, unit) in SudokuEngine.units.withIndex()) {
            val empty = unit.filter { board[it] == 0 }
            for (a in 1..8) {
                for (b in a + 1..9) {
                    val cellsA = empty.filter { SudokuEngine.candidateMask(board, it) and (1 shl a) != 0 }
                    val cellsB = empty.filter { SudokuEngine.candidateMask(board, it) and (1 shl b) != 0 }
                    if (cellsA.size == 2 && cellsB.size == 2 && cellsA.toSet() == cellsB.toSet()) {
                        val pair = cellsA
                        val eliminations = mutableListOf<Elimination>()
                        for (cell in pair) {
                            val mask = SudokuEngine.candidateMask(board, cell)
                            val remove = maskDigits(mask).filter { it != a && it != b }
                            if (remove.isNotEmpty()) eliminations.add(Elimination(cell, remove))
                        }
                        if (eliminations.isNotEmpty()) {
                            return LogicalStep(
                                technique = Technique.HIDDEN_PAIR,
                                eliminations = eliminations,
                                highlights = pair,
                                explanation = "Nella ${unitName(unitIndex)} i numeri $a e $b compaiono " +
                                    "solo nelle celle ${pair.joinToString(" e ") { SudokuEngine.coordinate(it) }}. " +
                                    "Quelle due celle devono quindi contenere proprio $a e $b: gli altri candidati si eliminano."
                            )
                        }
                    }
                }
            }
        }
        return null
    }

    fun findPointing(board: IntArray): LogicalStep? {
        for (boxIndex in 0 until 9) {
            val box = SudokuEngine.boxes[boxIndex]
            for (digit in 1..9) {
                val cells = box.filter { board[it] == 0 && SudokuEngine.candidateMask(board, it) and (1 shl digit) != 0 }
                if (cells.size !in 2..3) continue
                val rows = cells.map { it / 9 }.distinct()
                val cols = cells.map { it % 9 }.distinct()
                if (rows.size == 1) {
                    val row = rows.first()
                    val eliminations = SudokuEngine.rows[row]
                        .filter { it !in box && board[it] == 0 && SudokuEngine.candidateMask(board, it) and (1 shl digit) != 0 }
                        .map { Elimination(it, listOf(digit)) }
                    if (eliminations.isNotEmpty()) {
                        return LogicalStep(
                            technique = Technique.POINTING,
                            eliminations = eliminations,
                            highlights = cells + eliminations.map { it.cell },
                            explanation = "Nel quadrante ${boxIndex + 1} il numero $digit può stare solo sulla " +
                                "riga ${row + 1} (${cells.joinToString(", ") { SudokuEngine.coordinate(it) }}). " +
                                "Perciò eliminiamo $digit dal resto della riga ${row + 1}."
                        )
                    }
                }
                if (cols.size == 1) {
                    val col = cols.first()
                    val eliminations = SudokuEngine.cols[col]
                        .filter { it !in box && board[it] == 0 && SudokuEngine.candidateMask(board, it) and (1 shl digit) != 0 }
                        .map { Elimination(it, listOf(digit)) }
                    if (eliminations.isNotEmpty()) {
                        return LogicalStep(
                            technique = Technique.POINTING,
                            eliminations = eliminations,
                            highlights = cells + eliminations.map { it.cell },
                            explanation = "Nel quadrante ${boxIndex + 1} il numero $digit può stare solo sulla " +
                                "colonna ${col + 1} (${cells.joinToString(", ") { SudokuEngine.coordinate(it) }}). " +
                                "Perciò eliminiamo $digit dal resto della colonna ${col + 1}."
                        )
                    }
                }
            }
        }
        return null
    }

    fun findBoxLine(board: IntArray): LogicalStep? {
        for (row in 0 until 9) {
            val unit = SudokuEngine.rows[row]
            for (digit in 1..9) {
                val cells = unit.filter { board[it] == 0 && SudokuEngine.candidateMask(board, it) and (1 shl digit) != 0 }
                if (cells.size !in 2..3) continue
                val boxes = cells.map { SudokuEngine.boxOf(it) - 1 }.distinct()
                if (boxes.size == 1) {
                    val box = SudokuEngine.boxes[boxes.first()]
                    val eliminations = box
                        .filter { it !in unit && board[it] == 0 && SudokuEngine.candidateMask(board, it) and (1 shl digit) != 0 }
                        .map { Elimination(it, listOf(digit)) }
                    if (eliminations.isNotEmpty()) {
                        return LogicalStep(
                            technique = Technique.BOX_LINE,
                            eliminations = eliminations,
                            highlights = cells + eliminations.map { it.cell },
                            explanation = "Nella riga ${row + 1} il numero $digit può stare solo nel " +
                                "quadrante ${boxes.first() + 1}. Eliminiamo quindi $digit dalle altre celle di quel quadrante."
                        )
                    }
                }
            }
        }
        for (col in 0 until 9) {
            val unit = SudokuEngine.cols[col]
            for (digit in 1..9) {
                val cells = unit.filter { board[it] == 0 && SudokuEngine.candidateMask(board, it) and (1 shl digit) != 0 }
                if (cells.size !in 2..3) continue
                val boxes = cells.map { SudokuEngine.boxOf(it) - 1 }.distinct()
                if (boxes.size == 1) {
                    val box = SudokuEngine.boxes[boxes.first()]
                    val eliminations = box
                        .filter { it !in unit && board[it] == 0 && SudokuEngine.candidateMask(board, it) and (1 shl digit) != 0 }
                        .map { Elimination(it, listOf(digit)) }
                    if (eliminations.isNotEmpty()) {
                        return LogicalStep(
                            technique = Technique.BOX_LINE,
                            eliminations = eliminations,
                            highlights = cells + eliminations.map { it.cell },
                            explanation = "Nella colonna ${col + 1} il numero $digit può stare solo nel " +
                                "quadrante ${boxes.first() + 1}. Eliminiamo quindi $digit dalle altre celle di quel quadrante."
                        )
                    }
                }
            }
        }
        return null
    }

    fun findXWing(board: IntArray): LogicalStep? {
        for (digit in 1..9) {
            val rowPositions = (0 until 9).map { row ->
                SudokuEngine.rows[row].filter {
                    board[it] == 0 && SudokuEngine.candidateMask(board, it) and (1 shl digit) != 0
                }.map { it % 9 }
            }
            for (r1 in 0 until 8) {
                if (rowPositions[r1].size != 2) continue
                for (r2 in r1 + 1 until 9) {
                    if (rowPositions[r2] != rowPositions[r1]) continue
                    val cols = rowPositions[r1]
                    val eliminations = mutableListOf<Elimination>()
                    for (col in cols) {
                        for (row in 0 until 9) {
                            if (row == r1 || row == r2) continue
                            val cell = row * 9 + col
                            if (board[cell] == 0 && SudokuEngine.candidateMask(board, cell) and (1 shl digit) != 0) {
                                eliminations.add(Elimination(cell, listOf(digit)))
                            }
                        }
                    }
                    if (eliminations.isNotEmpty()) {
                        val corners = listOf(r1 * 9 + cols[0], r1 * 9 + cols[1], r2 * 9 + cols[0], r2 * 9 + cols[1])
                        return LogicalStep(
                            technique = Technique.X_WING,
                            eliminations = eliminations,
                            highlights = corners + eliminations.map { it.cell },
                            explanation = "X-Wing del $digit: nelle righe ${r1 + 1} e ${r2 + 1} il candidato " +
                                "compare solo nelle colonne ${cols[0] + 1} e ${cols[1] + 1}. " +
                                "Forma un rettangolo, quindi $digit va eliminato dalle altre celle di quelle colonne."
                        )
                    }
                }
            }
        }
        // Versione speculare: X-Wing per colonne.
        for (digit in 1..9) {
            val colPositions = (0 until 9).map { col ->
                SudokuEngine.cols[col].filter {
                    board[it] == 0 && SudokuEngine.candidateMask(board, it) and (1 shl digit) != 0
                }.map { it / 9 }
            }
            for (c1 in 0 until 8) {
                if (colPositions[c1].size != 2) continue
                for (c2 in c1 + 1 until 9) {
                    if (colPositions[c2] != colPositions[c1]) continue
                    val rows = colPositions[c1]
                    val eliminations = mutableListOf<Elimination>()
                    for (row in rows) {
                        for (col in 0 until 9) {
                            if (col == c1 || col == c2) continue
                            val cell = row * 9 + col
                            if (board[cell] == 0 && SudokuEngine.candidateMask(board, cell) and (1 shl digit) != 0) {
                                eliminations.add(Elimination(cell, listOf(digit)))
                            }
                        }
                    }
                    if (eliminations.isNotEmpty()) {
                        val corners = listOf(rows[0] * 9 + c1, rows[1] * 9 + c1, rows[0] * 9 + c2, rows[1] * 9 + c2)
                        return LogicalStep(
                            technique = Technique.X_WING,
                            eliminations = eliminations,
                            highlights = corners + eliminations.map { it.cell },
                            explanation = "X-Wing del $digit: nelle colonne ${c1 + 1} e ${c2 + 1} il candidato " +
                                "compare solo nelle righe ${rows[0] + 1} e ${rows[1] + 1}. " +
                                "Quindi $digit si elimina dalle altre celle di quelle righe."
                        )
                    }
                }
            }
        }
        return null
    }

    fun findSwordfish(board: IntArray): LogicalStep? {
        for (digit in 1..9) {
            val rowCols = (0 until 9).map { row ->
                SudokuEngine.rows[row].filter {
                    board[it] == 0 && SudokuEngine.candidateMask(board, it) and (1 shl digit) != 0
                }.map { it % 9 }.distinct()
            }
            val validRows = (0 until 9).filter { rowCols[it].size in 2..3 }
            for (i in validRows.indices) {
                for (j in i + 1 until validRows.size) {
                    for (k in j + 1 until validRows.size) {
                        val rows = listOf(validRows[i], validRows[j], validRows[k])
                        val cols = rows.flatMap { rowCols[it] }.distinct().sorted()
                        if (cols.size != 3) continue
                        val eliminations = mutableListOf<Elimination>()
                        for (col in cols) {
                            for (row in 0 until 9) {
                                if (row in rows) continue
                                val cell = row * 9 + col
                                if (board[cell] == 0 && SudokuEngine.candidateMask(board, cell) and (1 shl digit) != 0) {
                                    eliminations.add(Elimination(cell, listOf(digit)))
                                }
                            }
                        }
                        if (eliminations.isNotEmpty()) {
                            val highlights = rows.flatMap { row -> cols.map { row * 9 + it } }
                            return LogicalStep(
                                technique = Technique.SWORDFISH,
                                eliminations = eliminations,
                                highlights = highlights,
                                explanation = "Swordfish del $digit: nelle righe ${rows.joinToString(", ") { (it + 1).toString() }} " +
                                    "il candidato è confinato nelle colonne ${cols.joinToString(", ") { (it + 1).toString() }}. " +
                                    "Eliminiamo $digit dalle altre righe in quelle colonne."
                            )
                        }
                    }
                }
            }
        }
        return null
    }

    /** Prima mossa disponibile, incluse le eliminazioni. */
    fun findNextStep(board: IntArray): LogicalStep? {
        if (SudokuEngine.isSolved(board)) return null
        findNakedSingle(board)?.let { return it }
        findHiddenSingle(board)?.let { return it }
        findNakedPair(board)?.let { return it }
        findHiddenPair(board)?.let { return it }
        findPointing(board)?.let { return it }
        findBoxLine(board)?.let { return it }
        findXWing(board)?.let { return it }
        findSwordfish(board)?.let { return it }
        return null
    }

    /**
     * Cerca una vera collocazione. Se serve, applica mentalmente le eliminazioni
     * delle tecniche intermedie e spiega il percorso che porta al numero.
     */
    fun findNextPlacement(board: IntArray): LogicalStep? {
        if (SudokuEngine.isSolved(board)) return null
        val masks = candidates(board)
        var discoveredExplanation: String? = null
        var discoveredTechnique: Technique? = null
        var highlights = mutableListOf<Int>()

        repeat(20) {
            // Naked single sui candidati aggiornati.
            for (i in 0 until 81) {
                if (board[i] != 0) continue
                val mask = masks[i]
                if (Integer.bitCount(mask) == 1) {
                    val value = Integer.numberOfTrailingZeros(mask)
                    val base = "In ${SudokuEngine.coordinate(i)} resta un solo candidato: $value."
                    return LogicalStep(
                        technique = discoveredTechnique ?: Technique.NAKED_SINGLE,
                        placements = mapOf(i to value),
                        eliminations = emptyList(),
                        highlights = (highlights + i).distinct(),
                        explanation = listOfNotNull(discoveredExplanation, base).joinToString(" ")
                    )
                }
            }

            val hidden = findHiddenSingleWithMasks(board, masks)
            if (hidden != null) {
                val cell = hidden.primaryCell!!
                val value = hidden.primaryValue!!
                return hidden.copy(
                    technique = discoveredTechnique ?: Technique.HIDDEN_SINGLE,
                    highlights = (highlights + hidden.highlights + cell).distinct(),
                    explanation = listOfNotNull(discoveredExplanation, hidden.explanation).joinToString(" ")
                )
            }

            val elimination = findEliminationStepWithMasks(board, masks) ?: return null
            discoveredExplanation = listOfNotNull(discoveredExplanation, elimination.explanation).joinToString(" ")
            discoveredTechnique = elimination.technique
            highlights.addAll(elimination.highlights)
            applyMasksElimination(masks, elimination)
        }
        return null
    }

    private fun findEliminationStepWithMasks(board: IntArray, masks: IntArray): LogicalStep? {
        // Le tecniche seguenti usano la griglia per i valori e le maschere per i candidati.
        fun candidateIn(cell: Int, digit: Int): Boolean =
            board[cell] == 0 && masks[cell] and (1 shl digit) != 0

        // Naked pair
        for ((unitIndex, unit) in SudokuEngine.units.withIndex()) {
            val byMask = HashMap<Int, MutableList<Int>>()
            for (idx in unit) {
                if (board[idx] != 0) continue
                if (Integer.bitCount(masks[idx]) == 2) byMask.getOrPut(masks[idx]) { mutableListOf() }.add(idx)
            }
            for ((mask, cells) in byMask) {
                if (cells.size != 2) continue
                val digits = maskDigits(mask)
                val eliminations = mutableListOf<Elimination>()
                for (other in unit) {
                    if (other in cells || board[other] != 0) continue
                    val remove = digits.filter { candidateIn(other, it) }
                    if (remove.isNotEmpty()) eliminations.add(Elimination(other, remove))
                }
                if (eliminations.isNotEmpty()) {
                    return LogicalStep(
                        Technique.NAKED_PAIR,
                        eliminations = eliminations,
                        highlights = cells + eliminations.map { it.cell },
                        explanation = "Nella ${unitName(unitIndex)}, le celle " +
                            "${cells.joinToString(" e ") { SudokuEngine.coordinate(it) }} formano una Naked Pair " +
                            "con ${digits.joinToString(" e ")}: eliminiamo questi numeri dalle altre celle dell'unità."
                    )
                }
            }
        }

        // Hidden pair con maschere
        for ((unitIndex, unit) in SudokuEngine.units.withIndex()) {
            for (a in 1..8) {
                for (b in a + 1..9) {
                    val cellsA = unit.filter { candidateIn(it, a) }
                    val cellsB = unit.filter { candidateIn(it, b) }
                    if (cellsA.size == 2 && cellsB.size == 2 && cellsA.toSet() == cellsB.toSet()) {
                        val eliminations = cellsA.mapNotNull { cell ->
                            val remove = maskDigits(masks[cell]).filter { it != a && it != b }
                            if (remove.isEmpty()) null else Elimination(cell, remove)
                        }
                        if (eliminations.isNotEmpty()) {
                            return LogicalStep(
                                Technique.HIDDEN_PAIR,
                                eliminations = eliminations,
                                highlights = cellsA,
                                explanation = "Nella ${unitName(unitIndex)} i numeri $a e $b compaiono solo in " +
                                    "${cellsA.joinToString(" e ") { SudokuEngine.coordinate(it) }}: è una Hidden Pair. " +
                                    "Eliminiamo gli altri candidati da quelle due celle."
                            )
                        }
                    }
                }
            }
        }

        // Pointing con maschere
        for (boxIndex in 0 until 9) {
            val box = SudokuEngine.boxes[boxIndex]
            for (digit in 1..9) {
                val cells = box.filter { candidateIn(it, digit) }
                if (cells.size !in 2..3) continue
                val rows = cells.map { it / 9 }.distinct()
                val cols = cells.map { it % 9 }.distinct()
                if (rows.size == 1) {
                    val eliminations = SudokuEngine.rows[rows.first()]
                        .filter { it !in box && candidateIn(it, digit) }
                        .map { Elimination(it, listOf(digit)) }
                    if (eliminations.isNotEmpty()) {
                        return LogicalStep(
                            Technique.POINTING,
                            eliminations = eliminations,
                            highlights = cells,
                            explanation = "Nel quadrante ${boxIndex + 1} il $digit è confinato nella riga " +
                                "${rows.first() + 1}: lo eliminiamo dal resto della riga."
                        )
                    }
                }
                if (cols.size == 1) {
                    val eliminations = SudokuEngine.cols[cols.first()]
                        .filter { it !in box && candidateIn(it, digit) }
                        .map { Elimination(it, listOf(digit)) }
                    if (eliminations.isNotEmpty()) {
                        return LogicalStep(
                            Technique.POINTING,
                            eliminations = eliminations,
                            highlights = cells,
                            explanation = "Nel quadrante ${boxIndex + 1} il $digit è confinato nella colonna " +
                                "${cols.first() + 1}: lo eliminiamo dal resto della colonna."
                        )
                    }
                }
            }
        }

        // Box-line con maschere
        for (row in 0 until 9) {
            for (digit in 1..9) {
                val cells = SudokuEngine.rows[row].filter { candidateIn(it, digit) }
                if (cells.size !in 2..3) continue
                val boxes = cells.map { SudokuEngine.boxOf(it) - 1 }.distinct()
                if (boxes.size == 1) {
                    val eliminations = SudokuEngine.boxes[boxes.first()]
                        .filter { it / 9 != row && candidateIn(it, digit) }
                        .map { Elimination(it, listOf(digit)) }
                    if (eliminations.isNotEmpty()) {
                        return LogicalStep(
                            Technique.BOX_LINE,
                            eliminations = eliminations,
                            highlights = cells,
                            explanation = "Nella riga ${row + 1} il $digit è confinato nel quadrante " +
                                "${boxes.first() + 1}: lo eliminiamo dalle altre celle del quadrante."
                        )
                    }
                }
            }
        }
        for (col in 0 until 9) {
            for (digit in 1..9) {
                val cells = SudokuEngine.cols[col].filter { candidateIn(it, digit) }
                if (cells.size !in 2..3) continue
                val boxes = cells.map { SudokuEngine.boxOf(it) - 1 }.distinct()
                if (boxes.size == 1) {
                    val eliminations = SudokuEngine.boxes[boxes.first()]
                        .filter { it % 9 != col && candidateIn(it, digit) }
                        .map { Elimination(it, listOf(digit)) }
                    if (eliminations.isNotEmpty()) {
                        return LogicalStep(
                            Technique.BOX_LINE,
                            eliminations = eliminations,
                            highlights = cells,
                            explanation = "Nella colonna ${col + 1} il $digit è confinato nel quadrante " +
                                "${boxes.first() + 1}: lo eliminiamo dalle altre celle del quadrante."
                        )
                    }
                }
            }
        }

        // X-Wing con maschere
        for (digit in 1..9) {
            val rowCols = (0 until 9).map { row ->
                SudokuEngine.rows[row].filter { candidateIn(it, digit) }.map { it % 9 }
            }
            for (r1 in 0 until 8) {
                if (rowCols[r1].size != 2) continue
                for (r2 in r1 + 1 until 9) {
                    if (rowCols[r2] != rowCols[r1]) continue
                    val cols = rowCols[r1]
                    val eliminations = mutableListOf<Elimination>()
                    for (col in cols) for (row in 0 until 9) {
                        if (row == r1 || row == r2) continue
                        val cell = row * 9 + col
                        if (candidateIn(cell, digit)) eliminations.add(Elimination(cell, listOf(digit)))
                    }
                    if (eliminations.isNotEmpty()) {
                        return LogicalStep(
                            Technique.X_WING,
                            eliminations = eliminations,
                            highlights = listOf(r1 * 9 + cols[0], r1 * 9 + cols[1], r2 * 9 + cols[0], r2 * 9 + cols[1]),
                            explanation = "X-Wing del $digit sulle righe ${r1 + 1} e ${r2 + 1}, colonne " +
                                "${cols[0] + 1} e ${cols[1] + 1}: eliminiamo $digit dalle altre celle di quelle colonne."
                        )
                    }
                }
            }
        }

        // Swordfish con maschere
        for (digit in 1..9) {
            val rowCols = (0 until 9).map { row ->
                SudokuEngine.rows[row].filter { candidateIn(it, digit) }.map { it % 9 }.distinct()
            }
            val validRows = (0 until 9).filter { rowCols[it].size in 2..3 }
            for (i in validRows.indices) for (j in i + 1 until validRows.size) for (k in j + 1 until validRows.size) {
                val rows = listOf(validRows[i], validRows[j], validRows[k])
                val cols = rows.flatMap { rowCols[it] }.distinct()
                if (cols.size != 3) continue
                val eliminations = mutableListOf<Elimination>()
                for (col in cols) for (row in 0 until 9) {
                    if (row in rows) continue
                    val cell = row * 9 + col
                    if (candidateIn(cell, digit)) eliminations.add(Elimination(cell, listOf(digit)))
                }
                if (eliminations.isNotEmpty()) {
                    return LogicalStep(
                        Technique.SWORDFISH,
                        eliminations = eliminations,
                        highlights = rows.flatMap { row -> cols.map { row * 9 + it } },
                        explanation = "Swordfish del $digit nelle righe ${rows.joinToString(", ") { (it + 1).toString() }} " +
                            "e colonne ${cols.joinToString(", ") { (it + 1).toString() }}: eliminiamo $digit dalle altre righe."
                    )
                }
            }
        }
        return null
    }

    fun explainCell(board: IntArray, cell: Int): String {
        if (cell !in 0..80) return "Seleziona una cella della griglia."
        val coordinate = SudokuEngine.coordinate(cell)
        if (board[cell] != 0) {
            return "$coordinate contiene ${board[cell]}. Se è un indizio non si può cambiare. " +
                "Se l'hai inserito tu, controlla che quel numero non si ripeta nella riga, nella colonna o nel quadrante."
        }
        val mask = SudokuEngine.candidateMask(board, cell)
        val digits = SudokuEngine.maskToDigits(mask)
        if (digits.isEmpty()) {
            return "$coordinate non ha candidati: c'è una contraddizione nella griglia. " +
                "Controlla i numeri già inseriti nella riga, colonna e quadrante."
        }
        if (digits.size == 1) {
            return "In $coordinate resta solo il candidato ${digits.first()}. È un Naked Single: " +
                "tutti gli altri numeri sono già presenti nelle unità collegate."
        }
        val hidden = findHiddenSingle(board)
        if (hidden?.primaryCell == cell) {
            return "Il numero ${hidden.primaryValue} può stare solo in $coordinate nella sua unità. " +
                "È un Hidden Single: non guardare solo la cella, ma l'intera riga, colonna o quadrante."
        }
        return "I candidati di $coordinate sono ${digits.joinToString(", ")}. " +
            "Non c'è ancora una certezza: confronta questi candidati con le altre celle della stessa riga, " +
            "colonna e quadrante per trovare un Hidden Single o una coppia."
    }

    fun availableTechniques(board: IntArray): List<Technique> {
        val result = mutableListOf<Technique>()
        if (findNakedSingle(board) != null) result.add(Technique.NAKED_SINGLE)
        if (findHiddenSingle(board) != null) result.add(Technique.HIDDEN_SINGLE)
        if (findNakedPair(board) != null) result.add(Technique.NAKED_PAIR)
        if (findHiddenPair(board) != null) result.add(Technique.HIDDEN_PAIR)
        if (findPointing(board) != null) result.add(Technique.POINTING)
        if (findBoxLine(board) != null) result.add(Technique.BOX_LINE)
        if (findXWing(board) != null) result.add(Technique.X_WING)
        if (findSwordfish(board) != null) result.add(Technique.SWORDFISH)
        return result
    }

    private fun unitName(index: Int): String = when {
        index < 9 -> "riga ${index + 1}"
        index < 18 -> "colonna ${index - 8}"
        else -> "quadrante ${index - 17}"
    }

    fun describeBoard(board: IntArray): String {
        val empty = board.count { it == 0 }
        val singles = (0 until 81).count {
            board[it] == 0 && Integer.bitCount(SudokuEngine.candidateMask(board, it)) == 1
        }
        return "Ci sono $empty celle vuote. $singles hanno un solo candidato. " +
            "Le tecniche disponibili ora: ${availableTechniques(board).joinToString(", ") { it.title }.ifBlank { "deduzione avanzata" }}."
    }
}
