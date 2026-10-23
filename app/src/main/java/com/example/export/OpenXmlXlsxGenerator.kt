package com.example.export

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Pure Kotlin ECMA-376 OpenXML (.xlsx) Workbook Generator.
 * Creates standard Microsoft Excel spreadsheet archives compatible with
 * Microsoft Excel (Desktop, Office 365, Android & iOS), Google Sheets,
 * Apple Numbers, and LibreOffice Calc.
 */
object OpenXmlXlsxGenerator {

    enum class XlsxStyle(val id: Int) {
        DEFAULT(0),
        TITLE(1),
        HEADER_TEAL(2),
        HEADER_PURPLE(3),
        CURRENCY(4),
        PERCENTAGE(5),
        NUMBER_INT(6),
        DATE(7),
        TOTAL_CURRENCY(8),
        TOTAL_LABEL(9),
        KPI_LABEL(10),
        KPI_CURRENCY(11),
        KPI_NUMBER(12),
        KPI_TEXT(13),
        TEXT_BORDER(14),
        SUBHEADER(15)
    }

    sealed class XlsxCell(val style: XlsxStyle) {
        class Text(val value: String, style: XlsxStyle = XlsxStyle.DEFAULT) : XlsxCell(style)
        class Number(val value: Double, style: XlsxStyle = XlsxStyle.DEFAULT) : XlsxCell(style)
        class Formula(val formula: String, style: XlsxStyle = XlsxStyle.DEFAULT) : XlsxCell(style)
        class Empty(style: XlsxStyle = XlsxStyle.DEFAULT) : XlsxCell(style)
    }

    class XlsxRow(
        val cells: List<XlsxCell>,
        val height: Double? = null
    )

    class XlsxSheet(
        val name: String,
        val colWidths: List<Double> = emptyList(),
        val rows: List<XlsxRow> = emptyList(),
        val mergedRegions: List<String> = emptyList() // e.g. "A1:C1"
    )

    fun createWorkbook(outputFile: File, sheets: List<XlsxSheet>): File {
        outputFile.parentFile?.mkdirs()
        ZipOutputStream(FileOutputStream(outputFile).buffered()).use { zip ->
            // 1. [Content_Types].xml
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(buildContentTypesXml(sheets.size).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 2. _rels/.rels
            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(RELS_XML.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 3. xl/_rels/workbook.xml.rels
            zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            zip.write(buildWorkbookRelsXml(sheets.size).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 4. xl/workbook.xml
            zip.putNextEntry(ZipEntry("xl/workbook.xml"))
            zip.write(buildWorkbookXml(sheets).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 5. xl/styles.xml
            zip.putNextEntry(ZipEntry("xl/styles.xml"))
            zip.write(STYLES_XML.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 6. xl/worksheets/sheetN.xml
            for ((index, sheet) in sheets.withIndex()) {
                val sheetNumber = index + 1
                zip.putNextEntry(ZipEntry("xl/worksheets/sheet$sheetNumber.xml"))
                zip.write(buildWorksheetXml(sheet).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return outputFile
    }

    fun getColumnLetter(colIndex: Int): String {
        var temp = colIndex
        val sb = StringBuilder()
        while (temp > 0) {
            val rem = (temp - 1) % 26
            sb.append(('A'.code + rem).toChar())
            temp = (temp - 1) / 26
        }
        return sb.reverse().toString()
    }

    fun escapeXml(text: String): String {
        val sb = StringBuilder(text.length + 16)
        for (c in text) {
            when (c) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else -> {
                    if (c.code in 0x20..0xD7FF || c == '\t' || c == '\n' || c == '\r') {
                        sb.append(c)
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun sanitizeSheetName(name: String, fallbackIndex: Int): String {
        val clean = name.replace(Regex("[\\\\/?*\\[\\]:]"), "_").trim()
        return if (clean.isBlank()) "Sheet$fallbackIndex" else clean.take(31)
    }

    private fun buildContentTypesXml(sheetCount: Int): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n")
        append("  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n")
        append("  <Default Extension=\"xml\" ContentType=\"application/xml\"/>\n")
        append("  <Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>\n")
        append("  <Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>\n")
        for (i in 1..sheetCount) {
            append("  <Override PartName=\"/xl/worksheets/sheet$i.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\n")
        }
        append("</Types>")
    }

    private const val RELS_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
            "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>\n" +
            "</Relationships>"

    private fun buildWorkbookRelsXml(sheetCount: Int): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n")
        append("  <Relationship Id=\"rIdStyles\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>\n")
        for (i in 1..sheetCount) {
            append("  <Relationship Id=\"rIdSheet$i\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet$i.xml\"/>\n")
        }
        append("</Relationships>")
    }

    private fun buildWorkbookXml(sheets: List<XlsxSheet>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n")
        append("  <bookViews>\n")
        append("    <workbookView xWindow=\"0\" yWindow=\"0\" windowWidth=\"20480\" windowHeight=\"10240\"/>\n")
        append("  </bookViews>\n")
        append("  <sheets>\n")
        for ((index, sheet) in sheets.withIndex()) {
            val num = index + 1
            val safeName = sanitizeSheetName(sheet.name, num)
            append("    <sheet name=\"${escapeXml(safeName)}\" sheetId=\"$num\" r:id=\"rIdSheet$num\"/>\n")
        }
        append("  </sheets>\n")
        append("</workbook>")
    }

    private const val STYLES_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n" +
            "  <numFmts count=\"4\">\n" +
            "    <numFmt numFmtId=\"164\" formatCode=\"&quot;₹&quot;#,##0.00\"/>\n" +
            "    <numFmt numFmtId=\"165\" formatCode=\"0.0%\"/>\n" +
            "    <numFmt numFmtId=\"166\" formatCode=\"#,##0\"/>\n" +
            "    <numFmt numFmtId=\"167\" formatCode=\"yyyy-mm-dd\"/>\n" +
            "  </numFmts>\n" +
            "  <fonts count=\"7\">\n" +
            "    <font><sz val=\"11\"/><color rgb=\"FF0F172A\"/><name val=\"Calibri\"/><family val=\"2\"/></font>\n" +
            "    <font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/><family val=\"2\"/></font>\n" +
            "    <font><b/><sz val=\"11\"/><color rgb=\"FF0F172A\"/><name val=\"Calibri\"/><family val=\"2\"/></font>\n" +
            "    <font><b/><sz val=\"15\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/><family val=\"2\"/></font>\n" +
            "    <font><b/><sz val=\"10\"/><color rgb=\"FF475569\"/><name val=\"Calibri\"/><family val=\"2\"/></font>\n" +
            "    <font><b/><sz val=\"12\"/><color rgb=\"FF0F766E\"/><name val=\"Calibri\"/><family val=\"2\"/></font>\n" +
            "    <font><b/><sz val=\"12\"/><color rgb=\"FF4F378B\"/><name val=\"Calibri\"/><family val=\"2\"/></font>\n" +
            "  </fonts>\n" +
            "  <fills count=\"10\">\n" +
            "    <fill><patternFill patternType=\"none\"/></fill>\n" +
            "    <fill><patternFill patternType=\"gray125\"/></fill>\n" +
            "    <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF1E1B4B\"/></patternFill></fill>\n" +
            "    <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF0F766E\"/></patternFill></fill>\n" +
            "    <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF4F378B\"/></patternFill></fill>\n" +
            "    <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFF1F5F9\"/></patternFill></fill>\n" +
            "    <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFCCFBF1\"/></patternFill></fill>\n" +
            "    <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFEADDFF\"/></patternFill></fill>\n" +
            "    <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFE2E8F0\"/></patternFill></fill>\n" +
            "    <fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFF8FAFC\"/></patternFill></fill>\n" +
            "  </fills>\n" +
            "  <borders count=\"2\">\n" +
            "    <border><left/><right/><top/><bottom/><diagonal/></border>\n" +
            "    <border>\n" +
            "      <left style=\"thin\"><color rgb=\"FFCBD5E1\"/></left>\n" +
            "      <right style=\"thin\"><color rgb=\"FFCBD5E1\"/></right>\n" +
            "      <top style=\"thin\"><color rgb=\"FFCBD5E1\"/></top>\n" +
            "      <bottom style=\"thin\"><color rgb=\"FFCBD5E1\"/></bottom>\n" +
            "      <diagonal/>\n" +
            "    </border>\n" +
            "  </borders>\n" +
            "  <cellStyleXfs count=\"1\">\n" +
            "    <xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/>\n" +
            "  </cellStyleXfs>\n" +
            "  <cellXfs count=\"16\">\n" +
            "    <!-- 0: Default Normal -->\n" +
            "    <xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>\n" +
            "    <!-- 1: Title Banner (Font 3, Fill 2, Left) -->\n" +
            "    <xf numFmtId=\"0\" fontId=\"3\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyAlignment=\"1\">\n" +
            "      <alignment horizontal=\"left\" vertical=\"center\"/>\n" +
            "    </xf>\n" +
            "    <!-- 2: Header Teal (Font 1, Fill 3, Center) -->\n" +
            "    <xf numFmtId=\"0\" fontId=\"1\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\">\n" +
            "      <alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/>\n" +
            "    </xf>\n" +
            "    <!-- 3: Header Purple (Font 1, Fill 4, Center) -->\n" +
            "    <xf numFmtId=\"0\" fontId=\"1\" fillId=\"4\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\">\n" +
            "      <alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/>\n" +
            "    </xf>\n" +
            "    <!-- 4: Currency (Font 0, numFmtId 164, Right) -->\n" +
            "    <xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyBorder=\"1\" applyAlignment=\"1\">\n" +
            "      <alignment horizontal=\"right\" vertical=\"center\"/>\n" +
            "    </xf>\n" +
            "    <!-- 5: Percentage (Font 0, numFmtId 165, Right) -->\n" +
            "    <xf numFmtId=\"165\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyBorder=\"1\" applyAlignment=\"1\">\n" +
            "      <alignment horizontal=\"right\" vertical=\"center\"/>\n" +
            "    </xf>\n" +
            "    <!-- 6: Number Int (Font 0, numFmtId 166, Right) -->\n" +
            "    <xf numFmtId=\"166\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyBorder=\"1\" applyAlignment=\"1\">\n" +
            "      <alignment horizontal=\"right\" vertical=\"center\"/>\n" +
            "    </xf>\n" +
            "    <!-- 7: Date Cell (Font 0, Center) -->\n" +
            "    <xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyBorder=\"1\" applyAlignment=\"1\">\n" +
            "      <alignment horizontal=\"center\" vertical=\"center\"/>\n" +
            "    </xf>\n" +
            "    <!-- 8: Total Row Currency (Font 2, Fill 8, numFmtId 164, Right) -->\n" +
            "    <xf numFmtId=\"164\" fontId=\"2\" fillId=\"8\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\">\n" +
            "      <alignment horizontal=\"right\" vertical=\"center\"/>\n" +
            "    </xf>\n" +
            "    <!-- 9: Total Row Label (Font 2, Fill 8, Left) -->\n" +
            "    <xf numFmtId=\"0\" fontId=\"2\" fillId=\"8\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\">\n" +
            "      <alignment horizontal=\"left\" vertical=\"center\"/>\n" +
            "    </xf>\n" +
            "    <!-- 10: KPI Label (Font 4, Fill 5, Left) -->\n" +
            "    <xf numFmtId=\"0\" fontId=\"4\" fillId=\"5\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\">\n" +
            "      <alignment horizontal=\"left\" vertical=\"center\"/>\n" +
            "    </xf>\n" +
            "    <!-- 11: KPI Currency (Font 5, Fill 6, numFmtId 164, Right) -->\n" +
            "    <xf numFmtId=\"164\" fontId=\"5\" fillId=\"6\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\">\n" +
            "      <alignment horizontal=\"right\" vertical=\"center\"/>\n" +
            "    </xf>\n" +
            "    <!-- 12: KPI Number (Font 6, Fill 7, numFmtId 166, Right) -->\n" +
            "    <xf numFmtId=\"166\" fontId=\"6\" fillId=\"7\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\">\n" +
            "      <alignment horizontal=\"right\" vertical=\"center\"/>\n" +
            "    </xf>\n" +
            "    <!-- 13: KPI Text (Font 2, Fill 5, Left) -->\n" +
            "    <xf numFmtId=\"0\" fontId=\"2\" fillId=\"5\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\">\n" +
            "      <alignment horizontal=\"left\" vertical=\"center\"/>\n" +
            "    </xf>\n" +
            "    <!-- 14: Text with Grid Border (Font 0, Left) -->\n" +
            "    <xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyBorder=\"1\" applyAlignment=\"1\">\n" +
            "      <alignment horizontal=\"left\" vertical=\"center\"/>\n" +
            "    </xf>\n" +
            "    <!-- 15: SubHeader (Font 2, Fill 5, Left) -->\n" +
            "    <xf numFmtId=\"0\" fontId=\"2\" fillId=\"5\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyAlignment=\"1\">\n" +
            "      <alignment horizontal=\"left\" vertical=\"center\"/>\n" +
            "    </xf>\n" +
            "  </cellXfs>\n" +
            "</styleSheet>"

    private fun buildWorksheetXml(sheet: XlsxSheet): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n")
        append("  <sheetViews>\n")
        append("    <sheetView tabSelected=\"1\" workbookViewId=\"0\"/>\n")
        append("  </sheetViews>\n")
        append("  <sheetFormatPr defaultRowHeight=\"19\"/>\n")

        if (sheet.colWidths.isNotEmpty()) {
            append("  <cols>\n")
            for ((cIdx, width) in sheet.colWidths.withIndex()) {
                val colNum = cIdx + 1
                append("    <col min=\"$colNum\" max=\"$colNum\" width=\"$width\" customWidth=\"1\"/>\n")
            }
            append("  </cols>\n")
        }

        append("  <sheetData>\n")
        for ((rIdx, row) in sheet.rows.withIndex()) {
            val rowNum = rIdx + 1
            val heightAttr = if (row.height != null) " ht=\"${row.height}\" customHeight=\"1\"" else ""
            append("    <row r=\"$rowNum\"$heightAttr>\n")
            for ((cIdx, cell) in row.cells.withIndex()) {
                val colNum = cIdx + 1
                val cellRef = "${getColumnLetter(colNum)}$rowNum"
                val styleAttr = " s=\"${cell.style.id}\""
                when (cell) {
                    is XlsxCell.Text -> {
                        append("      <c r=\"$cellRef\"$styleAttr t=\"inlineStr\"><is><t xml:space=\"preserve\">${escapeXml(cell.value)}</t></is></c>\n")
                    }
                    is XlsxCell.Number -> {
                        append("      <c r=\"$cellRef\"$styleAttr><v>${cell.value}</v></c>\n")
                    }
                    is XlsxCell.Formula -> {
                        append("      <c r=\"$cellRef\"$styleAttr><f>${escapeXml(cell.formula)}</f></c>\n")
                    }
                    is XlsxCell.Empty -> {
                        append("      <c r=\"$cellRef\"$styleAttr/>\n")
                    }
                }
            }
            append("    </row>\n")
        }
        append("  </sheetData>\n")

        if (sheet.mergedRegions.isNotEmpty()) {
            append("  <mergeCells count=\"${sheet.mergedRegions.size}\">\n")
            for (region in sheet.mergedRegions) {
                append("    <mergeCell ref=\"$region\"/>\n")
            }
            append("  </mergeCells>\n")
        }

        append("</worksheet>")
    }
}
