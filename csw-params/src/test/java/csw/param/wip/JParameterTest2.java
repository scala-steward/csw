package csw.param.wip;

import csw.param.UnitsOfMeasure;
import csw.param.proposal2.BooleanKey;
import csw.param.proposal2.BooleanParameter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Vector;

public class JParameterTest2 {
    @Test
    public void testProposal2() {
        String encoder = "encoder";

        BooleanKey encoderKey = BooleanKey.apply(encoder);

        java.util.Vector<Boolean> v = new Vector<>();
        v.add(true);
        v.add(false);

        BooleanParameter params = encoderKey.jSet(v, UnitsOfMeasure.NoUnits$.MODULE$);

        Assert.assertEquals(v.get(0), params.value(0));
        Assert.assertEquals(v.get(1), params.value(1));

        Assert.assertEquals(encoder, params.keyName());
        Assert.assertEquals(UnitsOfMeasure.NoUnits$.MODULE$, params.units());

    }
}
