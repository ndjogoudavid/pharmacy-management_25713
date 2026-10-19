package rw.auca.ac.pharmacy.ui;

import rw.auca.ac.pharmacy.application.MedicineService;
import rw.auca.ac.pharmacy.domain.Medicine;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

final class MedicinePanel extends JPanel {
    private final MedicineService service;
    private final JTextField name=UiSupport.field(18), quantity=UiSupport.field(8), price=UiSupport.field(10), category=UiSupport.field(16);
    private final DefaultTableModel model=UiSupport.model("ID","Medicine","Quantity","Price (RWF)","Category");
    private final JTable table=new JTable(model);
    private List<Medicine> rows=List.of();

    MedicinePanel(MedicineService service) {
        super(new BorderLayout(8,8)); this.service=service; UiSupport.configureTable(table);
        JPanel form=UiSupport.formPanel(); UiSupport.addField(form,"Name",name,0); UiSupport.addField(form,"Quantity",quantity,1);
        UiSupport.addField(form,"Unit price",price,2); UiSupport.addField(form,"Category",category,3);
        JButton save=UiSupport.button("Save / update"), delete=UiSupport.button("Delete"), clear=UiSupport.button("Clear"), refresh=UiSupport.button("Refresh");
        GridBagConstraints c=UiSupport.constraints(1,4,1,GridBagConstraints.HORIZONTAL); form.add(UiSupport.actions(save,delete,clear,refresh),c);
        table.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting())selectRow();});
        save.addActionListener(e->save()); delete.addActionListener(e->delete()); clear.addActionListener(e->clear()); refresh.addActionListener(e->reload());
        add(form,BorderLayout.NORTH); add(new JScrollPane(table),BorderLayout.CENTER); reload();
    }
    private void reload(){try{rows=service.list();model.setRowCount(0);for(Medicine v:rows)model.addRow(new Object[]{v.id(),v.name(),v.quantity(),v.price(),v.category()});}catch(RuntimeException ex){UiSupport.error(this,ex);}}
    private void save(){try{int i=UiSupport.selectedModelRow(table);int id=i<0?0:rows.get(i).id();service.save(new Medicine(id,name.getText(),Integer.parseInt(quantity.getText().trim()),new BigDecimal(price.getText().trim()),category.getText()));clear();reload();}catch(NumberFormatException ex){UiSupport.error(this,new IllegalArgumentException("Quantity and price must be valid numbers."));}catch(RuntimeException ex){UiSupport.error(this,ex);}}
    private void delete(){try{int i=UiSupport.selectedModelRow(table);if(i<0)throw new IllegalArgumentException("Select a medicine first.");service.delete(rows.get(i).id());clear();reload();}catch(RuntimeException ex){UiSupport.error(this,ex);}}
    private void selectRow(){int i=UiSupport.selectedModelRow(table);if(i<0||i>=rows.size())return;Medicine v=rows.get(i);name.setText(v.name());quantity.setText(String.valueOf(v.quantity()));price.setText(v.price().toPlainString());category.setText(v.category());}
    private void clear(){table.clearSelection();name.setText("");quantity.setText("");price.setText("");category.setText("");}
}
