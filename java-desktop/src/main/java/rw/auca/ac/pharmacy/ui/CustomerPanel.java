package rw.auca.ac.pharmacy.ui;

import rw.auca.ac.pharmacy.application.CustomerService;
import rw.auca.ac.pharmacy.domain.Customer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

final class CustomerPanel extends JPanel {
    private final CustomerService service;
    private final JTextField name=UiSupport.field(18), phone=UiSupport.field(14), email=UiSupport.field(18), address=UiSupport.field(20);
    private final DefaultTableModel model=UiSupport.model("Name","Phone","Email","Address");
    private final JTable table=new JTable(model); private List<Customer> rows=List.of(); private String originalName;
    CustomerPanel(CustomerService service){super(new BorderLayout(8,8));this.service=service;UiSupport.configureTable(table);JPanel form=UiSupport.formPanel();
        UiSupport.addField(form,"Name",name,0);UiSupport.addField(form,"Phone",phone,1);UiSupport.addField(form,"Email",email,2);UiSupport.addField(form,"Address",address,3);
        JButton save=UiSupport.button("Save / update"),delete=UiSupport.button("Delete"),clear=UiSupport.button("Clear"),refresh=UiSupport.button("Refresh");
        form.add(UiSupport.actions(save,delete,clear,refresh),UiSupport.constraints(1,4,1,GridBagConstraints.HORIZONTAL));
        table.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting())selectRow();});
        save.addActionListener(e->save());delete.addActionListener(e->delete());clear.addActionListener(e->clear());refresh.addActionListener(e->reload());
        add(form,BorderLayout.NORTH);add(new JScrollPane(table),BorderLayout.CENTER);reload();}
    private void reload(){try{rows=service.list();model.setRowCount(0);for(Customer v:rows)model.addRow(new Object[]{v.name(),v.phone(),v.email(),v.address()});}catch(RuntimeException ex){UiSupport.error(this,ex);}}
    private void save(){try{service.save(new Customer(name.getText(),phone.getText(),email.getText(),address.getText()),originalName);clear();reload();}catch(RuntimeException ex){UiSupport.error(this,ex);}}
    private void delete(){try{if(originalName==null)throw new IllegalArgumentException("Select a customer first.");service.delete(originalName);clear();reload();}catch(RuntimeException ex){UiSupport.error(this,ex);}}
    private void selectRow(){int i=UiSupport.selectedModelRow(table);if(i<0||i>=rows.size())return;Customer v=rows.get(i);originalName=v.name();name.setText(v.name());phone.setText(v.phone());email.setText(v.email());address.setText(v.address());}
    private void clear(){originalName=null;table.clearSelection();name.setText("");phone.setText("");email.setText("");address.setText("");}
}
