package net.akehurst.datatype.common;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import net.akehurst.datatype.annotation.Datatype;

public class DatatypeClass {

    public static boolean isDatatype(final Class<?> cls) {
        if (null == cls) {
            return false;
        }
        final Datatype ann = cls.getAnnotation(Datatype.class);
        if (null == ann) {
            // check interfaces, superclasses are included because @Datatype is marked as @Inherited
            for (final Class<?> intf : cls.getInterfaces()) {
                if (DatatypeClass.isDatatype(intf)) {
                    return true;
                }
            }
            // check interfaces of superclass
            return DatatypeClass.isDatatype(cls.getSuperclass());
        } else {
            return true;
        }
    }

    private final Class<?> class_;
    private Set<DatatypeProperty> propertyIdentity_cache;
    private Set<DatatypeProperty> propertyReference_cache;
    private Set<DatatypeProperty> propertyComposite_cache;

    public DatatypeClass(final Class<?> class_) {
        this.class_ = class_;
    }

    public String getPackageName() {
        return this.class_.getPackage().getName();
    }

    public String getName() {
        return this.class_.getSimpleName();
    }

    public Set<DatatypeProperty> getPropertyIdentity() {
        if (null == this.propertyIdentity_cache) {
            if (null == this.class_) {
                this.propertyIdentity_cache = new HashSet<>();
            } else {
                this.propertyIdentity_cache = new HashSet<>();
                final Set<DatatypeProperty> superclassMethods = new DatatypeClass(this.class_.getSuperclass()).getPropertyIdentity();
                this.propertyIdentity_cache.addAll(superclassMethods);
                for (final Class<?> intf : this.class_.getInterfaces()) {
                    final Set<DatatypeProperty> interfaceMethods = new DatatypeClass(intf).getPropertyIdentity();
                    this.propertyIdentity_cache.addAll(interfaceMethods);
                }
                for (final Method m : this.class_.getDeclaredMethods()) {
                    final DatatypeProperty dp = new DatatypeProperty(m);
                    if (dp.isIdentity()) {
                        this.propertyIdentity_cache.add(dp);
                    }
                }
            }
        }
        return this.propertyIdentity_cache;
    }

    public Set<DatatypeProperty> getPropertyReference() {
        if (null == this.propertyReference_cache) {
        }
        return this.propertyReference_cache;
    }

    public Set<DatatypeProperty> getPropertyComposite() {
        if (null == this.propertyComposite_cache) {
        }
        return this.propertyComposite_cache;
    }
}
