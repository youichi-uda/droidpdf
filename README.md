# DroidPDF

**Kotlin-native PDF library for Android** — create, edit, merge, split, annotate, encrypt, and extract text from PDF documents.

An affordable [iText7](https://itextpdf.com/) alternative. Free for personal use, **$99/year** for commercial.

[![API](https://img.shields.io/badge/API-26%2B-brightgreen)](https://developer.android.com/about/versions/oreo)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-blue)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-BSL-orange)](LICENSE)
[![ISO](https://img.shields.io/badge/PDF-ISO%2032000--1-informational)](https://droidpdf.abyo.net)

---

## Features

| Category | Capabilities |
|---|---|
| **PDF Generation** | Text, images (JPEG/PNG/WebP), tables, custom fonts (TTF/OTF), page sizes |
| **Layout Engine** | `Document`, `Paragraph`, `Table`, `Image` — auto page breaks, margins, alignment |
| **Page Operations** | Merge, split, rotate, reorder, insert, delete pages |
| **Annotations** | Highlight, underline, strikeout, sticky notes, stamps, shapes, ink, links (11 types) |
| **Forms (AcroForm)** | Text fields, checkboxes, radio buttons, dropdowns |
| **Encryption** | AES-128 / AES-256, user & owner passwords, permission flags |
| **Text Extraction** | Extract text per page, case-insensitive search |
| **Reader** | Lenient parser — reads malformed PDFs without crashing |

## Quick Start

### Installation

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.droidpdf:droidpdf:1.0.0")
}
```

### Create a PDF

```kotlin
val pdf = PdfDocument(outputStream)
val document = Document(pdf)

document.add(
    Paragraph("Hello, DroidPDF!")
        .setFont(PdfFont.helveticaBold())
        .setFontSize(24f)
)

document.add(
    Table(3).apply {
        addCell("Name")
        addCell("Age")
        addCell("City")
        addCell("Alice")
        addCell("30")
        addCell("Tokyo")
    }
)

document.close()
```

### Merge PDFs

```kotlin
val merger = PdfMerger(outputStream)
merger.merge(inputStream1)           // all pages
merger.merge(inputStream2, 2, 5)     // pages 2–5
merger.close()
```

### Add Annotations

```kotlin
val pdf = PdfDocument(PdfReader(input), outputStream)
PdfHighlightAnnotation(Rectangle(72f, 700f, 200f, 14f))
    .setColor(Color.YELLOW)
    .addTo(pdf.firstPage)
pdf.close()
```

### Encrypt a PDF

```kotlin
val encryption = PdfEncryption.Builder()
    .setUserPassword("secret")
    .setKeyLength(256)
    .allowPrinting(true)
    .allowCopying(false)
    .build()
```

### Extract Text

```kotlin
val reader = PdfReader(inputStream)
val extractor = TextExtractor(reader)
val text = extractor.extractAll()
val results = extractor.search("invoice")
```

## Package Structure

```
com.droidpdf.core           PdfDocument, PdfPage, PdfReader, PdfWriter, TextExtractor
com.droidpdf.content        PdfCanvas, PdfFont, PdfImage
com.droidpdf.layout         Document, Paragraph, Table, Image
com.droidpdf.annotations    11 annotation types
com.droidpdf.forms          TextField, CheckboxField, RadioButtonField, DropdownField
com.droidpdf.manipulation   PdfMerger, PdfSplitter, PageOperations
com.droidpdf.security       PdfEncryption (AES-128/256)
```

## Requirements

- Android API 26+ (Android 8.0)
- Kotlin 2.1+
- JVM target 17

## Pricing

| Plan | Price | For |
|---|---|---|
| **Personal** | Free | Personal, non-commercial, OSS |
| **Commercial** | $99/year | Any commercial app — any team size, any revenue |

Free for personal use. 7-day free trial for commercial. The library is fully functional without a license key — a single warning log is emitted per session.

**[Buy a license](https://y1uda.gumroad.com/l/DroidPDF)**

## Documentation

- **Website & Docs**: [droidpdf.abyo.net](https://droidpdf.abyo.net)
- **Getting Started**: [droidpdf.abyo.net/docs/](https://droidpdf.abyo.net/docs/)
- **API Reference**: [droidpdf.abyo.net/api/](https://droidpdf.abyo.net/api/)

## Technical Details

- **PDF Spec**: ISO 32000-1 (PDF 1.7)
- **Font handling**: Apache FontBox (Apache 2.0)
- **Image handling**: Android BitmapFactory
- **Compression**: java.util.zip (Flate/Deflate)
- **Encryption**: javax.crypto (AES)
- **Design**: Single-thread, lazy page loading, lenient parsing

## License

[Business Source License (BSL)](LICENSE) — code is open and readable. Commercial use requires a paid license key.

## Contributing

Issues and pull requests are welcome. Please read the contribution guidelines before submitting.
