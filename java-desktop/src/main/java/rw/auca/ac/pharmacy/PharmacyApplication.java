package rw.auca.ac.pharmacy;

import rw.auca.ac.pharmacy.application.*;
import rw.auca.ac.pharmacy.persistence.jdbc.*;
import rw.auca.ac.pharmacy.ui.LoginWindow;
import rw.auca.ac.pharmacy.ui.PharmacyWindow;
import javax.swing.*;

public final class PharmacyApplication {
    private PharmacyApplication() { }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                Database database=new Database();
                MedicineService medicines=new MedicineService(new JdbcMedicineRepository(database));
                CustomerService customers=new CustomerService(new JdbcCustomerRepository(database));
                SupplierService suppliers=new SupplierService(new JdbcSupplierRepository(database));
                SalesService sales=new SalesService(new JdbcSalesRepository(database));
                ReportService reports=new ReportService(new JdbcReportRepository(database));
                AuthService auth=new AuthService(new JdbcUserRepository(database));
                Runnable showLogin=new Runnable(){@Override public void run(){new LoginWindow(auth,username->new PharmacyWindow(
                        username,medicines,customers,suppliers,sales,reports,this));}};
                showLogin.run();
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(null,ex.getMessage(),"Pharmacy startup failed",JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null,"Could not start the application.","Pharmacy startup failed",JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
