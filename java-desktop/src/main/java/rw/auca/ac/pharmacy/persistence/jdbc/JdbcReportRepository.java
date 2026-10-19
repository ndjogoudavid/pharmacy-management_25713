package rw.auca.ac.pharmacy.persistence.jdbc;

import rw.auca.ac.pharmacy.application.port.ReportRepository;
import rw.auca.ac.pharmacy.domain.PharmacyReport;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public final class JdbcReportRepository implements ReportRepository {
    private final Database database;
    public JdbcReportRepository(Database database) { this.database = database; }
    @Override public PharmacyReport summarize() {
        return database.query(connection -> {
            long count; long lowStock; BigDecimal stockValue;
            try (PreparedStatement s = connection.prepareStatement(
                    "SELECT COUNT(*) AS item_count, COALESCE(SUM(CASE WHEN Quantity <= 10 THEN 1 ELSE 0 END),0) AS low_stock, COALESCE(SUM(Quantity * Price),0) AS stock_value FROM inventory");
                 ResultSet r = s.executeQuery()) {
                r.next(); count = r.getLong("item_count"); lowStock = r.getLong("low_stock"); stockValue = r.getBigDecimal("stock_value");
            }
            BigDecimal sales;
            try (PreparedStatement s = connection.prepareStatement("SELECT COALESCE(SUM(total_amount),0) AS sales_value FROM sales"); ResultSet r = s.executeQuery()) {
                r.next(); sales = r.getBigDecimal("sales_value");
            }
            return new PharmacyReport(count, lowStock, stockValue, sales);
        });
    }
}
