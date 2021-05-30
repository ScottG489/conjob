package conjob.resource.admin.task;

import lombok.Value;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Value
public class ConjobConfigAccessor {
    Supplier<Long> readMethod;
    Consumer<Long> writeMethod;
}
