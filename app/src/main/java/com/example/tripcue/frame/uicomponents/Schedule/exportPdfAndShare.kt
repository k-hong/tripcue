package com.example.tripcue.frame.uicomponents.Schedule

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

fun exportPdfAndShare(context: Context, fileName: String, content: String) {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val paint = android.graphics.Paint()
        paint.textSize = 12f

        val lines = content.split("\n")
        var yPosition = 25

        for (line in lines) {
            canvas.drawText(line, 10f, yPosition.toFloat(), paint)
            yPosition += 20
        }

        pdfDocument.finishPage(page)

        val file = File(context.getExternalFilesDir(null), fileName)
        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        outputStream.close()

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(intent, "공유할 앱을 선택하세요")
        )

    } catch (e: Exception) {
        Toast.makeText(context, "PDF 생성 실패: ${e.message}", Toast.LENGTH_LONG).show()
    }
}