//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2007-2009 Openbravo, S.L.

package com.openbravo.data.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

/** A confirmation dialog with contextual actions and safe keyboard defaults. */
public final class JConfirmationDialog {

    private JConfirmationDialog() {
    }

    public static int show(Component parent, String message, String title, String safeAction,
            String primaryAction, boolean destructive) {
        return show(parent, message, title, new String[] {safeAction, primaryAction}, 0,
                destructive);
    }

    public static int show(Component parent, String message, String title, String safeAction,
            String secondaryAction, String primaryAction, boolean destructive) {
        return show(parent, message, title,
                new String[] {safeAction, secondaryAction, primaryAction}, 0, destructive);
    }

    private static int show(Component parent, String message, String title, String[] actions,
            int safeIndex, boolean destructive) {
        JOptionPane pane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, actions, actions[safeIndex]);
        JDialog dialog = pane.createDialog(parent, title);
        JButton safeButton = findButton(pane, actions[safeIndex]);
        if (safeButton != null) {
            dialog.getRootPane().setDefaultButton(safeButton);
        }
        if (destructive) {
            setDestructiveButton(pane, actions[actions.length - 1]);
        }
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);

        Object value = pane.getValue();
        if (value == null || value == JOptionPane.UNINITIALIZED_VALUE) {
            return actions.length == 2 ? JOptionPane.NO_OPTION : JOptionPane.CANCEL_OPTION;
        }
        for (int i = 0; i < actions.length; i++) {
            if (actions[i].equals(value)) {
                if (i == actions.length - 1) {
                    return JOptionPane.YES_OPTION;
                }
                return actions.length == 3 && i == 0 ? JOptionPane.CANCEL_OPTION : JOptionPane.NO_OPTION;
            }
        }
        return JOptionPane.CANCEL_OPTION;
    }

    private static JButton findButton(Container container, String action) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton && action.equals(((JButton) component).getText())) {
                return (JButton) component;
            }
            if (component instanceof Container) {
                JButton button = findButton((Container) component, action);
                if (button != null) {
                    return button;
                }
            }
        }
        return null;
    }

    private static void setDestructiveButton(Container container, String action) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton && action.equals(((JButton) component).getText())) {
                ((JButton) component).setForeground(new Color(170, 0, 0));
            } else if (component instanceof Container) {
                setDestructiveButton((Container) component, action);
            }
        }
    }
}
