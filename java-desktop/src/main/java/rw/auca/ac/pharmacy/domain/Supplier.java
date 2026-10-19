package rw.auca.ac.pharmacy.domain;

public record Supplier(int id, String companyName, String contactPerson, String email, String address, String products) {
    public Supplier {
        companyName = Medicine.required(companyName, "Company name");
        contactPerson = clean(contactPerson);
        email = clean(email);
        address = clean(address);
        products = clean(products);
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }
}
