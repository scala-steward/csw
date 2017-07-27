package csw.param.wip;

import csw.param.BooleanKey;
import csw.param.BooleanParameter;
import csw.param.JavaHelpers;
import csw.param.UnitsOfMeasure;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class JParameterTest {

    @Test
    public void testBooleanParameter() {
        String encoder = "encoder";
        BooleanKey encoderKey = BooleanKey.apply(encoder);

        List<Boolean> params = Arrays.asList(true, false);
        BooleanParameter p1 = JavaHelpers.jset(encoderKey, params, UnitsOfMeasure.NoUnits$.MODULE$);

        Assert.assertEquals(params.get(0), JavaHelpers.jget(p1, 0).get());
        Assert.assertEquals(params.get(1), JavaHelpers.jget(p1, 1).get());
        Assert.assertEquals(encoder, p1.keyName());
        Assert.assertEquals(UnitsOfMeasure.NoUnits$.MODULE$, p1.units());
    }
}
