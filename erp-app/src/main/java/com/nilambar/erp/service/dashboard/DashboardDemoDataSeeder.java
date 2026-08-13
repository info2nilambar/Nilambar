package com.nilambar.erp.service.dashboard;

import com.nilambar.erp.config.ErpProperties;
import com.nilambar.erp.domain.AttendanceRecord;
import com.nilambar.erp.domain.AttendanceStatus;
import com.nilambar.erp.domain.CustomerOrder;
import com.nilambar.erp.domain.Employee;
import com.nilambar.erp.domain.EmployeeStatus;
import com.nilambar.erp.domain.EmployeeTask;
import com.nilambar.erp.domain.FulfilmentType;
import com.nilambar.erp.domain.OrderFeedback;
import com.nilambar.erp.domain.OrderItem;
import com.nilambar.erp.domain.OrderStatus;
import com.nilambar.erp.domain.Product;
import com.nilambar.erp.domain.Project;
import com.nilambar.erp.domain.ProjectStatus;
import com.nilambar.erp.domain.PurchaseOrder;
import com.nilambar.erp.domain.PurchaseOrderStatus;
import com.nilambar.erp.domain.StockMovement;
import com.nilambar.erp.domain.StockMovementType;
import com.nilambar.erp.domain.Store;
import com.nilambar.erp.domain.TaskStatus;
import com.nilambar.erp.domain.TrainingRecord;
import com.nilambar.erp.domain.TrainingStatus;
import com.nilambar.erp.domain.User;
import com.nilambar.erp.domain.Vendor;
import com.nilambar.erp.repository.AttendanceRecordRepository;
import com.nilambar.erp.repository.EmployeeRepository;
import com.nilambar.erp.repository.EmployeeTaskRepository;
import com.nilambar.erp.repository.OrderFeedbackRepository;
import com.nilambar.erp.repository.OrderRepository;
import com.nilambar.erp.repository.ProductRepository;
import com.nilambar.erp.repository.ProjectRepository;
import com.nilambar.erp.repository.PurchaseOrderRepository;
import com.nilambar.erp.repository.StockMovementRepository;
import com.nilambar.erp.repository.StoreRepository;
import com.nilambar.erp.repository.TrainingRecordRepository;
import com.nilambar.erp.repository.UserRepository;
import com.nilambar.erp.repository.VendorRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Populates the operational history the dashboard reports on (staff, tasks, vendors, stock ledger
 * and past orders). It is deterministic and only fills tables that are still empty, so a real
 * deployment that already has data is never touched. Disable with
 * {@code erp.dashboard.seed-demo-data=false}.
 */
@Component
public class DashboardDemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DashboardDemoDataSeeder.class);

    private static final int HISTORY_DAYS = 180;
    private static final int ATTENDANCE_DAYS = 60;
    private static final String[] TEAMS = {"Warehouse", "Sales", "Support", "Logistics"};
    private static final String[] FIRST_NAMES = {"Asha", "Bikash", "Chandan", "Deepa", "Ekta", "Farhan", "Gita",
            "Harish", "Ipsita", "Jyoti", "Kiran", "Lalit", "Manoj", "Nisha", "Omkar", "Priya", "Rahul", "Sneha"};
    private static final String[] COURSES = {"Warehouse Safety", "ERP Fundamentals", "Customer Handling"};

    private final ErpProperties properties;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final EmployeeTaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TrainingRecordRepository trainingRepository;
    private final VendorRepository vendorRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final Clock clock;

    public DashboardDemoDataSeeder(ErpProperties properties,
                                   EmployeeRepository employeeRepository,
                                   AttendanceRecordRepository attendanceRepository,
                                   EmployeeTaskRepository taskRepository,
                                   ProjectRepository projectRepository,
                                   TrainingRecordRepository trainingRepository,
                                   VendorRepository vendorRepository,
                                   PurchaseOrderRepository purchaseOrderRepository,
                                   StockMovementRepository stockMovementRepository,
                                   ProductRepository productRepository,
                                   OrderRepository orderRepository,
                                   OrderFeedbackRepository feedbackRepository,
                                   UserRepository userRepository,
                                   StoreRepository storeRepository,
                                   Clock clock) {
        this.properties = properties;
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.trainingRepository = trainingRepository;
        this.vendorRepository = vendorRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.getDashboard().isSeedDemoData()) {
            return;
        }
        Random random = new Random(20240815L);
        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);

        List<Employee> employees = seedEmployees(today);
        seedAttendance(employees, today, random);
        List<Project> projects = seedProjects(today);
        seedTasks(employees, projects, today, random);
        seedTrainings(employees, random);
        seedProcurement(random, today);
        seedOrderHistory(random, today);
    }

    private List<Employee> seedEmployees(LocalDate today) {
        if (employeeRepository.count() > 0) {
            return employeeRepository.findAll();
        }
        List<Employee> employees = new ArrayList<>();
        for (int i = 0; i < FIRST_NAMES.length; i++) {
            Employee employee = new Employee();
            employee.setCode("EMP-%03d".formatted(i + 1));
            employee.setFullName(FIRST_NAMES[i] + " " + surname(i));
            employee.setTeam(TEAMS[i % TEAMS.length]);
            employee.setDesignation(i % 5 == 0 ? "Team Lead" : "Associate");
            employee.setStatus(statusFor(i));
            employee.setHiredOn(today.minusDays(200L + i * 37L));
            employee.setWeeklyCapacityHours(40);
            employee.setPerformanceScore(BigDecimal.valueOf(3.2 + (i % 9) * 0.2).setScale(2, RoundingMode.HALF_UP));
            if (employee.getStatus() == EmployeeStatus.EXITED) {
                employee.setExitedOn(today.minusDays(20L + i));
            }
            employees.add(employee);
        }
        log.info("Seeding {} demo employees for the dashboard", employees.size());
        return employeeRepository.saveAll(employees);
    }

    private EmployeeStatus statusFor(int index) {
        if (index == 4 || index == 13) {
            return EmployeeStatus.ON_LEAVE;
        }
        if (index == 17) {
            return EmployeeStatus.EXITED;
        }
        return index % 3 == 0 ? EmployeeStatus.AVAILABLE : EmployeeStatus.ALLOCATED;
    }

    private String surname(int index) {
        String[] surnames = {"Jena", "Mohanty", "Patra", "Sahoo", "Nayak", "Das"};
        return surnames[index % surnames.length];
    }

    private void seedAttendance(List<Employee> employees, LocalDate today, Random random) {
        if (attendanceRepository.count() > 0) {
            return;
        }
        List<AttendanceRecord> records = new ArrayList<>();
        for (Employee employee : employees) {
            if (!employee.isActive()) {
                continue;
            }
            for (int day = ATTENDANCE_DAYS; day >= 1; day--) {
                LocalDate date = today.minusDays(day);
                if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    continue;
                }
                AttendanceRecord record = new AttendanceRecord();
                record.setEmployee(employee);
                record.setWorkDate(date);
                double roll = random.nextDouble();
                if (roll < 0.04) {
                    record.setStatus(AttendanceStatus.ABSENT);
                    record.setHoursWorked(BigDecimal.ZERO);
                    record.setOvertimeHours(BigDecimal.ZERO);
                } else if (roll < 0.08) {
                    record.setStatus(AttendanceStatus.LEAVE);
                    record.setHoursWorked(BigDecimal.ZERO);
                    record.setOvertimeHours(BigDecimal.ZERO);
                } else {
                    double overtime = random.nextDouble() < 0.15 ? 1 + random.nextInt(2) : 0;
                    record.setStatus(AttendanceStatus.PRESENT);
                    record.setHoursWorked(BigDecimal.valueOf(7.5 + overtime).setScale(2, RoundingMode.HALF_UP));
                    record.setOvertimeHours(BigDecimal.valueOf(overtime).setScale(2, RoundingMode.HALF_UP));
                }
                records.add(record);
            }
        }
        attendanceRepository.saveAll(records);
    }

    private List<Project> seedProjects(LocalDate today) {
        if (projectRepository.count() > 0) {
            return projectRepository.findAll();
        }
        List<Project> projects = new ArrayList<>();
        projects.add(project("PRJ-001", "Warehouse re-layout", ProjectStatus.IN_PROGRESS, today.minusDays(70),
                today.minusDays(5), null));
        projects.add(project("PRJ-002", "Vendor consolidation", ProjectStatus.IN_PROGRESS, today.minusDays(40),
                today.plusDays(20), null));
        projects.add(project("PRJ-003", "Festive campaign", ProjectStatus.COMPLETED, today.minusDays(120),
                today.minusDays(60), today.minusDays(52)));
        projects.add(project("PRJ-004", "Last-mile routing", ProjectStatus.PLANNED, today.minusDays(10),
                today.plusDays(45), null));
        projects.add(project("PRJ-005", "Support playbook refresh", ProjectStatus.ON_HOLD, today.minusDays(90),
                today.plusDays(10), null));
        return projectRepository.saveAll(projects);
    }

    private Project project(String code, String name, ProjectStatus status, LocalDate start, LocalDate plannedEnd,
                            LocalDate actualEnd) {
        Project project = new Project();
        project.setCode(code);
        project.setName(name);
        project.setStatus(status);
        project.setStartedOn(start);
        project.setPlannedEndOn(plannedEnd);
        project.setActualEndOn(actualEnd);
        return project;
    }

    private void seedTasks(List<Employee> employees, List<Project> projects, LocalDate today, Random random) {
        if (taskRepository.count() > 0) {
            return;
        }
        List<Employee> active = employees.stream().filter(Employee::isActive).toList();
        List<EmployeeTask> tasks = new ArrayList<>();
        for (int i = 0; i < 140; i++) {
            Employee employee = active.get(random.nextInt(active.size()));
            LocalDate due = today.minusDays(random.nextInt(ATTENDANCE_DAYS)).plusDays(random.nextInt(10));
            EmployeeTask task = new EmployeeTask();
            task.setEmployee(employee);
            task.setProject(projects.get(random.nextInt(projects.size())));
            task.setTitle("%s task #%d".formatted(employee.getTeam(), i + 1));
            task.setDueOn(due);
            task.setEstimatedHours(BigDecimal.valueOf(2 + random.nextInt(6)).setScale(2, RoundingMode.HALF_UP));
            double roll = random.nextDouble();
            if (roll < 0.72) {
                task.setStatus(TaskStatus.COMPLETED);
                task.setCompletedOn(due.minusDays(random.nextInt(3)));
            } else if (roll < 0.85) {
                task.setStatus(TaskStatus.IN_PROGRESS);
            } else if (roll < 0.95) {
                task.setStatus(TaskStatus.PENDING);
            } else {
                task.setStatus(TaskStatus.BLOCKED);
            }
            tasks.add(task);
        }
        taskRepository.saveAll(tasks);
    }

    private void seedTrainings(List<Employee> employees, Random random) {
        if (trainingRepository.count() > 0) {
            return;
        }
        List<TrainingRecord> records = new ArrayList<>();
        for (Employee employee : employees) {
            if (!employee.isActive()) {
                continue;
            }
            for (String course : COURSES) {
                TrainingRecord record = new TrainingRecord();
                record.setEmployee(employee);
                record.setCourseName(course);
                double roll = random.nextDouble();
                if (roll < 0.78) {
                    record.setStatus(TrainingStatus.COMPLETED);
                    record.setCompletedOn(employee.getHiredOn().plusDays(30 + random.nextInt(60)));
                } else if (roll < 0.9) {
                    record.setStatus(TrainingStatus.IN_PROGRESS);
                } else {
                    record.setStatus(TrainingStatus.NOT_STARTED);
                }
                records.add(record);
            }
        }
        trainingRepository.saveAll(records);
    }

    private void seedProcurement(Random random, LocalDate today) {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            return;
        }
        if (vendorRepository.count() == 0) {
            vendorRepository.saveAll(List.of(
                    vendor("Meghna Distributors", "sales@meghna.example", 45),
                    vendor("Kalinga Supplies", "orders@kalinga.example", 120),
                    vendor("Utkal Traders", "contact@utkal.example", 210),
                    vendor("Sagar Wholesale", "hello@sagar.example", 90)));
        }
        List<Vendor> vendors = vendorRepository.findAll();

        if (purchaseOrderRepository.count() == 0) {
            List<PurchaseOrder> purchaseOrders = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                Product product = products.get(random.nextInt(products.size()));
                Instant ordered = instantAt(today.minusDays(5L + random.nextInt(HISTORY_DAYS - 10)));
                Instant expected = ordered.plusSeconds((3 + random.nextInt(5)) * 86400L);
                PurchaseOrder po = new PurchaseOrder();
                po.setPoNumber("PO-%05d".formatted(1000 + i));
                po.setVendor(vendors.get(random.nextInt(vendors.size())));
                po.setProduct(product);
                po.setQuantity(20 + random.nextInt(80));
                po.setUnitCost(product.getUnitCost());
                po.setOrderedAt(ordered);
                po.setExpectedAt(expected);
                double roll = random.nextDouble();
                if (roll < 0.85) {
                    po.setStatus(PurchaseOrderStatus.RECEIVED);
                    long drift = random.nextDouble() < 0.25 ? (1 + random.nextInt(4)) * 86400L : -43200L;
                    po.setReceivedAt(expected.plusSeconds(drift));
                } else if (roll < 0.95) {
                    po.setStatus(PurchaseOrderStatus.PENDING);
                } else {
                    po.setStatus(PurchaseOrderStatus.CANCELLED);
                }
                purchaseOrders.add(po);
            }
            purchaseOrderRepository.saveAll(purchaseOrders);

            List<StockMovement> movements = new ArrayList<>();
            for (PurchaseOrder po : purchaseOrders) {
                if (po.getStatus() != PurchaseOrderStatus.RECEIVED) {
                    continue;
                }
                movements.add(movement(po.getProduct(), StockMovementType.IN, po.getQuantity(), po.getUnitCost(),
                        po.getReceivedAt(), po.getPoNumber()));
                Product product = po.getProduct();
                if (product.getLastRestockedAt() == null || product.getLastRestockedAt().isBefore(po.getReceivedAt())) {
                    product.setLastRestockedAt(po.getReceivedAt());
                }
            }
            stockMovementRepository.saveAll(movements);
            productRepository.saveAll(products);
        }
    }

    private Vendor vendor(String name, String email, int responseMinutes) {
        Vendor vendor = new Vendor();
        vendor.setName(name);
        vendor.setContactEmail(email);
        vendor.setActive(true);
        vendor.setAvgResponseMinutes(responseMinutes);
        return vendor;
    }

    /**
     * Historical orders (with their stock ledger entries and feedback) so that revenue, margin and
     * demand series have something to trend over.
     */
    private void seedOrderHistory(Random random, LocalDate today) {
        if (orderRepository.count() > 0) {
            return;
        }
        List<Product> products = productRepository.findAll();
        List<Store> stores = storeRepository.findAll();
        if (products.isEmpty() || stores.isEmpty()) {
            return;
        }

        List<User> customers = new ArrayList<>();
        for (int i = 1; i <= 24; i++) {
            String mobile = "90000000%02d".formatted(i);
            if (userRepository.findByMobile(mobile).isPresent()) {
                continue;
            }
            User user = new User();
            user.setMobile(mobile);
            user.setName("Demo Customer " + i);
            user.setEmail("customer%02d@example.com".formatted(i));
            user.setProfileCompleted(true);
            user.setCreatedAt(instantAt(today.minusDays(HISTORY_DAYS - i * 5L)));
            customers.add(user);
        }
        if (customers.isEmpty()) {
            return;
        }
        customers = userRepository.saveAll(customers);

        List<CustomerOrder> orders = new ArrayList<>();
        List<StockMovement> movements = new ArrayList<>();
        List<OrderFeedback> feedback = new ArrayList<>();
        for (int day = HISTORY_DAYS; day >= 0; day--) {
            LocalDate date = today.minusDays(day);
            // Demand grows slowly over time and dips at weekends.
            double seasonality = date.getDayOfWeek().getValue() >= 6 ? 0.6 : 1.0;
            double growth = 1 + (HISTORY_DAYS - day) / (double) HISTORY_DAYS;
            int ordersToday = (int) Math.round((1 + random.nextInt(3)) * seasonality * growth);
            for (int o = 0; o < ordersToday; o++) {
                Instant placedAt = instantAt(date).plusSeconds(random.nextInt(43200));
                CustomerOrder order = new CustomerOrder();
                order.setOrderNumber("SEED-%s-%02d".formatted(date, o));
                order.setUser(customers.get(random.nextInt(customers.size())));
                order.setStore(stores.get(random.nextInt(stores.size())));
                order.setFulfilmentType(FulfilmentType.STORE_PICKUP);
                order.setDistanceKm(1 + random.nextDouble() * 4);
                order.setCreatedAt(placedAt);
                order.setUpdatedAt(placedAt);
                order.setPaymentReference("SEEDPAY-" + Math.abs(random.nextLong() % 1_000_000));

                BigDecimal subtotal = BigDecimal.ZERO;
                int lines = 1 + random.nextInt(3);
                for (int l = 0; l < lines; l++) {
                    Product product = products.get(random.nextInt(products.size()));
                    int quantity = 1 + random.nextInt(3);
                    OrderItem item = new OrderItem();
                    item.setProduct(product);
                    item.setProductName(product.getName());
                    item.setUnitPrice(product.getPrice());
                    item.setQuantity(quantity);
                    item.setLineTotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
                    order.addItem(item);
                    subtotal = subtotal.add(item.getLineTotal());
                    movements.add(movement(product, StockMovementType.OUT, quantity, product.getUnitCost(),
                            placedAt, order.getOrderNumber()));
                }
                order.setSubtotal(subtotal);
                order.setDeliveryFee(BigDecimal.ZERO);
                order.setTotal(subtotal);
                order.setStatus(seedStatus(day, random));
                orders.add(order);
            }
        }
        List<CustomerOrder> saved = orderRepository.saveAll(orders);
        stockMovementRepository.saveAll(movements);

        for (CustomerOrder order : saved) {
            if (order.getStatus() != OrderStatus.PICKED_UP || random.nextDouble() > 0.35) {
                continue;
            }
            OrderFeedback item = new OrderFeedback();
            item.setOrder(order);
            item.setRating(3 + random.nextInt(3));
            item.setComment(null);
            item.setCreatedAt(order.getCreatedAt().plusSeconds(86400));
            feedback.add(item);
        }
        feedbackRepository.saveAll(feedback);
        log.info(String.format(Locale.ENGLISH, "Seeded %d demo orders spanning %d days", saved.size(), HISTORY_DAYS));
    }

    private OrderStatus seedStatus(int daysAgo, Random random) {
        if (daysAgo <= 1) {
            return random.nextDouble() < 0.5 ? OrderStatus.PLACED : OrderStatus.CONFIRMED;
        }
        double roll = random.nextDouble();
        if (roll < 0.05) {
            return OrderStatus.CANCELLED;
        }
        if (roll < 0.12) {
            return OrderStatus.READY_FOR_PICKUP;
        }
        return OrderStatus.PICKED_UP;
    }

    private StockMovement movement(Product product, StockMovementType type, int quantity, BigDecimal unitCost,
                                   Instant occurredAt, String reference) {
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setMovementType(type);
        movement.setQuantity(quantity);
        movement.setUnitCost(unitCost);
        movement.setOccurredAt(occurredAt);
        movement.setReference(reference);
        return movement;
    }

    private Instant instantAt(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
