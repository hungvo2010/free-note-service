package sdk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StringCondition implements SingleCondition {
    private static final Logger log = LogManager.getLogger(StringCondition.class);
    private final String value;
    private final boolean isInvert;
    private final StringOperator operator;
    private final String expectKey;

    public StringCondition(boolean isInvert, String expectKey, String value, StringOperator operator) {
        this.isInvert = isInvert;
        this.operator = operator;
        this.expectKey = expectKey;
        this.value = value;
    }

    @Override
    public boolean evaluate(CompositeInput input) {
        log.debug("Evaluating StringCondition: key='{}', value='{}', operator='{}', isInvert='{}'", expectKey, value, operator, isInvert);
        return this.operator.evaluate(this.value, input.getValue(this.expectKey).toString()) != this.isInvert;
    }
}
