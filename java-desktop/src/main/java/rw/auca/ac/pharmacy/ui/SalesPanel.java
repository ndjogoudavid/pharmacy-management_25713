package rw.auca.ac.pharmacy.ui;

import rw.auca.ac.pharmacy.application.SalesService;
import rw.auca.ac.pharmacy.domain.Sale;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

final class SalesPanel extends JPanel {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final SalesService service;
    private final JTextField customer=UiSupport.field(18),medicine=UiSupport.field(18),quantity=UiSupport.field(8);
    private final DefaultTableModel model=UiSupport.model("ID","Date","Customer","Medicine","Quantity","Unit price","Total");
    private final JTable table=new JTable(model);private List<Sale> rows=List.of();
    SalesPanel(SalesService service){super(new BorderLayout(8,8));this.service=service;UiSupport.configureTable(table);JPanel form=UiSupport.formPanel();
        UiSupport.addField(form,"Customer name",customer,0);UiSupport.addField(form,"Medicine name",medicine,1);UiSupport.addField(form,"Quantity",quantity,2);
        JButton sell=UiSupport.button("Record sale"),delete=UiSupport.button("Delete sale + restock"),clear=UiSupport.button("Clear"),refresh=UiSupport.button("Refresh");form.add(UiSupport.actions(sell,delete,clear,refresh),UiSupport.constraints(1,3,1,GridBagConstraints.HORIZONTAL));
        sell.addActionListener(e->sell());delete.addActionListener(e->delete());clear.addActionListener(e->clear());refresh.addActionListener(e->reload());
        add(form,BorderLayout.NORTH);add(new JScrollPane(table),BorderLayout.CENTER);reload();}
    private void reload(){try{rows=service.list();model.setRowCount(0);for(Sale v:rows)model.addRow(new Object[]{v.id(),DATE.format(v.date()),v.customerName(),v.medicineName(),v.quantity(),v.unitPrice(),v.total()});}catch(RuntimeException ex){UiSupport.error(this,ex);}}
    private void sell(){try{service.sell(customer.getText(),medicine.getText(),Integer.parseInt(quantity.getText().trim()));clear();reload();}catch(NumberFormatException ex){UiSupport.error(this,new IllegalArgumentException("Quantity must be a whole number."));}catch(RuntimeException ex){UiSupport.error(this,ex);}}
    private void delete(){try{int i=UiSupport.selectedModelRow(table);if(i<0)throw new IllegalArgumentException("Select a sale first.");service.deleteAndRestock(rows.get(i).id());reload();}catch(RuntimeException ex){UiSupport.error(this,ex);}}
    private void clear(){customer.setText("");medicine.setText("");quantity.setText("");}
}
