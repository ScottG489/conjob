package conjob.service.job;

public interface RunJobLimitMeter {
    boolean isAtLimit();

    void countRun();

    void onJobComplete();
}
