package dci.api;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class JobResponse {
    public JobRunResponse jobRun;
    public JobResultResponse result;
}
