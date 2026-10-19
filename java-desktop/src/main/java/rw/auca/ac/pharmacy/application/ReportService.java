package rw.auca.ac.pharmacy.application;

import rw.auca.ac.pharmacy.application.port.ReportRepository;
import rw.auca.ac.pharmacy.domain.PharmacyReport;

public final class ReportService {
    private final ReportRepository repository;
    public ReportService(ReportRepository repository) { this.repository = repository; }
    public PharmacyReport summarize() { return repository.summarize(); }
}
