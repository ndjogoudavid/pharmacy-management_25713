package rw.auca.ac.pharmacy.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

final class UiSupport {
    private UiSupport() { }
    static JTextField field(int columns) { return new JTextField(columns); }
    static JPanel formPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        return panel;
    }
    static void addField(JPanel panel, String label, JComponent input, int row) {
        GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(5, 5, 5, 5); c.anchor = GridBagConstraints.WEST;
        c.gridx = 0; c.gridy = row; panel.add(new JLabel(label), c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1; panel.add(input, c);
    }
    static JButton button(String text) { return new JButton(text); }
    static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) { @Override public boolean isCellEditable(int row, int column) { return false; } };
    }
    static void configureTable(JTable table) { table.setAutoCreateRowSorter(true); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); table.setFillsViewportHeight(true); }
    static int selectedModelRow(JTable table) { int row = table.getSelectedRow(); return row < 0 ? -1 : table.convertRowIndexToModel(row); }
    static void error(Component parent, RuntimeException ex) {
        String message = ex.getMessage() == null ? "The operation failed." : ex.getMessage();
        JOptionPane.showMessageDialog(parent, message, "Pharmacy", JOptionPane.ERROR_MESSAGE);
    }
    static JPanel actions(JButton... buttons) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JButton button : buttons) panel.add(button);
        return panel;
    }
    static GridBagConstraints constraints(int x, int y, double weightx, int fill) {
        GridBagConstraints c = new GridBagConstraints(); c.gridx=x; c.gridy=y; c.weightx=weightx; c.fill=fill; c.insets=new Insets(6,6,6,6); return c;
    }
}
