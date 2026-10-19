package rw.auca.ac.pharmacy.domain;

public record Customer(String name, String phone, String email, String address) {
    public Customer {
        name = Medicine.required(name, "Customer name");
        phone = clean(phone);
        email = clean(email);
        address = clean(address);
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }
}
