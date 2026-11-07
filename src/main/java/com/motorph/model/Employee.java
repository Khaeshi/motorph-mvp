package com.motorph.model;

import java.math.BigDecimal;

/** One row of the employee table (payroll master data). */
public record Employee(long id, String fullName, String jobTitle,
                       BigDecimal basicSalary, BigDecimal allowance) {
}
