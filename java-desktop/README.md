# Pharmacy Management Desktop

A small Java Swing application organized in four layers:

```text
ui -> application -> domain
          ^
          | ports
persistence (PostgreSQL adapters)
```

The Swing screens contain no SQL. Application services own use cases and validation, repository interfaces define persistence needs, and JDBC adapters own SQL and transaction boundaries. The existing `inventory`, `customers`, `suppliers`, `sales`, and `users` tables remain the expected database contract.

## Requirements

- JDK 21+
- PostgreSQL with the existing `inventory_management` schema

Set `PHARMACY_DB_URL`, `PHARMACY_DB_USER`, and `PHARMACY_DB_PASSWORD` in the environment. The URL defaults to the local `inventory_management` database. The password is required; no credentials are stored in source.

PowerShell example:

```powershell
$env:PHARMACY_DB_URL = 'jdbc:postgresql://localhost:5432/inventory_management'
$env:PHARMACY_DB_USER = 'postgres'
$env:PHARMACY_DB_PASSWORD = '<your local password>'
mvn package
java -jar target/pharmacy-management-desktop-1.0.0.jar
```

The database must have the columns used by the original application: `inventory(ID, Name, Quantity, Price, Category)`, `customers(Name, Phone, Email, Address)`, `suppliers(ID, Company_Name, Contact_Person, Email, Address, Products)`, `sales(id, sale_date, customer_name, medicine_name, quantity, unit_price, total_amount)`, and `users(username, password)`. The `users.password` column must fit at least 100 characters for upgraded hashes. Existing customer identity remains name-based because the prior application did not establish a customer ID column. Existing user passwords are upgraded to PBKDF2 after a successful legacy plaintext login; add a unique constraint to customer names before relying on name-based editing in a multi-user deployment.

Sales are written with a row-locked inventory decrement in one transaction. Deleting a sale reverses its quantity only when the corresponding medicine still exists. The database table shape is preserved; `medicine_name` remains the sale reference to match existing records.

## Layer map

- `domain`: immutable records and business rules
- `application`: use cases and repository ports
- `persistence.jdbc`: connection management and PostgreSQL implementations
- `ui`: Swing windows and table/form interaction
- `PharmacyApplication`: dependency wiring and startup

The older NetBeans project remains in `../finalexam-main` for reference while this Maven replacement is reviewed.
