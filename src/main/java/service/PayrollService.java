package com.motorph.service;

import com.motorph.model.Employee;
import com.motorph.model.Payslip;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Simplified monthly payslip computation. The formula is a stand-in so the
 * controls have real payroll data to protect; it is not the statutory table.
 */
@Service
public class PayrollService {

    private static final BigDecimal SSS_RATE = new BigDecimal("0.045");
    private static final BigDecimal SSS_CAP = new BigDecimal("1350.00");
    private static final BigDecimal PHILHEALTH_RATE = new BigDecimal("0.025");
    private static final BigDecimal PHILHEALTH_CAP = new BigDecimal("2500.00");
    private static final BigDecimal PAGIBIG_RATE = new BigDecimal("0.02");
    private static final BigDecimal PAGIBIG_CAP = new BigDecimal("200.00");
    private static final BigDecimal TAX_THRESHOLD = new BigDecimal("20833.00");
    private static final BigDecimal TAX_RATE = new BigDecimal("0.20");

    public Payslip compute(Employee e) {
        BigDecimal basic = e.basicSalary();
        BigDecimal gross = basic.add(e.allowance());

        BigDecimal sss = capped(basic, SSS_RATE, SSS_CAP);
        BigDecimal philhealth = capped(basic, PHILHEALTH_RATE, PHILHEALTH_CAP);
        BigDecimal pagibig = capped(basic, PAGIBIG_RATE, PAGIBIG_CAP);

        // Tax is computed after the contributions, on what is left.
        BigDecimal taxable = gross.subtract(sss).subtract(philhealth).subtract(pagibig);
        BigDecimal tax = taxable.compareTo(TAX_THRESHOLD) > 0
                ? taxable.subtract(TAX_THRESHOLD).multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2);

        BigDecimal totalDeductions = sss.add(philhealth).add(pagibig).add(tax);
        BigDecimal net = gross.subtract(totalDeductions);

        return new Payslip(e, LocalDate.now(), gross, sss, philhealth, pagibig, tax, totalDeductions, net);
    }

    private static BigDecimal capped(BigDecimal base, BigDecimal rate, BigDecimal cap) {
        return base.multiply(rate).setScale(2, RoundingMode.HALF_UP).min(cap);
    }
}
