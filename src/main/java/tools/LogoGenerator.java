package tools;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class LogoGenerator {
    public static void main(String[] args) throws Exception {
        int width = 1024, height = 1024;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Fondo oscuro
            g.setPaint(new Color(13, 17, 23));
            g.fillRect(0, 0, width, height);

            // Decoración sutil con gradientes radiales
            RadialGradientPaint rg1 = new RadialGradientPaint(
                    new Point2D.Float(280, 240), 240f,
                    new float[]{0f, 1f},
                    new Color[]{new Color(255, 179, 0, 70), new Color(13, 17, 23, 0)});
            g.setPaint(rg1);
            g.fill(new Ellipse2D.Float(120, 100, 420, 420));

            RadialGradientPaint rg2 = new RadialGradientPaint(
                    new Point2D.Float(800, 780), 260f,
                    new float[]{0f, 1f},
                    new Color[]{new Color(255, 61, 0, 60), new Color(13, 17, 23, 0)});
            g.setPaint(rg2);
            g.fill(new Ellipse2D.Float(720, 680, 360, 360));

            // Texto principal
            String text = "CrMenu";
            Font font = new Font("Segoe UI", Font.BOLD, 180);
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics(font);
            int textWidth = fm.stringWidth(text);
            int x = (width - textWidth) / 2;
            int y = (height + fm.getAscent() - fm.getDescent()) / 2;

            // Sombra sutil
            g.setColor(new Color(0, 0, 0, 90));
            g.drawString(text, x + 3, y + 6);

            // Relleno con gradiente lineal
            LinearGradientPaint lg = new LinearGradientPaint(
                    new Point2D.Float(x, y - fm.getAscent()),
                    new Point2D.Float(x + textWidth, y - fm.getAscent()),
                    new float[]{0f, 0.5f, 1f},
                    new Color[]{new Color(255, 179, 0), new Color(255, 111, 0), new Color(255, 61, 0)});

            Shape textShape = font.createGlyphVector(g.getFontRenderContext(), text).getOutline(x, y);
            g.setPaint(lg);
            g.fill(textShape);

            // Trazo blanco muy tenue
            g.setStroke(new BasicStroke(2f));
            g.setColor(new Color(255, 255, 255, 35));
            g.draw(textShape);

            // Subtítulo
            String sub = "Configurable Server Menus";
            Font subFont = new Font("Segoe UI", Font.PLAIN, 36);
            g.setFont(subFont);
            FontMetrics sfm = g.getFontMetrics(subFont);
            int subX = (width - sfm.stringWidth(sub)) / 2;
            int subY = Math.min(y + 100, height - 80);
            g.setColor(new Color(201, 209, 217, 215));
            g.drawString(sub, subX, subY);
        } finally {
            g.dispose();
        }

        // Guardar como JPG
        File dir = new File("assets");
        if (!dir.exists()) dir.mkdirs();
        File out = new File(dir, "crmenu-logo-square.jpg");
        ImageIO.write(img, "jpg", out);
        System.out.println("Generated: " + out.getAbsolutePath());
    }
}