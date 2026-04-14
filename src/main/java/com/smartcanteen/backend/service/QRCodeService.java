package com.smartcanteen.backend.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class QRCodeService {

    public byte[] generateQRCode(String text) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 250, 250);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        for (int x = 0; x < 250; x++) {
            for (int y = 0; y < 250; y++) {
                stream.write(matrix.get(x, y) ? 0 : 255);
            }
        }

        return stream.toByteArray();
    }
}
