package com.hotel.desktop.reservation;

import com.hotel.core.model.AdminPaymentRecord;
import com.hotel.core.model.AdminReservationDetails;
import com.hotel.core.model.AuthenticatedUser;
import com.hotel.core.model.Room;
import com.hotel.core.model.enums.GuestClassification;
import com.hotel.core.model.enums.ReservationStatus;
import com.hotel.core.model.enums.RoomMonitoringStatus;
import com.hotel.core.service.PaymentProofStorage;
import com.hotel.core.service.ReservationService;
import com.hotel.core.service.RoomService;
import com.hotel.desktop.ui.StaffUi;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

public final class OnlineReservationScreen extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final ReservationService reservationService;
    private final RoomService roomService;
    private final Supplier<AuthenticatedUser> currentAdmin;
    private final Runnable onReservationChanged;
    private final List<AdminReservationDetails> reservations = new java.util.ArrayList<>();
    private final Map<Integer, Room> roomsByNumber = new HashMap<>();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[0][10],
            new String[]{"Reference", "Guest", "Room", "Stay", "Reservation", "Payment",
                "Method", "Submitted", "Verified", "Room status"}) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JLabel detail = new JLabel("Select a reservation to review its details.");
    private final JLabel feedback = StaffUi.feedbackLabel();
    private final JComboBox<AdminPaymentRecord> paymentChoice = new JComboBox<>();
    private final JButton approve = StaffUi.primaryButton("Approve online booking");
    private final JButton reject = StaffUi.secondaryButton("Reject booking");
    private final JButton verifyPayment = StaffUi.secondaryButton("Verify payment");
    private final JButton verifyId = StaffUi.secondaryButton("Verify discount ID");
    private final JButton previewPaymentProof = StaffUi.secondaryButton("Preview payment proof");
    private final JButton openDiscountId = StaffUi.secondaryButton("Open discount ID");
    private final JButton refresh = StaffUi.secondaryButton("Refresh");
    private boolean loading;
    private AdminReservationDetails selected;
    private String selectedReferenceBeforeRefresh;

    public OnlineReservationScreen(
            ReservationService reservationService,
            RoomService roomService,
            Supplier<AuthenticatedUser> currentAdmin,
            Runnable onReservationChanged) {
        this.reservationService = Objects.requireNonNull(reservationService);
        this.roomService = Objects.requireNonNull(roomService);
        this.currentAdmin = Objects.requireNonNull(currentAdmin);
        this.onReservationChanged = Objects.requireNonNull(onReservationChanged);

        JPanel body = StaffUi.createPage(
                this, "Online reservations", "Review customer bookings and payment proofs");
        body.add(buildContent(), BorderLayout.CENTER);
        StaffUi.styleTable(table);
        table.setAutoCreateRowSorter(false);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showSelectedReservation();
            }
        });
        paymentChoice.setFont(StaffUi.bodyFont(13));
        paymentChoice.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index, boolean selected, boolean focus) {
                String text = value instanceof AdminPaymentRecord payment
                        ? payment.type() + " · " + payment.method() + " · "
                                + money(payment.amount()) + (payment.verified() ? " · verified" : " · pending")
                        : "No payment submitted";
                return super.getListCellRendererComponent(list, text, index, selected, focus);
            }
        });
        paymentChoice.addActionListener(event -> updatePaymentActions());
        refresh.addActionListener(event -> refreshReservations());
        approve.addActionListener(event -> reviewSelected(true));
        reject.addActionListener(event -> reviewSelected(false));
        verifyPayment.addActionListener(event -> verifySelectedPayment());
        verifyId.addActionListener(event -> verifySelectedId());
        previewPaymentProof.addActionListener(event -> previewSelectedPaymentProof());
        openDiscountId.addActionListener(event -> openSelectedDiscountId());
        clearSelection();
        Timer autoRefresh = new Timer(15_000, event -> {
            if (isShowing()) {
                refreshReservations();
            }
        });
        autoRefresh.start();
        refreshReservations();
    }

    public void refreshReservations() {
        AuthenticatedUser admin = currentAdmin.get();
        if (admin == null) {
            return;
        }
        if (loading) {
            return;
        }
        selectedReferenceBeforeRefresh = selected == null ? null
                : selected.booking().reservation().reference();
        loading = true;
        refresh.setEnabled(false);
        StaffUi.showFeedback(feedback, "Loading reservation and room details…", false);
        new SwingWorker<RefreshData, Void>() {
            @Override
            protected RefreshData doInBackground() throws Exception {
                return new RefreshData(
                        reservationService.findAdminReservations(admin),
                        roomService.findAll());
            }

            @Override
            protected void done() {
                loading = false;
                refresh.setEnabled(true);
                try {
                    RefreshData data = get();
                    reservations.clear();
                    data.reservations().stream()
                            .filter(item -> item.booking().reservation().type()
                                    == com.hotel.core.model.enums.ReservationType.ONLINE)
                            .filter(item -> item.booking().reservation().status() != ReservationStatus.CANCELLED
                                    && item.booking().reservation().status() != ReservationStatus.CHECKED_OUT)
                            .forEach(reservations::add);
                    roomsByNumber.clear();
                    for (Room room : data.rooms()) {
                        roomsByNumber.put(room.getRoomNumber(), room);
                    }
                    renderReservations();
                    StaffUi.showFeedback(feedback,
                            reservations.size() + " active online reservation(s) loaded.", false);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Loading online reservations was interrupted.");
                } catch (ExecutionException exception) {
                    showError(message(exception.getCause()));
                }
            }
        }.execute();
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        JPanel listCard = StaffUi.card();
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        JLabel heading = new JLabel("Online booking review");
        heading.setFont(StaffUi.headingFont(21));
        heading.setForeground(StaffUi.INK);
        header.add(heading, BorderLayout.WEST);
        header.add(refresh, BorderLayout.EAST);
        listCard.add(header, BorderLayout.NORTH);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.getViewport().setBackground(java.awt.Color.WHITE);
        tableScroll.setPreferredSize(new Dimension(900, 320));
        listCard.add(tableScroll, BorderLayout.CENTER);

        JPanel reviewCard = StaffUi.card();
        reviewCard.setLayout(new BorderLayout(0, 8));
        detail.setFont(StaffUi.bodyFont(13));
        detail.setForeground(StaffUi.INK);
        reviewCard.add(detail, BorderLayout.NORTH);

        JPanel paymentPanel = new JPanel(new BorderLayout(8, 0));
        paymentPanel.setOpaque(false);
        paymentPanel.add(paymentChoice, BorderLayout.CENTER);
        JPanel proofActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        proofActions.setOpaque(false);
        proofActions.add(openDiscountId);
        proofActions.add(previewPaymentProof);
        paymentPanel.add(proofActions, BorderLayout.EAST);
        reviewCard.add(paymentPanel, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, 3, 8, 0));
        actions.setOpaque(false);
        actions.add(approve);
        actions.add(reject);
        actions.add(verifyPayment);
        JPanel idAction = new JPanel(new BorderLayout(8, 0));
        idAction.setOpaque(false);
        idAction.add(feedback, BorderLayout.CENTER);
        idAction.add(verifyId, BorderLayout.EAST);
        JPanel footer = new JPanel(new BorderLayout(0, 6));
        footer.setOpaque(false);
        footer.add(actions, BorderLayout.CENTER);
        footer.add(idAction, BorderLayout.SOUTH);
        reviewCard.add(footer, BorderLayout.SOUTH);

        JPanel lower = new JPanel(new BorderLayout(0, 8));
        lower.setOpaque(false);
        JLabel note = new JLabel(
                "Online bookings start Pending. Approve or reject only after reviewing the guest, room, and submitted proofs.");
        note.setFont(StaffUi.bodyFont(12));
        note.setForeground(StaffUi.MUTED);
        lower.add(note, BorderLayout.NORTH);
        lower.add(reviewCard, BorderLayout.CENTER);
        content.add(listCard, BorderLayout.CENTER);
        content.add(lower, BorderLayout.SOUTH);
        return content;
    }

    private void renderReservations() {
        String referenceToRestore = selectedReferenceBeforeRefresh;
        tableModel.setRowCount(0);
        for (AdminReservationDetails details : reservations) {
            var booking = details.booking();
            var reservation = booking.reservation();
            Room room = roomsByNumber.get(reservation.roomNumber());
            String roomStatus = room == null
                    ? "Unknown" : RoomMonitoringStatus.fromRoom(room).getDisplayName();
            tableModel.addRow(new Object[]{
                reservation.reference(),
                reservation.guest().fullName(),
                reservation.roomNumber() + " · " + reservation.roomType(),
                DATE.format(reservation.checkIn()) + " " + TIME.format(booking.checkInTime())
                        + " – " + DATE.format(reservation.checkOut()) + " "
                        + TIME.format(booking.checkOutTime()),
                reservation.status().getDatabaseValue(),
                booking.paymentStatus().getDatabaseValue(),
                booking.paymentMethods(),
                money(booking.amountSubmitted()),
                money(details.verifiedAmount()),
                roomStatus
            });
        }
        int restoreRow = -1;
        if (referenceToRestore != null) {
            for (int row = 0; row < reservations.size(); row++) {
                if (reservations.get(row).booking().reservation().reference().equals(referenceToRestore)) {
                    restoreRow = row;
                    break;
                }
            }
        }
        if (restoreRow >= 0) {
            table.setRowSelectionInterval(restoreRow, restoreRow);
        } else {
            clearSelection();
        }
        selectedReferenceBeforeRefresh = null;
    }

    private void showSelectedReservation() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= reservations.size()) {
            clearSelection();
            return;
        }
        selected = reservations.get(row);
        var booking = selected.booking();
        var reservation = booking.reservation();
        Room room = roomsByNumber.get(reservation.roomNumber());
        String roomStatus = room == null
                ? "Unknown" : RoomMonitoringStatus.fromRoom(room).getDisplayName();
        detail.setText("<html><b>" + escape(reservation.guest().fullName())
                + "</b> · " + escape(reservation.guest().email())
                + " · " + escape(reservation.guest().contactNumber())
                + "<br>Room " + reservation.roomNumber() + " (" + escape(reservation.roomType())
                + ") · " + roomStatus
                + " · " + reservation.guestCount() + " guest(s)"
                + "<br>Stay: " + DATE.format(reservation.checkIn()) + " " + TIME.format(booking.checkInTime())
                + " to " + DATE.format(reservation.checkOut()) + " " + TIME.format(booking.checkOutTime())
                + " · " + reservation.nights() + " night(s)"
                + "<br>Total: " + money(booking.total()) + " · Submitted: "
                + money(booking.amountSubmitted()) + " · Verified: " + money(selected.verifiedAmount())
                + " · Balance: " + money(booking.balance())
                + " · Payment methods: " + escape(booking.paymentMethods())
                + "<br>Discount ID number: "
                + escape(orDash(reservation.guest().discountIdNumber()))
                + " · ID status: " + (reservation.guest().discountVerified() ? "Verified" : "Not verified")
                + "</html>");
        paymentChoice.setModel(new DefaultComboBoxModel<>(
                selected.payments().toArray(AdminPaymentRecord[]::new)));
        boolean pending = reservation.status() == ReservationStatus.PENDING;
        approve.setEnabled(pending);
        reject.setEnabled(pending);
        verifyId.setEnabled(reservation.guest().classification() == GuestClassification.DISCOUNTED
                && !reservation.guest().discountVerified()
                && reservation.guest().discountIdProofPath() != null);
        updatePaymentActions();
    }

    private void updatePaymentActions() {
        AdminPaymentRecord payment = (AdminPaymentRecord) paymentChoice.getSelectedItem();
        verifyPayment.setEnabled(payment != null && !payment.verified());
        previewPaymentProof.setEnabled(payment != null && payment.proofFilePath() != null);
        openDiscountId.setEnabled(selected != null
                && selected.booking().reservation().guest().discountIdProofPath() != null);
    }

    private void clearSelection() {
        selected = null;
        detail.setText("Select a reservation to review its details.");
        paymentChoice.setModel(new DefaultComboBoxModel<>());
        approve.setEnabled(false);
        reject.setEnabled(false);
        verifyPayment.setEnabled(false);
        verifyId.setEnabled(false);
        previewPaymentProof.setEnabled(false);
        openDiscountId.setEnabled(false);
    }

    private void reviewSelected(boolean confirm) {
        if (selected == null) {
            return;
        }
        String reference = selected.booking().reservation().reference();
        runAction(confirm ? "Approving reservation…" : "Rejecting reservation…",
                () -> reservationService.reviewOnlineReservation(currentAdmin.get(), reference, confirm));
    }

    private void verifySelectedPayment() {
        if (selected == null) {
            return;
        }
        AdminPaymentRecord payment = (AdminPaymentRecord) paymentChoice.getSelectedItem();
        if (payment == null) {
            return;
        }
        String reference = selected.booking().reservation().reference();
        runAction("Verifying payment…", () -> {
            reservationService.verifyOnlinePayment(currentAdmin.get(), reference, payment.id());
            return null;
        });
    }

    private void verifySelectedId() {
        if (selected == null) {
            return;
        }
        String reference = selected.booking().reservation().reference();
        runAction("Verifying discount ID…", () -> {
            reservationService.verifyDiscountId(currentAdmin.get(), reference);
            return null;
        });
    }

    private void previewSelectedPaymentProof() {
        AdminPaymentRecord payment = (AdminPaymentRecord) paymentChoice.getSelectedItem();
        if (payment == null || payment.proofFilePath() == null) {
            return;
        }
        Path storedPath;
        try {
            storedPath = PaymentProofStorage.fromConfiguration().resolveStored(
                    Path.of(payment.proofFilePath()));
        } catch (IOException | RuntimeException exception) {
            showError("Unable to read the uploaded proof: " + exception.getMessage());
            return;
        }
        previewPaymentProof.setEnabled(false);
        StaffUi.showFeedback(feedback, "Loading payment proof preview…", false);
        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                return readPaymentProofPreview(storedPath);
            }

            @Override
            protected void done() {
                try {
                    BufferedImage image = get();
                    if (image == null) {
                        showError("This payment proof is not a supported image for in-app preview.");
                        return;
                    }
                    showPaymentProofPreview(image);
                    StaffUi.showFeedback(feedback, "Payment proof preview opened.", false);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Loading the payment proof preview was interrupted.");
                } catch (ExecutionException exception) {
                    showError("Unable to preview the payment proof: "
                            + message(exception.getCause()));
                } finally {
                    updatePaymentActions();
                }
            }
        }.execute();
    }

    private static BufferedImage readPaymentProofPreview(Path path) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(path.toFile())) {
            if (input == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > 40_000_000L) {
                    throw new IOException("Image dimensions are too large to preview safely.");
                }
                int sample = Math.max(1, Math.max((width + 1199) / 1200, (height + 799) / 800));
                ImageReadParam parameters = reader.getDefaultReadParam();
                parameters.setSourceSubsampling(sample, sample, 0, 0);
                return reader.read(0, parameters);
            } finally {
                reader.dispose();
            }
        }
    }

    private void showPaymentProofPreview(BufferedImage image) {
        double scale = Math.min(1.0, Math.min(900.0 / image.getWidth(), 600.0 / image.getHeight()));
        int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
        ImageIcon icon = new ImageIcon(image.getScaledInstance(
                width, height, java.awt.Image.SCALE_SMOOTH));
        JLabel preview = new JLabel(icon);
        preview.setHorizontalAlignment(JLabel.CENTER);
        JOptionPane.showMessageDialog(
                this, new JScrollPane(preview), "Payment proof preview",
                JOptionPane.PLAIN_MESSAGE);
    }

    private void openSelectedDiscountId() {
        if (selected != null) {
            openStoredFile(selected.booking().reservation().guest().discountIdProofPath());
        }
    }

    private void openStoredFile(String storedPath) {
        try {
            Path path = PaymentProofStorage.fromConfiguration().resolveStored(
                    storedPath == null ? null : Path.of(storedPath));
            if (!Desktop.isDesktopSupported()) {
                throw new IOException("Opening files is not supported on this computer.");
            }
            Desktop.getDesktop().open(path.toFile());
        } catch (IOException | RuntimeException exception) {
            showError("Unable to open the uploaded proof: " + exception.getMessage());
        }
    }

    private void runAction(String message, SqlAction action) {
        setActionsEnabled(false);
        StaffUi.showFeedback(feedback, message, false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                action.run();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    onReservationChanged.run();
                    refreshReservations();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("The reservation action was interrupted.");
                } catch (ExecutionException exception) {
                    showError(message(exception.getCause()));
                    setActionsEnabled(true);
                }
            }
        }.execute();
    }

    private void setActionsEnabled(boolean enabled) {
        approve.setEnabled(enabled && selected != null
                && selected.booking().reservation().status() == ReservationStatus.PENDING);
        reject.setEnabled(approve.isEnabled());
        verifyPayment.setEnabled(enabled
                && paymentChoice.getSelectedItem() instanceof AdminPaymentRecord payment
                && !payment.verified());
        verifyId.setEnabled(enabled && selected != null
                && selected.booking().reservation().guest().classification() == GuestClassification.DISCOUNTED
                && !selected.booking().reservation().guest().discountVerified());
    }

    private void showError(String message) {
        StaffUi.showFeedback(feedback, message, true);
    }

    private static String message(Throwable cause) {
        return cause == null || cause.getMessage() == null
                ? "Unable to complete the reservation action." : cause.getMessage();
    }

    private static String money(java.math.BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;");
    }

    private record RefreshData(List<AdminReservationDetails> reservations, List<Room> rooms) {
    }

    @FunctionalInterface
    private interface SqlAction {
        Object run() throws Exception;
    }
}
