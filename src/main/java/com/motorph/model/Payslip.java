package com.motorph.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Computed payslip. Built on demand, never stored. */
public record Payslip(Employee employee, LocalDate generatedOn,
                      BigDecimal gross, BigDecimal sss, BigDecimal philhealth,
                      BigDecimal pagibig, BigDecimal tax,
                      BigDecimal totalDeductions, BigDecimal net) {
}
