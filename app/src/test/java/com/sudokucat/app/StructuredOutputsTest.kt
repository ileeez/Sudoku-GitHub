package com.sudokucat.app

import com.sudokucat.app.ai.StructuredOutputs
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Verifica il contratto degli Structured Outputs: lo schema richiesto a Gemini
 * e il parser usato dall'app devono restare allineati.
 */
class StructuredOutputsTest {

    private val gridJson = """
        {
          "grid": [
            [5,3,0,0,7,0,0,0,0],
            [6,0,0,1,9,5,0,0,0],
            [0,9,8,0,0,0,0,6,0],
            [8,0,0,0,6,0,0,0,3],
            [4,0,0,8,0,3,0,0,1],
            [7,0,0,0,2,0,0,0,6],
            [0,6,0,0,0,0,2,8,0],
            [0,0,0,4,1,9,0,0,5],
            [0,0,0,0,8,0,0,7,9]
          ]
        }
    """.trimIndent()

    @Test
    fun `grid schema declares a required 9x9 integer matrix`() {
        val type = StructuredOutputs.gridSchema["type"].toString()
        assertEquals("\"OBJECT\"", type)
        assertEquals("grid", StructuredOutputs.gridSchema["required"].toString().replace("[", "").replace("]", "").replace("\"", ""))
    }

    @Test
    fun `parseGrid reads a valid 9x9 response`() {
        val grid = StructuredOutputs.parseGrid(gridJson)
        assertNotNull(grid)
        assertEquals(81, grid!!.size)
        assertEquals(5, grid[0])
        assertEquals(3, grid[1])
        assertEquals(0, grid[2])
        assertEquals(9, grid[80])
        assertEquals(30, grid.count { it != 0 })
    }

    @Test
    fun `parseGrid tolerates markdown code fences and prose`() {
        val raw = "Ecco la griglia:\n```json\n$gridJson\n```\nSpero sia utile."
        val grid = StructuredOutputs.parseGrid(raw)
        assertNotNull(grid)
        assertEquals(5, grid!![0])
    }

    @Test
    fun `parseGrid rejects wrong row count`() {
        val raw = """{"grid": [[1,2,3],[4,5,6]]}"""
        assertNull(StructuredOutputs.parseGrid(raw))
    }

    @Test
    fun `parseGrid rejects wrong column count`() {
        val raw = """{"grid": [[1,2,3,4,5,6,7,8],[1,2,3,4,5,6,7,8,9]]}"""
        assertNull(StructuredOutputs.parseGrid(raw))
    }

    @Test
    fun `parseGrid rejects out of range values`() {
        val bad = gridJson.replaceFirst("[5,3,0", "[5,10,0")
        assertNull(StructuredOutputs.parseGrid(bad))
    }

    @Test
    fun `parseGrid rejects non numeric values`() {
        val bad = gridJson.replaceFirst("[5,3,0", "[5,\"x\",0")
        assertNull(StructuredOutputs.parseGrid(bad))
    }

    @Test
    fun `parseGrid rejects empty or malformed input`() {
        assertNull(StructuredOutputs.parseGrid(""))
        assertNull(StructuredOutputs.parseGrid("nessun json qui"))
        assertNull(StructuredOutputs.parseGrid("{}"))
    }

    @Test
    fun `move schema requires the four didactic fields`() {
        val required = StructuredOutputs.moveSchema["required"].toString()
        assertEquals(true, required.contains("row"))
        assertEquals(true, required.contains("col"))
        assertEquals(true, required.contains("valore"))
        assertEquals(true, required.contains("spiegazione"))
    }

    @Test
    fun `parseMove reads a valid structured suggestion`() {
        val raw = """
            {
              "row": 0,
              "col": 2,
              "valore": 4,
              "spiegazione": "Naked Single: la cella r0c2 ha un solo candidato possibile."
            }
        """.trimIndent()
        val move = StructuredOutputs.parseMove(raw)
        assertNotNull(move)
        assertEquals(0, move!!.row)
        assertEquals(2, move.col)
        assertEquals(4, move.value)
        assertEquals(2, move.cell)
        assertEquals(true, move.explanation.contains("Naked Single"))
    }

    @Test
    fun `parseMove accepts english fallback keys`() {
        val raw = """{"row":8,"col":8,"value":"9","explanation":"Hidden Single nella colonna."}"""
        val move = StructuredOutputs.parseMove(raw)
        assertNotNull(move)
        assertEquals(80, move!!.cell)
        assertEquals(9, move.value)
    }

    @Test
    fun `parseMove rejects out of bounds coordinates`() {
        assertNull(StructuredOutputs.parseMove("""{"row":9,"col":0,"valore":1,"spiegazione":"x"}"""))
        assertNull(StructuredOutputs.parseMove("""{"row":0,"col":-1,"valore":1,"spiegazione":"x"}"""))
    }

    @Test
    fun `parseMove rejects invalid values and blank explanations`() {
        assertNull(StructuredOutputs.parseMove("""{"row":0,"col":0,"valore":0,"spiegazione":"x"}"""))
        assertNull(StructuredOutputs.parseMove("""{"row":0,"col":0,"valore":10,"spiegazione":"x"}"""))
        assertNull(StructuredOutputs.parseMove("""{"row":0,"col":0,"valore":1,"spiegazione":"   "}"""))
        assertNull(StructuredOutputs.parseMove("""{"row":0,"col":0,"valore":1}"""))
    }

    @Test
    fun `parsed grid can feed the local solver`() {
        val grid = StructuredOutputs.parseGrid(gridJson)!!
        val board = IntArray(81) { grid[it] }
        val solution = com.sudokucat.app.domain.engine.SudokuEngine.solve(board)
        assertNotNull(solution)
        assertArrayEquals(
            com.sudokucat.app.domain.engine.SudokuEngine.parse(
                "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
            ),
            solution
        )
    }
}
