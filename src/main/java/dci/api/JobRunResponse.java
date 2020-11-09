package dci.api;

import lombok.Value;

@Value
public class JobRunResponse {
    String output;
    long exitCode;
}
