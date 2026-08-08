package com.nilambar.erp.service.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.nilambar.erp.config.ErpProperties;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class PaymentQrServiceTest {

    private final ErpProperties properties = new ErpProperties();
    private final PaymentQrService service = new PaymentQrService(properties);

    @Test
    void bankPayloadCarriesAccountIfscAndAmount() {
        String payload = service.payload(new BigDecimal("1299.00"), "ORD-1234");

        assertThat(payload)
                .contains("11540334561")
                .contains("SBIN0007021")
                .contains("1299.00")
                .contains("ORD-1234");
    }

    @Test
    void upiPayloadIsUsedWhenAUpiIdIsConfigured() {
        properties.getPayment().setUpiId("nilambar@sbi");

        assertThat(service.payload(new BigDecimal("50.00"), "ORD-1"))
                .startsWith("upi://pay?pa=nilambar@sbi")
                .contains("am=50.00")
                .contains("cu=INR");
    }

    @Test
    void renderedPngDecodesBackToThePayload() throws Exception {
        BigDecimal amount = new BigDecimal("2499.00");
        byte[] png = service.pngFor(amount, "ORD-ABCD1234");

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(
                ImageIO.read(new ByteArrayInputStream(png)))));

        assertThat(new QRCodeReader().decode(bitmap).getText())
                .isEqualTo(service.payload(amount, "ORD-ABCD1234"));
    }
}
