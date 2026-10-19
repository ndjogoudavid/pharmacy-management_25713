package rw.auca.ac.pharmacy.ui;

import rw.auca.ac.pharmacy.application.ReportService;
import rw.auca.ac.pharmacy.domain.PharmacyReport;
import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;

final class ReportsPanel extends JPanel {
    private final ReportService service;
    private final JLabel summary=new JLabel(" ");
    ReportsPanel(ReportService service){super(new BorderLayout(12,12));this.service=service;summary.setFont(summary.getFont().deriveFont(Font.PLAIN,18f));summary.setBorder(BorderFactory.createEmptyBorder(24,24,24,24));JButton refresh=UiSupport.button("Refresh report");refresh.addActionListener(e->reload());add(summary,BorderLayout.CENTER);add(refresh,BorderLayout.SOUTH);reload();}
    private void reload(){try{PharmacyReport r=service.summarize();NumberFormat money=NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-RW"));summary.setText("<html><h2>Pharmacy summary</h2><p>Medicines: "+r.medicineCount()+"</p><p>Low-stock medicines (10 or fewer): "+r.lowStockCount()+"</p><p>Inventory value: "+money.format(r.inventoryValue())+"</p><p>Recorded sales: "+money.format(r.salesValue())+"</p></html>");}catch(RuntimeException ex){UiSupport.error(this,ex);}}
}
