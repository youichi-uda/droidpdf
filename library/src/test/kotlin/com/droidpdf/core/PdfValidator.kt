package com.droidpdf.core

/**
 * Validates PDF binary structure for correctness.
 * Used in tests to catch structural issues that would prevent
 * real PDF viewers from opening the file.
 */
object PdfValidator {
    data class ValidationResult(
        val valid: Boolean,
        val errors: List<String>,
        val warnings: List<String>,
    ) {
        override fun toString(): String =
            buildString {
                appendLine("Valid: $valid")
                if (errors.isNotEmpty()) {
                    appendLine("Errors:")
                    errors.forEach { appendLine("  - $it") }
                }
                if (warnings.isNotEmpty()) {
                    appendLine("Warnings:")
                    warnings.forEach { appendLine("  - $it") }
                }
            }
    }

    fun validate(pdfBytes: ByteArray): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val content = String(pdfBytes, Charsets.ISO_8859_1)

        // 1. Header check
        if (!content.startsWith("%PDF-")) {
            errors.add("Missing %PDF- header")
        } else {
            val version =
                content.substring(5, content.indexOf('\n').coerceAtMost(12))
                    .trim().trimEnd('\r')
            if (version != "1.7") {
                warnings.add("PDF version is $version, expected 1.7")
            }
        }

        // 2. EOF check
        val trimmed = content.trimEnd()
        if (!trimmed.endsWith("%%EOF")) {
            errors.add("Missing %%EOF at end of file")
        }

        // 3. Find startxref
        val startxrefPos = content.lastIndexOf("startxref")
        if (startxrefPos < 0) {
            errors.add("Missing startxref keyword")
            return ValidationResult(errors.isEmpty(), errors, warnings)
        }

        // 4. Parse xref offset
        val afterStartxref = content.substring(startxrefPos + "startxref".length).trim()
        val xrefOffsetStr = afterStartxref.lines().first().trim()
        val xrefOffset = xrefOffsetStr.toIntOrNull()
        if (xrefOffset == null) {
            errors.add("Invalid startxref value: '$xrefOffsetStr'")
            return ValidationResult(errors.isEmpty(), errors, warnings)
        }

        // 5. Verify xref keyword at offset
        if (xrefOffset >= pdfBytes.size) {
            errors.add("startxref offset $xrefOffset exceeds file size ${pdfBytes.size}")
            return ValidationResult(errors.isEmpty(), errors, warnings)
        }

        val atXref = content.substring(xrefOffset)
        if (!atXref.startsWith("xref")) {
            errors.add("Expected 'xref' at offset $xrefOffset, got '${atXref.take(20)}'")
            return ValidationResult(errors.isEmpty(), errors, warnings)
        }

        // 6. Parse xref entries and validate offsets
        val xrefLines = atXref.lines()
        var lineIdx = 1 // skip "xref"
        var objectCount = 0
        while (lineIdx < xrefLines.size) {
            val line = xrefLines[lineIdx].trim()
            if (line.startsWith("trailer") || line.isEmpty()) break

            val subsectionParts = line.split("\\s+".toRegex())
            if (subsectionParts.size == 2) {
                val startObj = subsectionParts[0].toIntOrNull()
                val count = subsectionParts[1].toIntOrNull()
                if (startObj != null && count != null) {
                    lineIdx++
                    for (i in 0 until count) {
                        if (lineIdx >= xrefLines.size) break
                        val entryLine = xrefLines[lineIdx].trim()
                        lineIdx++
                        if (entryLine.length < 17) {
                            warnings.add("Short xref entry for object ${startObj + i}: '$entryLine'")
                            continue
                        }
                        val entryParts = entryLine.split("\\s+".toRegex())
                        if (entryParts.size < 3) continue

                        val offset = entryParts[0].toLongOrNull() ?: continue
                        val inUse = entryParts[2] == "n"

                        if (inUse) {
                            objectCount++
                            // Validate object exists at offset
                            if (offset >= pdfBytes.size) {
                                errors.add("Object ${startObj + i}: offset $offset exceeds file size")
                                continue
                            }
                            val atObj = content.substring(offset.toInt())
                            val objHeader = atObj.lines().first().trim()
                            val expectedPrefix = "${startObj + i} "
                            if (!objHeader.startsWith(expectedPrefix)) {
                                errors.add(
                                    "Object ${startObj + i}: expected '$expectedPrefix... obj' " +
                                        "at offset $offset, got '$objHeader'",
                                )
                            } else if (!objHeader.endsWith("obj")) {
                                errors.add(
                                    "Object ${startObj + i}: line at offset $offset " +
                                        "doesn't end with 'obj': '$objHeader'",
                                )
                            }
                        }
                    }
                    continue
                }
            }
            lineIdx++
        }

        if (objectCount == 0) {
            errors.add("No in-use objects found in xref table")
        }

        // 7. Check trailer has Root
        val trailerPos = content.lastIndexOf("trailer")
        if (trailerPos < 0) {
            errors.add("Missing trailer dictionary")
        } else {
            val trailerContent = content.substring(trailerPos)
            if (!trailerContent.contains("/Root")) {
                errors.add("Trailer missing /Root entry")
            }
            if (!trailerContent.contains("/Size")) {
                errors.add("Trailer missing /Size entry")
            }
        }

        // 8. Check stream/endstream pairing
        var searchPos = 0
        var streamCount = 0
        while (true) {
            val streamPos = content.indexOf("\nstream\n", searchPos)
            if (streamPos < 0) break
            streamCount++
            val endstreamPos = content.indexOf("endstream", streamPos)
            if (endstreamPos < 0) {
                errors.add("stream at offset $streamPos has no matching endstream")
                break
            }
            searchPos = endstreamPos + 9
        }

        // 9. Check obj/endobj pairing
        val objPattern = Regex("\\d+ \\d+ obj\\b")
        val objMatches = objPattern.findAll(content).toList()
        val endobjCount = Regex("\\bendobj\\b").findAll(content).count()
        if (objMatches.size != endobjCount) {
            errors.add("obj/endobj mismatch: ${objMatches.size} obj vs $endobjCount endobj")
        }

        return ValidationResult(errors.isEmpty(), errors, warnings)
    }
}
