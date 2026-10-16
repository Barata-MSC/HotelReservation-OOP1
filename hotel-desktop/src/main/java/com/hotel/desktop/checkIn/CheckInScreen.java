package com.hotel.desktop.checkIn;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/** Placeholder workspace card until reservation-backed check-in is implemented. */
public final class CheckInScreen extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Color INK = new Color(20, 18, 26);
    private static final Color SUN = new Color(255, 204, 0);
    private static final Color CANVAS = new Color(255, 222, 62);

    public CheckInScreen() {
        super(new BorderLayout());
        setBackground(CANVAS);

        JPanel topStrip = new JPanel();
        topStrip.setBackground(SUN);
        topStrip.setPreferredSize(new java.awt.Dimension(1, 15));
        add(topStrip, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 24));
        content.setBackground(CANVAS);
        content.setBorder(new EmptyBorder(28, 38, 40, 38));

        JLabel title = new JLabel("Check in");
        title.setFont(new Font("Georgia", Font.BOLD, 36));
        title.setForeground(INK);
        content.add(title, BorderLayout.NORTH);

        JPanel notice = new JPanel(new BorderLayout());
        notice.setBackground(Color.WHITE);
        notice.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INK, 2, true),
                new EmptyBorder(20, 22, 20, 22)));
        JLabel message = new JLabel(
                "<html><b>Check-in is not available yet.</b><br>"
                        + "Reservation records and check-in processing have not been implemented.</html>");
        message.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        message.setForeground(INK);
        notice.add(message, BorderLayout.CENTER);
        content.add(notice, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);
    }
}
