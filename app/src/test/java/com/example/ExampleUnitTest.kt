package com.example

import com.example.data.model.BudgetEntity
import com.example.data.model.ExpenseEntity
import com.example.export.OpenXmlXlsxGenerator
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun openXmlXlsxGenerator_createsValidZipWithSpreadsheetStructure() {
        val tempFile = File.createTempFile("test_dashboard", ".xlsx")
        tempFile.deleteOnExit()

        val sheet1 = OpenXmlXlsxGenerator.XlsxSheet(
            name = "Dashboard_KPIs",
            colWidths = listOf(30.0, 20.0),
            rows = listOf(
                OpenXmlXlsxGenerator.XlsxRow(
                    cells = listOf(
                        OpenXmlXlsxGenerator.XlsxCell.Text("Total Spent", OpenXmlXlsxGenerator.XlsxStyle.KPI_LABEL),
                        OpenXmlXlsxGenerator.XlsxCell.Number(54000.0, OpenXmlXlsxGenerator.XlsxStyle.KPI_CURRENCY)
                    )
                )
            ),
            mergedRegions = listOf("A1:B1")
        )

        val resultFile = OpenXmlXlsxGenerator.createWorkbook(tempFile, listOf(sheet1))
        assertTrue("Output file must exist", resultFile.exists())
        assertTrue("Output file must not be empty", resultFile.length() > 0)

        // Verify valid ZIP and OpenXML required parts
        val zip = ZipFile(resultFile)
        val entries = zip.entries().asSequence().map { it.name }.toSet()

        assertTrue("Must contain [Content_Types].xml", entries.contains("[Content_Types].xml"))
        assertTrue("Must contain _rels/.rels", entries.contains("_rels/.rels"))
        assertTrue("Must contain xl/_rels/workbook.xml.rels", entries.contains("xl/_rels/workbook.xml.rels"))
        assertTrue("Must contain xl/workbook.xml", entries.contains("xl/workbook.xml"))
        assertTrue("Must contain xl/styles.xml", entries.contains("xl/styles.xml"))
        assertTrue("Must contain xl/worksheets/sheet1.xml", entries.contains("xl/worksheets/sheet1.xml"))

        zip.close()
    }
}
