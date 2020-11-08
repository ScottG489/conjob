package dci.core.job.model;

import lombok.Value;

@Value
public class JobRun {
    public String output;
    public long exitCode;
}
