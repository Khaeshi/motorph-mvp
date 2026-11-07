package com.motorph.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.motorph.dao.EmployeeDao;
import com.motorph.model.Employee;
import com.motorph.model.Payslip;
import com.motorph.service.PayrollService;
import com.motorph.service.PayslipPdfService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The four payroll features the controls are applied to. Access rules live in
 * SecurityConfig (/employee/** and /admin/**); every feature writes an AUDIT line.
 */
@Controller
public class PayrollPagesController {

    private static final Logger audit = LoggerFactory.getLogger("AUDIT");
    private static final BigDecimal MAX_SALARY = new BigDecimal("1000000");

    private final EmployeeDao employeeDao;
    private final PayrollService payroll;
    private final PayslipPdfService pdfService;

    public PayrollPagesController(EmployeeDao employeeDao, PayrollService payroll, PayslipPdfService pdfService) {
        this.employeeDao = employeeDao;
        this.payroll = payroll;
        this.pdfService = pdfService;
    }

    public record SalaryUpdate(BigDecimal basicSalary) {}

    @GetMapping("/home")
    public String home(Authentication auth, Model model) {
        model.addAttribute("username", auth.getName());
        model.addAttribute("admin", auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));
        return "home";
    }

    /** Own payslip only: the employee comes from the session username, not from the URL. */
    @GetMapping("/employee/me")
    public String myPayslip(Authentication auth, HttpServletRequest req, Model model) {
        Employee e = ownRecord(auth);
        Payslip slip = payroll.compute(e);
        audit.info("action=PAYSLIP_VIEW actor={} employeeId={} ip={} timestamp={}",
                auth.getName(), e.id(), req.getRemoteAddr(), Instant.now());
        model.addAttribute("slip", slip);
        return "my-payslip";
    }

    @GetMapping("/employee/me/payslip.pdf")
    public ResponseEntity<byte[]> myPayslipPdf(Authentication auth, HttpServletRequest req) throws IOException {
        Employee e = ownRecord(auth);
        byte[] pdf = pdfService.generate(payroll.compute(e));
        audit.info("action=PAYSLIP_EXPORT actor={} employeeId={} ip={} timestamp={}",
                auth.getName(), e.id(), req.getRemoteAddr(), Instant.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename("payslip.pdf").build());
        headers.setCacheControl(CacheControl.noStore());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @GetMapping("/admin/employees")
    public String employees(Authentication auth, HttpServletRequest req, Model model) {
        List<Employee> all = employeeDao.findAll();
        audit.info("action=EMPLOYEE_LIST_VIEW actor={} rows={} ip={} timestamp={}",
                auth.getName(), all.size(), req.getRemoteAddr(), Instant.now());
        model.addAttribute("employees", all);
        return "admin-employees";
    }

    /**
     * The one write action. WebConfig's interceptor has already required a checker
     * (X-Checker-Id) different from the logged-in admin before this method runs.
     */
    @PostMapping("/admin/employees/{id}/salary")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateSalary(@PathVariable long id,
                                                            @RequestBody SalaryUpdate body,
                                                            Authentication auth,
                                                            HttpServletRequest req) {
        String checker = clean(req.getHeader("X-Checker-Id"));
        BigDecimal newSalary = body == null ? null : body.basicSalary();

        if (newSalary == null || newSalary.signum() <= 0 || newSalary.compareTo(MAX_SALARY) > 0) {
            return reject(HttpStatus.BAD_REQUEST, "Salary must be greater than 0 and at most 1,000,000", auth, id, checker, req);
        }
        Optional<Employee> found = employeeDao.findById(id);
        if (found.isEmpty()) {
            return reject(HttpStatus.NOT_FOUND, "Employee not found", auth, id, checker, req);
        }

        BigDecimal oldSalary = found.get().basicSalary();
        BigDecimal scaled = newSalary.setScale(2, RoundingMode.HALF_UP);
        employeeDao.updateBasicSalary(id, scaled);

        audit.info("action=SALARY_UPDATE actor={} employeeId={} oldSalary={} newSalary={} checker={} ip={} timestamp={}",
                auth.getName(), id, oldSalary.toPlainString(), scaled.toPlainString(),
                checker, req.getRemoteAddr(), Instant.now());
        return ResponseEntity.ok(Map.of("id", id, "basicSalary", scaled.toPlainString()));
    }

    private Employee ownRecord(Authentication auth) {
        return employeeDao.findByUsername(auth.getName()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No employee record linked to this account"));
    }

    private ResponseEntity<Map<String, Object>> reject(HttpStatus status, String message, Authentication auth,
                                                       long id, String checker, HttpServletRequest req) {
        audit.info("action=SALARY_UPDATE_REJECTED actor={} employeeId={} reason={} checker={} ip={} timestamp={}",
                auth.getName(), id, status.value(), checker, req.getRemoteAddr(), Instant.now());
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    /** Keeps user-supplied header text from forging extra log lines. */
    private static String clean(String s) {
        return s == null ? "none" : s.replaceAll("[\\r\\n]", "_").trim();
    }
}
