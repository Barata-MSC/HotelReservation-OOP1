package com.hotel.desktop.nav;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JPanel;

public class WorkspaceShell extends JPanel {

    private static final long serialVersionUID = 1L;

    private final SidebarPanel sidebar = new SidebarPanel();
    private final JPanel screens = new JPanel(new CardLayout());
    private final Map<NavItem, JPanel> registeredScreens = new EnumMap<>(NavItem.class);

    public WorkspaceShell() {
        super(new BorderLayout());
        sidebar.setOnNavigate(this::show);
        add(sidebar, BorderLayout.WEST);
        add(screens, BorderLayout.CENTER);
    }

    public void addScreen(NavItem item, JPanel screen) {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(screen, "screen");
        if (registeredScreens.containsKey(item)) {
            throw new IllegalArgumentException("A screen is already registered for " + item);
        }
        if (registeredScreens.containsValue(screen)) {
            throw new IllegalArgumentException("This screen panel is already registered");
        }

        registeredScreens.put(item, screen);
        JScrollPane pageScroll = new JScrollPane(
                screen,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pageScroll.setBorder(null);
        pageScroll.setMinimumSize(new java.awt.Dimension(0, 0));
        pageScroll.getViewport().setBackground(screen.getBackground());
        pageScroll.getVerticalScrollBar().setUnitIncrement(18);
        screens.add(pageScroll, item.name());
    }

    public void show(NavItem item) {
        Objects.requireNonNull(item, "item");
        if (!registeredScreens.containsKey(item)) {
            throw new IllegalArgumentException("No screen is registered for " + item);
        }

        ((CardLayout) screens.getLayout()).show(screens, item.name());
        sidebar.setActive(item);
    }

    public SidebarPanel getSidebar() {
        return sidebar;
    }
}
