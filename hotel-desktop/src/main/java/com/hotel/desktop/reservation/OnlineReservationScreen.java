package com.hotel.desktop.reservation;

import com.hotel.desktop.ui.StaffUi;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public final class OnlineReservationScreen extends JPanel {

    private static final long serialVersionUID = 1L;

    public OnlineReservationScreen() {
        JPanel body = StaffUi.createPage(this, "Online reservations",
                "Reserved for the final implementation");
        JPanel placeholder = StaffUi.card();
        JLabel message = new JLabel("For final implementation", SwingConstants.CENTER);
        message.setFont(new Font("Georgia", Font.BOLD, 24));
        message.setForeground(StaffUi.MUTED);
        placeholder.add(message, BorderLayout.CENTER);
        body.add(placeholder, BorderLayout.CENTER);
    }
}
