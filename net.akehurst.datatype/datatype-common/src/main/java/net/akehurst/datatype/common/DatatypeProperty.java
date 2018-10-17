package net.akehurst.datatype.common;

import java.lang.reflect.Method;

import net.akehurst.datatype.annotation.Datatype;
import net.akehurst.datatype.annotation.Identity;
import net.akehurst.datatype.annotation.Query;
import net.akehurst.datatype.annotation.Reference;

public class DatatypeProperty {

    private final Method accessor;

    public DatatypeProperty(final Method accessor) {
        this.accessor = accessor;
    }

    public String getName() {
        return this.accessor.getName().substring(3, 4).toLowerCase() + this.accessor.getName().substring(4);
    }

    public boolean isIdentity() {
        boolean res = true;
        res &= null != this.accessor.getDeclaringClass().getDeclaredAnnotation(Datatype.class);
        res &= null != this.accessor.getDeclaredAnnotation(Identity.class);
        res &= 0 == this.accessor.getParameters().length; // TODO: may need to change this '&& 0==m.getParameters().length' to support getters with args for e.g. maps!
        res &= this.accessor.getName().startsWith("get");
        return res;
    }

    public boolean isReference() {
        boolean res = true;
        res &= null != this.accessor.getDeclaringClass().getDeclaredAnnotation(Datatype.class);
        res &= null != this.accessor.getDeclaredAnnotation(Reference.class);
        res &= 0 == this.accessor.getParameters().length; // TODO: may need to change this '&& 0==m.getParameters().length' to support getters with args for e.g. maps!
        res &= this.accessor.getName().startsWith("get");
        return res;
    }

    private boolean isComposite() {
        boolean res = true;
        res &= null != this.accessor.getDeclaringClass().getDeclaredAnnotation(Datatype.class);
        res &= null == this.accessor.getDeclaredAnnotation(Identity.class);
        res &= null == this.accessor.getDeclaredAnnotation(Query.class);
        res &= null == this.accessor.getDeclaredAnnotation(Reference.class);
        res &= 0 == this.accessor.getParameters().length; // TODO: may need to change this '&& 0==m.getParameters().length' to support getters with args for e.g. maps!
        res &= this.accessor.getName().startsWith("get");
        return res;
    }

}
