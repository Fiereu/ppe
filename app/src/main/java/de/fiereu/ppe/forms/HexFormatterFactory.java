package de.fiereu.ppe.forms;

import javax.swing.*;

public class HexFormatterFactory extends JFormattedTextField.AbstractFormatterFactory {
    @Override
    public JFormattedTextField.AbstractFormatter getFormatter(JFormattedTextField tf) {
        return new JFormattedTextField.AbstractFormatter() {
            @Override
            public Object stringToValue(String text) {
                try {
                    return Integer.parseInt(text, 16);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }

            @Override
            public String valueToString(Object value) {
                return Integer.toString((Integer) value, 16).toUpperCase();
            }
        };
    }
}
