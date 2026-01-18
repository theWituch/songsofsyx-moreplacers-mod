package view.tool;

import init.sprite.SPRITES;
import init.sprite.UI.Icon;
import snake2d.util.map.MAP_SETTER;
import snake2d.util.sets.ArrayList;
import snake2d.util.sets.LIST;
import util.text.D;
import view.keyboard.KEYS;

public abstract class PLACER_TYPE {

    static {
        D.gInit(PLACER_TYPE.class);
    }

    public final static PLACER_TYPE SQUARE = new PLACER_TYPE(true, false, D.g("rectangle")) {
        @Override
        Icon icon() {
            return SPRITES.icons().m.place_rec;
        }

        @Override
        void paint(int x1, int y1, int x2, int y2, int size, MAP_SETTER area) {
            paintRectangle(x1, y1, x2, y2, area);
        }

    };

    static void paintRectangle(int x1, int y1, int x2, int y2, MAP_SETTER area) {
        int x11 = Math.min(x1, x2);
        int x22 = Math.max(x1, x2);
        int y11 = Math.min(y1, y2);
        int y22 = Math.max(y1, y2);
        for (int y = y11; y <= y22; y++)
            for (int x = x11; x <= x22; x++)
                area.set(x, y);
    }

    public final static PLACER_TYPE SQUARE_HOLLOW = new PLACER_TYPE(true, true, D.g("hollow rectangle")) {
        @Override
        Icon icon() {
            return SPRITES.icons().m.place_rec_hollow;
        }

        @Override
        void paint(int x1, int y1, int x2, int y2, int size, MAP_SETTER area) {
            int x11 = Math.min(x1, x2);
            int x22 = Math.max(x1, x2);
            int y11 = Math.min(y1, y2);
            int y22 = Math.max(y1, y2);

            while (size >= 0 && x11 <= x22 && y11 <= y22) {
                outline(x11, y11, x22, y22, area);
                x11++;
                x22--;
                y11++;
                y22--;
                size--;
            }

        }

        void outline(int x1, int y1, int x2, int y2, MAP_SETTER area) {
            for (int y = y1; y <= y2; y++) {
                if (y == y1 || y == y2) {
                    for (int x = x1; x <= x2; x++) {
                        area.set(x, y);
                    }
                } else {
                    area.set(x1, y);
                    area.set(x2, y);
                }
            }
        }
    };

    public final static PLACER_TYPE FREE_SQUARE = new PLACER_TYPE(true, false, D.g("free rectangle")) {

        private int x3, y3 = 0;

        @Override
        Icon icon() {
            return SPRITES.icons().m.place_free_rect;
        }

        @Override
        void paint(int x1, int y1, int x2, int y2, int size, MAP_SETTER area) {
            if (KEYS.MAIN().MOD.isPressed() && x1 == x2 && y1 == y2) {
                setupStartPoint(x1, y1, area);
                return;
            }

            paint3PointRectangle(x3, y3, x2, y2, x1, y1, area);
        }

        private void setupStartPoint(int x1, int y1, MAP_SETTER area) {
            area.set(x1, y1);
            x3 = x1;
            y3 = y1;
        }

        private void paint3PointRectangle(int x1, int y1, int x2, int y2, int x3, int y3, MAP_SETTER area) {
            if (x1 == x2 && y1 == y2) {
                area.set(x1, y1);
                return;
            }

            double vx = x2 - x1;
            double vy = y2 - y1;
            double lenAB = Math.sqrt(vx * vx + vy * vy);
            double nx = -vy;
            double ny = vx;
            double lenN = Math.sqrt(nx * nx + ny * ny);
            nx /= lenN;
            ny /= lenN;

            double cx = x3 - x1;
            double cy = y3 - y1;
            double cross = cx * vy - cy * vx;
            double signedDistance = cross / lenAB;

            if (signedDistance > 0) {
                nx = -nx;
                ny = -ny;
            }

            signedDistance = Math.abs(signedDistance);

            if (Math.abs(signedDistance) < 0.5) {
                drawLineBresenham(x1, y1, x2, y2, area);
                return;
            }

            double d4x = x2 + nx * signedDistance;
            double d4y = y2 + ny * signedDistance;
            double c4x = x1 + nx * signedDistance;
            double c4y = y1 + ny * signedDistance;

            int minX = (int) Math.floor(Math.min(Math.min(x1, x2), Math.min(c4x, d4x)));
            int maxX = (int) Math.ceil(Math.max(Math.max(x1, x2), Math.max(c4x, d4x)));
            int minY = (int) Math.floor(Math.min(Math.min(y1, y2), Math.min(c4y, d4y)));
            int maxY = (int) Math.ceil(Math.max(Math.max(y1, y2), Math.max(c4y, d4y)));

            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    if (isPointInRectangle(x, y, x1, y1, x2, y2, c4x, c4y, d4x, d4y)) {
                        area.set(x, y);
                    }
                }
            }
        }

        private boolean isPointInRectangle(double px, double py,
                                           double ax, double ay,
                                           double bx, double by,
                                           double cx, double cy,
                                           double dx, double dy) {
            double cross1 = crossProduct(bx - ax, by - ay, px - ax, py - ay);
            double cross2 = crossProduct(dx - bx, dy - by, px - bx, py - by);
            double cross3 = crossProduct(cx - dx, cy - dy, px - dx, py - dy);
            double cross4 = crossProduct(ax - cx, ay - cy, px - cx, py - cy);

            boolean allNonNegative = cross1 >= -1e-9 && cross2 >= -1e-9 && cross3 >= -1e-9 && cross4 >= -1e-9;
            boolean allNonPositive = cross1 <= 1e-9 && cross2 <= 1e-9 && cross3 <= 1e-9 && cross4 <= 1e-9;

            return allNonNegative || allNonPositive;
        }

        private double crossProduct(double v1x, double v1y, double v2x, double v2y) {
            return v1x * v2y - v1y * v2x;
        }

        private void drawLineBresenham(int x0, int y0, int x1, int y1, MAP_SETTER area) {
            int dx = Math.abs(x1 - x0);
            int dy = Math.abs(y1 - y0);
            int sx = x0 < x1 ? 1 : -1;
            int sy = y0 < y1 ? 1 : -1;
            int err = dx - dy;

            int x = x0;
            int y = y0;

            while (true) {
                area.set(x, y);

                if (x == x1 && y == y1) {
                    break;
                }

                int e2 = 2 * err;

                if (e2 > -dy) {
                    err -= dy;
                    x += sx;
                }

                if (e2 < dx) {
                    err += dx;
                    y += sy;
                }
            }
        }

    };

    public final static PLACER_TYPE BRUSH = new PLACER_TYPE(false, true, D.g("brush")) {
        @Override
        Icon icon() {
            return SPRITES.icons().m.place_brush;
        }

        @Override
        void paint(int x1, int y1, int x2, int y2, int size, MAP_SETTER area) {

            size += 1;
            int min = size / 2;

            double s = size / 2.0;
            double s2 = s * s;
            double d = (size & 1) == 0 ? 0.5 : 0;

            for (int dy = -min; dy <= min; dy++) {
                for (int dx = -min; dx <= min; dx++) {
                    double dist = (dx + d) * (dx + d) + (dy + d) * (dy + d);
                    if (dist <= s2) {
                        area.set(x1 + dx, y1 + dy);
                    }
                }
            }
        }
    };

    public final static PLACER_TYPE LINE = new PLACER_TYPE(true, true, D.g("line")) {
        @Override
        Icon icon() {
            return SPRITES.icons().m.place_line;
        }

        @Override
        void paint(int x1, int y1, int x2, int y2, int size, MAP_SETTER area) {

            if (x1 == x2 && y1 == y2) {
                area.set(x1, y1);
                return;
            }

            if (KEYS.MAIN().MOD.isPressed()) {
                paintLShape(x1, y1, x2, y2, size/2, area);
            } else {
                paintSlantLine(x1, y1, x2, y2, size, area);
            }
        }

        private void paintLShape(int x1, int y1, int x2, int y2, int size, MAP_SETTER area) {
            int dx = Math.abs(x2 - x1);
            int dy = Math.abs(y2 - y1);

            final boolean dominantX;
            int midX, midY;
            if (dx > dy) {
                dominantX = true;
                midX = x1;
                midY = y2;
            } else {
                dominantX = false;
                midX = x2;
                midY = y1;
            }

            Integer offset = null;
            for (int i = 0; i <= size; i++) {
                offset = offset == null ? 0 : (offset >= 0 ? ++offset * -1  : offset * -1);

                if (dominantX) {
                    if (y1 > midY) {
                        if (x1 > x2) {
                            paintRectangle(x2, y2 - offset, midX + size/2, midY - offset, area); // X axis
                            paintRectangle(x1 + offset, y1, midX + offset, midY + size/2, area); // Y axis
                        } else {
                            paintRectangle(x2, y2 - offset, midX - size/2, midY - offset, area); // X axis
                            paintRectangle(x1 - offset, y1, midX - offset, midY + size/2, area); // Y axis
                        }
                    } else {
                        if (x1 > x2) {
                            paintRectangle(x2, y2 + offset, midX + size/2, midY + offset, area); // X axis
                            paintRectangle(x1 + offset, y1, midX + offset, midY + size/2, area); // Y axis
                        } else {
                            paintRectangle(x2, y2 + offset, midX - size/2, midY + offset, area); // X axis
                            paintRectangle(x1 - offset, y1, midX - offset, midY + size/2, area); // Y axis
                        }
                    }
                } else {
                    if (x1 > midX) {
                        if (y1 > y2) { // top-to-bottom
                            paintRectangle(x2 - offset, y2, midX - offset, midY + size/2, area); // Y axis
                            paintRectangle(x1, y1 + offset, midX - size/2, midY + offset, area); // X axis
                        } else { // bottom-to-top
                            paintRectangle(x2 - offset, y2, midX - offset, midY - size/2, area); // Y axis
                            paintRectangle(x1, y1 - offset, midX - size/2, midY - offset, area); // X axis
                        }
                    } else {
                        if (y1 > y2) { // top-to-bottom
                            paintRectangle(x2 + offset, y2, midX + offset, midY + size/2, area); // Y axis
                            paintRectangle(x1, y1 + offset, midX - size/2, midY + offset, area); // X axis
                        } else { // bottom-to-top
                            paintRectangle(x2 + offset, y2, midX + offset, midY - size/2, area); // Y axis
                            paintRectangle(x1, y1 - offset, midX - size/2, midY - offset, area); // X axis
                        }
                    }
                }
            }
        }

        private void paintSlantLine(int x1, int y1, int x2, int y2, int size, MAP_SETTER area) {
            int dx = x2 - x1;
            int dy = y2 - y1;
            dx = dx < 0 ? -1 : (dx > 0 ? 1 : 0);
            dy = dy < 0 ? -1 : (dy > 0 ? 1 : 0);
            boolean startX = dy * dx >= 0;

            int newX = -dy;
            int newY = dx;
            dx = newX;
            dy = newY;

            int offX1 = 0;
            int offY1 = 0;

            {
                for (int i = 1; i <= size / 2; i+=2) {
                    if ((i & 1) == 1) {
                        x1 -= dx;
                        x2-= dx;
                        y1 -= dy;
                        y2 -= dy;
                    }

                }
            }

            for (int i = 0; i <= size; i++) {

                drawLine(x1 + offX1, y1 + offY1, x2 + offX1, y2 + offY1, area, (i & 1) == 0);
                if (startX) {
                    offX1 += dx;
                } else {
                    offY1 += dy;
                }
                startX = !startX;
            }

        }

        private void drawLine(int x1, int y1, int x2, int y2, MAP_SETTER area, boolean first) {
            int dx = x2 - x1;
            int dy = y2 - y1;
            dx = dx < 0 ? -1 : (dx > 0 ? 1 : 0);
            dy = dy < 0 ? -1 : (dy > 0 ? 1 : 0);
            // first |= dx < 0 || dy < 0;
            if (first)
                area.set(x1, y1);
            else {
                // x2-=dx;
                // y2-=dy;
            }
            while (x1 != x2 || y1 != y2) {
                if (x1 != x2) {
                    x1 += dx;
                }

                if (y1 != y2) {
                    y1 += dy;
                }
                area.set(x1, y1);
            }

        }

    };

    public final static PLACER_TYPE FREE_LINE = new PLACER_TYPE(true, true, D.g("free line")) {
        @Override
        Icon icon() {
            return SPRITES.icons().m.place_free_line;
        }

        @Override
        void paint(int x1, int y1, int x2, int y2, int size, MAP_SETTER area) {

            if (x1 == x2 && y1 == y2) {
                area.set(x1, y1);
                return;
            }

            int dx = x2 - x1;
            int dy = y2 - y1;
            dx = Integer.compare(dx, 0);
            dy = Integer.compare(dy, 0);
            boolean startX = dy * dx >= 0;

            int newX = -dy;
            int newY = dx;
            dx = newX;
            dy = newY;

            int offX1 = 0;
            int offY1 = 0;

            {
                for (int i = 1; i <= size / 2; i+=2) {
                    if ((i & 1) == 1) {
                        x1 -= dx;
                        x2-= dx;
                        y1 -= dy;
                        y2 -= dy;
                    }

                }
            }

            for (int i = 0; i <= size; i++) {

                drawLine(x1 + offX1, y1 + offY1, x2 + offX1, y2 + offY1, area, (i & 1) == 0);
                if (startX) {
                    offX1 += dx;
                } else {
                    offY1 += dy;
                }
                startX = !startX;
            }

        }

        private void drawLine(int x1, int y1, int x2, int y2, MAP_SETTER area, boolean first) {
            int dx = Math.abs(x2 - x1);
            int dy = Math.abs(y2 - y1);
            int sx = x1 < x2 ? 1 : -1;
            int sy = y1 < y2 ? 1 : -1;
            int err = dx - dy;

            int x = x1;
            int y = y1;

            while (true) {
                area.set(x, y);

                if (x == x2 && y == y2) {
                    break;
                }

                int e2 = 2 * err;

                if (e2 > -dy) {
                    err -= dy;
                    x += sx;
                }

                if (e2 < dx) {
                    err += dx;
                    y += sy;
                }
            }
        }

    };

    public final static PLACER_TYPE FILL = new PLACER_TYPE(false, true, D.g("fill")) {
        @Override
        Icon icon() {
            return SPRITES.icons().m.place_fill;
        }

        @Override
        void paint(int x1, int y1, int x2, int y2, int size, MAP_SETTER area) {}

    };

    public final static PLACER_TYPE OVAL = new PLACER_TYPE(true, false, D.g("ellipse")) {
        @Override
        Icon icon() {
            return SPRITES.icons().m.place_ellispse;
        }

        @Override
        void paint(int x1, int y1, int x2, int y2, int size, MAP_SETTER area) {
            paintEllipse(x1, y1, x2, y2, size, false, area);
        }
    };

    public final static PLACER_TYPE OVAL_HOLLOW = new PLACER_TYPE(true, true, D.g("hollow ellipse")) {
        @Override
        Icon icon() {
            return SPRITES.icons().m.place_ellispse_hollow;
        }

        @Override
        void paint(int x1, int y1, int x2, int y2, int size, MAP_SETTER area) {
            paintEllipse(x1, y1, x2, y2, size, true, area);
        }
    };

    static void paintEllipse(int x1, int y1, int x2, int y2, int size, boolean hollow, MAP_SETTER area) {
        if (x1 == x2 && y1 == y2) {
            area.set(x1, y1);
        }

        int x11 = Math.min(x1, x2);
        int x22 = Math.max(x1, x2);
        int y11 = Math.min(y1, y2);
        int y22 = Math.max(y1, y2);

        double width = x22 - x11;
        double height = y22 - y11;


        if (KEYS.MAIN().MOD.isPressed()) {
            width = Math.max(width, height);
            height = Math.max(width, height);
        }

        double divisor = Math.max(width, height);
        double r2 = Math.max(width, height);

        r2 *= r2;
        for (double y = -height; y <= height; y++) {
            for (double x = -width; x <= width; x++) {
                double distX = x * (divisor / width);
                double distY = y * (divisor / height);
                double r = distX * distX + distY * distY;
                if (hollow ? (Math.abs(Math.sqrt(r) - Math.sqrt(r2)) < (1+size) && (r <= r2)): (r <= r2) ) {
                    area.set((int) (x11 + x), (int) (y11+y));
                }
            }
        }
    }

    public final static PLACER_TYPE HEXAGON_HOLLOW = new PLACER_TYPE(true, true, D.g("hollow hexagon")) {
        @Override
        Icon icon() {
            return SPRITES.icons().m.place_hex_hollow;
        }

        @Override
        void paint(int x1, int y1, int x2, int y2, int size, MAP_SETTER area) {
            paintHexagon(x1, y1, x2, y2, size, true, area);
        }
    };

    public final static PLACER_TYPE HEXAGON = new PLACER_TYPE(true, true, D.g("hexagon")) {
        @Override
        Icon icon() {
            return SPRITES.icons().m.place_hex;
        }

        @Override
        void paint(int x1, int y1, int x2, int y2, int size, MAP_SETTER area) {
            paintHexagon(x1, y1, x2, y2, size, false, area);
        }
    };

    static void paintHexagon(int x1, int y1, int x2, int y2, int size, boolean hollow, MAP_SETTER area) {
        int centerX = (x1 + x2) / 2;
        int centerY = (y1 + y2) / 2;
        int radius = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1)) / 2;

        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                double dx = Math.abs(x);
                double dy = Math.abs(y);
                double r = (dx + dy / 2);
                if (hollow ? (r > (radius - 1 - size) || dy > (radius -1 - size)) && r <= radius : (dx + dy / 2) <= radius) {
                    area.set(centerX + x, centerY + y);
                }
            }
        }

    }

    public final static PLACER_TYPE ARC_3_POINT = new PLACER_TYPE(true, true, D.g("3 point arc")) {

        private int x3, y3 = 0;

        @Override
        Icon icon() {
            return SPRITES.icons().m.place_arc;
        }

        @Override
        void paint(int x1, int y1, int x2, int y2, int size, MAP_SETTER area) {
            if (KEYS.MAIN().MOD.isPressed() && x1 == x2 && y1 == y2) {
                setupStartPoint(x1, y1, area);
                return;
            }

            drawCircularArc(x3, y3, x1, y1, x2, y2, size, area);
        }

        private void setupStartPoint(int x1, int y1, MAP_SETTER area) {
            area.set(x1, y1);
            x3 = x1;
            y3 = y1;
        }

        private void drawCircularArc(int x0, int y0, int x1, int y1, int x2, int y2, int size, MAP_SETTER area) {
            CircleCenter center = findCircleCenter(x0, y0, x1, y1, x2, y2);

            if (center == null) {
                drawStraightLine(x0, y0, x2, y2, size, area);
                return;
            }

            double cx = center.x;
            double cy = center.y;
            double radius = center.radius;

            double angle0 = Math.atan2(y0 - cy, x0 - cx);
            double angle1 = Math.atan2(y1 - cy, x1 - cx);
            double angle2 = Math.atan2(y2 - cy, x2 - cx);

            double v1x = x1 - x0;
            double v1y = y1 - y0;
            double v2x = x2 - x1;
            double v2y = y2 - y1;
            double cross = v1x * v2y - v1y * v2x;

            double startAngle = angle0;
            double endAngle = angle2;

            if (cross > 0) {
                while (endAngle <= startAngle) {
                    endAngle += 2 * Math.PI;
                }

                double midAngleNorm = angle1;
                while (midAngleNorm <= startAngle) {
                    midAngleNorm += 2 * Math.PI;
                }

                if (midAngleNorm > endAngle) {
                    endAngle += 2 * Math.PI;
                }
            } else {
                while (endAngle >= startAngle) {
                    endAngle -= 2 * Math.PI;
                }

                double midAngleNorm = angle1;
                while (midAngleNorm >= startAngle) {
                    midAngleNorm -= 2 * Math.PI;
                }

                if (midAngleNorm < endAngle) {
                    endAngle -= 2 * Math.PI;
                }
            }

            double arcAngle = Math.abs(endAngle - startAngle);
            double arcLength = arcAngle * radius;
            int steps = Math.max(2, (int) Math.ceil(arcLength));

            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                double angle = startAngle + t * (endAngle - startAngle);

                int x = (int) Math.round(cx + radius * Math.cos(angle));
                int y = (int) Math.round(cy + radius * Math.sin(angle));

                paintThickPointArc(x, y, size, area);
            }
        }

        private CircleCenter findCircleCenter(int x0, int y0, int x1, int y1, int x2, int y2) {
            double ax = x1 - x0;
            double ay = y1 - y0;
            double bx = x2 - x0;
            double by = y2 - y0;

            double d = 2 * (ax * by - ay * bx);

            if (Math.abs(d) < 1e-6) {
                return null;
            }

            double aSq = ax * ax + ay * ay;
            double bSq = bx * bx + by * by;

            double cx = x0 + (by * aSq - ay * bSq) / d;
            double cy = y0 + (ax * bSq - bx * aSq) / d;

            double radius = Math.sqrt((cx - x0) * (cx - x0) + (cy - y0) * (cy - y0));

            double maxDist = Math.max(
                    Math.sqrt((x2 - x0) * (x2 - x0) + (y2 - y0) * (y2 - y0)),
                    Math.max(
                            Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0)),
                            Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
                    )
            );

            if (radius > maxDist * 100) {
                return null;
            }

            return new CircleCenter(cx, cy, radius);
        }

        private void drawStraightLine(int x0, int y0, int x2, int y2, int size, MAP_SETTER area) {
            int dx = Math.abs(x2 - x0);
            int dy = Math.abs(y2 - y0);
            int steps = Math.max(dx, dy);

            if (steps == 0) {
                paintThickPointArc(x0, y0, size, area);
                return;
            }

            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                int x = (int) Math.round(x0 + t * (x2 - x0));
                int y = (int) Math.round(y0 + t * (y2 - y0));
                paintThickPointArc(x, y, size, area);
            }
        }

        private void paintThickPointArc(int x, int y, int size, MAP_SETTER area) {
            int thickness = size + 1;
            int offset = size / 2;

            for (int dy = 0; dy < thickness; dy++) {
                for (int dx = 0; dx < thickness; dx++) {
                    area.set(x + dx - offset, y + dy - offset);
                }
            }
        }

        class CircleCenter {
            double x;
            double y;
            double radius;

            CircleCenter(double x, double y, double radius) {
                this.x = x;
                this.y = y;
                this.radius = radius;
            }
        }

    };

    public final static PLACER_TYPE BEZIER_CURVE = new PLACER_TYPE(true, true, D.g("bezier curve")) {

        private int x3, y3 = 0;

        @Override
        Icon icon() {
            return SPRITES.icons().m.place_bezier;
        }

        @Override
        void paint(int x1, int y1, int x2, int y2, int size, MAP_SETTER area) {
            if (KEYS.MAIN().MOD.isPressed() && x1 == x2 && y1 == y2) {
                setupStartPoint(x1, y1, area);
                return;
            }

            drawQuadraticBezier(x3, y3, x1, y1, x2, y2, size, area);
        }

        private void setupStartPoint(int x1, int y1, MAP_SETTER area) {
            area.set(x1, y1);
            x3 = x1;
            y3 = y1;
        }

        private void drawQuadraticBezier(int x0, int y0, int x1, int y1, int x2, int y2, int size, MAP_SETTER area) {
            double dist1 = Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0));
            double dist2 = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
            int steps = (int) Math.ceil((dist1 + dist2) * 2);

            if (steps < 2) steps = 2;

            int prevX = x0;
            int prevY = y0;

            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;

                double oneMinusT = 1.0 - t;
                double x = oneMinusT * oneMinusT * x0 + 2 * oneMinusT * t * x1 + t * t * x2;
                double y = oneMinusT * oneMinusT * y0 + 2 * oneMinusT * t * y1 + t * t * y2;

                int currentX = (int) Math.round(x);
                int currentY = (int) Math.round(y);

                paintThickPointArc(currentX, currentY, size, area);

                if (i > 0) {
                    interpolatePoints(prevX, prevY, currentX, currentY, size, area);
                }

                prevX = currentX;
                prevY = currentY;
            }
        }

        private void interpolatePoints(int x1, int y1, int x2, int y2, int size, MAP_SETTER area) {
            int dx = Math.abs(x2 - x1);
            int dy = Math.abs(y2 - y1);

            if (dx <= 1 && dy <= 1) {
                return;
            }

            int steps = Math.max(dx, dy);

            for (int i = 1; i < steps; i++) {
                double t = (double) i / steps;
                int x = (int) Math.round(x1 + t * (x2 - x1));
                int y = (int) Math.round(y1 + t * (y2 - y1));
                paintThickPointArc(x, y, size, area);
            }
        }

        private void paintThickPointArc(int x, int y, int size, MAP_SETTER area) {
            int thickness = size + 1;
            int offset = size / 2;

            for (int dy = 0; dy < thickness; dy++) {
                for (int dx = 0; dx < thickness; dx++) {
                    area.set(x + dx - offset, y + dy - offset);
                }
            }
        }
    };


    public static final LIST<PLACER_TYPE> all = new ArrayList<>(SQUARE, SQUARE_HOLLOW, FREE_SQUARE, BRUSH, LINE, FREE_LINE, FILL, OVAL, OVAL_HOLLOW, HEXAGON, HEXAGON_HOLLOW, ARC_3_POINT, BEZIER_CURVE);


    final boolean drag;
    final boolean usesSize;
    final CharSequence name;

    PLACER_TYPE(boolean drag, boolean usesSize, CharSequence name) {
        this.drag = drag;
        this.name = name;
        this.usesSize = usesSize;
    }

    abstract void paint(int x1, int y1, int x2, int y2, int size, MAP_SETTER area);

    abstract Icon icon();

}