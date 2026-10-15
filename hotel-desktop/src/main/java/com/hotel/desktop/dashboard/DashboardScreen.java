package com.hotel.desktop.dashboard;

import roomMonitoring.RoomInventory;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

/**
 * Staff dashboard (1280 x 800), shown after sign-in.
 *
 * Add it to a host frame like the other screens:
 * {@code new DashboardScreenRef(this::showWelcome)}.
 * Room counts use the shared sample inventory; reservations remain static preview data.
 */
public class DashboardScreen extends JPanel {

    private static final long serialVersionUID = 1L;

    // Palette
    private static final Color INK = new Color(20, 18, 26);
    private static final Color SUN = new Color(255, 204, 0);
    private static final Color CANVAS = new Color(255, 222, 62);
    private static final Color CREAM = new Color(233, 223, 168);
    private static final Color MUTED = new Color(105, 91, 33);
    private static final Color HEADER_TEXT = new Color(74, 65, 0);
    private static final Color NAV_LINE = new Color(52, 48, 62);
    private static final Color NAV_HOVER = new Color(38, 35, 48);
    private static final Color ROW_LINE = new Color(236, 230, 196);
    private static final Color TRACK = new Color(241, 236, 208);
    private static final Color SELECTED = new Color(255, 243, 184);
    private static final Color ERROR_RED = new Color(179, 38, 30);
    private static final Color AVAILABLE_GREEN = new Color(0, 230, 80);

    private final Runnable onLogout;
    private final Runnable onRoomMonitoring;
    private final RoomInventory roomInventory;
    private final Runnable onManageRooms;

    public DashboardScreen() {
        this(() -> {
        }, () -> {
        });
    }

    public DashboardScreen(Runnable onLogout) {
        this(onLogout, () -> { });
    }
    public DashboardScreen(Runnable onLogout, Runnable onRoomMonitoring) {
        this(onLogout, () -> { }, onRoomMonitoring, new RoomInventory());
    }

    public DashboardScreen(Runnable onLogout, Runnable onManageRooms, Runnable onRoomMonitoring, RoomInventory roomInventory) {
        this.onLogout = Objects.requireNonNull(onLogout, "onLogout");
        this.onManageRooms = Objects.requireNonNull(onManageRooms, "onManageRooms");
        this.onRoomMonitoring = Objects.requireNonNull(onRoomMonitoring, "onRoomMonitoring");
        this.roomInventory = Objects.requireNonNull(roomInventory, "roomInventory");
        initComponents();
        lblDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)));
        styleReservationTable();
        btnRoomManagement.addActionListener(event -> this.onManageRooms.run());
        btnManageRooms.addActionListener(event -> this.onManageRooms.run());
        refreshRoomCounts();
    }

    /** @return the button used to leave the staff dashboard (also the host frame's Enter key action). */
    public JButton getLogoutButton() {
        return btnLogout;
    }

    /** Shows the signed-in staff member's name in the sidebar. */
    public void setStaffName(String name) {
        lblWelcome.setText("Welcome, " + name);
    }

    /** Shows the signed-in staff member's role in the sidebar. */
    public void setStaffRole(String role) {
        lblRole.setText(role);
    }

    /** Refreshes the room summary from the shared sample inventory. */
    public void refreshRoomCounts() {
        lblAvailableCount.setText(Integer.toString(roomInventory.count(RoomInventory.Status.AVAILABLE)));
        lblOccupiedCount.setText(Integer.toString(roomInventory.count(RoomInventory.Status.OCCUPIED)));
        lblCleaningCount.setText(Integer.toString(roomInventory.count(RoomInventory.Status.DIRTY)));
        lblMaintenanceCount.setText(Integer.toString(roomInventory.count(RoomInventory.Status.UNDER_MAINTENANCE)));
        lblOutOfServiceCount.setText(Integer.toString(roomInventory.count(RoomInventory.Status.OUT_OF_SERVICE)));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlSidebar = new javax.swing.JPanel();
        pnlBrand = new BrandPanel();
        logoTile = new LogoTile();
        lblBrand = new javax.swing.JLabel();
        lblBrandSubtitle = new javax.swing.JLabel();
        pnlNavigation = new javax.swing.JPanel();
        btnDashboard = new NavButton(Glyph.HOME, NavButton.ACTIVE);
        btnRoomManagement = new NavButton(Glyph.DOOR, NavButton.ITEM);
        btnRoomMonitoring = new NavButton(Glyph.GRID, NavButton.ITEM);
        btnHousekeeping = new NavButton(Glyph.SPARKLE, NavButton.ITEM);
        btnWalkIn = new NavButton(Glyph.PERSON_PLUS, NavButton.ITEM);
        btnOnlineReservations = new NavButton(Glyph.GLOBE, NavButton.ITEM);
        btnCheckIn = new NavButton(Glyph.CHECK_IN, NavButton.ITEM);
        btnCheckOut = new NavButton(Glyph.CHECK_OUT, NavButton.ITEM);
        pnlProfile = new javax.swing.JPanel();
        pnlProfileBand = new javax.swing.JPanel();
        pnlUser = new javax.swing.JPanel();
        avatar = new AvatarBadge();
        lblWelcome = new javax.swing.JLabel();
        lblRole = new javax.swing.JLabel();
        pnlLogout = new javax.swing.JPanel();
        btnLogout = new NavButton(Glyph.POWER, NavButton.OUTLINE);
        pnlContent = new javax.swing.JPanel();
        pnlTopStrip = new javax.swing.JPanel();
        pnlBody = new javax.swing.JPanel();
        pnlHeader = new javax.swing.JPanel();
        lblTitle = new javax.swing.JLabel();
        lblDate = new javax.swing.JLabel();
        pnlMain = new javax.swing.JPanel();
        pnlSummary = new javax.swing.JPanel();
        pnlAvailableCard = new RoundedCard();
        badgeAvailable = new StatusBadge(StatusBadge.Kind.AVAILABLE);
        lblAvailableTitle = new javax.swing.JLabel();
        lblAvailableCount = new javax.swing.JLabel();
        pnlOccupiedCard = new RoundedCard();
        badgeOccupied = new StatusBadge(StatusBadge.Kind.OCCUPIED);
        lblOccupiedTitle = new javax.swing.JLabel();
        lblOccupiedCount = new javax.swing.JLabel();
        pnlCleaningCard = new RoundedCard();
        badgeCleaning = new StatusBadge(StatusBadge.Kind.CLEANING);
        lblCleaningTitle = new javax.swing.JLabel();
        lblCleaningCount = new javax.swing.JLabel();
        pnlMaintenanceCard = new RoundedCard();
        badgeMaintenance = new StatusBadge(StatusBadge.Kind.MAINTENANCE);
        lblMaintenanceTitle = new javax.swing.JLabel();
        lblMaintenanceCount = new javax.swing.JLabel();
        pnlOutOfServiceCard = new RoundedCard();
        badgeOutOfService = new StatusBadge(StatusBadge.Kind.OUT_OF_SERVICE);
        lblOutOfServiceTitle = new javax.swing.JLabel();
        lblOutOfServiceCount = new javax.swing.JLabel();
        pnlLower = new javax.swing.JPanel();
        pnlReservations = new RoundedCard();
        pnlReservationsHeader = new javax.swing.JPanel();
        lblReservationTitle = new javax.swing.JLabel();
        lblArrivals = new javax.swing.JLabel();
        scrollReservations = new javax.swing.JScrollPane();
        tblReservations = new javax.swing.JTable();
        pnlRightColumn = new javax.swing.JPanel();
        pnlQuickActions = new RoundedCard();
        lblQuickActions = new javax.swing.JLabel();
        pnlQuickButtons = new javax.swing.JPanel();
        btnNewWalkIn = new ActionButton(Glyph.PLUS);
        btnCheckInAction = new ActionButton(null);
        btnCheckOutAction = new ActionButton(null);
        btnManageRooms = new ActionButton(null);
        pnlAvailability = new RoundedCard();
        lblAvailability = new javax.swing.JLabel();
        pnlAvailabilityRows = new javax.swing.JPanel();
        pnlSingleRow = new javax.swing.JPanel();
        lblSingle = new javax.swing.JLabel();
        barSingle = new AvailabilityBar();
        lblSingleCount = new javax.swing.JLabel();
        pnlDoubleRow = new javax.swing.JPanel();
        lblDouble = new javax.swing.JLabel();
        barDouble = new AvailabilityBar();
        lblDoubleCount = new javax.swing.JLabel();
        pnlDeluxeRow = new javax.swing.JPanel();
        lblDeluxe = new javax.swing.JLabel();
        barDeluxe = new AvailabilityBar();
        lblDeluxeCount = new javax.swing.JLabel();

        setBackground(new java.awt.Color(255, 222, 62));
        setPreferredSize(new java.awt.Dimension(1280, 800));
        setLayout(new java.awt.BorderLayout());

        // ---------------------------------------------------------------- sidebar
        pnlSidebar.setBackground(new java.awt.Color(20, 18, 26));
        pnlSidebar.setPreferredSize(new java.awt.Dimension(256, 800));
        pnlSidebar.setLayout(new java.awt.BorderLayout());

        pnlBrand.setBackground(new java.awt.Color(20, 18, 26));
        pnlBrand.setPreferredSize(new java.awt.Dimension(256, 93));

        logoTile.setPreferredSize(new java.awt.Dimension(46, 46));

        lblBrand.setFont(new java.awt.Font("Georgia", 1, 17)); // NOI18N
        lblBrand.setForeground(new java.awt.Color(255, 255, 255));
        lblBrand.setText("Hotel Reservation");

        lblBrandSubtitle.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        lblBrandSubtitle.setForeground(new java.awt.Color(233, 223, 168));
        lblBrandSubtitle.setText("Front desk");

        javax.swing.GroupLayout pnlBrandLayout = new javax.swing.GroupLayout(pnlBrand);
        pnlBrand.setLayout(pnlBrandLayout);
        pnlBrandLayout.setHorizontalGroup(
            pnlBrandLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlBrandLayout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addComponent(logoTile, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(11, 11, 11)
                .addGroup(pnlBrandLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblBrand)
                    .addComponent(lblBrandSubtitle))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pnlBrandLayout.setVerticalGroup(
            pnlBrandLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlBrandLayout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addGroup(pnlBrandLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(logoTile, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(pnlBrandLayout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addComponent(lblBrand)
                        .addGap(1, 1, 1)
                        .addComponent(lblBrandSubtitle)))
                .addContainerGap(24, Short.MAX_VALUE))
        );

        pnlSidebar.add(pnlBrand, java.awt.BorderLayout.NORTH);

        pnlNavigation.setBackground(new java.awt.Color(20, 18, 26));
        pnlNavigation.setBorder(javax.swing.BorderFactory.createEmptyBorder(13, 14, 0, 14));
        pnlNavigation.setLayout(new javax.swing.BoxLayout(pnlNavigation, javax.swing.BoxLayout.Y_AXIS));

        btnDashboard.setText("Dashboard");
        btnDashboard.setMaximumSize(new java.awt.Dimension(2147483647, 48));
        btnDashboard.setPreferredSize(new java.awt.Dimension(228, 48));
        pnlNavigation.add(btnDashboard);

        btnRoomManagement.setText("Room management");
        btnRoomManagement.setMaximumSize(new java.awt.Dimension(2147483647, 48));
        btnRoomManagement.setPreferredSize(new java.awt.Dimension(228, 48));
        pnlNavigation.add(btnRoomManagement);

        btnRoomMonitoring.setText("Room monitoring");
        btnRoomMonitoring.setMaximumSize(new java.awt.Dimension(2147483647, 48));
        btnRoomMonitoring.setPreferredSize(new java.awt.Dimension(228, 48));
        btnRoomMonitoring.addActionListener(evt -> onRoomMonitoring.run());
        pnlNavigation.add(btnRoomMonitoring);

        btnHousekeeping.setText("Housekeeping");
        btnHousekeeping.setMaximumSize(new java.awt.Dimension(2147483647, 48));
        btnHousekeeping.setPreferredSize(new java.awt.Dimension(228, 48));
        pnlNavigation.add(btnHousekeeping);

        btnWalkIn.setText("Walk-in reservation");
        btnWalkIn.setMaximumSize(new java.awt.Dimension(2147483647, 48));
        btnWalkIn.setPreferredSize(new java.awt.Dimension(228, 48));
        pnlNavigation.add(btnWalkIn);

        btnOnlineReservations.setText("Online reservations");
        btnOnlineReservations.putClientProperty("badge", "3");
        btnOnlineReservations.setMaximumSize(new java.awt.Dimension(2147483647, 48));
        btnOnlineReservations.setPreferredSize(new java.awt.Dimension(228, 48));
        pnlNavigation.add(btnOnlineReservations);

        btnCheckIn.setText("Check in");
        btnCheckIn.setMaximumSize(new java.awt.Dimension(2147483647, 48));
        btnCheckIn.setPreferredSize(new java.awt.Dimension(228, 48));
        pnlNavigation.add(btnCheckIn);

        btnCheckOut.setText("Check out");
        btnCheckOut.setMaximumSize(new java.awt.Dimension(2147483647, 48));
        btnCheckOut.setPreferredSize(new java.awt.Dimension(228, 48));
        pnlNavigation.add(btnCheckOut);

        pnlSidebar.add(pnlNavigation, java.awt.BorderLayout.CENTER);

        pnlProfile.setBackground(new java.awt.Color(20, 18, 26));
        pnlProfile.setLayout(new java.awt.BorderLayout());

        pnlProfileBand.setBackground(new java.awt.Color(255, 204, 0));
        pnlProfileBand.setPreferredSize(new java.awt.Dimension(256, 15));
        pnlProfile.add(pnlProfileBand, java.awt.BorderLayout.NORTH);

        pnlUser.setBackground(new java.awt.Color(20, 18, 26));
        pnlUser.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 0, 0, 0));
        pnlUser.setPreferredSize(new java.awt.Dimension(256, 87));

        avatar.setPreferredSize(new java.awt.Dimension(36, 36));

        lblWelcome.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblWelcome.setForeground(new java.awt.Color(255, 255, 255));
        lblWelcome.setText("Welcome, Staff");

        lblRole.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        lblRole.setForeground(new java.awt.Color(233, 223, 168));
        lblRole.setText("Receptionist");

        javax.swing.GroupLayout pnlUserLayout = new javax.swing.GroupLayout(pnlUser);
        pnlUser.setLayout(pnlUserLayout);
        pnlUserLayout.setHorizontalGroup(
            pnlUserLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlUserLayout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addComponent(avatar, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addGroup(pnlUserLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblWelcome)
                    .addComponent(lblRole))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pnlUserLayout.setVerticalGroup(
            pnlUserLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlUserLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addGroup(pnlUserLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(avatar, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(pnlUserLayout.createSequentialGroup()
                        .addComponent(lblWelcome)
                        .addGap(1, 1, 1)
                        .addComponent(lblRole)))
                .addGap(0, 0, Short.MAX_VALUE))
        );

        pnlProfile.add(pnlUser, java.awt.BorderLayout.CENTER);

        pnlLogout.setBackground(new java.awt.Color(20, 18, 26));
        pnlLogout.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 14, 34, 14));
        pnlLogout.setLayout(new java.awt.BorderLayout());

        btnLogout.setText("Log out");
        btnLogout.setPreferredSize(new java.awt.Dimension(228, 48));
        btnLogout.addActionListener(this::btnLogoutActionPerformed);
        pnlLogout.add(btnLogout, java.awt.BorderLayout.CENTER);

        pnlProfile.add(pnlLogout, java.awt.BorderLayout.SOUTH);

        pnlSidebar.add(pnlProfile, java.awt.BorderLayout.SOUTH);

        add(pnlSidebar, java.awt.BorderLayout.WEST);

        // ---------------------------------------------------------------- content
        pnlContent.setBackground(new java.awt.Color(255, 222, 62));
        pnlContent.setLayout(new java.awt.BorderLayout());

        pnlTopStrip.setBackground(new java.awt.Color(255, 204, 0));
        pnlTopStrip.setPreferredSize(new java.awt.Dimension(10, 15));
        pnlContent.add(pnlTopStrip, java.awt.BorderLayout.NORTH);

        pnlBody.setOpaque(false);
        pnlBody.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 40, 52, 40));
        pnlBody.setLayout(new java.awt.BorderLayout(0, 25));

        pnlHeader.setOpaque(false);
        pnlHeader.setPreferredSize(new java.awt.Dimension(10, 66));
        pnlHeader.setLayout(new java.awt.BorderLayout());

        lblTitle.setFont(new java.awt.Font("Georgia", 1, 36)); // NOI18N
        lblTitle.setForeground(new java.awt.Color(20, 18, 26));
        lblTitle.setText("Dashboard");
        pnlHeader.add(lblTitle, java.awt.BorderLayout.NORTH);

        lblDate.setFont(new java.awt.Font("Segoe UI", 0, 15)); // NOI18N
        lblDate.setForeground(new java.awt.Color(105, 91, 33));
        lblDate.setText("Wednesday, 14 October 2026");
        pnlHeader.add(lblDate, java.awt.BorderLayout.CENTER);

        pnlBody.add(pnlHeader, java.awt.BorderLayout.NORTH);

        pnlMain.setOpaque(false);
        pnlMain.setLayout(new java.awt.BorderLayout(0, 28));

        // ---- room status cards
        pnlSummary.setOpaque(false);
        pnlSummary.setPreferredSize(new java.awt.Dimension(10, 120));
        pnlSummary.setLayout(new java.awt.GridLayout(1, 5, 16, 0));

        pnlAvailableCard.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 4, 12));
        pnlAvailableCard.setLayout(new javax.swing.BoxLayout(pnlAvailableCard, javax.swing.BoxLayout.Y_AXIS));
        pnlAvailableCard.add(badgeAvailable);
        lblAvailableTitle.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        lblAvailableTitle.setForeground(new java.awt.Color(20, 18, 26));
        lblAvailableTitle.setText("Available");
        lblAvailableTitle.setBorder(javax.swing.BorderFactory.createEmptyBorder(11, 0, 0, 0));
        pnlAvailableCard.add(lblAvailableTitle);
        lblAvailableCount.setFont(new java.awt.Font("Georgia", 1, 38)); // NOI18N
        lblAvailableCount.setForeground(new java.awt.Color(20, 18, 26));
        lblAvailableCount.setText("12");
        pnlAvailableCard.add(lblAvailableCount);
        pnlSummary.add(pnlAvailableCard);

        pnlOccupiedCard.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 4, 12));
        pnlOccupiedCard.setLayout(new javax.swing.BoxLayout(pnlOccupiedCard, javax.swing.BoxLayout.Y_AXIS));
        pnlOccupiedCard.add(badgeOccupied);
        lblOccupiedTitle.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        lblOccupiedTitle.setForeground(new java.awt.Color(20, 18, 26));
        lblOccupiedTitle.setText("Occupied");
        lblOccupiedTitle.setBorder(javax.swing.BorderFactory.createEmptyBorder(11, 0, 0, 0));
        pnlOccupiedCard.add(lblOccupiedTitle);
        lblOccupiedCount.setFont(new java.awt.Font("Georgia", 1, 38)); // NOI18N
        lblOccupiedCount.setForeground(new java.awt.Color(20, 18, 26));
        lblOccupiedCount.setText("8");
        pnlOccupiedCard.add(lblOccupiedCount);
        pnlSummary.add(pnlOccupiedCard);

        pnlCleaningCard.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 4, 12));
        pnlCleaningCard.setLayout(new javax.swing.BoxLayout(pnlCleaningCard, javax.swing.BoxLayout.Y_AXIS));
        pnlCleaningCard.add(badgeCleaning);
        lblCleaningTitle.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        lblCleaningTitle.setForeground(new java.awt.Color(20, 18, 26));
        lblCleaningTitle.setText("Dirty");
        lblCleaningTitle.setBorder(javax.swing.BorderFactory.createEmptyBorder(11, 0, 0, 0));
        pnlCleaningCard.add(lblCleaningTitle);
        lblCleaningCount.setFont(new java.awt.Font("Georgia", 1, 38)); // NOI18N
        lblCleaningCount.setForeground(new java.awt.Color(20, 18, 26));
        lblCleaningCount.setText("4");
        pnlCleaningCard.add(lblCleaningCount);
        pnlSummary.add(pnlCleaningCard);

        pnlMaintenanceCard.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 4, 12));
        pnlMaintenanceCard.setLayout(new javax.swing.BoxLayout(pnlMaintenanceCard, javax.swing.BoxLayout.Y_AXIS));
        pnlMaintenanceCard.add(badgeMaintenance);
        lblMaintenanceTitle.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        lblMaintenanceTitle.setForeground(new java.awt.Color(20, 18, 26));
        lblMaintenanceTitle.setText("Under Maintenance");
        lblMaintenanceTitle.setBorder(javax.swing.BorderFactory.createEmptyBorder(11, 0, 0, 0));
        pnlMaintenanceCard.add(lblMaintenanceTitle);
        lblMaintenanceCount.setFont(new java.awt.Font("Georgia", 1, 38)); // NOI18N
        lblMaintenanceCount.setForeground(new java.awt.Color(20, 18, 26));
        lblMaintenanceCount.setText("2");
        pnlMaintenanceCard.add(lblMaintenanceCount);
        pnlSummary.add(pnlMaintenanceCard);

        pnlOutOfServiceCard.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 4, 12));
        pnlOutOfServiceCard.setLayout(new javax.swing.BoxLayout(pnlOutOfServiceCard, javax.swing.BoxLayout.Y_AXIS));
        pnlOutOfServiceCard.add(badgeOutOfService);
        lblOutOfServiceTitle.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        lblOutOfServiceTitle.setForeground(new java.awt.Color(20, 18, 26));
        lblOutOfServiceTitle.setText("Out of service");
        lblOutOfServiceTitle.setBorder(javax.swing.BorderFactory.createEmptyBorder(11, 0, 0, 0));
        pnlOutOfServiceCard.add(lblOutOfServiceTitle);
        lblOutOfServiceCount.setFont(new java.awt.Font("Georgia", 1, 38)); // NOI18N
        lblOutOfServiceCount.setForeground(new java.awt.Color(20, 18, 26));
        lblOutOfServiceCount.setText("1");
        pnlOutOfServiceCard.add(lblOutOfServiceCount);
        pnlSummary.add(pnlOutOfServiceCard);

        pnlMain.add(pnlSummary, java.awt.BorderLayout.NORTH);

        // ---- reservations + right column
        pnlLower.setOpaque(false);
        pnlLower.setLayout(new java.awt.BorderLayout(16, 0));

        pnlReservations.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2));
        pnlReservations.setLayout(new java.awt.BorderLayout());

        pnlReservationsHeader.setOpaque(false);
        pnlReservationsHeader.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createMatteBorder(0, 0, 2, 0, new java.awt.Color(20, 18, 26)),
            javax.swing.BorderFactory.createEmptyBorder(0, 22, 0, 22)));
        pnlReservationsHeader.setPreferredSize(new java.awt.Dimension(10, 62));
        pnlReservationsHeader.setLayout(new java.awt.BorderLayout());

        lblReservationTitle.setFont(new java.awt.Font("Georgia", 1, 20)); // NOI18N
        lblReservationTitle.setForeground(new java.awt.Color(20, 18, 26));
        lblReservationTitle.setText("Today's reservations");
        pnlReservationsHeader.add(lblReservationTitle, java.awt.BorderLayout.WEST);

        lblArrivals.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        lblArrivals.setForeground(new java.awt.Color(105, 91, 33));
        lblArrivals.setText("5 arrivals");
        pnlReservationsHeader.add(lblArrivals, java.awt.BorderLayout.EAST);

        pnlReservations.add(pnlReservationsHeader, java.awt.BorderLayout.NORTH);

        scrollReservations.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 18, 18, 18));

        tblReservations.setFont(new java.awt.Font("Segoe UI", 0, 15)); // NOI18N
        tblReservations.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"RES-001", "Juan Dela Cruz", "203", "Walk-in", "Confirmed"},
                {"RES-002", "Maria Santos", "105", "Online", "Pending"},
                {"RES-003", "Pedro Reyes", "301", "Walk-in", "Checked-In"},
                {"RES-004", "Ana Lim", "208", "Online", "Confirmed"},
                {"RES-005", "Carlo Mendoza", "110", "Walk-in", "Confirmed"}
            },
            new String [] {
                "Reference", "Guest", "Room", "Type", "Status"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        scrollReservations.setViewportView(tblReservations);

        pnlReservations.add(scrollReservations, java.awt.BorderLayout.CENTER);

        pnlLower.add(pnlReservations, java.awt.BorderLayout.CENTER);

        pnlRightColumn.setOpaque(false);
        pnlRightColumn.setPreferredSize(new java.awt.Dimension(336, 100));
        pnlRightColumn.setLayout(new java.awt.BorderLayout(0, 16));

        pnlQuickActions.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 24, 9, 24));
        pnlQuickActions.setPreferredSize(new java.awt.Dimension(336, 298));
        pnlQuickActions.setLayout(new java.awt.BorderLayout());

        lblQuickActions.setFont(new java.awt.Font("Georgia", 1, 20)); // NOI18N
        lblQuickActions.setForeground(new java.awt.Color(20, 18, 26));
        lblQuickActions.setText("Quick actions");
        lblQuickActions.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 26, 0));
        pnlQuickActions.add(lblQuickActions, java.awt.BorderLayout.NORTH);

        pnlQuickButtons.setOpaque(false);
        pnlQuickButtons.setLayout(new java.awt.GridLayout(4, 1, 0, 10));

        btnNewWalkIn.setBackground(new java.awt.Color(20, 18, 26));
        btnNewWalkIn.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        btnNewWalkIn.setForeground(new java.awt.Color(255, 204, 0));
        btnNewWalkIn.setText("New walk-in reservation");
        pnlQuickButtons.add(btnNewWalkIn);

        btnCheckInAction.setBackground(new java.awt.Color(255, 255, 255));
        btnCheckInAction.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        btnCheckInAction.setForeground(new java.awt.Color(20, 18, 26));
        btnCheckInAction.setText("Check in guest");
        pnlQuickButtons.add(btnCheckInAction);

        btnCheckOutAction.setBackground(new java.awt.Color(255, 255, 255));
        btnCheckOutAction.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        btnCheckOutAction.setForeground(new java.awt.Color(20, 18, 26));
        btnCheckOutAction.setText("Check out guest");
        pnlQuickButtons.add(btnCheckOutAction);

        btnManageRooms.setBackground(new java.awt.Color(255, 255, 255));
        btnManageRooms.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        btnManageRooms.setForeground(new java.awt.Color(20, 18, 26));
        btnManageRooms.setText("Manage rooms");
        pnlQuickButtons.add(btnManageRooms);

        pnlQuickActions.add(pnlQuickButtons, java.awt.BorderLayout.CENTER);

        pnlRightColumn.add(pnlQuickActions, java.awt.BorderLayout.NORTH);

        pnlAvailability.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 24, 18, 24));
        pnlAvailability.setLayout(new java.awt.BorderLayout());

        lblAvailability.setFont(new java.awt.Font("Georgia", 1, 20)); // NOI18N
        lblAvailability.setForeground(new java.awt.Color(20, 18, 26));
        lblAvailability.setText("Available by type");
        lblAvailability.setPreferredSize(new java.awt.Dimension(100, 30));
        pnlAvailability.add(lblAvailability, java.awt.BorderLayout.NORTH);

        pnlAvailabilityRows.setOpaque(false);
        pnlAvailabilityRows.setLayout(new java.awt.GridLayout(3, 1, 0, 0));

        pnlSingleRow.setOpaque(false);
        pnlSingleRow.setLayout(new java.awt.BorderLayout(8, 0));
        lblSingle.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        lblSingle.setForeground(new java.awt.Color(20, 18, 26));
        lblSingle.setText("Single");
        lblSingle.setPreferredSize(new java.awt.Dimension(71, 20));
        pnlSingleRow.add(lblSingle, java.awt.BorderLayout.WEST);
        barSingle.setMaximum(8);
        barSingle.setValue(5);
        pnlSingleRow.add(barSingle, java.awt.BorderLayout.CENTER);
        lblSingleCount.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblSingleCount.setForeground(new java.awt.Color(20, 18, 26));
        lblSingleCount.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblSingleCount.setText("5 of 8");
        lblSingleCount.setPreferredSize(new java.awt.Dimension(62, 20));
        pnlSingleRow.add(lblSingleCount, java.awt.BorderLayout.EAST);
        pnlAvailabilityRows.add(pnlSingleRow);

        pnlDoubleRow.setOpaque(false);
        pnlDoubleRow.setLayout(new java.awt.BorderLayout(8, 0));
        lblDouble.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        lblDouble.setForeground(new java.awt.Color(20, 18, 26));
        lblDouble.setText("Double");
        lblDouble.setPreferredSize(new java.awt.Dimension(71, 20));
        pnlDoubleRow.add(lblDouble, java.awt.BorderLayout.WEST);
        barDouble.setMaximum(12);
        barDouble.setValue(4);
        pnlDoubleRow.add(barDouble, java.awt.BorderLayout.CENTER);
        lblDoubleCount.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblDoubleCount.setForeground(new java.awt.Color(20, 18, 26));
        lblDoubleCount.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblDoubleCount.setText("4 of 12");
        lblDoubleCount.setPreferredSize(new java.awt.Dimension(62, 20));
        pnlDoubleRow.add(lblDoubleCount, java.awt.BorderLayout.EAST);
        pnlAvailabilityRows.add(pnlDoubleRow);

        pnlDeluxeRow.setOpaque(false);
        pnlDeluxeRow.setLayout(new java.awt.BorderLayout(8, 0));
        lblDeluxe.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        lblDeluxe.setForeground(new java.awt.Color(20, 18, 26));
        lblDeluxe.setText("Deluxe");
        lblDeluxe.setPreferredSize(new java.awt.Dimension(71, 20));
        pnlDeluxeRow.add(lblDeluxe, java.awt.BorderLayout.WEST);
        barDeluxe.setMaximum(7);
        barDeluxe.setValue(3);
        pnlDeluxeRow.add(barDeluxe, java.awt.BorderLayout.CENTER);
        lblDeluxeCount.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblDeluxeCount.setForeground(new java.awt.Color(20, 18, 26));
        lblDeluxeCount.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblDeluxeCount.setText("3 of 7");
        lblDeluxeCount.setPreferredSize(new java.awt.Dimension(62, 20));
        pnlDeluxeRow.add(lblDeluxeCount, java.awt.BorderLayout.EAST);
        pnlAvailabilityRows.add(pnlDeluxeRow);

        pnlAvailability.add(pnlAvailabilityRows, java.awt.BorderLayout.CENTER);

        pnlRightColumn.add(pnlAvailability, java.awt.BorderLayout.CENTER);

        pnlLower.add(pnlRightColumn, java.awt.BorderLayout.EAST);

        pnlMain.add(pnlLower, java.awt.BorderLayout.CENTER);

        pnlBody.add(pnlMain, java.awt.BorderLayout.CENTER);

        pnlContent.add(pnlBody, java.awt.BorderLayout.CENTER);

        add(pnlContent, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void btnLogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLogoutActionPerformed
        onLogout.run();
    }//GEN-LAST:event_btnLogoutActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel avatar;
    private javax.swing.JProgressBar barDeluxe;
    private javax.swing.JProgressBar barDouble;
    private javax.swing.JProgressBar barSingle;
    private javax.swing.JPanel badgeAvailable;
    private javax.swing.JPanel badgeCleaning;
    private javax.swing.JPanel badgeMaintenance;
    private javax.swing.JPanel badgeOccupied;
    private javax.swing.JPanel badgeOutOfService;
    private javax.swing.JButton btnCheckIn;
    private javax.swing.JButton btnCheckInAction;
    private javax.swing.JButton btnCheckOut;
    private javax.swing.JButton btnCheckOutAction;
    private javax.swing.JButton btnDashboard;
    private javax.swing.JButton btnHousekeeping;
    private javax.swing.JButton btnLogout;
    private javax.swing.JButton btnManageRooms;
    private javax.swing.JButton btnNewWalkIn;
    private javax.swing.JButton btnOnlineReservations;
    private javax.swing.JButton btnRoomManagement;
    private javax.swing.JButton btnRoomMonitoring;
    private javax.swing.JButton btnWalkIn;
    private javax.swing.JLabel lblArrivals;
    private javax.swing.JLabel lblAvailability;
    private javax.swing.JLabel lblAvailableCount;
    private javax.swing.JLabel lblAvailableTitle;
    private javax.swing.JLabel lblBrand;
    private javax.swing.JLabel lblBrandSubtitle;
    private javax.swing.JLabel lblCleaningCount;
    private javax.swing.JLabel lblCleaningTitle;
    private javax.swing.JLabel lblDate;
    private javax.swing.JLabel lblDeluxe;
    private javax.swing.JLabel lblDeluxeCount;
    private javax.swing.JLabel lblDouble;
    private javax.swing.JLabel lblDoubleCount;
    private javax.swing.JLabel lblMaintenanceCount;
    private javax.swing.JLabel lblMaintenanceTitle;
    private javax.swing.JLabel lblOccupiedCount;
    private javax.swing.JLabel lblOccupiedTitle;
    private javax.swing.JLabel lblOutOfServiceCount;
    private javax.swing.JLabel lblOutOfServiceTitle;
    private javax.swing.JLabel lblQuickActions;
    private javax.swing.JLabel lblReservationTitle;
    private javax.swing.JLabel lblRole;
    private javax.swing.JLabel lblSingle;
    private javax.swing.JLabel lblSingleCount;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JLabel lblWelcome;
    private javax.swing.JPanel logoTile;
    private javax.swing.JPanel pnlAvailability;
    private javax.swing.JPanel pnlAvailabilityRows;
    private javax.swing.JPanel pnlAvailableCard;
    private javax.swing.JPanel pnlBody;
    private javax.swing.JPanel pnlBrand;
    private javax.swing.JPanel pnlCleaningCard;
    private javax.swing.JPanel pnlContent;
    private javax.swing.JPanel pnlDeluxeRow;
    private javax.swing.JPanel pnlDoubleRow;
    private javax.swing.JPanel pnlHeader;
    private javax.swing.JPanel pnlLogout;
    private javax.swing.JPanel pnlLower;
    private javax.swing.JPanel pnlMain;
    private javax.swing.JPanel pnlMaintenanceCard;
    private javax.swing.JPanel pnlNavigation;
    private javax.swing.JPanel pnlOccupiedCard;
    private javax.swing.JPanel pnlOutOfServiceCard;
    private javax.swing.JPanel pnlProfile;
    private javax.swing.JPanel pnlProfileBand;
    private javax.swing.JPanel pnlQuickActions;
    private javax.swing.JPanel pnlQuickButtons;
    private javax.swing.JPanel pnlReservations;
    private javax.swing.JPanel pnlReservationsHeader;
    private javax.swing.JPanel pnlRightColumn;
    private javax.swing.JPanel pnlSidebar;
    private javax.swing.JPanel pnlSingleRow;
    private javax.swing.JPanel pnlSummary;
    private javax.swing.JPanel pnlTopStrip;
    private javax.swing.JPanel pnlUser;
    private javax.swing.JScrollPane scrollReservations;
    private javax.swing.JTable tblReservations;
    // End of variables declaration//GEN-END:variables

    // ------------------------------------------------------------------
    // Table styling. Kept outside initComponents because the cell
    // renderers cannot be set from the Form Editor.
    // ------------------------------------------------------------------

    private void styleReservationTable() {
        tblReservations.setOpaque(false);
        tblReservations.setShowGrid(false);
        tblReservations.setIntercellSpacing(new Dimension(0, 0));
        tblReservations.setRowHeight(66);
        tblReservations.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblReservations.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        TableColumnModel columns = tblReservations.getColumnModel();
        int[] widths = {88, 180, 60, 98, 126};
        for (int i = 0; i < widths.length; i++) {
            columns.getColumn(i).setPreferredWidth(widths[i]);
        }
        columns.getColumn(0).setCellRenderer(new TextCellRenderer(true));
        columns.getColumn(1).setCellRenderer(new TextCellRenderer(false));
        columns.getColumn(2).setCellRenderer(new TextCellRenderer(true));
        columns.getColumn(3).setCellRenderer(new TextCellRenderer(false));
        columns.getColumn(4).setCellRenderer(new StatusPillRenderer());

        JTableHeader header = tblReservations.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(10, 40));
        header.setDefaultRenderer(new HeaderRenderer());

        scrollReservations.setOpaque(false);
        scrollReservations.getViewport().setOpaque(false);
        scrollReservations.setColumnHeaderView(header);
        scrollReservations.getColumnHeader().setOpaque(false);
    }

    // ------------------------------------------------------------------
    // Custom-painted components (created from the form via "custom creation
    // code"). In the Design view they show up as plain JPanel / JButton /
    // JProgressBar; the rounded look appears at runtime.
    // ------------------------------------------------------------------

    private static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    /** Sidebar icons, drawn as vectors so no image files are needed. */
    public enum Glyph {
        HOME, DOOR, GRID, SPARKLE, PERSON_PLUS, GLOBE, CHECK_IN, CHECK_OUT, POWER, PLUS
    }

    private static final class Icons {

        private Icons() {
        }

        /** Paints a glyph in a 22 x 22 design space scaled to {@code size}. */
        static void paint(Graphics2D base, Glyph glyph, float x, float y, float size, Color color) {
            Graphics2D g = (Graphics2D) base.create();
            smooth(g);
            float scale = size / 22f;
            g.translate(x, y);
            g.scale(scale, scale);
            g.setColor(color);
            g.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (glyph) {
                case HOME -> {
                    Path2D roof = new Path2D.Float();
                    roof.moveTo(3, 10.5f);
                    roof.lineTo(11, 3.5f);
                    roof.lineTo(19, 10.5f);
                    g.draw(roof);
                    Path2D walls = new Path2D.Float();
                    walls.moveTo(5.5f, 9.5f);
                    walls.lineTo(5.5f, 19);
                    walls.lineTo(16.5f, 19);
                    walls.lineTo(16.5f, 9.5f);
                    g.draw(walls);
                    Path2D door = new Path2D.Float();
                    door.moveTo(9, 19);
                    door.lineTo(9, 13);
                    door.lineTo(13, 13);
                    door.lineTo(13, 19);
                    g.draw(door);
                }
                case DOOR -> {
                    g.draw(new RoundRectangle2D.Float(6, 3, 10, 16, 3, 3));
                    g.fill(new Ellipse2D.Float(12, 10, 2.4f, 2.4f));
                }
                case GRID -> {
                    g.draw(new RoundRectangle2D.Float(3, 3, 6.5f, 6.5f, 2, 2));
                    g.draw(new RoundRectangle2D.Float(12.5f, 3, 6.5f, 6.5f, 2, 2));
                    g.draw(new RoundRectangle2D.Float(3, 12.5f, 6.5f, 6.5f, 2, 2));
                    g.draw(new RoundRectangle2D.Float(12.5f, 12.5f, 6.5f, 6.5f, 2, 2));
                }
                case SPARKLE -> {
                    Path2D star = new Path2D.Float();
                    star.moveTo(11, 2);
                    star.quadTo(12, 10, 20, 11);
                    star.quadTo(12, 12, 11, 20);
                    star.quadTo(10, 12, 2, 11);
                    star.quadTo(10, 10, 11, 2);
                    star.closePath();
                    g.draw(star);
                }
                case PERSON_PLUS -> {
                    g.draw(new Ellipse2D.Float(4.5f, 3.5f, 7, 7));
                    Path2D body = new Path2D.Float();
                    body.moveTo(2, 19);
                    body.quadTo(2, 13, 8, 13);
                    body.quadTo(14, 13, 14, 19);
                    g.draw(body);
                    g.draw(new Line2D.Float(17, 7, 17, 13));
                    g.draw(new Line2D.Float(14, 10, 20, 10));
                }
                case GLOBE -> {
                    g.draw(new Ellipse2D.Float(3, 3, 16, 16));
                    g.draw(new Ellipse2D.Float(7.4f, 3, 7.2f, 16));
                    g.draw(new Line2D.Float(3, 11, 19, 11));
                }
                case CHECK_IN -> {
                    Path2D bracket = new Path2D.Float();
                    bracket.moveTo(12, 3);
                    bracket.lineTo(18, 3);
                    bracket.lineTo(18, 19);
                    bracket.lineTo(12, 19);
                    g.draw(bracket);
                    g.draw(new Line2D.Float(2.5f, 11, 13, 11));
                    Path2D head = new Path2D.Float();
                    head.moveTo(9, 7);
                    head.lineTo(13, 11);
                    head.lineTo(9, 15);
                    g.draw(head);
                }
                case CHECK_OUT -> {
                    Path2D bracket = new Path2D.Float();
                    bracket.moveTo(10, 3);
                    bracket.lineTo(4, 3);
                    bracket.lineTo(4, 19);
                    bracket.lineTo(10, 19);
                    g.draw(bracket);
                    g.draw(new Line2D.Float(9, 11, 19.5f, 11));
                    Path2D head = new Path2D.Float();
                    head.moveTo(15.5f, 7);
                    head.lineTo(19.5f, 11);
                    head.lineTo(15.5f, 15);
                    g.draw(head);
                }
                case POWER -> {
                    g.draw(new Arc2D.Float(3.5f, 4.5f, 15, 15, 125, 290, Arc2D.OPEN));
                    g.draw(new Line2D.Float(11, 2.5f, 11, 11));
                }
                case PLUS -> {
                    g.draw(new Line2D.Float(11, 5, 11, 17));
                    g.draw(new Line2D.Float(5, 11, 17, 11));
                }
                default -> {
                }
            }
            g.dispose();
        }
    }

    /** Sidebar divider under the brand block. */
    public static final class BrandPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setColor(NAV_LINE);
            g.fillRect(24, getHeight() - 1, Math.max(0, getWidth() - 48), 1);
            g.dispose();
        }
    }

    /** Yellow rounded tile with the "H" mark. */
    public static final class LogoTile extends JPanel {

        private static final long serialVersionUID = 1L;

        public LogoTile() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            smooth(g);
            int w = getWidth();
            int h = getHeight();
            g.setColor(SUN);
            g.fillRoundRect(0, 0, w, h, 24, 24);

            g.setColor(INK);
            float dot = h * 0.135f;
            g.fill(new Ellipse2D.Float((w - dot) / 2f, h * 0.23f - dot / 2f, dot, dot));

            int size = Math.round(h * 0.46f);
            g.setFont(new Font("Georgia", Font.BOLD, size));
            FontMetrics fm = g.getFontMetrics();
            g.drawString("H", (w - fm.stringWidth("H")) / 2, Math.round(h * 0.63f + size * 0.35f));
            g.dispose();
        }
    }

    /** Yellow circle with a dark person glyph. */
    public static final class AvatarBadge extends JPanel {

        private static final long serialVersionUID = 1L;

        public AvatarBadge() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            smooth(g);
            int w = getWidth();
            int h = getHeight();
            g.setColor(SUN);
            g.fillOval(0, 0, w, h);
            g.setColor(INK);
            float head = w * 0.22f;
            g.fill(new Ellipse2D.Float((w - head) / 2f, h * 0.24f, head, head));
            float bw = w * 0.42f;
            float bh = h * 0.30f;
            g.fill(new Arc2D.Float((w - bw) / 2f, h * 0.52f, bw, bh, 0, 180, Arc2D.CHORD));
            g.dispose();
        }
    }

    /** Sidebar button: icon + label, yellow when active, outlined for log out. */
    public static final class NavButton extends JButton {

        private static final long serialVersionUID = 1L;
        public static final int ITEM = 0;
        public static final int ACTIVE = 1;
        public static final int OUTLINE = 2;

        private final Glyph glyph;
        private final int style;

        public NavButton(Glyph glyph, int style) {
            this.glyph = glyph;
            this.style = style;
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setHorizontalAlignment(LEFT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(new Font("Segoe UI", Font.PLAIN, 15));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            smooth(g);
            int w = getWidth();
            int h = getHeight();
            // 2 px of transparent margin top and bottom gives the 4 px gap between buttons
            int top = style == ACTIVE || style == ITEM ? 2 : 2;
            int bh = h - 4;
            boolean active = style == ACTIVE;
            ButtonModel model = getModel();

            if (active) {
                g.setColor(SUN);
                g.fillRoundRect(0, top, w, bh, 20, 20);
            } else if (model.isRollover() || model.isPressed()) {
                g.setColor(NAV_HOVER);
                g.fillRoundRect(0, top, w, bh, 20, 20);
            }
            if (style == OUTLINE) {
                g.setColor(NAV_LINE);
                g.setStroke(new BasicStroke(1.5f));
                g.draw(new RoundRectangle2D.Float(0.75f, top + 0.75f, w - 1.5f, bh - 1.5f, 20, 20));
            }

            Color fg = active ? INK : CREAM;
            Icons.paint(g, glyph, 16, (h - 22) / 2f, 22, fg);

            g.setFont(getFont().deriveFont(active ? Font.BOLD : Font.PLAIN));
            g.setColor(fg);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(getText(), 52, (h - fm.getHeight()) / 2 + fm.getAscent());

            Object badge = getClientProperty("badge");
            if (badge != null) {
                int pillW = 24;
                int pillH = 22;
                int px = w - 10 - pillW;
                int py = (h - pillH) / 2;
                g.setColor(active ? INK : SUN);
                g.fillRoundRect(px, py, pillW, pillH, pillH, pillH);
                g.setColor(active ? SUN : INK);
                g.setFont(new Font("Segoe UI", Font.BOLD, 13));
                fm = g.getFontMetrics();
                String text = String.valueOf(badge);
                g.drawString(text, px + (pillW - fm.stringWidth(text)) / 2, py + (pillH - fm.getHeight()) / 2 + fm.getAscent());
            }
            g.dispose();
        }
    }

    /** White rounded card with a dark outline. */
    private static final class RoundedCard extends JPanel {

        private static final long serialVersionUID = 1L;

        RoundedCard() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            smooth(g);
            int w = getWidth();
            int h = getHeight();
            g.setColor(Color.WHITE);
            g.fillRoundRect(0, 0, w, h, 28, 28);
            g.setColor(INK);
            g.setStroke(new BasicStroke(2f));
            g.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 28, 28));
            g.dispose();
        }
    }

    /**
     * Room status marker. The pattern carries the meaning, so it still reads
     * without color: solid, filled, striped or dashed.
     */
    public static final class StatusBadge extends JPanel {

        private static final long serialVersionUID = 1L;

        public enum Kind { AVAILABLE, OCCUPIED, CLEANING, MAINTENANCE, OUT_OF_SERVICE }

        private final Kind kind;

        public StatusBadge(Kind kind) {
            this.kind = kind;
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
            Dimension size = new Dimension(46, 18);
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            smooth(g);
            int w = getWidth();
            int h = getHeight();
            Shape pill = new RoundRectangle2D.Float(1, 1, w - 2, h - 2, h - 2, h - 2);

            if (kind == Kind.OUT_OF_SERVICE) {
                Color grey = new Color(150, 145, 160);
                g.setColor(grey);
                g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f, new float[]{3f, 2.5f}, 0f));
                g.draw(pill);
                g.setStroke(new BasicStroke(1.4f));
                g.draw(new Line2D.Float(w * 0.2f, h * 0.78f, w * 0.8f, h * 0.22f));
                g.dispose();
                return;
            }

            Color base;
            Color stripe = null;
            switch (kind) {
                case AVAILABLE -> base = AVAILABLE_GREEN;
                case OCCUPIED -> base = INK;
                case CLEANING -> {
                    base = Color.WHITE;
                    stripe = SUN;
                }
                default -> {
                    base = INK;
                    stripe = SUN;
                }
            }
            g.setColor(base);
            g.fill(pill);
            if (stripe != null) {
                Shape oldClip = g.getClip();
                g.clip(pill);
                g.setColor(stripe);
                g.setStroke(new BasicStroke(3.4f));
                for (int x = -h; x < w + h; x += 8) {
                    g.drawLine(x, h, x + h, 0);
                }
                g.setClip(oldClip);
            }
            g.setColor(INK);
            g.setStroke(new BasicStroke(1.6f));
            g.draw(pill);
            g.dispose();
        }
    }

    /** Quick-action button. Dark background = primary, light background = secondary. */
    private static final class ActionButton extends JButton {

        private static final long serialVersionUID = 1L;
        private final Glyph icon;

        ActionButton(Glyph icon) {
            this.icon = icon;
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            smooth(g);
            int w = getWidth();
            int h = getHeight();
            Color base = getBackground();
            boolean dark = base.getRed() + base.getGreen() + base.getBlue() < 300;
            ButtonModel model = getModel();
            Color fill = model.isPressed() ? (dark ? Color.BLACK : new Color(255, 224, 102))
                    : model.isRollover() ? (dark ? new Color(44, 43, 56) : SELECTED)
                    : base;
            g.setColor(fill);
            g.fillRoundRect(0, 0, w, h, 20, 20);
            if (!dark) {
                g.setColor(INK);
                g.setStroke(new BasicStroke(2f));
                g.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 20, 20));
            }

            g.setFont(getFont());
            g.setColor(getForeground());
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(getText());
            int iconWidth = icon != null ? 18 + 10 : 0;
            int startX = (w - (textWidth + iconWidth)) / 2;
            if (icon != null) {
                Icons.paint(g, icon, startX, (h - 18) / 2f, 18, getForeground());
            }
            g.drawString(getText(), startX + iconWidth, (h - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }

    /** Progress bar drawn as a rounded track with a yellow fill. */
    private static final class AvailabilityBar extends JProgressBar {

        private static final long serialVersionUID = 1L;

        AvailabilityBar() {
            setOpaque(false);
            setBorderPainted(false);
            setStringPainted(false);
        }

        @Override
        public void updateUI() {
            setUI(new BasicProgressBarUI()); // same look under any L&F
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            smooth(g);
            int w = getWidth();
            int barH = 16;
            int y = (getHeight() - barH) / 2;
            RoundRectangle2D.Float track = new RoundRectangle2D.Float(1, y + 1, w - 2, barH - 2, barH - 2, barH - 2);
            g.setColor(TRACK);
            g.fill(track);

            double range = Math.max(1, getMaximum() - getMinimum());
            double share = Math.max(0, Math.min(1, (getValue() - getMinimum()) / range));
            if (share > 0) {
                float fillW = (float) Math.max(barH - 2, (w - 2) * share);
                RoundRectangle2D.Float fill = new RoundRectangle2D.Float(1, y + 1, fillW, barH - 2, barH - 2, barH - 2);
                g.setColor(SUN);
                g.fill(fill);
                g.setColor(INK);
                g.setStroke(new BasicStroke(1.8f));
                g.draw(fill);
            }
            g.setColor(INK);
            g.setStroke(new BasicStroke(2f));
            g.draw(track);
            g.dispose();
        }
    }

    /** Plain table text with a thin line above every row. */
    private static final class TextCellRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;
        private final boolean bold;

        TextCellRenderer(boolean bold) {
            this.bold = bold;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, 15));
            setForeground(INK);
            setOpaque(isSelected);
            setBackground(SELECTED);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, ROW_LINE),
                    BorderFactory.createEmptyBorder(0, 4, 0, 4)));
            return this;
        }
    }

    /** Reservation status shown as a pill. */
    private static final class StatusPillRenderer extends JPanel implements TableCellRenderer {

        private static final long serialVersionUID = 1L;
        private String status = "";
        private boolean selected;

        StatusPillRenderer() {
            setOpaque(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            status = value == null ? "" : value.toString();
            selected = isSelected;
            return this;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            smooth(g);
            int w = getWidth();
            int h = getHeight();
            if (selected) {
                g.setColor(SELECTED);
                g.fillRect(0, 0, w, h);
            }
            g.setColor(ROW_LINE);
            g.fillRect(0, 0, w, 1);

            Color fill = Color.WHITE;
            Color line = INK;
            Color text = INK;
            switch (status) {
                case "Confirmed" -> fill = SUN;
                case "Checked-In" -> {
                    fill = INK;
                    text = SUN;
                }
                case "Checked-Out" -> {
                    fill = new Color(226, 220, 190);
                    line = new Color(92, 83, 104);
                }
                case "Cancelled" -> {
                    line = ERROR_RED;
                    text = ERROR_RED;
                }
                default -> {
                }
            }
            int pillW = 106;
            int pillH = 28;
            int py = (h - pillH) / 2;
            RoundRectangle2D.Float pill = new RoundRectangle2D.Float(5, py + 1, pillW - 2, pillH - 2, pillH - 2, pillH - 2);
            g.setColor(fill);
            g.fill(pill);
            g.setColor(line);
            g.setStroke(new BasicStroke(1.8f));
            g.draw(pill);

            g.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g.setColor(text);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(status, 4 + (pillW - fm.stringWidth(status)) / 2, py + (pillH - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }

    /** Small bold column titles. */
    private static final class HeaderRenderer extends JLabel implements TableCellRenderer {

        private static final long serialVersionUID = 1L;

        HeaderRenderer() {
            setOpaque(false);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setForeground(HEADER_TEXT);
            setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            setText(value == null ? "" : value.toString());
            return this;
        }
    }
}
