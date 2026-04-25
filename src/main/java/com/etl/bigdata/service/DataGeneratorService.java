package com.etl.bigdata.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Random;

/**
 * Generates sample CSV data for testing.
 * Creates realistic employee records with configurable count.
 */
@Service
public class DataGeneratorService {

    private static final String[] FIRST_NAMES = {
        "Ahmed", "Mohamed", "Ali", "Omar", "Hassan", "Youssef", "Mahmoud", "Mostafa",
        "Fatma", "Nour", "Sara", "Mona", "Hana", "Layla", "Amira", "Dina",
        "John", "Jane", "Robert", "Emily", "Michael", "Sarah", "David", "Lisa",
        "Carlos", "Maria", "James", "Anna", "Daniel", "Sofia", "Andrew", "Emma"
    };

    private static final String[] LAST_NAMES = {
        "Ibrahim", "Hassan", "Ahmed", "Ali", "Mohamed", "Mahmoud", "Youssef", "Khalil",
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
        "Rodriguez", "Martinez", "Anderson", "Taylor", "Thomas", "Moore", "Jackson", "Lee"
    };

    private static final String[] CITIES = {
        "Cairo", "Alexandria", "Giza", "Luxor", "Aswan", "Mansoura", "Tanta",
        "New York", "London", "Paris", "Berlin", "Tokyo", "Dubai", "Toronto",
        "Sydney", "Mumbai", "Singapore", "Amsterdam", "Madrid", "Rome"
    };

    private static final String[] COUNTRIES = {
        "Egypt", "USA", "UK", "France", "Germany", "Japan", "UAE",
        "Canada", "Australia", "India", "Singapore", "Netherlands", "Spain", "Italy"
    };

    private static final String[] DEPARTMENTS = {
        "Engineering", "Marketing", "Sales", "HR", "Finance",
        "Operations", "IT", "Research", "Legal", "Support"
    };

    private static final String[] EMAIL_DOMAINS = {
        "gmail.com", "yahoo.com", "outlook.com", "company.com", "work.org"
    };

    public byte[] generateCsv(int recordCount) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos))) {
            // Header
            writer.println("record_id,first_name,last_name,email,phone,city,country,salary,department");

            Random random = new Random(42); // fixed seed for reproducibility

            for (int i = 1; i <= recordCount; i++) {
                String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
                String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
                String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + i + "@" + EMAIL_DOMAINS[random.nextInt(EMAIL_DOMAINS.length)];
                String phone = "+20" + (100000000 + random.nextInt(900000000));
                String city = CITIES[random.nextInt(CITIES.length)];
                String country = COUNTRIES[random.nextInt(COUNTRIES.length)];
                double salary = 20000 + random.nextInt(180000) + random.nextDouble() * 100;
                String department = DEPARTMENTS[random.nextInt(DEPARTMENTS.length)];

                // Introduce some bad data (~5%)
                if (random.nextInt(100) < 2) email = "invalid-email";
                if (random.nextInt(100) < 1) firstName = "";
                if (random.nextInt(100) < 2) salary = -salary;
                if (random.nextInt(100) < 1) phone = "abc";

                writer.printf("%d,%s,%s,%s,%s,%s,%s,%.2f,%s%n",
                        i, firstName, lastName, email, phone, city, country, salary, department);
            }
        }
        return baos.toByteArray();
    }
}
