package uk.gov.digital.ho.proving.income.audit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

@AllArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class ArchivedResult {

    @JsonProperty(value = "results")
    private Map<String, Integer> results;
}
