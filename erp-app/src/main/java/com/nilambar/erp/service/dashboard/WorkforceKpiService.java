package com.nilambar.erp.service.dashboard;

import com.nilambar.erp.domain.AttendanceRecord;
import com.nilambar.erp.domain.AttendanceStatus;
import com.nilambar.erp.domain.Employee;
import com.nilambar.erp.domain.EmployeeStatus;
import com.nilambar.erp.domain.EmployeeTask;
import com.nilambar.erp.domain.TaskStatus;
import com.nilambar.erp.domain.TrainingRecord;
import com.nilambar.erp.domain.TrainingStatus;
import com.nilambar.erp.dto.dashboard.KpiCard;
import com.nilambar.erp.dto.dashboard.KpiGroup;
import com.nilambar.erp.dto.dashboard.KpiStatus;
import com.nilambar.erp.dto.dashboard.NamedValue;
import com.nilambar.erp.dto.dashboard.TrendPoint;
import com.nilambar.erp.repository.AttendanceRecordRepository;
import com.nilambar.erp.repository.EmployeeRepository;
import com.nilambar.erp.repository.EmployeeTaskRepository;
import com.nilambar.erp.repository.TrainingRecordRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Employee, attendance, task and training KPIs. */
@Service
public class WorkforceKpiService {

    private static final int WORKING_DAYS_PER_WEEK = 5;

    private final EmployeeRepository employeeRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final EmployeeTaskRepository taskRepository;
    private final TrainingRecordRepository trainingRepository;
    private final Clock clock;

    public WorkforceKpiService(EmployeeRepository employeeRepository,
                               AttendanceRecordRepository attendanceRepository,
                               EmployeeTaskRepository taskRepository,
                               TrainingRecordRepository trainingRepository,
                               Clock clock) {
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.taskRepository = taskRepository;
        this.trainingRepository = trainingRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public WorkforceMetrics metrics(int windowDays) {
        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        LocalDate from = today.minusDays(windowDays);

        List<Employee> employees = employeeRepository.findAll();
        List<Employee> active = employees.stream().filter(Employee::isActive).toList();
        List<AttendanceRecord> attendance = attendanceRepository.findByWorkDateBetween(from, today);
        List<EmployeeTask> tasks = taskRepository.findAll();
        List<TrainingRecord> trainings = trainingRepository.findAll();

        int available = (int) active.stream().filter(e -> e.getStatus() == EmployeeStatus.AVAILABLE).count();
        int allocated = (int) active.stream().filter(e -> e.getStatus() == EmployeeStatus.ALLOCATED).count();
        int onLeave = (int) active.stream().filter(e -> e.getStatus() == EmployeeStatus.ON_LEAVE).count();

        double hoursWorked = attendance.stream()
                .mapToDouble(a -> a.getHoursWorked().doubleValue())
                .sum();
        double overtime = attendance.stream()
                .mapToDouble(a -> a.getOvertimeHours().doubleValue())
                .sum();
        double capacityHours = active.stream()
                .mapToDouble(e -> e.getWeeklyCapacityHours() / (double) WORKING_DAYS_PER_WEEK)
                .sum() * countWorkingDays(from, today);
        double utilization = KpiFormat.ratio(hoursWorked - overtime, capacityHours);

        long scheduled = attendance.stream().filter(a -> a.getStatus() != AttendanceStatus.HOLIDAY).count();
        long present = attendance.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        long absent = attendance.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();

        List<EmployeeTask> tasksInWindow = tasks.stream().filter(t -> !t.getDueOn().isBefore(from)).toList();
        long completed = tasksInWindow.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        int pending = (int) tasks.stream().filter(t -> t.getStatus() != TaskStatus.COMPLETED).count();
        int overdue = (int) tasks.stream()
                .filter(t -> t.getStatus() != TaskStatus.COMPLETED && t.getDueOn().isBefore(today))
                .count();

        long trainingsCompleted = trainings.stream().filter(t -> t.getStatus() == TrainingStatus.COMPLETED).count();

        double performance = active.stream()
                .map(Employee::getPerformanceScore)
                .filter(score -> score != null)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0d);

        List<NamedValue> perTeam = productivityPerTeam(active, tasks, from);
        int shortageTeams = teamsWithoutAvailability(active).size();

        return new WorkforceMetrics(active.size(),
                available,
                allocated,
                onLeave,
                utilization,
                overtime,
                "%d allocated / %d available / %d on leave".formatted(allocated, available, onLeave),
                KpiFormat.ratio(present, scheduled),
                KpiFormat.ratio(absent, scheduled),
                performance,
                KpiFormat.ratio(completed, tasksInWindow.size()),
                KpiFormat.ratio(trainingsCompleted, trainings.size()),
                retentionRate(employees, from, today),
                pending,
                overdue,
                shortageTeams,
                perTeam,
                weeklyCompletedTasks(tasks, today, windowDays));
    }

    public KpiGroup group(WorkforceMetrics m) {
        List<KpiCard> cards = List.of(
                new KpiCard("available-employees", "Available Employees", KpiFormat.count(m.availableEmployees()),
                        "of %d active staff".formatted(m.headcount()),
                        KpiFormat.higherIsBetter(m.availableEmployees(), 3, 1)),
                new KpiCard("utilization", "Utilization Rate", KpiFormat.percent(m.utilizationRate()),
                        "worked hours vs capacity", utilizationStatus(m.utilizationRate())),
                new KpiCard("overtime", "Overtime Hours", KpiFormat.number(m.overtimeHours()) + " h",
                        "logged in the window", KpiFormat.lowerIsBetter(m.overtimeHours(), 80, 160)),
                new KpiCard("allocation", "Resource Allocation Status", m.allocationStatus(),
                        "current staffing split", KpiStatus.NEUTRAL),
                new KpiCard("team-productivity", "Productivity per Team", topTeam(m),
                        "completed tasks per person", KpiStatus.NEUTRAL),
                new KpiCard("attendance", "Attendance Rate", KpiFormat.percent(m.attendanceRate()),
                        "present vs scheduled days", KpiFormat.higherIsBetter(m.attendanceRate(), 92, 85)),
                new KpiCard("absenteeism", "Absenteeism Rate", KpiFormat.percent(m.absenteeismRate()),
                        "unplanned absence share", KpiFormat.lowerIsBetter(m.absenteeismRate(), 5, 10)),
                new KpiCard("performance", "Employee Performance Score", KpiFormat.number(m.performanceScore()) + " / 5",
                        "appraisal average", KpiFormat.higherIsBetter(m.performanceScore(), 3.5, 3)),
                new KpiCard("task-completion", "Task Completion Rate", KpiFormat.percent(m.taskCompletionRate()),
                        "%d task(s) still open".formatted(m.pendingTasks()),
                        KpiFormat.higherIsBetter(m.taskCompletionRate(), 75, 60)),
                new KpiCard("training", "Training Completion Status", KpiFormat.percent(m.trainingCompletionRate()),
                        "mandatory courses completed",
                        KpiFormat.higherIsBetter(m.trainingCompletionRate(), 80, 60)),
                new KpiCard("retention", "Employee Retention Rate", KpiFormat.percent(m.retentionRate()),
                        "staff retained over the window",
                        KpiFormat.higherIsBetter(m.retentionRate(), 90, 80)));
        return new KpiGroup("workforce", "Employees & Resources", "\uD83D\uDC68\u200D\uD83D\uDCBC", cards);
    }

    private KpiStatus utilizationStatus(double utilization) {
        if (utilization > 95d || utilization < 55d) {
            return KpiStatus.WARN;
        }
        return KpiStatus.GOOD;
    }

    private String topTeam(WorkforceMetrics m) {
        return m.productivityPerTeam().isEmpty() ? "n/a" : m.productivityPerTeam().get(0).display();
    }

    private List<NamedValue> productivityPerTeam(List<Employee> active, List<EmployeeTask> tasks, LocalDate from) {
        Map<String, Integer> headcount = new LinkedHashMap<>();
        Map<Long, String> teamOf = new LinkedHashMap<>();
        for (Employee employee : active) {
            headcount.merge(employee.getTeam(), 1, Integer::sum);
            teamOf.put(employee.getId(), employee.getTeam());
        }
        Map<String, Integer> completedPerTeam = new LinkedHashMap<>();
        for (EmployeeTask task : tasks) {
            if (task.getStatus() != TaskStatus.COMPLETED || task.getCompletedOn() == null
                    || task.getCompletedOn().isBefore(from)) {
                continue;
            }
            String team = teamOf.get(task.getEmployee().getId());
            if (team != null) {
                completedPerTeam.merge(team, 1, Integer::sum);
            }
        }
        List<NamedValue> values = new ArrayList<>();
        headcount.forEach((team, people) -> {
            double perPerson = completedPerTeam.getOrDefault(team, 0) / (double) people;
            values.add(new NamedValue(team, perPerson, "%s %s".formatted(team, KpiFormat.number(perPerson))));
        });
        values.sort(Comparator.comparingDouble(NamedValue::value).reversed());
        return values;
    }

    private List<String> teamsWithoutAvailability(List<Employee> active) {
        Map<String, Long> availablePerTeam = new LinkedHashMap<>();
        for (Employee employee : active) {
            availablePerTeam.merge(employee.getTeam(), employee.getStatus() == EmployeeStatus.AVAILABLE ? 1L : 0L,
                    Long::sum);
        }
        return availablePerTeam.entrySet().stream().filter(e -> e.getValue() == 0L).map(Map.Entry::getKey).toList();
    }

    private double retentionRate(List<Employee> employees, LocalDate from, LocalDate today) {
        long atStart = employees.stream()
                .filter(e -> !e.getHiredOn().isAfter(from))
                .filter(e -> e.getExitedOn() == null || e.getExitedOn().isAfter(from))
                .count();
        long leavers = employees.stream()
                .filter(e -> e.getExitedOn() != null && !e.getExitedOn().isBefore(from)
                        && !e.getExitedOn().isAfter(today))
                .count();
        return atStart == 0 ? 100d : KpiFormat.ratio(atStart - leavers, atStart);
    }

    private List<TrendPoint> weeklyCompletedTasks(List<EmployeeTask> tasks, LocalDate today, int windowDays) {
        int weeks = Math.max(1, windowDays / 7);
        List<TrendPoint> points = new ArrayList<>();
        for (int w = weeks - 1; w >= 0; w--) {
            LocalDate end = today.minusDays(7L * w);
            LocalDate start = end.minusDays(6);
            long completed = tasks.stream()
                    .filter(t -> t.getCompletedOn() != null && !t.getCompletedOn().isBefore(start)
                            && !t.getCompletedOn().isAfter(end))
                    .count();
            points.add(new TrendPoint("W-" + w, completed));
        }
        return points;
    }

    private long countWorkingDays(LocalDate from, LocalDate to) {
        long days = from.datesUntil(to.plusDays(1))
                .filter(d -> d.getDayOfWeek().getValue() <= WORKING_DAYS_PER_WEEK)
                .count();
        return Math.max(1, days);
    }
}
