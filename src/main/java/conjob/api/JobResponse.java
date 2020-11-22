package conjob.api;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class JobResponse {
    // TODO: Rename this from since the 'job' portion is redundant. Just 'run' seems too ambiguous though.
    public JobRunResponse jobRun;
    public JobResultResponse result;
    public String message;
}
