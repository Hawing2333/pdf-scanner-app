package com.pipixia.pdf_scanner_app;

import android.graphics.Bitmap;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;

import java.util.concurrent.TimeUnit;

public class OcrHelper {

    private static TextRecognizer recognizer;

    private static TextRecognizer getRecognizer() {
        if (recognizer == null) {
            recognizer = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
        }
        return recognizer;
    }

    public static String recognize(Bitmap bitmap) throws Exception {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        com.google.mlkit.vision.text.Text result =
                Tasks.await(getRecognizer().process(image), 30, TimeUnit.SECONDS);

        StringBuilder sb = new StringBuilder();
        for (com.google.mlkit.vision.text.Text.TextBlock block : result.getTextBlocks()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(block.getText());
        }
        return sb.toString();
    }

    public static void release() {
        if (recognizer != null) {
            recognizer.close();
            recognizer = null;
        }
    }
}
