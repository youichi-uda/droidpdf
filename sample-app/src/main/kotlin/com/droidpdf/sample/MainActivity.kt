package com.droidpdf.sample

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.droidpdf.DroidPDF
import com.droidpdf.annotations.PdfCircleAnnotation
import com.droidpdf.annotations.PdfFreeTextAnnotation
import com.droidpdf.annotations.PdfHighlightAnnotation
import com.droidpdf.annotations.PdfLinkAnnotation
import com.droidpdf.annotations.PdfSquareAnnotation
import com.droidpdf.annotations.PdfStampAnnotation
import com.droidpdf.annotations.PdfTextAnnotation
import com.droidpdf.content.PdfCanvas
import com.droidpdf.content.PdfFont
import com.droidpdf.content.PdfImage
import com.droidpdf.core.PdfDocument
import com.droidpdf.core.PageSize
import com.droidpdf.core.PdfReader
import com.droidpdf.core.Rectangle
import com.droidpdf.core.TextExtractor
import com.droidpdf.forms.CheckboxField
import com.droidpdf.forms.DropdownField
import com.droidpdf.forms.PdfAcroForm
import com.droidpdf.forms.TextField
import com.droidpdf.layout.Color
import com.droidpdf.layout.Document
import com.droidpdf.layout.Image
import com.droidpdf.layout.Paragraph
import com.droidpdf.layout.Table
import com.droidpdf.layout.Table.Cell
import com.droidpdf.manipulation.PdfMerger
import com.droidpdf.manipulation.PdfSplitter
import com.droidpdf.security.PdfEncryption
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : Activity() {

    private lateinit var logView: TextView
    private val outputDir: File
        get() = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "droidpdf-samples").also {
            it.mkdirs()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DroidPDF.initialize(this)

        val scrollView = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "DroidPDF Sample"
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        root.addView(title)

        addButton(root, "1. Layout API (Text, Table, Image)") { demoLayoutApi() }
        addButton(root, "2. Low-Level Canvas Drawing") { demoCanvasDrawing() }
        addButton(root, "3. Annotations") { demoAnnotations() }
        addButton(root, "4. AcroForm (Forms)") { demoForms() }
        addButton(root, "5. Text Extraction & Search") { demoTextExtraction() }
        addButton(root, "6. Merge PDFs") { demoMerge() }
        addButton(root, "7. Split PDF") { demoSplit() }
        addButton(root, "8. Encryption") { demoEncryption() }
        addButton(root, "9. Run All Demos") { runAllDemos() }

        logView = TextView(this).apply {
            textSize = 12f
            setPadding(0, 24, 0, 0)
            setTextIsSelectable(true)
        }
        root.addView(logView)

        scrollView.addView(root)
        setContentView(scrollView)
    }

    private fun addButton(parent: LinearLayout, text: String, action: () -> Unit) {
        parent.addView(Button(this).apply {
            this.text = text
            isAllCaps = false
            setOnClickListener {
                logView.text = ""
                try {
                    action()
                } catch (e: Exception) {
                    log("ERROR: ${e.message}")
                    Log.e("DroidPDF", "Demo failed", e)
                }
            }
        })
    }

    private fun log(msg: String) {
        logView.append("$msg\n")
        Log.d("DroidPDF", msg)
    }

    // ── Demo 1: Layout API ──────────────────────────────────────────────

    private fun demoLayoutApi() {
        val file = File(outputDir, "demo_layout.pdf")
        FileOutputStream(file).use { fos ->
            val pdf = PdfDocument(fos)
            val doc = Document(pdf).setMargins(50f)

            // Title
            doc.add(
                Paragraph("DroidPDF Layout Demo")
                    .setFont(PdfFont.helveticaBold())
                    .setFontSize(24f)
                    .setColor(Color.BLUE)
                    .setAlignment(Paragraph.Alignment.CENTER)
                    .setMarginBottom(20f)
            )

            // Body text
            doc.add(
                Paragraph("This document demonstrates the high-level layout API of DroidPDF. " +
                    "Text is automatically wrapped and pages are created as needed.")
                    .setFont(PdfFont.timesRoman())
                    .setFontSize(12f)
                    .setLineSpacing(1.5f)
                    .setMarginBottom(16f)
            )

            // Colored paragraphs
            listOf(Color.RED to "Red paragraph", Color.GREEN to "Green paragraph", Color.BLUE to "Blue paragraph")
                .forEach { (color, text) ->
                    doc.add(
                        Paragraph(text)
                            .setFontSize(14f)
                            .setColor(color)
                            .setMarginBottom(8f)
                    )
                }

            // Table with header
            doc.add(
                Paragraph("Product Catalog")
                    .setFont(PdfFont.helveticaBold())
                    .setFontSize(16f)
                    .setMarginBottom(10f)
            )

            val table = Table(4)
                .setHeaderRows(1)
                .setHeaderFont(PdfFont.helveticaBold())
                .setFontSize(10f)
                .setCellPadding(6f)
                .setBorderWidth(0.5f)
                .setBorderColor(Color.DARK_GRAY)
                .setMarginBottom(16f)

            // Header
            table.addCell(Cell("Product", backgroundColor = Color.LIGHT_GRAY))
            table.addCell(Cell("Category", backgroundColor = Color.LIGHT_GRAY))
            table.addCell(Cell("Price", backgroundColor = Color.LIGHT_GRAY))
            table.addCell(Cell("Stock", backgroundColor = Color.LIGHT_GRAY))

            // Data rows
            val products = listOf(
                listOf("DroidPDF Pro", "Software", "$99/yr", "Unlimited"),
                listOf("PDF Viewer", "App", "Free", "N/A"),
                listOf("Font Pack", "Add-on", "$29", "500"),
                listOf("Support Plan", "Service", "$199/yr", "Limited"),
                listOf("Enterprise", "License", "$499/yr", "50"),
            )
            products.forEach { row ->
                row.forEach { table.addCell(it) }
            }
            doc.add(table)

            // Embedded image from RGB bytes
            val imgWidth = 100
            val imgHeight = 60
            val rgbData = ByteArray(imgWidth * imgHeight * 3)
            for (y in 0 until imgHeight) {
                for (x in 0 until imgWidth) {
                    val i = (y * imgWidth + x) * 3
                    rgbData[i] = (x * 255 / imgWidth).toByte()       // R gradient
                    rgbData[i + 1] = (y * 255 / imgHeight).toByte()   // G gradient
                    rgbData[i + 2] = 128.toByte()                     // B constant
                }
            }
            val pdfImage = PdfImage.fromRgbBytes(rgbData, imgWidth, imgHeight)
            doc.add(
                Paragraph("Embedded RGB Gradient Image:")
                    .setFontSize(12f)
                    .setMarginBottom(4f)
            )
            doc.add(
                Image(pdfImage)
                    .setWidth(200f)
                    .setHeight(120f)
                    .setAlignment(Paragraph.Alignment.CENTER)
                    .setMarginBottom(16f)
            )

            // Right-aligned paragraph
            doc.add(
                Paragraph("Right-aligned text for layout verification.")
                    .setFont(PdfFont.courier())
                    .setFontSize(10f)
                    .setAlignment(Paragraph.Alignment.RIGHT)
                    .setMarginBottom(16f)
            )

            doc.close()
        }
        log("Layout demo: ${file.absolutePath}")
        log("  Pages: created successfully")
    }

    // ── Demo 2: Canvas Drawing ──────────────────────────────────────────

    private fun demoCanvasDrawing() {
        val file = File(outputDir, "demo_canvas.pdf")
        FileOutputStream(file).use { fos ->
            val pdf = PdfDocument(fos)
            val page = pdf.addNewPage(PageSize.A4)
            val canvas = PdfCanvas(page)

            val font = PdfFont.helveticaBold()
            val fontName = font.registerOn(page)

            // Title
            canvas.beginText()
                .setFont(fontName, 20f)
                .moveText(150f, 780f)
                .showText("Canvas Drawing Demo")
                .endText()

            // Colored rectangles
            canvas.saveState()
                .setFillColor(1f, 0f, 0f)
                .rectangle(50f, 650f, 150f, 80f)
                .fill()
                .restoreState()

            canvas.saveState()
                .setFillColor(0f, 0.7f, 0f)
                .rectangle(220f, 650f, 150f, 80f)
                .fill()
                .restoreState()

            canvas.saveState()
                .setFillColor(0f, 0f, 1f)
                .rectangle(390f, 650f, 150f, 80f)
                .fill()
                .restoreState()

            // Labels on rectangles
            val labelFont = PdfFont.helvetica()
            val labelFontName = labelFont.registerOn(page)
            canvas.beginText()
                .setFont(labelFontName, 12f)
                .setFillColor(1f, 1f, 1f)
                .moveText(95f, 685f)
                .showText("Red Box")
                .endText()

            canvas.beginText()
                .setFont(labelFontName, 12f)
                .setFillColor(1f, 1f, 1f)
                .moveText(258f, 685f)
                .showText("Green Box")
                .endText()

            canvas.beginText()
                .setFont(labelFontName, 12f)
                .setFillColor(1f, 1f, 1f)
                .moveText(430f, 685f)
                .showText("Blue Box")
                .endText()

            // Lines with different widths
            val regularFont = PdfFont.timesRoman()
            val regularFontName = regularFont.registerOn(page)
            canvas.beginText()
                .setFont(regularFontName, 14f)
                .setFillColor(0f, 0f, 0f)
                .moveText(50f, 610f)
                .showText("Line width samples:")
                .endText()

            listOf(0.5f, 1f, 2f, 4f).forEachIndexed { i, width ->
                val y = 590f - i * 25f
                canvas.saveState()
                    .setLineWidth(width)
                    .setStrokeColor(0.2f, 0.2f, 0.2f)
                    .moveTo(50f, y)
                    .lineTo(300f, y)
                    .stroke()
                    .restoreState()

                canvas.beginText()
                    .setFont(labelFontName, 9f)
                    .setFillColor(0f, 0f, 0f)
                    .moveText(310f, y - 3f)
                    .showText("width = $width pt")
                    .endText()
            }

            // Stroked rectangle with fill
            canvas.saveState()
                .setFillColor(1f, 1f, 0.8f)
                .setStrokeColor(0f, 0f, 0.5f)
                .setLineWidth(2f)
                .rectangle(50f, 400f, 200f, 80f)
                .fillStroke()
                .restoreState()

            canvas.beginText()
                .setFont(regularFontName, 11f)
                .setFillColor(0f, 0f, 0.5f)
                .moveText(65f, 445f)
                .showText("Fill + Stroke rectangle")
                .endText()

            // Gray shades
            canvas.beginText()
                .setFont(regularFontName, 14f)
                .setFillColor(0f, 0f, 0f)
                .moveText(50f, 370f)
                .showText("Gray scale:")
                .endText()

            for (i in 0..9) {
                val gray = i / 9f
                canvas.saveState()
                    .setFillGray(gray)
                    .rectangle(50f + i * 45f, 330f, 40f, 30f)
                    .fill()
                    .restoreState()
            }

            canvas.flush()
            pdf.close()
        }
        log("Canvas demo: ${file.absolutePath}")
    }

    // ── Demo 3: Annotations ─────────────────────────────────────────────

    private fun demoAnnotations() {
        val file = File(outputDir, "demo_annotations.pdf")
        FileOutputStream(file).use { fos ->
            val pdf = PdfDocument(fos)
            val page = pdf.addNewPage(PageSize.A4)
            val canvas = PdfCanvas(page)
            val font = PdfFont.helveticaBold()
            val fontName = font.registerOn(page)

            canvas.beginText()
                .setFont(fontName, 18f)
                .moveText(150f, 780f)
                .showText("Annotations Demo")
                .endText()

            val regularFont = PdfFont.helvetica()
            val regFontName = regularFont.registerOn(page)
            canvas.beginText()
                .setFont(regFontName, 11f)
                .moveText(50f, 740f)
                .showText("This page contains various PDF annotation types.")
                .endText()
            canvas.flush()

            // Sticky note
            val stickyNote = PdfTextAnnotation(Rectangle(50f, 680f, 24f, 24f))
            stickyNote.setOpen(true)
            stickyNote.setIcon(PdfTextAnnotation.ICON_NOTE)
            stickyNote.setContents("This is a sticky note annotation!")
            stickyNote.setColor(Color.YELLOW)
            stickyNote.addTo(page)

            canvas.beginText()
                .setFont(regFontName, 10f)
                .moveText(80f, 685f)
                .showText("<-- Sticky Note (open)")
                .endText()

            // Highlight annotation
            PdfHighlightAnnotation(Rectangle(50f, 640f, 200f, 15f))
                .setColor(Color.YELLOW)
                .setContents("Highlighted region")
                .addTo(page)

            canvas.beginText()
                .setFont(regFontName, 10f)
                .moveText(50f, 643f)
                .showText("This text has a highlight annotation")
                .endText()

            // Stamp
            PdfStampAnnotation(Rectangle(350f, 620f, 180f, 60f), PdfStampAnnotation.APPROVED)
                .setColor(Color.GREEN)
                .setContents("Approved by DroidPDF")
                .addTo(page)

            // Free text
            val freeText = PdfFreeTextAnnotation(Rectangle(50f, 560f, 250f, 30f))
            freeText.setFontSize(14f)
            freeText.setContents("Free text annotation content")
            freeText.setColor(Color.BLUE)
            freeText.addTo(page)

            // Square annotation
            val square = PdfSquareAnnotation(Rectangle(50f, 480f, 120f, 60f))
            square.setBorderWidth(2f)
            square.setInteriorColor(Color(1f, 0.9f, 0.9f))
            square.setColor(Color.RED)
            square.setContents("Square annotation")
            square.addTo(page)

            // Circle annotation
            val circle = PdfCircleAnnotation(Rectangle(200f, 480f, 80f, 60f))
            circle.setBorderWidth(2f)
            circle.setInteriorColor(Color(0.9f, 0.9f, 1f))
            circle.setColor(Color.BLUE)
            circle.setContents("Circle annotation")
            circle.addTo(page)

            // Link annotation
            PdfLinkAnnotation(Rectangle(50f, 430f, 200f, 15f))
                .setUri("https://github.com/youichi-uda/droidpdf")
                .addTo(page)

            canvas.beginText()
                .setFont(regFontName, 10f)
                .setFillColor(0f, 0f, 1f)
                .moveText(50f, 433f)
                .showText("Click here -> GitHub Link Annotation")
                .endText()

            canvas.flush()
            pdf.close()
        }
        log("Annotations demo: ${file.absolutePath}")
    }

    // ── Demo 4: AcroForm ────────────────────────────────────────────────

    private fun demoForms() {
        val file = File(outputDir, "demo_forms.pdf")
        FileOutputStream(file).use { fos ->
            val pdf = PdfDocument(fos)
            val page = pdf.addNewPage(PageSize.A4)
            val canvas = PdfCanvas(page)
            val font = PdfFont.helveticaBold()
            val fontName = font.registerOn(page)
            val regularFont = PdfFont.helvetica()
            val regFontName = regularFont.registerOn(page)

            canvas.beginText()
                .setFont(fontName, 18f)
                .moveText(180f, 780f)
                .showText("AcroForm Demo")
                .endText()

            // Labels
            val labels = listOf(
                700f to "Full Name:",
                650f to "Email Address:",
                600f to "Agree to Terms:",
                550f to "Country:",
            )
            labels.forEach { (y, label) ->
                canvas.beginText()
                    .setFont(regFontName, 12f)
                    .moveText(50f, y)
                    .showText(label)
                    .endText()
            }
            canvas.flush()

            val form = PdfAcroForm()

            // Text field - Name
            val nameField = TextField("fullName", Rectangle(200f, 695f, 300f, 20f))
                .setFontSize(12f)
                .setMaxLength(100)
            nameField.setValue("John Doe")
            form.addField(nameField, page)

            // Text field - Email
            val emailField = TextField("email", Rectangle(200f, 645f, 300f, 20f))
                .setFontSize(12f)
            emailField.setValue("john@example.com")
            form.addField(emailField, page)

            // Checkbox
            val agreeField = CheckboxField("agree", Rectangle(200f, 595f, 20f, 20f))
                .setChecked(true)
            form.addField(agreeField, page)

            // Dropdown
            val countryField = DropdownField(
                "country",
                Rectangle(200f, 545f, 200f, 20f),
                listOf("Japan", "USA", "Germany", "UK", "France", "Australia")
            ).setFontSize(12f)
            countryField.setValue("Japan")
            form.addField(countryField, page)

            // Multiline text area
            canvas.beginText()
                .setFont(regFontName, 12f)
                .moveText(50f, 500f)
                .showText("Comments:")
                .endText()
            canvas.flush()

            val commentsField = TextField("comments", Rectangle(200f, 445f, 300f, 70f))
                .setFontSize(10f)
                .setMultiline(true)
            commentsField.setValue("This is a multiline\ntext field for comments.")
            form.addField(commentsField, page)

            pdf.close()
        }
        log("Forms demo: ${file.absolutePath}")
    }

    // ── Demo 5: Text Extraction ─────────────────────────────────────────

    private fun demoTextExtraction() {
        // First create a PDF with text content
        val buffer = ByteArrayOutputStream()
        val pdf = PdfDocument(buffer)
        val doc = Document(pdf)
        doc.add(
            Paragraph("Chapter 1: Introduction to DroidPDF")
                .setFont(PdfFont.helveticaBold())
                .setFontSize(16f)
                .setMarginBottom(12f)
        )
        doc.add(
            Paragraph("DroidPDF is a Kotlin-native PDF library for Android. " +
                "It provides comprehensive PDF creation and manipulation capabilities. " +
                "Features include text rendering, image embedding, table layout, " +
                "annotations, form fields, encryption, and document merging.")
                .setFontSize(12f)
                .setMarginBottom(12f)
        )
        doc.add(
            Paragraph("Chapter 2: Getting Started")
                .setFont(PdfFont.helveticaBold())
                .setFontSize(16f)
                .setMarginBottom(12f)
        )
        doc.add(
            Paragraph("Add the DroidPDF dependency to your build.gradle.kts file " +
                "and initialize it in your Application or Activity.")
                .setFontSize(12f)
        )
        doc.close()

        // Now read it back and extract text
        val bytes = buffer.toByteArray()
        val reader = PdfReader(ByteArrayInputStream(bytes))
        val extractor = TextExtractor(reader)

        val allText = extractor.extractAll()
        log("Text Extraction demo:")
        log("  Extracted ${allText.length} characters")
        log("  Preview: ${allText.take(120)}...")

        // Search
        val results = extractor.search("DroidPDF")
        log("  Search 'DroidPDF': ${results.size} matches found")
        results.forEachIndexed { i, result ->
            log("    Match ${i + 1}: page ${result.pageIndex}, offset ${result.charOffset}")
        }

        // Per-page extraction
        val page1Text = extractor.extractFromPage(0)
        log("  Page 1 text length: ${page1Text.length} chars")
    }

    // ── Demo 6: Merge ───────────────────────────────────────────────────

    private fun demoMerge() {
        // Create two small PDFs
        val pdf1Bytes = createSimplePdf("Document A - First file for merging")
        val pdf2Bytes = createSimplePdf("Document B - Second file for merging")

        val file = File(outputDir, "demo_merged.pdf")
        FileOutputStream(file).use { fos ->
            val merger = PdfMerger(fos)
            merger.merge(ByteArrayInputStream(pdf1Bytes))
            merger.merge(ByteArrayInputStream(pdf2Bytes))
            log("Merge demo: ${file.absolutePath}")
            log("  Merged ${merger.numberOfPages} pages")
            merger.close()
        }
    }

    // ── Demo 7: Split ───────────────────────────────────────────────────

    private fun demoSplit() {
        // Create a multi-page PDF
        val buffer = ByteArrayOutputStream()
        val pdf = PdfDocument(buffer)
        val doc = Document(pdf)
        for (i in 1..3) {
            doc.add(
                Paragraph("Page $i Content")
                    .setFont(PdfFont.helveticaBold())
                    .setFontSize(20f)
                    .setAlignment(Paragraph.Alignment.CENTER)
            )
            if (i < 3) doc.addNewLayoutPage()
        }
        doc.close()
        val sourceBytes = buffer.toByteArray()

        // Extract page 1-2
        val file = File(outputDir, "demo_split_p1_p2.pdf")
        FileOutputStream(file).use { fos ->
            val splitter = PdfSplitter(ByteArrayInputStream(sourceBytes))
            log("Split demo: source has ${splitter.numberOfPages} pages")
            splitter.extractPages(1, 2, fos)
        }
        log("  Extracted pages 1-2: ${file.absolutePath}")

        // Split each page individually
        val splitter2 = PdfSplitter(ByteArrayInputStream(sourceBytes))
        splitter2.splitByPage { pageNumber ->
            val pageFile = File(outputDir, "demo_split_page_$pageNumber.pdf")
            log("  Split page $pageNumber: ${pageFile.absolutePath}")
            FileOutputStream(pageFile)
        }
    }

    // ── Demo 8: Encryption ──────────────────────────────────────────────

    private fun demoEncryption() {
        val encryption = PdfEncryption.Builder()
            .setUserPassword("user123")
            .setOwnerPassword("owner456")
            .setKeyLength(256)
            .allowPrinting(true)
            .allowCopying(false)
            .allowModifying(false)
            .allowAnnotations(true)
            .build()

        log("Encryption demo:")
        log("  Key length: 256-bit AES")
        log("  User password set: yes")
        log("  Owner password set: yes")
        log("  Permissions: print=yes, copy=no, modify=no, annotate=yes")

        // Test encryption/decryption round-trip
        val key = encryption.generateEncryptionKey()
        val testData = "Hello, encrypted PDF!".toByteArray()
        val encrypted = encryption.encrypt(testData, key)
        val decrypted = encryption.decrypt(encrypted, key)
        val roundTrip = String(decrypted)
        log("  Round-trip test: ${if (roundTrip == "Hello, encrypted PDF!") "PASS" else "FAIL"}")
        log("  Verify user password: ${encryption.verifyUserPassword("user123")}")
        log("  Verify wrong password: ${encryption.verifyUserPassword("wrong")}")
    }

    // ── Run All ─────────────────────────────────────────────────────────

    private fun runAllDemos() {
        log("=== Running All Demos ===\n")
        demoLayoutApi()
        log("")
        demoCanvasDrawing()
        log("")
        demoAnnotations()
        log("")
        demoForms()
        log("")
        demoTextExtraction()
        log("")
        demoMerge()
        log("")
        demoSplit()
        log("")
        demoEncryption()
        log("\n=== All Demos Complete ===")
        Toast.makeText(this, "All demos completed!", Toast.LENGTH_SHORT).show()
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private fun createSimplePdf(title: String): ByteArray {
        val buffer = ByteArrayOutputStream()
        val pdf = PdfDocument(buffer)
        val doc = Document(pdf)
        doc.add(
            Paragraph(title)
                .setFont(PdfFont.helveticaBold())
                .setFontSize(18f)
                .setAlignment(Paragraph.Alignment.CENTER)
                .setMarginBottom(20f)
        )
        doc.add(
            Paragraph("This is sample content for testing PDF operations.")
                .setFontSize(12f)
        )
        doc.close()
        return buffer.toByteArray()
    }
}
