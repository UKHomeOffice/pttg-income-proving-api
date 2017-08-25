package uk.gov.digital.ho.proving.income.audit;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditEntryJpaRepository extends CrudRepository<AuditEntry, Long> {
    @Query("SELECT COUNT(audit) FROM AuditEntry audit WHERE audit.timestamp BETWEEN :startDate AND :endDate AND audit.type = :type")
    Long countEntriesBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("type") AuditEventType type);

    @Query("SELECT audit FROM AuditEntry audit WHERE audit.timestamp BETWEEN :startDate AND :endDate AND audit.type = :type")
    List<AuditEntry> getEntriesBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("type") AuditEventType type);

    @Query("SELECT new uk.gov.digital.ho.proving.income.audit.CountByUser(COUNT(audit), audit.userId) FROM AuditEntry audit WHERE audit.timestamp BETWEEN :startDate AND :endDate AND audit.type = :type GROUP BY audit.userId")
    List<CountByUser> countEntriesBetweenDatesGroupedByUser(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("type") AuditEventType type);

}