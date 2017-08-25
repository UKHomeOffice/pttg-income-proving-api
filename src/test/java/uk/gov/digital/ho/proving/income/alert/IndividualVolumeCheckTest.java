package uk.gov.digital.ho.proving.income.alert;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.digital.ho.proving.income.audit.AuditEntryJpaRepository;
import uk.gov.digital.ho.proving.income.audit.AuditEventType;
import uk.gov.digital.ho.proving.income.audit.CountByUser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IndividualVolumeCheckTest {
    private IndividualVolumeCheck individualVolumeCheck;
    @Mock
    private AuditEntryJpaRepository repository;
    @Captor
    private ArgumentCaptor<LocalDateTime> startTimeCaptor;
    @Captor
    private ArgumentCaptor<LocalDateTime> endTimeCaptor;

    @Before
    public void before() throws Exception {
        individualVolumeCheck = new IndividualVolumeCheck(10, "dev");
    }

    @Test
    public void shouldRetrieveCountsForToday() throws Exception {
        individualVolumeCheck.check(repository);

        verify(repository).countEntriesBetweenDatesGroupedByUser(startTimeCaptor.capture(), endTimeCaptor.capture(), Mockito.eq(AuditEventType.INCOME_PROVING_FINANCIAL_STATUS_RESPONSE), Mockito.eq("dev"));

        LocalDateTime startTime = startTimeCaptor.getValue();
        LocalDateTime endTime = endTimeCaptor.getValue();

        assertThat(startTime.toLocalDate()).isEqualTo(LocalDate.now());
        assertThat(startTime.getHour()).isEqualTo(0);
        assertThat(startTime.getMinute()).isEqualTo(0);
        assertThat(startTime.getSecond()).isEqualTo(0);

        assertThat(endTime.toLocalDate()).isEqualTo(LocalDate.now().plusDays(1));
        assertThat(endTime.getHour()).isEqualTo(0);
        assertThat(endTime.getMinute()).isEqualTo(0);
        assertThat(endTime.getSecond()).isEqualTo(0);

    }

    @Test
    public void shouldBeSuspectIfAtLeastOneCountIsGreaterThanThreshold() {
        List<CountByUser> twoCountsWithOneOverThreshold = ImmutableList.of(
            new CountByUser(11, "tony"),
            new CountByUser(1, "betty")
        );
        when(repository.countEntriesBetweenDatesGroupedByUser(any(), any(), any(), any())).thenReturn(twoCountsWithOneOverThreshold);


        IndividualVolumeUsage individualVolumeUsage = individualVolumeCheck.check(repository);

        assertThat(individualVolumeUsage.isSuspect()).isTrue();
    }

    @Test
    public void shouldReturnSuspectCountsIfAtLeastOneCountIsGreaterThanThreshold() {
        List<CountByUser> twoCountsWithOneOverThreshold = ImmutableList.of(
            new CountByUser(11, "tony"),
            new CountByUser(1, "betty")
        );
        when(repository.countEntriesBetweenDatesGroupedByUser(any(), any(), any(), any())).thenReturn(twoCountsWithOneOverThreshold);


        IndividualVolumeUsage individualVolumeUsage = individualVolumeCheck.check(repository);

        assertThat(individualVolumeUsage.getCountsByUser()).hasSize(1);
        assertThat(individualVolumeUsage.getCountsByUser()).containsOnlyKeys("tony");
        assertThat(individualVolumeUsage.getCountsByUser()).containsValues(Long.valueOf(11));
    }

    @Test
    public void shouldNotBeSuspectIfCountsAllEqualToThreshold() {
        List<CountByUser> twoCountsWithBothEqualToThreshold = ImmutableList.of(
            new CountByUser(10, "tony"),
            new CountByUser(10, "betty")
        );

        when(repository.countEntriesBetweenDatesGroupedByUser(any(), any(), any(), any())).thenReturn(twoCountsWithBothEqualToThreshold);


        IndividualVolumeUsage individualVolumeUsage = individualVolumeCheck.check(repository);
        assertThat(individualVolumeUsage.isSuspect()).isFalse();
        assertThat(individualVolumeUsage.getCountsByUser()).hasSize(0);
    }

    @Test
    public void shouldNotBeSuspectIfNothingFound() {
        when(repository.countEntriesBetweenDatesGroupedByUser(any(), any(), any(), any())).thenReturn(ImmutableList.of());

        IndividualVolumeUsage individualVolumeUsage = individualVolumeCheck.check(repository);

        assertThat(individualVolumeUsage.isSuspect()).isFalse();
        assertThat(individualVolumeUsage.getCountsByUser()).hasSize(0);
    }

}
