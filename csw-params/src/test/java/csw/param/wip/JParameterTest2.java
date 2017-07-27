package csw.param.wip;

import csw.param.UnitsOfMeasure;
import csw.param.proposal2.BooleanKey;
import csw.param.proposal2.BooleanParameter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Vector;

public class JParameterTest2 {
    @Test
    public void testProposal2() {
        String encoder = "encoder";

        BooleanKey encoderKey = BooleanKey.apply(encoder);

        java.util.Vector<Boolean> v = new Vector<>();
        v.add(true);
        v.add(false);

        //Do we need this overload for java ?
        BooleanParameter p1 = encoderKey.jSet(v, UnitsOfMeasure.NoUnits$.MODULE$);
        BooleanParameter p2 = encoderKey.set(true, false).withUnits(UnitsOfMeasure.NoUnits$.MODULE$);

        Arrays.asList(p1, p2).forEach((parameter) -> {
            Assert.assertEquals(v.get(0), parameter.value(0));
            Assert.assertEquals(v.get(1), parameter.value(1));
            Assert.assertEquals(encoder, parameter.keyName());
            Assert.assertEquals(UnitsOfMeasure.NoUnits$.MODULE$, parameter.units());
        });
    }
}
