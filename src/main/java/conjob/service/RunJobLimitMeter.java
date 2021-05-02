package conjob.service;

public interface RunJobLimitMeter {
    boolean isAtLimit();
    void countRun();
    void onJobComplete();
}
