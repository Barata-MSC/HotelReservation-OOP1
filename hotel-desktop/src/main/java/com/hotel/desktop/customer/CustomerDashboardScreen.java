package com.hotel.desktop.customer;

import com.hotel.desktop.ui.StaffUi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public final class CustomerDashboardScreen extends JPanel {

    private static final long serialVersionUID = 1L;

    public CustomerDashboardScreen(Runnable onBookRoom) {
        JPanel body = CustomerUi.createPage(this, "Welcome to Okada Hotel", "Your stay, your way");
        JPanel content = new DashboardContentPanel();
        content.setLayout(new BorderLayout(0, 14));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(0, 0, 12, 0));
        content.add(new HeroPanel(onBookRoom), BorderLayout.NORTH);

        JPanel lower = new JPanel(new BorderLayout(0, 12));
        lower.setOpaque(false);
        JLabel amenitiesHeading = new JLabel("Make the most of your stay");
        amenitiesHeading.setFont(StaffUi.headingFont(24));
        amenitiesHeading.setForeground(StaffUi.INK);
        lower.add(amenitiesHeading, BorderLayout.NORTH);

        JPanel amenities = new JPanel(new GridLayout(0, 3, 16, 16));
        amenities.setOpaque(false);
        amenities.add(amenity("pool.jpg", "Resort-style pools",
                "Relax and unwind beside the water."));
        amenities.add(amenity("gym.webp", "Fitness center",
                "Keep your routine going during your stay."));
        amenities.add(amenity("gaming.jpg", "Entertainment",
                "Enjoy games and activities for every guest."));
        amenities.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                int width = amenities.getWidth();
                int columns = width >= 900 ? 3 : width >= 580 ? 2 : 1;
                GridLayout layout = (GridLayout) amenities.getLayout();
                if (layout.getColumns() != columns) {
                    amenities.setLayout(new GridLayout(0, columns, 16, 16));
                    amenities.revalidate();
                }
            }
        });
        lower.add(amenities, BorderLayout.CENTER);
        content.add(lower, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        body.add(scroll, BorderLayout.CENTER);
    }

    private static JPanel amenity(String imageName, String title, String description) {
        JPanel card = StaffUi.card();
        BufferedImage image = loadImage(imageName);
        JLabel imageLabel = new JLabel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                int imageWidth = getWidth();
                int imageHeight = Math.min(145, Math.max(1, (int) (imageWidth * 0.56)));
                Graphics2D copy = (Graphics2D) graphics.create();
                copy.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                copy.drawImage(image, 0, 0, imageWidth, imageHeight, this);
                copy.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(260, 145);
            }
        };
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(imageLabel, BorderLayout.NORTH);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(StaffUi.headingFont(19));
        titleLabel.setForeground(StaffUi.INK);
        JLabel descriptionLabel = new JLabel("<html><div style='width:220px'>"
                + description + "</div></html>");
        descriptionLabel.setFont(StaffUi.bodyFont(13));
        descriptionLabel.setForeground(StaffUi.MUTED);
        text.add(titleLabel);
        text.add(javax.swing.Box.createVerticalStrut(5));
        text.add(descriptionLabel);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private static final class DashboardContentPanel extends JPanel
            implements javax.swing.Scrollable {

        private static final long serialVersionUID = 1L;

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(16, visibleRect.height - 16);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static BufferedImage loadImage(String name) {
        URL resource = CustomerDashboardScreen.class.getResource("/images/customer/" + name);
        if (resource == null) {
            throw new IllegalStateException("Customer dashboard image is missing: " + name);
        }
        try {
            BufferedImage image = ImageIO.read(resource);
            if (image == null) {
                throw new IOException("No image reader accepted the resource.");
            }
            return image;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load customer dashboard image: " + name, exception);
        }
    }

    private static final class HeroPanel extends JPanel {

        private static final long serialVersionUID = 1L;
        private final BufferedImage background = loadImage("okada.jpg");
        private final Runnable onBookRoom;

        private HeroPanel(Runnable onBookRoom) {
            this.onBookRoom = onBookRoom;
            setLayout(new BorderLayout(0, 12));
            setPreferredSize(new Dimension(900, 300));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(StaffUi.INK, 2, true),
                    BorderFactory.createEmptyBorder(25, 30, 24, 30)));
            setOpaque(false);

            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));
            JLabel headline = new JLabel("A brighter kind of getaway");
            headline.setFont(new Font("Georgia", Font.BOLD, 34));
            headline.setForeground(Color.WHITE);
            JLabel subhead = new JLabel("Discover comfort, entertainment and memorable moments.");
            subhead.setFont(StaffUi.bodyFont(16));
            subhead.setForeground(Color.WHITE);
            JButton bookButton = StaffUi.primaryButton("Book a room");
            bookButton.addActionListener(event -> this.onBookRoom.run());
            text.add(headline);
            text.add(javax.swing.Box.createVerticalStrut(8));
            text.add(subhead);
            text.add(javax.swing.Box.createVerticalStrut(18));
            text.add(bookButton);
            add(text, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics2D.drawImage(background, 0, 0, getWidth(), getHeight(), null);
            graphics2D.setColor(new Color(20, 18, 26, 130));
            graphics2D.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            graphics2D.dispose();
        }
    }
}
