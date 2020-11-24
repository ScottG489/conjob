package conjob.core.job.model;

import lombok.Value;

@Value
public class JobRun {
    JobRunConclusion conclusion;
    String output;
    long exitCode;
}
