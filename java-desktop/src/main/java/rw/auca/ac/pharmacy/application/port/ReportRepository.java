package rw.auca.ac.pharmacy.application.port;

import rw.auca.ac.pharmacy.domain.PharmacyReport;

public interface ReportRepository {
    PharmacyReport summarize();
}
