package dci.core.job.model;

import lombok.Value;

@Value
public class Job {
    public JobRun jobRun;
    public JobResult result;
}
