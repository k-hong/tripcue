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

/**
 * 주어진 문자열 내용을 PDF로 저장하고, 공유 인텐트를 통해 사용자가 앱으로 공유할 수 있도록 함.
 *
 * @param context Context (Activity 또는 Application Context)
 * @param fileName 생성할 PDF 파일 이름 (예: "schedule.pdf")
 * @param content PDF에 출력할 텍스트 내용 (줄바꿈 포함)
 */
fun exportPdfAndShare(context: Context, fileName: String, content: String) {
    try {
        // 1. PDF 문서 객체 생성
        val pdfDocument = PdfDocument()

        // 2. PDF 페이지 설정 (크기: 너비 300, 높이 600, 페이지 번호 1)
        val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        // 3. Canvas에 텍스트 그리기
        val canvas = page.canvas
        val paint = android.graphics.Paint().apply {
            textSize = 12f // 텍스트 크기 설정
        }

        // 4. 텍스트 줄 단위로 나눠서 한 줄씩 출력
        val lines = content.split("\n")
        var yPosition = 25 // 첫 줄의 시작 y 좌표

        for (line in lines) {
            canvas.drawText(line, 10f, yPosition.toFloat(), paint)
            yPosition += 20 // 줄 간격
        }

        // 5. 페이지 완성 및 문서에 추가
        pdfDocument.finishPage(page)

        // 6. 파일 생성 경로: 앱의 외부 저장 디렉터리 (앱 전용, 접근 가능)
        val file = File(context.getExternalFilesDir(null), fileName)
        val outputStream = FileOutputStream(file)

        // 7. PDF 내용을 파일로 저장
        pdfDocument.writeTo(outputStream)

        // 8. 스트림 및 문서 닫기
        pdfDocument.close()
        outputStream.close()

        // 9. 파일 공유를 위한 URI 생성 (FileProvider를 통해 앱 외부 접근 허용)
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider", // FileProvider authority (AndroidManifest에 정의되어야 함)
            file
        )

        // 10. 공유 인텐트 구성 (PDF 파일을 다른 앱으로 공유)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri) // PDF 파일 첨부
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // 수신 앱에 읽기 권한 부여
        }

        // 11. 공유 앱 선택 창 띄우기
        context.startActivity(
            Intent.createChooser(intent, "공유할 앱을 선택하세요")
        )

    } catch (e: Exception) {
        // 예외 발생 시 사용자에게 에러 메시지 표시
        Toast.makeText(context, "PDF 생성 실패: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
