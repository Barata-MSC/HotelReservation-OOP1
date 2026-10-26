package com.hotel.desktop.auth;

import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.AuthenticatedUser;
import com.hotel.core.service.AuthenticationService;
import com.hotel.desktop.ui.StaffUi;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

public final class CustomerSignUpPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(CustomerSignUpPanel.class.getName());

    private final AuthenticationService authenticationService;
    private final Consumer<AuthenticatedUser> onRegistered;
    private final Runnable onCancel;
    private final JTextField fullNameField = StaffUi.textField(24);
    private final JTextField emailField = StaffUi.textField(24);
    private final JTextField contactField = StaffUi.textField(24);
    private final JPasswordField passwordField = StaffUi.passwordField(24);
    private final JPasswordField confirmPasswordField = StaffUi.passwordField(24);
    private final JLabel feedback = StaffUi.feedbackLabel();
    private final JButton createAccountButton = StaffUi.primaryButton("Create account");
    private final JButton backButton = StaffUi.secondaryButton("Back to sign in");

    public CustomerSignUpPanel(
            AuthenticationService authenticationService,
            Consumer<AuthenticatedUser> onRegistered,
            Runnable onCancel) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "authenticationService");
        this.onRegistered = Objects.requireNonNull(onRegistered, "onRegistered");
        this.onCancel = Objects.requireNonNull(onCancel, "onCancel");
        buildContent();
    }

    public JButton getDefaultButton() {
        return createAccountButton;
    }

    public void reset() {
        fullNameField.setText("");
        emailField.setText("");
        contactField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
        setBusy(false);
        StaffUi.showFeedback(feedback, "Create a customer account to continue.", false);
    }

    private void buildContent() {
        JPanel body = StaffUi.createPage(this, "Customer sign up", "Create your Okada hotel account");
        JPanel card = StaffUi.card();

        JPanel fields = new JPanel();
        fields.setOpaque(false);
        fields.setLayout(new javax.swing.BoxLayout(fields, javax.swing.BoxLayout.Y_AXIS));
        addField(fields, "Full name", fullNameField);
        addField(fields, "Email", emailField);
        addField(fields, "Contact number", contactField);
        addField(fields, "Password (10-128 characters)", passwordField);
        addField(fields, "Confirm password", confirmPasswordField);
        card.add(fields, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        footer.add(feedback, BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        backButton.addActionListener(event -> onCancel.run());
        createAccountButton.addActionListener(event -> submit());
        actions.add(backButton);
        actions.add(createAccountButton);
        footer.add(actions, BorderLayout.SOUTH);
        card.add(footer, BorderLayout.SOUTH);
        body.add(card, BorderLayout.CENTER);
        reset();
    }

    private static void addField(JPanel fields, String label, java.awt.Component input) {
        fields.add(StaffUi.fieldLabel(label));
        fields.add(javax.swing.Box.createVerticalStrut(4));
        input.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 42));
        fields.add(input);
        fields.add(javax.swing.Box.createVerticalStrut(10));
    }

    private void submit() {
        char[] password = passwordField.getPassword();
        char[] confirmation = confirmPasswordField.getPassword();
        passwordField.setText("");
        confirmPasswordField.setText("");
        if (!Arrays.equals(password, confirmation)) {
            clear(password, confirmation);
            StaffUi.showFeedback(feedback, "The passwords do not match.", true);
            return;
        }

        String fullName = fullNameField.getText();
        String email = emailField.getText();
        String contact = contactField.getText();
        setBusy(true);
        StaffUi.showFeedback(feedback, "Creating your account...", false);
        new SwingWorker<AuthenticatedUser, Void>() {
            @Override
            protected AuthenticatedUser doInBackground() throws SQLException {
                try {
                    return authenticationService.registerCustomer(fullName, email, contact, password);
                } finally {
                    clear(password, confirmation);
                }
            }

            @Override
            protected void done() {
                clear(password, confirmation);
                try {
                    onRegistered.accept(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    setBusy(false);
                    StaffUi.showFeedback(feedback, "Sign up was interrupted. Please try again.", true);
                } catch (ExecutionException exception) {
                    setBusy(false);
                    showFailure(exception.getCause());
                }
            }
        }.execute();
    }

    private void showFailure(Throwable cause) {
        if (cause instanceof ValidationException validationException) {
            StaffUi.showFeedback(feedback, validationException.getMessage(), true);
        } else if (cause instanceof SQLException sqlException) {
            LOGGER.warning("Customer sign-up database failure (SQLState="
                    + sqlException.getSQLState() + ", code=" + sqlException.getErrorCode() + ").");
            StaffUi.showFeedback(feedback, "Unable to create the account. Check the database and try again.", true);
        } else {
            String errorType = cause == null ? "Unknown" : cause.getClass().getSimpleName();
            LOGGER.severe("Unexpected customer sign-up failure: " + errorType);
            StaffUi.showFeedback(feedback, "Unable to create the account. Please try again.", true);
        }
    }

    private void setBusy(boolean busy) {
        createAccountButton.setEnabled(!busy);
        backButton.setEnabled(!busy);
        fullNameField.setEnabled(!busy);
        emailField.setEnabled(!busy);
        contactField.setEnabled(!busy);
        passwordField.setEnabled(!busy);
        confirmPasswordField.setEnabled(!busy);
    }

    private static void clear(char[]... secrets) {
        for (char[] secret : secrets) {
            Arrays.fill(secret, '\0');
        }
    }
}
