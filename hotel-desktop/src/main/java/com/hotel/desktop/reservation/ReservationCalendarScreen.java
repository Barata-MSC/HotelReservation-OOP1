package com.hotel.desktop.reservation;

import com.hotel.core.model.AdminReservationDetails;
import com.hotel.core.model.AuthenticatedUser;
import com.hotel.core.service.ReservationService;
import com.hotel.desktop.ui.StaffUi;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.EmptyBorder;

public final class ReservationCalendarScreen extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final ReservationService service;
    private final Supplier<AuthenticatedUser> currentAdmin;
    private final JPanel dayGrid = new JPanel(new GridLayout(0, 7, 5, 5));
    private final JLabel monthLabel = new JLabel();
    private final JLabel selectedDayLabel = new JLabel("Select a date");
    private final JLabel feedback = StaffUi.feedbackLabel();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[0][7],
            new String[]{"Reference", "Guest", "Room", "Time", "Type", "Reservation", "Payment"}) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable reservationTable = new JTable(tableModel);
    private YearMonth month = YearMonth.now();
    private List<AdminReservationDetails> reservations = List.of();
    private LocalDate selectedDate = LocalDate.now();

    public ReservationCalendarScreen(
            ReservationService service, Supplier<AuthenticatedUser> currentAdmin) {
        this.service = Objects.requireNonNull(service);
        this.currentAdmin = Objects.requireNonNull(currentAdmin);
        JPanel body = StaffUi.createPage(this, "Reservation calendar", "View arrivals, departures, and stays by date");
        body.add(buildCalendar(), BorderLayout.CENTER);
        StaffUi.styleTable(reservationTable);
        refreshMonth();
    }

    public void refreshMonth() {
        AuthenticatedUser admin = currentAdmin.get();
        if (admin == null) {
            return;
        }
        StaffUi.showFeedback(feedback, "Loading reservations for " + month.format(MONTH_FORMAT) + "…", false);
        new SwingWorker<List<AdminReservationDetails>, Void>() {
            @Override
            protected List<AdminReservationDetails> doInBackground() throws SQLException {
                return service.findAdminCalendarReservations(
                        admin, month.atDay(1), month.atEndOfMonth());
            }

            @Override
            protected void done() {
                try {
                    reservations = get();
                    renderCalendar();
                    showDate(selectedDate.getMonth() == month.getMonth()
                            && selectedDate.getYear() == month.getYear()
                                    ? selectedDate : month.atDay(1));
                    StaffUi.showFeedback(feedback,
                            reservations.size() + " reservation(s) in this month.", false);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Calendar loading was interrupted.");
                } catch (ExecutionException exception) {
                    showError(exception.getCause() == null
                            ? "Unable to load the reservation calendar."
                            : exception.getCause().getMessage());
                }
            }
        }.execute();
    }

    private JPanel buildCalendar() {
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        JPanel calendarCard = StaffUi.card();
        JPanel monthHeader = new JPanel(new BorderLayout(8, 0));
        monthHeader.setOpaque(false);
        JButton previous = StaffUi.secondaryButton("‹ Previous");
        JButton next = StaffUi.secondaryButton("Next ›");
        previous.addActionListener(event -> changeMonth(-1));
        next.addActionListener(event -> changeMonth(1));
        monthLabel.setFont(StaffUi.headingFont(20));
        monthLabel.setForeground(StaffUi.INK);
        monthLabel.setHorizontalAlignment(JLabel.CENTER);
        monthHeader.add(previous, BorderLayout.WEST);
        monthHeader.add(monthLabel, BorderLayout.CENTER);
        monthHeader.add(next, BorderLayout.EAST);
        calendarCard.add(monthHeader, BorderLayout.NORTH);

        JPanel weekdays = new JPanel(new GridLayout(1, 7, 5, 0));
        weekdays.setOpaque(false);
        for (String weekday : List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")) {
            JLabel label = new JLabel(weekday, JLabel.CENTER);
            label.setFont(StaffUi.bodyFont(12).deriveFont(java.awt.Font.BOLD));
            label.setForeground(StaffUi.MUTED);
            weekdays.add(label);
        }
        JPanel gridContainer = new JPanel(new BorderLayout(0, 4));
        gridContainer.setOpaque(false);
        gridContainer.add(weekdays, BorderLayout.NORTH);
        dayGrid.setOpaque(false);
        gridContainer.add(dayGrid, BorderLayout.CENTER);
        calendarCard.add(gridContainer, BorderLayout.CENTER);

        JPanel selectedPanel = StaffUi.card();
        selectedDayLabel.setFont(StaffUi.headingFont(18));
        selectedDayLabel.setForeground(StaffUi.INK);
        selectedPanel.add(selectedDayLabel, BorderLayout.NORTH);
        JScrollPane tableScroll = new JScrollPane(reservationTable);
        tableScroll.getViewport().setBackground(java.awt.Color.WHITE);
        tableScroll.setPreferredSize(new java.awt.Dimension(850, 190));
        selectedPanel.add(tableScroll, BorderLayout.CENTER);

        JPanel stack = new JPanel(new BorderLayout(0, 10));
        stack.setOpaque(false);
        stack.add(calendarCard, BorderLayout.CENTER);
        stack.add(selectedPanel, BorderLayout.SOUTH);
        JPanel page = new JPanel(new BorderLayout(0, 8));
        page.setOpaque(false);
        page.add(stack, BorderLayout.CENTER);
        page.add(feedback, BorderLayout.SOUTH);
        content.add(page, BorderLayout.CENTER);
        return content;
    }

    private void changeMonth(int delta) {
        month = month.plusMonths(delta);
        selectedDate = month.atDay(1);
        refreshMonth();
    }

    private void renderCalendar() {
        monthLabel.setText(month.format(MONTH_FORMAT));
        dayGrid.removeAll();
        LocalDate firstCell = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int dayCount = 42;
        for (int index = 0; index < dayCount; index++) {
            LocalDate date = firstCell.plusDays(index);
            long count = reservations.stream()
                    .filter(item -> includesDate(item, date))
                    .count();
            JButton day = new JButton("<html><b>" + date.getDayOfMonth()
                    + "</b><br>" + (count == 0 ? " " : count + " booking(s)") + "</html>");
            day.setFont(StaffUi.bodyFont(11));
            day.setForeground(date.getMonth() == month.getMonth()
                    ? StaffUi.INK : new java.awt.Color(155, 158, 165));
            day.setBackground(date.equals(selectedDate)
                    ? new java.awt.Color(255, 243, 184) : java.awt.Color.WHITE);
            day.setOpaque(true);
            day.setBorder(javax.swing.BorderFactory.createLineBorder(
                    date.equals(selectedDate) ? StaffUi.SUN : new java.awt.Color(220, 224, 230)));
            day.setEnabled(date.getMonth() == month.getMonth());
            day.addActionListener(event -> showDate(date));
            dayGrid.add(day);
        }
        dayGrid.revalidate();
        dayGrid.repaint();
    }

    private void showDate(LocalDate date) {
        selectedDate = date;
        selectedDayLabel.setText(date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")));
        tableModel.setRowCount(0);
        List<AdminReservationDetails> dated = new ArrayList<>();
        for (AdminReservationDetails details : reservations) {
            if (!includesDate(details, date)) {
                continue;
            }
            dated.add(details);
            var booking = details.booking();
            var reservation = booking.reservation();
            tableModel.addRow(new Object[]{
                reservation.reference(),
                reservation.guest().fullName(),
                reservation.roomNumber() + " · " + reservation.roomType(),
                TIME_FORMAT.format(booking.checkInTime()) + "–" + TIME_FORMAT.format(booking.checkOutTime()),
                reservation.type().getDatabaseValue(),
                reservation.status().getDatabaseValue(),
                booking.paymentStatus().getDatabaseValue()
            });
        }
        renderCalendar();
    }

    private static boolean includesDate(AdminReservationDetails details, LocalDate date) {
        var reservation = details.booking().reservation();
        return !date.isBefore(reservation.checkIn()) && !date.isAfter(reservation.checkOut());
    }

    private void showError(String message) {
        StaffUi.showFeedback(feedback, message, true);
    }
}
