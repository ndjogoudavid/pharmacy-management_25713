package rw.auca.ac.pharmacy.ui;

import rw.auca.ac.pharmacy.application.*;
import javax.swing.*;
import java.awt.*;

public final class PharmacyWindow extends JFrame {
    public PharmacyWindow(String username, MedicineService medicines, CustomerService customers,
                          SupplierService suppliers, SalesService sales, ReportService reports, Runnable logout){
        super("Pharmacy Management — "+username);setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);setMinimumSize(new Dimension(900,600));
        JTabbedPane tabs=new JTabbedPane();tabs.addTab("Inventory",new MedicinePanel(medicines));tabs.addTab("Customers",new CustomerPanel(customers));
        tabs.addTab("Suppliers",new SupplierPanel(suppliers));tabs.addTab("Sales",new SalesPanel(sales));tabs.addTab("Reports",new ReportsPanel(reports));
        JButton signOut=new JButton("Sign out");signOut.addActionListener(e->{dispose();logout.run();});JPanel footer=new JPanel(new FlowLayout(FlowLayout.RIGHT));footer.add(signOut);
        add(tabs,BorderLayout.CENTER);add(footer,BorderLayout.SOUTH);setLocationRelativeTo(null);setVisible(true);
    }
}
