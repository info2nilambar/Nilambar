package com.nilambar.erp.service.payment;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.nilambar.erp.config.ErpProperties;
import com.nilambar.erp.service.BusinessException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Renders the payee's collection details as a scannable QR code. A UPI intent is used when a UPI id
 * is configured, otherwise the bank transfer details are encoded as plain text.
 */
@Service
public class PaymentQrService {

    private static final int SIZE_PX = 320;

    private final ErpProperties properties;

    public PaymentQrService(ErpProperties properties) {
        this.properties = properties;
    }

    public String payload(BigDecimal amount, String reference) {
        ErpProperties.Payment payment = properties.getPayment();
        if (StringUtils.hasText(payment.getUpiId())) {
            return "upi://pay?pa=%s&pn=%s&am=%s&cu=INR&tn=%s".formatted(
                    payment.getUpiId(),
                    encode(payment.getPayeeName()),
                    amount.toPlainString(),
                    encode(reference));
        }
        return """
                Beneficiary: %s
                Bank: %s
                A/c: %s
                IFSC: %s
                Amount: INR %s
                Reference: %s""".formatted(payment.getPayeeName(), payment.getBankName(),
                payment.getAccountNumber(), payment.getIfsc(), amount.toPlainString(), reference);
    }

    public byte[] pngFor(BigDecimal amount, String reference) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(payload(amount, reference), BarcodeFormat.QR_CODE,
                    SIZE_PX, SIZE_PX, Map.of(EncodeHintType.MARGIN, 1,
                            EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M));
            BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < matrix.getWidth(); x++) {
                for (int y = 0; y < matrix.getHeight(); y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new BusinessException("Could not render the payment QR code.");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
