package conjob.api;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class JobRunResponse {
    JobRunConclusionResponse conclusion;
    String output;
    long exitCode;
    String message;
}
