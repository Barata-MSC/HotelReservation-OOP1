package com.hotel.desktop.customer;

import com.hotel.desktop.ui.StaffUi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

final class CustomerUi {

    static final Color CANVAS = StaffUi.CANVAS;
    static final Color SURFACE = Color.WHITE;
    static final Color INK = StaffUi.INK;
    static final Color MUTED = StaffUi.MUTED;
    static final Color LINE = StaffUi.LINE;
    static final Color ACCENT = StaffUi.SUN;

    private CustomerUi() {
    }

    static javax.swing.JPanel createPage(javax.swing.JPanel screen, String title, String subtitle) {
        screen.setLayout(new BorderLayout());
        screen.setBackground(CANVAS);

        JPanel strip = new JPanel();
        strip.setBackground(ACCENT);
        strip.setPreferredSize(new Dimension(1, 15));
        screen.add(strip, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 20));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(14, 38, 24, 38));
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new javax.swing.BoxLayout(heading, javax.swing.BoxLayout.Y_AXIS));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(StaffUi.HEADING_FONT);
        titleLabel.setForeground(INK);
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(StaffUi.bodyFont(15));
        subtitleLabel.setForeground(MUTED);
        heading.add(titleLabel);
        heading.add(javax.swing.Box.createVerticalStrut(3));
        heading.add(subtitleLabel);
        body.add(heading, BorderLayout.NORTH);
        screen.add(body, BorderLayout.CENTER);
        return body;
    }

    static javax.swing.JPanel card() {
        return StaffUi.card();
    }

    static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setOpaque(true);
        scrollPane.setBackground(SURFACE);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(SURFACE);
        scrollPane.setBorder(BorderFactory.createLineBorder(LINE));
    }
}
