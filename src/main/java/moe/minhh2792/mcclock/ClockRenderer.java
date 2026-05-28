package moe.minhh2792.mcclock;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@SuppressWarnings("deprecation")
public class ClockRenderer extends MapRenderer {

    private final BufferedImage scaledFace;
    private final int thickness;
    private final boolean smoothMode;
    private volatile ZoneId zoneId;
    private final long compensationNanos;

    private final byte handColor;
    private final byte secondColor;

    public ClockRenderer(BufferedImage scaledFace, int thickness, boolean smoothMode, ZoneId zoneId, long compensationMs) {
        super(false);
        this.scaledFace = scaledFace;
        this.thickness = Math.max(1, thickness);
        this.smoothMode = smoothMode;
        this.zoneId = zoneId;
        this.compensationNanos = compensationMs * 1_000_000L;
        this.handColor = MapPalette.matchColor(40, 40, 40);
        this.secondColor = MapPalette.matchColor(200, 30, 30);
    }

    @Override
    public void render(@NotNull MapView view, MapCanvas canvas, @NotNull Player player) {
        canvas.drawImage(0, 0, scaledFace);

        ZonedDateTime now = ZonedDateTime.now(zoneId).plusNanos(compensationNanos);
        int h = now.getHour(), m = now.getMinute(), s = now.getSecond();
        double subSec = smoothMode ? now.getNano() / 1_000_000_000.0 : 0.0;

        double secAngle  = (s + subSec) * 6.0;
        double minAngle  = m * 6.0 + (s + subSec) * 0.1;
        double hourAngle = (h % 12) * 30.0 + m * 0.5;

        drawHand(canvas, hourAngle, 28, handColor);
        drawHand(canvas, minAngle,  40, handColor);
        drawHand(canvas, secAngle,  46, secondColor);
    }

    private void drawHand(MapCanvas canvas, double angleDeg, int length, byte color) {
        double rad = Math.toRadians(angleDeg - 90);
        int x1 = 64 + (int) Math.round(length * Math.cos(rad));
        int y1 = 64 + (int) Math.round(length * Math.sin(rad));
        drawLine(canvas, 64, 64, x1, y1, color);
    }

    private void drawLine(MapCanvas canvas, int x0, int y0, int x1, int y1, byte color) {
        double dx = x1 - x0, dy = y1 - y0;
        double len = Math.sqrt(dx * dx + dy * dy);
        double half = (thickness - 1) / 2.0;

        int minX = Math.max(0, Math.min(x0, x1) - thickness);
        int maxX = Math.min(127, Math.max(x0, x1) + thickness);
        int minY = Math.max(0, Math.min(y0, y1) - thickness);
        int maxY = Math.min(127, Math.max(y0, y1) + thickness);

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                double dist;
                if (len == 0) {
                    double ex = x - x0, ey = y - y0;
                    dist = Math.sqrt(ex * ex + ey * ey);
                } else {
                    double t = ((x - x0) * dx + (y - y0) * dy) / (len * len);
                    if (t < 0) {
                        double ex = x - x0, ey = y - y0;
                        dist = Math.sqrt(ex * ex + ey * ey);
                    } else if (t > 1) {
                        double ex = x - x1, ey = y - y1;
                        dist = Math.sqrt(ex * ex + ey * ey);
                    } else {
                        dist = Math.abs(dy * x - dx * y + (double) x1 * y0 - (double) y1 * x0) / len;
                    }
                }
                if (dist <= half + 0.5) canvas.setPixel(x, y, color);
            }
        }
    }
}
