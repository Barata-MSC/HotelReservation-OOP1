package com.mycompany.hotelreservationsystem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;

/**
 * The login screen as a reusable object.
 * Create it with new LoginFrame(), read the input through the getters,
 * and react to the button with setOnLogin(...).
 */
public class LoginFrame extends JFrame {

    private static final Color YELLOW = new Color(0xFFCC00);
    private static final Color DARK_BAR = new Color(0x1E1B26);
    private static final Font LABEL_FONT = new Font(Font.SERIF, Font.BOLD, 20);

    private final RoundedTextField usernameField = new RoundedTextField();
    private final RoundedPasswordField passwordField = new RoundedPasswordField();
    private final JButton loginButton = new JButton("LOG IN");

    public LoginFrame() {
        super("Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1020, 820);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(createBar(), BorderLayout.NORTH);
        add(createFormPanel(), BorderLayout.CENTER);
        add(createBar(), BorderLayout.SOUTH);

        getRootPane().setDefaultButton(loginButton); // Enter key = log in
    }

    // ---------- public API ----------

    public String getUsername() {
        return usernameField.getText();
    }

    public char[] getPassword() {
        return passwordField.getPassword();
    }

    public void setOnLogin(ActionListener listener) {
        for (ActionListener l : loginButton.getActionListeners()) {
            loginButton.removeActionListener(l);
        }
        loginButton.addActionListener(listener);
    }

    public void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
    }

    // ---------- UI building ----------

    private JPanel createBar() {
        JPanel bar = new JPanel();
        bar.setBackground(DARK_BAR);
        bar.setPreferredSize(new Dimension(0, 35));
        return bar;
    }

    private JPanel createFormPanel() {
        JPanel background = new JPanel(new GridBagLayout());
        background.setBackground(YELLOW);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        AvatarIcon avatar = new AvatarIcon();
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);

        form.add(avatar);
        form.add(Box.createVerticalStrut(50));
        form.add(createLabel("Username:"));
        form.add(Box.createVerticalStrut(6));
        form.add(usernameField);
        form.add(Box.createVerticalStrut(30));
        form.add(createLabel("Password:"));
        form.add(Box.createVerticalStrut(6));
        form.add(passwordField);
        form.add(Box.createVerticalStrut(26));
        styleButton();
        form.add(loginButton);

        background.add(form);
        return background;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(LABEL_FONT);
        label.setForeground(Color.BLACK);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void styleButton() {
        loginButton.setFont(new Font(Font.SERIF, Font.PLAIN, 30));
        loginButton.setForeground(YELLOW);
        loginButton.setBackground(Color.BLACK);
        loginButton.setFocusPainted(false);
        loginButton.setBorder(new EmptyBorder(4, 40, 4, 40));
        loginButton.setPreferredSize(new Dimension(195, 50));
        loginButton.setMaximumSize(new Dimension(195, 50));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // ---------- custom components ----------

    /** Black ellipse with a white "person" silhouette. */
    private static class AvatarIcon extends JComponent {
        private static final int W = 234, H = 192;

        AvatarIcon() {
            Dimension size = new Dimension(W, H);
            setPreferredSize(size);
            setMaximumSize(size);
            setMinimumSize(size);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Ellipse2D outer = new Ellipse2D.Double(0, 0, W - 1, H - 1);
            g2.setColor(Color.BLACK);
            g2.fill(outer);

            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Double(W / 2.0 - 42, 24, 84, 84)); // head

            // shoulders, clipped to the outer ellipse
            Area body = new Area(new Ellipse2D.Double(W / 2.0 - 75, 120, 150, 150));
            body.intersect(new Area(outer));
            g2.fill(body);
            g2.dispose();
        }
    }

    /** White rounded text field. */
    private static class RoundedTextField extends JTextField {
        RoundedTextField() {
            setup(this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            paintBackground(g, this);
            super.paintComponent(g);
        }
    }

    /** White rounded password field. */
    private static class RoundedPasswordField extends JPasswordField {
        RoundedPasswordField() {
            setup(this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            paintBackground(g, this);
            super.paintComponent(g);
        }
    }

    private static void setup(JTextField field) {
        field.setOpaque(false);
        field.setBorder(new EmptyBorder(6, 14, 6, 14));
        field.setFont(new Font(Font.SERIF, Font.PLAIN, 20));
        Dimension size = new Dimension(284, 57);
        field.setPreferredSize(size);
        field.setMaximumSize(size);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private static void paintBackground(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 22, 22);
        g2.dispose();
    }
}
