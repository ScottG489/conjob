package conjob.core.job.model;

import lombok.ToString;
import lombok.Value;

@Value
public class JobRun {
    JobRunConclusion conclusion;
    @ToString.Exclude
    String output;
    long exitCode;
}
