package com.hotel.desktop.nav;

import com.hotel.desktop.dashboard.DashboardScreen;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class SidebarPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color BACKGROUND = new Color(20, 18, 26);
    private static final Color ACCENT = new Color(255, 204, 0);
    private static final Color CREAM = new Color(233, 223, 168);

    private final JPanel navigation = new JPanel();
    private final Map<NavItem, DashboardScreen.NavButton> navButtons = new EnumMap<>(NavItem.class);
    private final Map<NavItem, String> badges = new EnumMap<>(NavItem.class);
    private final JLabel staffName = new JLabel();
    private final JLabel staffRole = new JLabel();
    private final DashboardScreen.NavButton logoutButton;

    private NavItem activeItem = NavItem.DASHBOARD;
    private Consumer<NavItem> onNavigate;
    private Runnable onLogout;

    public SidebarPanel() {
        super(new BorderLayout());
        setBackground(BACKGROUND);
        setPreferredSize(new Dimension(256, 800));

        add(createBrandPanel(), BorderLayout.NORTH);
        createNavigation();
        add(navigation, BorderLayout.CENTER);

        JPanel profile = createProfilePanel();
        logoutButton = createLogoutButton();
        profile.add(createLogoutWrapper(logoutButton), BorderLayout.SOUTH);
        add(profile, BorderLayout.SOUTH);

        setStaffName("Staff");
        setStaffRole("Receptionist");
        setBadge(NavItem.ONLINE_RESERVATIONS, "3");
    }

    public void setActive(NavItem item) {
        Objects.requireNonNull(item, "item");
        if (item == activeItem) {
            return;
        }

        NavItem previousItem = activeItem;
        activeItem = item;
        replaceNavigationButton(previousItem);
        replaceNavigationButton(item);
    }

    public void setBadge(NavItem item, String badge) {
        Objects.requireNonNull(item, "item");
        if (badge == null) {
            badges.remove(item);
        } else {
            badges.put(item, badge);
        }
        applyBadge(item);
    }

    public void setStaffName(String name) {
        staffName.setText("Welcome, " + Objects.requireNonNull(name, "name"));
    }

    public void setStaffRole(String role) {
        staffRole.setText(Objects.requireNonNull(role, "role"));
    }

    public void setOnNavigate(Consumer<NavItem> onNavigate) {
        this.onNavigate = onNavigate;
    }

    public void setOnLogout(Runnable onLogout) {
        this.onLogout = onLogout;
    }

    public DashboardScreen.NavButton getLogoutButton() {
        return logoutButton;
    }

    private JPanel createBrandPanel() {
        DashboardScreen.BrandPanel brand = new DashboardScreen.BrandPanel();
        brand.setBackground(BACKGROUND);
        brand.setPreferredSize(new Dimension(256, 93));
        brand.setLayout(new BorderLayout(11, 0));
        brand.setBorder(new EmptyBorder(23, 23, 24, 23));

        DashboardScreen.LogoTile logo = new DashboardScreen.LogoTile();
        logo.setPreferredSize(new Dimension(46, 46));
        brand.add(logo, BorderLayout.WEST);

        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Hotel Reservation");
        title.setFont(new Font("Georgia", Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        brandText.add(title);

        JLabel subtitle = new JLabel("Front desk");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(CREAM);
        brandText.add(subtitle);

        brand.add(brandText, BorderLayout.CENTER);
        return brand;
    }

    private void createNavigation() {
        navigation.setBackground(BACKGROUND);
        navigation.setBorder(new EmptyBorder(13, 14, 0, 14));
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));
        for (NavItem item : NavItem.values()) {
            DashboardScreen.NavButton button = createNavigationButton(item);
            navButtons.put(item, button);
            navigation.add(button);
        }
    }

    private DashboardScreen.NavButton createNavigationButton(NavItem item) {
        int style = item == activeItem
                ? DashboardScreen.NavButton.ACTIVE
                : DashboardScreen.NavButton.ITEM;
        DashboardScreen.NavButton button = new DashboardScreen.NavButton(item.getGlyph(), style);
        button.setText(item.getLabel());
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        button.setPreferredSize(new Dimension(228, 48));
        button.addActionListener(event -> {
            Consumer<NavItem> listener = onNavigate;
            if (listener != null) {
                listener.accept(item);
            }
        });
        applyBadge(button, badges.get(item));
        return button;
    }

    private void replaceNavigationButton(NavItem item) {
        DashboardScreen.NavButton previousButton = navButtons.get(item);
        int index = navigation.getComponentZOrder(previousButton);
        DashboardScreen.NavButton replacement = createNavigationButton(item);
        navigation.remove(index);
        navigation.add(replacement, index);
        navButtons.put(item, replacement);
        navigation.revalidate();
        navigation.repaint();
    }

    private void applyBadge(NavItem item) {
        applyBadge(navButtons.get(item), badges.get(item));
    }

    private static void applyBadge(DashboardScreen.NavButton button, String badge) {
        if (button != null) {
            button.putClientProperty("badge", badge);
        }
    }

    private JPanel createProfilePanel() {
        JPanel profile = new JPanel(new BorderLayout());
        profile.setBackground(BACKGROUND);

        JPanel band = new JPanel();
        band.setBackground(ACCENT);
        band.setPreferredSize(new Dimension(256, 15));
        profile.add(band, BorderLayout.NORTH);

        JPanel user = new JPanel(new BorderLayout(10, 0));
        user.setBackground(BACKGROUND);
        user.setBorder(new EmptyBorder(0, 28, 0, 0));
        user.setPreferredSize(new Dimension(256, 87));

        JPanel avatarWrapper = new JPanel(new GridBagLayout());
        avatarWrapper.setOpaque(false);
        Dimension avatarSize = new Dimension(36, 36);
        DashboardScreen.AvatarBadge avatar = new DashboardScreen.AvatarBadge();
        avatar.setPreferredSize(avatarSize);
        avatar.setMinimumSize(avatarSize);
        avatar.setMaximumSize(avatarSize);
        avatarWrapper.add(avatar);
        Dimension avatarColumnSize = new Dimension(36, 87);
        avatarWrapper.setPreferredSize(avatarColumnSize);
        avatarWrapper.setMinimumSize(avatarColumnSize);
        avatarWrapper.setMaximumSize(avatarColumnSize);
        user.add(avatarWrapper, BorderLayout.WEST);

        JPanel identity = new JPanel();
        identity.setOpaque(false);
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        identity.add(javax.swing.Box.createVerticalGlue());

        staffName.setFont(new Font("Segoe UI", Font.BOLD, 14));
        staffName.setForeground(Color.WHITE);
        identity.add(staffName);

        staffRole.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        staffRole.setForeground(CREAM);
        identity.add(staffRole);
        identity.add(javax.swing.Box.createVerticalGlue());

        user.add(identity, BorderLayout.CENTER);
        profile.add(user, BorderLayout.CENTER);
        return profile;
    }

    private DashboardScreen.NavButton createLogoutButton() {
        DashboardScreen.NavButton button = new DashboardScreen.NavButton(
                DashboardScreen.Glyph.POWER, DashboardScreen.NavButton.OUTLINE);
        button.setText("Log out");
        button.setPreferredSize(new Dimension(228, 48));
        button.addActionListener(event -> {
            Runnable listener = onLogout;
            if (listener != null) {
                listener.run();
            }
        });
        return button;
    }

    private static JPanel createLogoutWrapper(DashboardScreen.NavButton button) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BACKGROUND);
        wrapper.setBorder(new EmptyBorder(0, 14, 34, 14));
        wrapper.add(button, BorderLayout.CENTER);
        return wrapper;
    }
}
