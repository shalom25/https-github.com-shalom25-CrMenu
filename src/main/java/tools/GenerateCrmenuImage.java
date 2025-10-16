package tools;

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
        boolean configMode = args != null && args.length > 0 && "config".equalsIgnoreCase(args[0]);
        boolean permissionsMode = args != null && args.length > 0 && "permissions".equalsIgnoreCase(args[0]);
        boolean smallMode = configMode || permissionsMode;
        int width = smallMode ? 800 : 1200;
        int height = smallMode ? 400 : 628;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // Background: deep, modern gradient
        GradientPaint bg = new GradientPaint(0, 0, new Color(0x0f, 0x17, 0x2a), width, height, new Color(0x11, 0x18, 0x27));
        g.setPaint(bg);
        g.fillRect(0, 0, width, height);

        // Subtle vignette for depth
        Composite prev = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        g.setColor(new Color(0, 0, 0));
        g.fillOval(-200, -200, 800, 800);
        g.fillOval(width - 600, height - 600, 1000, 1000);
        g.setComposite(prev);

        // Accent bar
        g.setColor(new Color(0x00, 0xd1, 0xb2));
        g.fillRect(0, 0, width, 8);

        // Header
        g.setFont(loadFont("Segoe UI", Font.BOLD, smallMode ? 40 : 52));
        String header = configMode ? "CrMenu — Config" : permissionsMode ? "CrMenu — Permissions & Commands" : "CrMenu — Server Menu Plugin";
        int headerY = smallMode ? 92 : 112;
        drawTextWithShadow(g, header, 60, headerY, Color.WHITE);

        g.setColor(new Color(0x9c, 0xa3, 0xaf));
        g.setFont(loadFont("Segoe UI", Font.PLAIN, smallMode ? 20 : 24));
        if (configMode) {
            g.drawString("Config", 60, headerY + 36);
        } else if (permissionsMode) {
            g.drawString("Permissions and Commands", 60, headerY + 36);
        } else {
            g.drawString("Configurable menus for your Minecraft server", 60, 160);
        }

        if (!smallMode) {
            // Left content card (features)
            int cardX = 48, cardY = 200, cardW = 720, cardH = 340;
            drawShadowCard(g, cardX, cardY, cardW, cardH, 22);

            g.setColor(new Color(0x00, 0xd1, 0xb2));
            g.setFont(loadFont("Segoe UI", Font.BOLD, 26));
            g.drawString("Key Features", cardX + 24, cardY + 40);

            g.setColor(new Color(0xe5, 0xe7, 0xeb));
            g.setFont(loadFont("Segoe UI", Font.PLAIN, 23));
            int y = cardY + 80;
            int step = 36;
            g.drawString("• Multiple menus with per-item actions", cardX + 24, y); y += step;
            g.drawString("• Title, border, and back-button customization", cardX + 24, y); y += step;
            g.drawString("• PlaceholderAPI support across titles, names, lore, commands", cardX + 24, y); y += step;
            g.drawString("• Language files (English/Spanish) and color codes (&)", cardX + 24, y); y += step;
            g.drawString("• Per-menu and per-item permissions", cardX + 24, y); y += step;
            g.drawString("• Legacy config support and resource seeding", cardX + 24, y);
        } else {
            // Small mode card (config or permissions)
            int cX = 48, cY = 140, cW = width - 96, cH = 220;
            drawShadowCard(g, cX, cY, cW, cH, 20);
            g.setColor(new Color(0x00, 0xd1, 0xb2));
            g.setFont(loadFont("Segoe UI", Font.BOLD, 24));
            String cardTitle = configMode ? "Config" : "Permissions & Commands";
            g.drawString(cardTitle, cX + 20, cY + 36);

            g.setColor(new Color(0xe5, 0xe7, 0xeb));
            g.setFont(loadFont("Segoe UI", Font.PLAIN, 20));
            int y = cY + 70;
            int step = 30;
            if (configMode) {
                g.drawString("• config.yml — Global settings", cX + 20, y); y += step;
                g.drawString("• menus/*.yml — Menu definitions", cX + 20, y); y += step;
                g.drawString("• back_button, border — UI options", cX + 20, y); y += step;
                g.drawString("• PlaceholderAPI — dynamic values", cX + 20, y);
            } else {
                g.drawString("• Command: /menu (alias /crmenu)", cX + 20, y); y += step;
                g.drawString("• Admin: /crmenu reload | /crmenu diag", cX + 20, y); y += step;
                g.drawString("• Permission: crmenu.use (access menu)", cX + 20, y); y += step;
                g.drawString("• Permission: crmenu.admin (admin commands)", cX + 20, y);
            }
        }

        if (!smallMode) {
            // Right visual: stylized menu preview grid
            int gridX = 800, gridY = 220, gridW = 360, gridH = 280;
            drawShadowCard(g, gridX, gridY, gridW, gridH, 18);
            g.setColor(new Color(0x00, 0xd1, 0xb2));
            g.setFont(loadFont("Segoe UI", Font.BOLD, 22));
            g.drawString("Vista in-game", gridX + 20, gridY + 36);
            drawInventoryPreview(g, gridX + 20, gridY + 64, 9, 5, 32, 10, "Menu");
        }

        if (!smallMode) {
            // Footer info
            g.setColor(new Color(0x00, 0xd1, 0xb2));
            g.setFont(loadFont("Segoe UI", Font.BOLD, 26));
            g.drawString("Commands", 60, 520);
            g.setColor(new Color(0xe5, 0xe7, 0xeb));
            g.setFont(loadFont("Segoe UI", Font.PLAIN, 22));
            g.drawString("/menu (alias /crmenu), admin reload/diag", 60, 555);

            g.setColor(new Color(0x00, 0xd1, 0xb2));
            g.setFont(loadFont("Segoe UI", Font.BOLD, 26));
            g.drawString("Supported Versions", 750, 520);
            g.setColor(new Color(0xe5, 0xe7, 0xeb));
            g.setFont(loadFont("Segoe UI", Font.PLAIN, 22));
            g.drawString("Paper/Spigot 1.8–1.21.8", 750, 555);

            g.setColor(new Color(0x9c, 0xa3, 0xaf));
            g.setFont(loadFont("Segoe UI", Font.PLAIN, 20));
            g.drawString("CrMenu v1.0.0 — Simple, flexible server menus", 60, 600);
        }

    g.dispose();

        File outDir = new File("assets");
        if (!outDir.exists()) outDir.mkdirs();
        File outFile = new File(outDir, configMode ? "crmenu-config.jpg" : permissionsMode ? "crmenu-permissions.jpg" : "crmenu-features.jpg");
        saveJpeg(img, outFile, 0.92f);
        System.out.println("Generated: " + outFile.getAbsolutePath());
    }

    private static Font loadFont(String name, int style, int size) {
        try {
            Font f = new Font(name, style, size);
            return f;
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
    private static void drawShadowCard(Graphics2D g, int x, int y, int w, int h, int arc) {
        // Shadow
        Composite prev = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        g.setColor(new Color(0, 0, 0));
        g.fillRoundRect(x + 6, y + 8, w, h, arc, arc);
        g.setComposite(prev);
        // Card
        g.setColor(new Color(0x16, 0x1e, 0x2e));
        g.fillRoundRect(x, y, w, h, arc, arc);
        // Border
        g.setStroke(new BasicStroke(1.2f));
        g.setColor(new Color(0x22, 0x2b, 0x3a));
        g.drawRoundRect(x, y, w, h, arc, arc);
    }

    private static void drawTextWithShadow(Graphics2D g, String text, int x, int y, Color color) {
        Color shadow = new Color(0, 0, 0, 70);
        g.setColor(shadow);
        g.drawString(text, x + 2, y + 2);
        g.setColor(color);
        g.drawString(text, x, y);
    }


    private static void drawInventoryPreview(Graphics2D g, int x, int y, int cols, int rows, int cellSize, int gap, String title) {
        int w = cols * cellSize + (cols - 1) * gap;
        int h = rows * cellSize + (rows - 1) * gap;
        // Container background
        g.setColor(new Color(0x12, 0x19, 0x2a));
        g.fillRoundRect(x - 12, y - 44, w + 24, h + 68, 12, 12);
        g.setColor(new Color(0x22, 0x2b, 0x3a));
        g.drawRoundRect(x - 12, y - 44, w + 24, h + 68, 12, 12);

        // Title bar
        g.setColor(new Color(0x1a, 0x23, 0x35));
        g.fillRoundRect(x - 8, y - 36, w + 16, 28, 10, 10);
        g.setColor(new Color(0xe5, 0xe7, 0xeb));
        g.setFont(loadFont("Segoe UI", Font.BOLD, 16));
        drawTextWithShadow(g, title, x, y - 18, new Color(0xe5, 0xe7, 0xeb));

        // Slots grid
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int cx = x + c * (cellSize + gap);
                int cy = y + r * (cellSize + gap);
                Color fill = new Color(0x1f, 0x27, 0x3a);
                Color border = new Color(0x28, 0x33, 0x49);
                // Border panes on edges
                if (r == 0 || r == rows - 1 || c == 0 || c == cols - 1) {
                    fill = new Color(0x14, 0x18, 0x24);
                    border = new Color(0x1e, 0x26, 0x37);
                }
                g.setColor(fill);
                g.fillRoundRect(cx, cy, cellSize, cellSize, 8, 8);
                g.setColor(border);
                g.drawRoundRect(cx, cy, cellSize, cellSize, 8, 8);
            }
        }

        // Helper to place item with label
        java.util.function.BiConsumer<Point, ItemPreview> place = (pt, item) -> {
            int cx = x + pt.x * (cellSize + gap);
            int cy = y + pt.y * (cellSize + gap);
            // Item block
            g.setColor(item.fill);
            g.fillRoundRect(cx, cy, cellSize, cellSize, 8, 8);
            g.setColor(new Color(0x22, 0x2b, 0x3a));
            g.drawRoundRect(cx, cy, cellSize, cellSize, 8, 8);
            // Label
            g.setFont(loadFont("Segoe UI", Font.PLAIN, 12));
            FontMetrics fm = g.getFontMetrics();
            int tx = cx + (cellSize / 2) - (fm.stringWidth(item.label) / 2);
            int ty = cy + (cellSize / 2) + (fm.getAscent() / 2) - 2;
            drawTextWithShadow(g, item.label, tx, ty, item.text);
        };

        // Items approximating main.yml
        place.accept(new Point(1, 1), new ItemPreview("PvP", new Color(0x00, 0xd1, 0xb2), Color.WHITE)); // diamond_sword
        place.accept(new Point(4, 1), new ItemPreview("Lobby", new Color(0x3b, 0x82, 0xf6), Color.WHITE)); // compass
        place.accept(new Point(7, 1), new ItemPreview("Info", new Color(0xf5, 0x72, 0x2b), Color.WHITE)); // book
        place.accept(new Point(1, 3), new ItemPreview("Shop", new Color(0xf5, 0xc2, 0x3b), Color.BLACK)); // chest
        place.accept(new Point(4, 3), new ItemPreview("Admin", new Color(0xef, 0x44, 0x44), Color.WHITE)); // redstone
        place.accept(new Point(7, 3), new ItemPreview("Perfil", new Color(0xd9, 0x35, 0xC5), Color.WHITE)); // player head
        place.accept(new Point(4, 4), new ItemPreview("Atrás", new Color(0x22, 0x22, 0x22), new Color(0xff, 0x3b, 0x30))); // barrier
    }

    private static class ItemPreview {
        final String label;
        final Color fill;
        final Color text;
        ItemPreview(String label, Color fill, Color text) {
            this.label = label;
            this.fill = fill;
            this.text = text;
        }
    }
}