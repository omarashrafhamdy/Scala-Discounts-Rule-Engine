CREATE TABLE Orders (
    Order_Date timestamp,
    Product_Name VARCHAR(255),
    Expiry_Date DATE,
    Quantity INT,
    Unit_Price DECIMAL(10,2),
    Channel VARCHAR(50),
    Payment_Method VARCHAR(50),
    Discount DECIMAL(4,3),
    Final_Price DECIMAL(10,2)
);

truncate table orders;

select count(*)
from orders
where discount <> 0.0