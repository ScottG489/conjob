package conjob.api;

import lombok.Value;

@Value
public class JobRunResponse {
    JobRunConclusionResponse conclusion;
    String output;
    long exitCode;
    String message;
}
