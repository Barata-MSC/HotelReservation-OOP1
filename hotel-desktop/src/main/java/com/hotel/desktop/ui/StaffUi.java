package com.hotel.desktop.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

public final class StaffUi {

    public static final Color INK = new Color(20, 18, 26);
    public static final Color SUN = new Color(255, 204, 0);
    public static final Color CANVAS = new Color(255, 222, 62);
    public static final Color MUTED = new Color(105, 91, 33);
    public static final Color LINE = new Color(236, 230, 196);

    private StaffUi() {
    }

    public static JPanel createPage(JPanel screen, String title, String subtitle) {
        screen.setLayout(new BorderLayout());
        screen.setBackground(CANVAS);
        screen.setPreferredSize(new Dimension(1024, 800));

        JPanel strip = new JPanel();
        strip.setBackground(SUN);
        strip.setPreferredSize(new Dimension(1, 15));
        screen.add(strip, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 20));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(14, 38, 40, 38));
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new javax.swing.BoxLayout(heading, javax.swing.BoxLayout.Y_AXIS));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 36));
        titleLabel.setForeground(INK);
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        subtitleLabel.setForeground(MUTED);
        heading.add(titleLabel);
        heading.add(javax.swing.Box.createVerticalStrut(3));
        heading.add(subtitleLabel);
        body.add(heading, BorderLayout.NORTH);
        screen.add(body, BorderLayout.CENTER);
        return body;
    }

    public static JPanel card() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INK, 2, true),
                BorderFactory.createEmptyBorder(18, 20, 16, 20)));
        return panel;
    }

    public static JButton primaryButton(String text) {
        return button(text, INK, SUN);
    }

    public static JButton secondaryButton(String text) {
        JButton button = button(text, Color.WHITE, INK);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INK, 1, true),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));
        return button;
    }

    public static JTextField textField(int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(INK);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(190, 185, 168), 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        return field;
    }

    public static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(INK);
        return label;
    }

    public static void styleTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setForeground(INK);
        table.setBackground(Color.WHITE);
        table.setRowHeight(44);
        table.setSelectionBackground(new Color(255, 243, 184));
        table.setSelectionForeground(INK);
        table.setGridColor(LINE);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setForeground(INK);
        table.getTableHeader().setBackground(new Color(250, 248, 238));
        table.getTableHeader().setPreferredSize(new Dimension(10, 40));
        table.getTableHeader().setReorderingAllowed(false);
    }

    public static JLabel feedbackLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(MUTED);
        label.setBorder(BorderFactory.createEmptyBorder(8, 2, 2, 2));
        label.setVerticalAlignment(SwingConstants.CENTER);
        return label;
    }

    public static void showFeedback(JLabel label, String message, boolean error) {
        label.setText(message);
        label.setForeground(error ? new Color(179, 38, 30) : MUTED);
    }

    public static <T> void runInBackground(
            Callable<T> operation,
            Consumer<T> onSuccess,
            Consumer<Throwable> onFailure) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return operation.call();
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    onFailure.accept(exception);
                } catch (ExecutionException | CancellationException exception) {
                    Throwable cause = exception instanceof ExecutionException
                            && exception.getCause() != null ? exception.getCause() : exception;
                    onFailure.accept(cause);
                }
            }
        }.execute();
    }

    private static JButton button(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(foreground);
        button.setBackground(background);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INK, 2, true),
                BorderFactory.createEmptyBorder(9, 18, 9, 18)));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }
}
