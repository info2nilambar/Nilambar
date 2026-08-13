package com.nilambar.erp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private ProjectStatus status;

    @Column(name = "started_on", nullable = false)
    private LocalDate startedOn;

    @Column(name = "planned_end_on", nullable = false)
    private LocalDate plannedEndOn;

    @Column(name = "actual_end_on")
    private LocalDate actualEndOn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public LocalDate getStartedOn() {
        return startedOn;
    }

    public void setStartedOn(LocalDate startedOn) {
        this.startedOn = startedOn;
    }

    public LocalDate getPlannedEndOn() {
        return plannedEndOn;
    }

    public void setPlannedEndOn(LocalDate plannedEndOn) {
        this.plannedEndOn = plannedEndOn;
    }

    public LocalDate getActualEndOn() {
        return actualEndOn;
    }

    public void setActualEndOn(LocalDate actualEndOn) {
        this.actualEndOn = actualEndOn;
    }
}
