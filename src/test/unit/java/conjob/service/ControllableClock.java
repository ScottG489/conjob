package conjob.service;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ControllableClock extends com.codahale.metrics.Clock {
    private long tickTime;

    public void increment(long tickTime) {
        this.tickTime += tickTime;
    }

    @Override
    public long getTick() {
        return tickTime;
    }
}
