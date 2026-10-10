package com.mycompany.hotelreservationsystem;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class WelcomeScreen extends JFrame {

    private static final Color DARK_BAR   = new Color(0x1C1A27);
    private static final Color HEADER     = new Color(0xFFC02A);
    private static final Color FOOTER     = new Color(0xFFCB05);
    private static final Color START_BTN  = new Color(0xF9EE2B);
    private static final Color TEXT_COLOR = new Color(0x111111);

    public WelcomeScreen() {
        super("Hotel Reservation Management Program");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildTop(), BorderLayout.NORTH);
        root.add(buildCenter(), BorderLayout.CENTER);
        root.add(buildBottom(), BorderLayout.SOUTH);

        setContentPane(root);
        setSize(1020, 820);
        setMinimumSize(new Dimension(640, 520));
        setLocationRelativeTo(null);
    }

    /** Dark strip + yellow title bar. */
    private JPanel buildTop() {
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        top.add(solidBar(DARK_BAR, 35));

        JPanel header = new JPanel(new GridBagLayout());
        header.setBackground(HEADER);
        header.setPreferredSize(new Dimension(0, 60));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        JLabel title = new JLabel("Hotel Reservation Management Program");
        title.setFont(font(Font.BOLD, 22));
        title.setForeground(TEXT_COLOR);
        header.add(title);
        top.add(header);
        return top;
    }

    /** Yellow footer + dark strip. */
    private JPanel buildBottom() {
        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.add(solidBar(FOOTER, 50));
        bottom.add(solidBar(DARK_BAR, 35));
        return bottom;
    }

    /** Hotel photo background with welcome text and Start button. */
    private JPanel buildCenter() {
        BackgroundPanel center = new BackgroundPanel("/hotel.jpg");
        center.setLayout(new GridBagLayout());

        JLabel welcome = new JLabel("Welcome back to Hotel!");
        welcome.setFont(font(Font.BOLD, 24));
        welcome.setForeground(Color.BLACK);

        RoundedButton start = new RoundedButton("Start", START_BTN);
        start.setFont(font(Font.PLAIN, 22));
        start.setPreferredSize(new Dimension(282, 62));
        start.addActionListener(e -> onStart());

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.insets = new Insets(0, 0, 14, 0);
        center.add(welcome, gc);

        gc.gridy = 1;
        gc.insets = new Insets(0, 0, 0, 0);
        center.add(start, gc);
        return center;
    }

    /** TODO: open your next screen (login / dashboard) here. */
    private void onStart() {
        JOptionPane.showMessageDialog(this, "Start clicked - open the next screen here.");
    }

    private static JPanel solidBar(Color color, int height) {
        JPanel p = new JPanel();
        p.setBackground(color);
        p.setPreferredSize(new Dimension(0, height));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return p;
    }

    /** Uses Poppins if installed, otherwise a clean sans-serif fallback. */
    private static Font font(int style, int size) {
        String family = "SansSerif";
        for (String f : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (f.equalsIgnoreCase("Poppins")) {
                family = f;
                break;
            }
        }
        return new Font(family, style, size);
    }

    // ---------------------------------------------------------------- //

    /** Paints a "cover"-scaled image with a white wash on top (faded look). */
    private static class BackgroundPanel extends JPanel {
        private BufferedImage image;

        BackgroundPanel(String resourcePath) {
            try (InputStream in = WelcomeScreen.class.getResourceAsStream(resourcePath)) {
                if (in != null) image = ImageIO.read(in);
            } catch (IOException ignored) {
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            int w = getWidth(), h = getHeight();

            if (image != null) {
                double scale = Math.max((double) w / image.getWidth(), (double) h / image.getHeight());
                int dw = (int) (image.getWidth() * scale);
                int dh = (int) (image.getHeight() * scale);
                g2.drawImage(image, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
            } else {
                // Placeholder until src/main/resources/hotel.jpg is added
                g2.setPaint(new GradientPaint(0, 0, new Color(0xBFD8EE), 0, h, new Color(0x7EC8E8)));
                g2.fillRect(0, 0, w, h);
            }

            g2.setColor(new Color(255, 255, 255, 150)); // faded overlay
            g2.fillRect(0, 0, w, h);
            g2.dispose();
        }
    }

    /** Flat rounded button. */
    private static class RoundedButton extends JButton {
        private final Color base;

        RoundedButton(String text, Color base) {
            super(text);
            this.base = base;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setForeground(Color.BLACK);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = getModel().isPressed() ? base.darker()
                    : getModel().isRollover() ? base.brighter() : base;
            g2.setColor(c);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
            g2.dispose();
            super.paintComponent(g);
        }
    }

}
