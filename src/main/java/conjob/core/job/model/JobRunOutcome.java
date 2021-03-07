package conjob.core.job.model;

import lombok.Value;

@Value
public class JobRunOutcome {
    Long exitStatusCode;
    String output;
}
