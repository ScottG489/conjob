package conjob.service;

import lombok.Value;

@Value
public class JobRunOutcome {
    Long exitStatusCode;
    String output;
}
