# Scala Discounts Rule Engine
 A simple Scala Rule Engine project that applies different discount rules to transaction data and writes the results to a Oracle database. It also keeps tracking the logs data, and backups all data into CSV files.

## Overview

The Scala Discounts Rule Engine is a rules engine designed to automate discount calculations for a retail store based on specific qualifying criteria. This application reads order data from a CSV file, applies discount rules, calculates final prices, and inserts the processed data into an Oracle database. It's a versatile solution that can handle various discount scenarios commonly encountered in retail environments.

## Features

- **Discount Rules**: Implements a set of rules to determine eligibility for discounts based on factors such as product type, quantity, expiration date, and special dates.
- **Database Interaction**: Utilizes Oracle JDBC driver to connect to a database and efficiently insert processed order data for further analysis and reporting.
- **Logging Mechanisms**: Logs detailed information about rule interactions and errors to facilitate debugging and auditing.

## Implemented Discount Rules

1. **More Than 5 Qualifier Rule**:
   - Identifies orders with quantities exceeding 5 units.
   - Offers tiered discounts based on quantity: 5%, 7%, or 10% depending on the quantity sold.

2. **Cheese and Wine Qualifier Rule**:
   - Detects orders containing wine or cheese products.
   - Applies specific discounts for wine (5%) and cheese (10%) products.

3. **Less Than 30 Days to Expiry Qualifier Rule**:
   - Determines if the product in the order has less than 30 days remaining before expiration.
   - Provides a gradual discount based on the number of days remaining, starting from 1% and increasing by 1% per day, up to a maximum of 30%.

4. **Products Sold on 23rd of March Qualifier Rule**:
   - Identifies orders made on the 23rd of March.
   - Offers a special 50% discount for orders placed on this date.

5. **App Usage Qualifier Rule**:
   - Checks if the sale was made through the App.
   - Offers discounts based on quantity: 5%, 10%, or 15% depending on the number of units sold.

6. **Visa Card Usage Qualifier Rule**:
   - Determines if the order was made using a Visa card.
   - Offers a flat 5% discount for orders paid with Visa cards.

## Usage

1. **Clone Repository**: Clone this repository to your local machine.
  ```
   git clone https://github.com/omarashrafhamdy/Scala-Discounts-Rule-Engine/
   ```

2. **Import Project**: Import the project into your preferred Scala development environment.

3. **Database Configuration**:
- Update the database connection details in the code (`Singleton` object) with your Oracle database URL, username, and password.

4. **Run Application**:
- Compile and run the `main.scala` file to execute the discount calculation process.
- Ensure that the required dependencies are installed and the CSV file containing order data (`TRX1000.csv`) is available in the specified location.

5. **Verify Results**:
- Check the `OrdersWithDiscounts.csv` file for processed order data with discounts and final prices.
- Verify the `logs.txt` file for logged events and any potential errors encountered during processing.
