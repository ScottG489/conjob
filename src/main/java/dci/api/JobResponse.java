package dci.api;

import lombok.Value;

@Value
public class JobResponse {
    public JobRunResponse jobRun;
    public JobResultResponse result;
}
