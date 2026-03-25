package com.droidpdf.forms

import com.droidpdf.core.PageSize
import com.droidpdf.core.PdfDocument
import com.droidpdf.core.PdfLog
import com.droidpdf.core.Rectangle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class PdfAcroFormTest {
    @BeforeEach
    fun setup() {
        PdfLog.logger = PdfLog.NoOpLogger()
    }

    @Test
    fun `text field produces valid PDF`() {
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        val page = doc.addNewPage(PageSize.A4)

        val form = PdfAcroForm()
        val field = TextField("name", Rectangle(72f, 700f, 200f, 20f))
        field.setValue("John Doe")
        form.addField(field, page)

        doc.close()

        val pdf = output.toString("US-ASCII")
        assertTrue(pdf.contains("/FT /Tx"))
        assertTrue(pdf.contains("(name)"))
        assertTrue(pdf.contains("(John Doe)"))
    }

    @Test
    fun `text field with max length`() {
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        val page = doc.addNewPage()

        val field = TextField("zip", Rectangle(72f, 700f, 100f, 20f))
        field.setMaxLength(5)
        PdfAcroForm().addField(field, page)
        doc.close()

        val pdf = output.toString("US-ASCII")
        assertTrue(pdf.contains("/MaxLen 5"))
    }

    @Test
    fun `multiline text field`() {
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        val page = doc.addNewPage()

        val field = TextField("comments", Rectangle(72f, 600f, 300f, 100f))
        field.setMultiline(true)
        PdfAcroForm().addField(field, page)
        doc.close()

        val pdf = output.toString("US-ASCII")
        assertTrue(pdf.contains("/Ff"))
    }

    @Test
    fun `checkbox field`() {
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        val page = doc.addNewPage()

        val field = CheckboxField("agree", Rectangle(72f, 700f, 15f, 15f))
        field.setChecked(true)
        PdfAcroForm().addField(field, page)
        doc.close()

        val pdf = output.toString("US-ASCII")
        assertTrue(pdf.contains("/FT /Btn"))
        assertTrue(pdf.contains("/V /Yes"))
    }

    @Test
    fun `unchecked checkbox`() {
        val field = CheckboxField("agree", Rectangle(72f, 700f, 15f, 15f))
        assertEquals(false, field.isChecked())

        field.setChecked(true)
        assertEquals(true, field.isChecked())
    }

    @Test
    fun `dropdown field`() {
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        val page = doc.addNewPage()

        val field =
            DropdownField(
                "country",
                Rectangle(72f, 700f, 200f, 20f),
                listOf("Japan", "USA", "UK", "Germany"),
            )
        field.setValue("Japan")
        PdfAcroForm().addField(field, page)
        doc.close()

        val pdf = output.toString("US-ASCII")
        assertTrue(pdf.contains("/FT /Ch"))
        assertTrue(pdf.contains("/Opt"))
        assertTrue(pdf.contains("(Japan)"))
    }

    @Test
    fun `radio button field`() {
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        val page = doc.addNewPage()

        val field =
            RadioButtonField(
                "size",
                listOf(
                    RadioButtonField.RadioOption("S", Rectangle(72f, 700f, 15f, 15f)),
                    RadioButtonField.RadioOption("M", Rectangle(92f, 700f, 15f, 15f)),
                    RadioButtonField.RadioOption("L", Rectangle(112f, 700f, 15f, 15f)),
                ),
            )
        field.setSelected(1) // M
        PdfAcroForm().addField(field, page)
        doc.close()

        val pdf = output.toString("US-ASCII")
        assertTrue(pdf.contains("/FT /Btn"))
        assertTrue(pdf.contains("/V /M"))
    }

    @Test
    fun `acroform dictionary is built correctly`() {
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        val page = doc.addNewPage()

        val form = PdfAcroForm()
        form.addField(TextField("f1", Rectangle(72f, 700f, 200f, 20f)), page)
        form.addField(TextField("f2", Rectangle(72f, 670f, 200f, 20f)), page)

        val dict = form.buildAcroFormDictionary()
        assertNotNull(dict)

        doc.close()
    }

    @Test
    fun `multiple form fields on one page`() {
        val output = ByteArrayOutputStream()
        val doc = PdfDocument(output)
        val page = doc.addNewPage()

        val form = PdfAcroForm()
        form.addField(
            TextField("name", Rectangle(72f, 700f, 200f, 20f)).apply { setValue("Alice") },
            page,
        )
        form.addField(
            CheckboxField("agree", Rectangle(72f, 670f, 15f, 15f)).apply { setChecked(true) },
            page,
        )
        form.addField(
            DropdownField("lang", Rectangle(72f, 640f, 200f, 20f), listOf("Kotlin", "Java")),
            page,
        )
        doc.close()

        val pdf = output.toString("US-ASCII")
        assertTrue(pdf.contains("/Tx"))
        assertTrue(pdf.contains("/Btn"))
        assertTrue(pdf.contains("/Ch"))
    }
}
