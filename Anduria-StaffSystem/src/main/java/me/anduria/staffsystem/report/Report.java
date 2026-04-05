package me.anduria.staffsystem.report;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * POJO reprezentujici jeden report.
 * Obsahuje vsechna data ktera se ukladaji do YAML nebo MySQL.
 */
public class Report {

    private final UUID reporterUuid;
    private final String reporterName;
    private final UUID targetUuid;
    private final String targetName;
    private final String category;
    private final LocalDateTime createdAt;

    public Report(
            UUID reporterUuid,
            String reporterName,
            UUID targetUuid,
            String targetName,
            String category,
            LocalDateTime createdAt
    ) {
        this.reporterUuid = reporterUuid;
        this.reporterName = reporterName;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.category = category;
        this.createdAt = createdAt;
    }

    public UUID getReporterUuid() { return reporterUuid; }
    public String getReporterName() { return reporterName; }
    public UUID getTargetUuid() { return targetUuid; }
    public String getTargetName() { return targetName; }
    public String getCategory() { return category; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return "Report{reporter='" + reporterName + "', target='" + targetName +
               "', category='" + category + "', at=" + createdAt + "}";
    }
}