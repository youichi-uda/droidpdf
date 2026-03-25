package com.droidpdf.forms

import com.droidpdf.core.PdfArray
import com.droidpdf.core.PdfDictionary
import com.droidpdf.core.PdfInteger
import com.droidpdf.core.PdfName
import com.droidpdf.core.PdfPage
import com.droidpdf.core.PdfString
import com.droidpdf.core.Rectangle

/**
 * AcroForm field types (ISO 32000-1, 12.7).
 */
sealed class FormField(
    protected val name: String,
    protected val rect: Rectangle,
) {
    var fieldValue: String? = null
        private set

    fun setValue(v: String): FormField {
        this.fieldValue = v
        return this
    }

    abstract fun buildDictionary(page: PdfPage): PdfDictionary

    protected fun buildBase(
        page: PdfPage,
        fieldType: String,
    ): PdfDictionary {
        val dict = PdfDictionary()
        dict.put(PdfName.TYPE, PdfName("Annot"))
        dict.put(PdfName.SUBTYPE, PdfName("Widget"))
        dict.put("FT", PdfName(fieldType))
        dict.put("T", PdfString(name))
        dict.put("Rect", rect.toPdfArray())
        return dict
    }
}

/**
 * Text input field (ISO 32000-1, 12.7.4.3).
 */
class TextField(
    name: String,
    rect: Rectangle,
) : FormField(name, rect) {
    private var defaultAppearance: String = "/Helv 12 Tf 0 g"
    private var maxLength: Int? = null
    private var multiline: Boolean = false

    fun setFontSize(size: Float): TextField {
        this.defaultAppearance = "/Helv ${"%.0f".format(size)} Tf 0 g"
        return this
    }

    fun setMaxLength(length: Int): TextField {
        this.maxLength = length
        return this
    }

    fun setMultiline(multiline: Boolean): TextField {
        this.multiline = multiline
        return this
    }

    override fun buildDictionary(page: PdfPage): PdfDictionary {
        val dict = buildBase(page, "Tx")
        dict.put("DA", PdfString(defaultAppearance))
        fieldValue?.let { dict.put("V", PdfString(it)) }
        maxLength?.let { dict.put("MaxLen", PdfInteger(it)) }
        if (multiline) {
            // Ff bit 13 = Multiline (ISO 32000-1, Table 228)
            dict.put("Ff", PdfInteger(1 shl 12))
        }
        return dict
    }
}

/**
 * Checkbox field (ISO 32000-1, 12.7.4.2.3).
 */
class CheckboxField(
    name: String,
    rect: Rectangle,
) : FormField(name, rect) {
    private var checked: Boolean = false

    fun setChecked(checked: Boolean): CheckboxField {
        this.checked = checked
        return this
    }

    fun isChecked(): Boolean = checked

    override fun buildDictionary(page: PdfPage): PdfDictionary {
        val dict = buildBase(page, "Btn")
        val onValue = PdfName("Yes")
        dict.put("V", if (checked) onValue else PdfName("Off"))
        dict.put("AS", if (checked) onValue else PdfName("Off"))

        // Appearance dictionary for visual rendering
        val ap = PdfDictionary()
        val n = PdfDictionary()
        n.put("Yes", PdfDictionary()) // Placeholder appearance stream
        n.put("Off", PdfDictionary())
        ap.put("N", n)
        dict.put("AP", ap)

        return dict
    }
}

/**
 * Radio button group (ISO 32000-1, 12.7.4.2.4).
 */
class RadioButtonField(
    name: String,
    private val options: List<RadioOption>,
) : FormField(name, options.firstOrNull()?.rect ?: Rectangle(0f, 0f, 0f, 0f)) {
    private var selectedIndex: Int = -1

    fun setSelected(index: Int): RadioButtonField {
        require(index in options.indices) { "Index $index out of range" }
        this.selectedIndex = index
        return this
    }

    override fun buildDictionary(page: PdfPage): PdfDictionary {
        // Radio button group is a parent field with child widgets
        val dict = PdfDictionary()
        dict.put("FT", PdfName("Btn"))
        dict.put("T", PdfString(name))
        // Ff bit 16 = Radio (ISO 32000-1, Table 227)
        dict.put("Ff", PdfInteger((1 shl 15) or (1 shl 14)))

        val kids = PdfArray()
        options.forEachIndexed { index, option ->
            val kid = PdfDictionary()
            kid.put(PdfName.TYPE, PdfName("Annot"))
            kid.put(PdfName.SUBTYPE, PdfName("Widget"))
            kid.put("Rect", option.rect.toPdfArray())
            val optName = PdfName(option.value)
            kid.put("AS", if (index == selectedIndex) optName else PdfName("Off"))

            val ap = PdfDictionary()
            val n = PdfDictionary()
            n.put(option.value, PdfDictionary())
            n.put("Off", PdfDictionary())
            ap.put("N", n)
            kid.put("AP", ap)

            kids.add(kid)
        }
        dict.put("Kids", kids)

        if (selectedIndex >= 0) {
            dict.put("V", PdfName(options[selectedIndex].value))
        }

        return dict
    }

    data class RadioOption(val value: String, val rect: Rectangle)
}

/**
 * Dropdown / combo box field (ISO 32000-1, 12.7.4.4).
 */
class DropdownField(
    name: String,
    rect: Rectangle,
    private val options: List<String>,
) : FormField(name, rect) {
    private var defaultAppearance: String = "/Helv 12 Tf 0 g"

    fun setFontSize(size: Float): DropdownField {
        this.defaultAppearance = "/Helv ${"%.0f".format(size)} Tf 0 g"
        return this
    }

    override fun buildDictionary(page: PdfPage): PdfDictionary {
        val dict = buildBase(page, "Ch")
        dict.put("DA", PdfString(defaultAppearance))
        // Ff bit 18 = Combo (ISO 32000-1, Table 229)
        dict.put("Ff", PdfInteger(1 shl 17))

        val optArray = PdfArray()
        options.forEach { optArray.add(PdfString(it)) }
        dict.put("Opt", optArray)

        fieldValue?.let { dict.put("V", PdfString(it)) }

        return dict
    }
}

/**
 * Manages AcroForm fields on a document.
 */
class PdfAcroForm {
    private val fields = mutableListOf<Pair<FormField, PdfPage>>()

    /**
     * Add a form field to the specified page.
     */
    fun addField(
        field: FormField,
        page: PdfPage,
    ): PdfAcroForm {
        fields.add(field to page)
        val dict = field.buildDictionary(page)
        page.addAnnotation(dict)
        return this
    }

    /**
     * Get all field dictionaries for inclusion in the document catalog.
     */
    fun buildAcroFormDictionary(): PdfDictionary? {
        if (fields.isEmpty()) return null
        val dict = PdfDictionary()
        val fieldsArray = PdfArray()
        fields.forEach { (field, page) ->
            fieldsArray.add(field.buildDictionary(page))
        }
        dict.put("Fields", fieldsArray)
        // Need default resources for form rendering
        val dr = PdfDictionary()
        val fontDict = PdfDictionary()
        val helv = PdfDictionary()
        helv.put(PdfName.TYPE, PdfName.FONT)
        helv.put(PdfName.SUBTYPE, PdfName("Type1"))
        helv.put("BaseFont", PdfName("Helvetica"))
        fontDict.put("Helv", helv)
        dr.put(PdfName.FONT, fontDict)
        dict.put("DR", dr)
        dict.put("DA", PdfString("/Helv 12 Tf 0 g"))
        return dict
    }
}
