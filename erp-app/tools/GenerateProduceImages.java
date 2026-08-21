import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Draws the placeholder tiles for the fresh-produce catalog rows added by
 * {@code FreshProduceCatalogSeeder}. Images are generated locally (no third-party assets) so the
 * catalog stays self-contained, and re-running the tool is idempotent.
 *
 * <p>Usage from {@code erp-app/}: {@code java tools/GenerateProduceImages.java}
 */
public final class GenerateProduceImages {

    private static final int SIZE = 600;
    private static final Path IMAGE_DIR = Path.of("src/main/resources/static/images/products");

    private record Tile(String sku, String label, Color top, Color bottom) {
    }

    private static final List<Tile> TILES = List.of(
            tile("VEG-001", "Tomato", 0xE05A3B, 0x8C2A1C),
            tile("VEG-002", "Potato", 0xC79A5B, 0x7A5A2E),
            tile("VEG-003", "Onion", 0xB1668F, 0x6B2E4E),
            tile("VEG-004", "Cauliflower", 0xE9E4C9, 0x9C9673),
            tile("VEG-005", "Spinach", 0x4E8A3C, 0x24501C),
            tile("VEG-006", "Carrot", 0xE08A2E, 0x8E4C10),
            tile("VEG-007", "Brinjal", 0x6B4A8C, 0x36214F),
            tile("VEG-008", "Okra", 0x6FA24A, 0x365E22),
            tile("VEG-009", "Capsicum", 0x3F9E5E, 0x1E5733),
            tile("VEG-010", "Cucumber", 0x8CBF5A, 0x4A6E2A),
            tile("FRT-001", "Banana", 0xE7C64A, 0x9C8018),
            tile("FRT-002", "Apple", 0xD8412F, 0x861D14),
            tile("FRT-003", "Mango", 0xE9A22B, 0x9A5C0C),
            tile("FRT-004", "Orange", 0xE87722, 0x9A4A08),
            tile("FRT-005", "Grapes", 0x6E5A9E, 0x38285C),
            tile("FRT-006", "Papaya", 0xE0863F, 0x8F4A17),
            tile("FRT-007", "Pomegranate", 0xC02F45, 0x71121F),
            tile("FRT-008", "Watermelon", 0x3E8B4A, 0x1D4A21),
            tile("FRT-009", "Pineapple", 0xD9B23E, 0x8A6C11),
            tile("FRT-010", "Guava", 0x9DBF63, 0x5A7330));

    private GenerateProduceImages() {
    }

    public static void main(String[] args) throws IOException {
        Files.createDirectories(IMAGE_DIR);
        for (Tile tile : TILES) {
            Path target = IMAGE_DIR.resolve(tile.sku() + ".png");
            ImageIO.write(render(tile), "png", target.toFile());
            System.out.println("wrote " + target);
        }
    }

    private static BufferedImage render(Tile tile) {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setPaint(new GradientPaint(0, 0, tile.top(), 0, SIZE, tile.bottom()));
        g.fillRect(0, 0, SIZE, SIZE);

        // Stylised produce shape so the tiles are distinguishable at catalog thumbnail size.
        g.setColor(new Color(255, 255, 255, 46));
        g.fillOval(120, 90, 360, 300);

        // Card panel keeps the label readable regardless of the gradient.
        g.setColor(new Color(245, 245, 245));
        g.fillRoundRect(60, SIZE - 190, SIZE - 120, 130, 18, 18);

        g.setColor(new Color(30, 30, 30));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 54));
        g.drawString(tile.sku(), 96, SIZE - 128);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 34));
        g.drawString(tile.label(), 96, SIZE - 82);
        g.dispose();
        return image;
    }

    private static Tile tile(String sku, String label, int top, int bottom) {
        return new Tile(sku, label, new Color(top), new Color(bottom));
    }
}
