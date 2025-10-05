import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class GenerateCrmenuImage {
    public static void main(String[] args) throws IOException {
        int width = 1200;
        int height = 628;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background gradient
        GradientPaint gp = new GradientPaint(0, 0, new Color(0x0f,0x17,0x2a), width, height, new Color(0x11,0x18,0x27));
        g.setPaint(gp);
        g.fillRect(0, 0, width, height);

        // Accent bar
        g.setColor(new Color(0x00, 0xd1, 0xb2));
        g.fillRect(0, 0, width, 8);

        // Title
        g.setColor(Color.WHITE);
        g.setFont(loadFont("Segoe UI", Font.BOLD, 48));
        g.drawString("CrMenu — Server Menu Plugin", 60, 110);

        // Subtitle
        g.setColor(new Color(0x9c, 0xa3, 0xaf));
        g.setFont(loadFont("Segoe UI", Font.PLAIN, 24));
        g.drawString("Modern, configurable menus for your Minecraft server", 60, 160);

        // Features heading
        g.setColor(new Color(0x00, 0xd1, 0xb2));
        g.setFont(loadFont("Segoe UI", Font.BOLD, 28));
        g.drawString("Key Features", 60, 220);

        // Features list
        g.setColor(new Color(0xe5, 0xe7, 0xeb));
        g.setFont(loadFont("Segoe UI", Font.PLAIN, 24));
        int y = 265;
        int step = 40;
        g.drawString("• Multiple menus with per-item actions", 60, y); y += step;
        g.drawString("• Title, border and back-button customization", 60, y); y += step;
        g.drawString("• PlaceholderAPI support (titles, names, lore, commands)", 60, y); y += step;
        g.drawString("• Language files (English/Spanish) and color codes (&)", 60, y); y += step;
        g.drawString("• Per-menu and per-item permissions", 60, y); y += step;
        g.drawString("• Legacy config support and automatic resource seeding", 60, y);

        // Commands
        g.setColor(new Color(0x00, 0xd1, 0xb2));
        g.setFont(loadFont("Segoe UI", Font.BOLD, 26));
        g.drawString("Commands", 60, 520);
        g.setColor(new Color(0xe5, 0xe7, 0xeb));
        g.setFont(loadFont("Segoe UI", Font.PLAIN, 22));
        g.drawString("/menu (alias /crmenu), admin reload/diag", 60, 555);

        // Requirements
        g.setColor(new Color(0x00, 0xd1, 0xb2));
        g.setFont(loadFont("Segoe UI", Font.BOLD, 26));
        g.drawString("Requirements", 750, 520);
        g.setColor(new Color(0xe5, 0xe7, 0xeb));
        g.setFont(loadFont("Segoe UI", Font.PLAIN, 22));
        g.drawString("Java 17+, Paper/Spigot 1.20+, PlaceholderAPI (optional)", 750, 555);

        // Footer
        g.setColor(new Color(0x9c, 0xa3, 0xaf));
        g.setFont(loadFont("Segoe UI", Font.PLAIN, 20));
        g.drawString("CrMenu v1.0.0 — Simple, flexible server menus", 60, 600);

        g.dispose();

        File outDir = new File("assets");
        if (!outDir.exists()) outDir.mkdirs();
        File outFile = new File(outDir, "crmenu-features.jpg");
        saveJpeg(img, outFile, 0.92f);
        System.out.println("Generated: " + outFile.getAbsolutePath());
    }

    private static Font loadFont(String name, int style, int size) {
        try {
            Font f = new Font(name, style, size);
            if (f.getFamily() != null) return f;
        } catch (Throwable ignored) {}
        return new Font(Font.SANS_SERIF, style, size);
    }

    private static void saveJpeg(BufferedImage img, File file, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writers available");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(file)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(img, null, null), param);
        } finally {
            writer.dispose();
        }
    }
}