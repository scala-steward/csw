package csw.param.wip;

import csw.param.Parameter;

import java.io.Serializable;
import java.util.Vector;

abstract public class JKey<S, I extends Parameter<S>> implements Serializable {
    public abstract I set(Vector<S> v);
//        public I set(java.util.Vector<S> v); // = key.set(v.asScala.toVector, units)
        }