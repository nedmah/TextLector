package com.nedmah.textlector.data.db

import com.nedmah.textlector.db.Paragraph

fun Paragraph.toDomain() : com.nedmah.textlector.domain.model.Paragraph =
    com.nedmah.textlector.domain.model.Paragraph(
        id = id,
        documentId = document_id,
        index = index_in_doc.toInt(),
        text = text
    )