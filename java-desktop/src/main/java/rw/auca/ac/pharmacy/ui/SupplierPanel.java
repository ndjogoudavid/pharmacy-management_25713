package rw.auca.ac.pharmacy.ui;

import rw.auca.ac.pharmacy.application.SupplierService;
import rw.auca.ac.pharmacy.domain.Supplier;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

final class SupplierPanel extends JPanel {
    private final SupplierService service;
    private final JTextField company=UiSupport.field(18),contact=UiSupport.field(18),email=UiSupport.field(18),address=UiSupport.field(20),products=UiSupport.field(20);
    private final DefaultTableModel model=UiSupport.model("ID","Company","Contact","Email","Address","Products");
    private final JTable table=new JTable(model);private List<Supplier> rows=List.of();
    SupplierPanel(SupplierService service){super(new BorderLayout(8,8));this.service=service;UiSupport.configureTable(table);JPanel form=UiSupport.formPanel();
        UiSupport.addField(form,"Company",company,0);UiSupport.addField(form,"Contact person",contact,1);UiSupport.addField(form,"Email",email,2);UiSupport.addField(form,"Address",address,3);UiSupport.addField(form,"Products",products,4);
        JButton save=UiSupport.button("Save / update"),delete=UiSupport.button("Delete"),clear=UiSupport.button("Clear"),refresh=UiSupport.button("Refresh");form.add(UiSupport.actions(save,delete,clear,refresh),UiSupport.constraints(1,5,1,GridBagConstraints.HORIZONTAL));
        table.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting())selectRow();});save.addActionListener(e->save());delete.addActionListener(e->delete());clear.addActionListener(e->clear());refresh.addActionListener(e->reload());
        add(form,BorderLayout.NORTH);add(new JScrollPane(table),BorderLayout.CENTER);reload();}
    private void reload(){try{rows=service.list();model.setRowCount(0);for(Supplier v:rows)model.addRow(new Object[]{v.id(),v.companyName(),v.contactPerson(),v.email(),v.address(),v.products()});}catch(RuntimeException ex){UiSupport.error(this,ex);}}
    private void save(){try{int i=UiSupport.selectedModelRow(table);int id=i<0?0:rows.get(i).id();service.save(new Supplier(id,company.getText(),contact.getText(),email.getText(),address.getText(),products.getText()));clear();reload();}catch(RuntimeException ex){UiSupport.error(this,ex);}}
    private void delete(){try{int i=UiSupport.selectedModelRow(table);if(i<0)throw new IllegalArgumentException("Select a supplier first.");service.delete(rows.get(i).id());clear();reload();}catch(RuntimeException ex){UiSupport.error(this,ex);}}
    private void selectRow(){int i=UiSupport.selectedModelRow(table);if(i<0||i>=rows.size())return;Supplier v=rows.get(i);company.setText(v.companyName());contact.setText(v.contactPerson());email.setText(v.email());address.setText(v.address());products.setText(v.products());}
    private void clear(){table.clearSelection();company.setText("");contact.setText("");email.setText("");address.setText("");products.setText("");}
}
