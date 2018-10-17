/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.datatype.transform.jdo.datanucleus.rule;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.jdo.JDOHelper;
import javax.jdo.metadata.ClassMetadata;
import javax.jdo.metadata.JDOMetadata;
import javax.jdo.metadata.PackageMetadata;

import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

import net.akehurst.datatype.annotation.Datatype;
import net.akehurst.datatype.annotation.Identity;
import net.akehurst.datatype.annotation.Query;
import net.akehurst.datatype.annotation.Reference;
import net.akehurst.datatype.common.DatatypeClass;
import net.akehurst.datatype.common.DatatypeProperty;
import net.akehurst.transform.binary.api.BinaryRule;
import net.akehurst.transform.binary.api.BinaryTransformer;
import net.akehurst.transform.binary.api.TransformException;

/**
 * The class for the LHS objects must be annotated with @Datatype.
 * Constructor arguments for the LHS object must correspond to an accessor annotated with @Identity, declared in the corresponding order to the constructor arguments.
 * The RHS object will contain an additional member named "_class" that contains the full class name of the transformed LHS object.
 *
 */
public class Datatype2ClassMetadata implements BinaryRule<DatatypeClass, ClassMetadata> {

    private JDOMetadata getJdoMd() {
        // TODO: create one of these in transformer
        final Map<String, String> props = null;
        return JDOHelper.getPersistenceManagerFactory(props).newMetadata();
    }

    private String getMemberName(final Method accessor) {
        return accessor.getName().substring(3, 4).toLowerCase() + accessor.getName().substring(4);
    }

    private Method getMutator(final Method accessor) {
        final String muName = "set" + accessor.getName().substring(3);
        final Class<?> type = accessor.getReturnType();
        try {
            return accessor.getDeclaringClass().getMethod(muName, type);
        } catch (NoSuchMethodException | SecurityException e) {
            return null;
        }
    }

    private List<String> createPath(final Object from, final Object to) {
        if (null == from) {
            return null;
        } else if (from == to) {
            return new ArrayList<>();
        } else {
            if (from instanceof Collection<?>) {
                final Collection<?> arr = (Collection<?>) from;
                int i = 0;
                for (final Object o : arr) {
                    final List<String> path = this.createPath(o, to);
                    if (null != path) {
                        path.add(0, Integer.toString(i));
                        return path;
                    }
                    ++i;
                }
                return null;
            } else if (this.isDatatype(from.getClass())) { // treat from as an Object
                for (final Method m : this.getNavigableMethods(from.getClass())) {
                    final Object value = RT.wrap(() -> m.invoke(from));
                    final List<String> path = this.createPath(value, to);
                    if (null != path) {
                        path.add(0, this.getMemberName(m));
                        return path;
                    }
                }
                return null;
            } else {
                return null;
            }
        }
    }

    private JsonObject getReferenceTo(final Object referedToObject, final BinaryTransformer transformer) {
        final HJsonTransformerDefault hjt = (HJsonTransformerDefault) transformer;

        final List<String> path = this.createPath(hjt.getJavaRoot(), referedToObject);
        if (null == path) {
            final JsonObject reference = new JsonObject();
            final String refStr = "<Unknown reference>";
            reference.add("$type", "Reference");
            reference.add("$ref", refStr);
            return reference;
        } else {
            final JsonObject reference = new JsonObject();
            final String refStr = "#/" + Seq.seq(path).toString("/");
            reference.add("$type", "Reference");
            reference.add("$ref", refStr);
            return reference;
        }
    }

    private JsonValue resolveReference(final List<String> path, final JsonValue from) {
        if (path.isEmpty()) {
            return from;
        } else {
            final Tuple2<Optional<String>, Seq<String>> t = Seq.seq(path).splitAtHead();
            final String head = t.v1().get();
            final List<String> tail = t.v2().toList();

            if (from.isArray()) {
                final int index = Integer.parseInt(head);
                final JsonValue v = from.asArray().get(index);
                return this.resolveReference(tail, v);
            } else if (from.isObject()) {
                final JsonObject jo = from.asObject();
                final String type = null == jo.get("$type") ? null : jo.get("$type").asString();
                if (null != type && Objects.equals("Set", type)) {
                    return this.resolveReference(path, jo.get("$elements"));
                } else if (null != type && Objects.equals("List", type)) {
                    return this.resolveReference(path, jo.get("$elements"));
                } else if (null != type && Objects.equals("Map", type)) {
                    throw new UnsupportedOperationException(); // TODO:
                } else if (null != type && Objects.equals("Enum", type)) {
                    return null;
                } else if (null != type && Objects.equals("Reference", type)) {
                    return null;
                } else {
                    final JsonValue v = from.asObject().get(head);
                    return this.resolveReference(tail, v);
                }
            } else {
                return null;
            }
        }
    }

    private JsonValue resolveReference(final JsonObject referenceObject, final BinaryTransformer transformer) {
        if (null != referenceObject.get("$ref")) {
            final String pathStr = referenceObject.get("$ref").asString();
            if (pathStr.startsWith("#/")) {
                final String pathStr2 = pathStr.substring(2);
                final List<String> path = pathStr2.isEmpty() ? Arrays.asList() : Arrays.asList(pathStr2.split("/"));
                final HJsonTransformerDefault hjt = (HJsonTransformerDefault) transformer;
                return this.resolveReference(path, hjt.getHJsonRoot());
            } else {
                // throw new TransformException("$ref is not a valid Json Path expression: " + pathStr, null);
                // TODO: need to log a warning really!
                return null;
            }
        } else {
            throw new TransformException("JsonObject is not a reference: " + referenceObject.toString(), null);
        }
    }

    private boolean isPropertyAccessor(final Method m) {
        boolean res = true;
        res &= null != m.getDeclaringClass().getDeclaredAnnotation(Datatype.class);
        res &= null == m.getDeclaredAnnotation(Identity.class);
        res &= null == m.getDeclaredAnnotation(Query.class);
        res &= 0 == m.getParameters().length; // TODO: may need to change this '&& 0==m.getParameters().length' to support getters with args for e.g. maps!
        res &= m.getName().startsWith("get");
        return res;
    }

    private Set<Method> getNavigableMethods(final Class<?> cls) {
        if (null == cls) {
            return new HashSet<>();
        } else {
            final Set<Method> result = new HashSet<>();
            final Set<Method> superclassMethods = this.getNavigableMethods(cls.getSuperclass());
            result.addAll(superclassMethods);
            for (final Class<?> intf : cls.getInterfaces()) {
                final Set<Method> interfaceMethods = this.getNavigableMethods(intf);
                result.addAll(interfaceMethods);
            }
            for (final Method m : cls.getDeclaredMethods()) {
                if (this.isNavigableAccessor(m)) {
                    result.add(m);
                }
            }
            return result;
        }
    }

    private Set<Method> getPropertyMethods(final Class<?> cls) {
        if (null == cls) {
            return new HashSet<>();
        } else {
            final Set<Method> result = new HashSet<>();
            final Set<Method> superclassMethods = this.getPropertyMethods(cls.getSuperclass());
            result.addAll(superclassMethods);
            for (final Class<?> intf : cls.getInterfaces()) {
                final Set<Method> interfaceMethods = this.getPropertyMethods(intf);
                result.addAll(interfaceMethods);
            }
            for (final Method m : cls.getDeclaredMethods()) {
                if (this.isPropertyAccessor(m)) {
                    result.add(m);
                }
            }
            return result;
        }
    }

    private void setValueRight2Left(final Object left, final Method accessor, final JsonValue rightValue, final BinaryTransformer transformer) {
        if (List.class.isAssignableFrom(accessor.getReturnType())) {
            final List leftValue = transformer.transformRight2Left((Class<BinaryRule<List, JsonValue>>) (Object) List2JsonArray.class, rightValue);
            final Method mutator = this.getMutator(accessor);
            if (null == mutator) {
                final List lv = RT.wrap(() -> (List) accessor.invoke(left));
                lv.addAll(leftValue);
            } else {
                RT.wrap(() -> mutator.invoke(left, leftValue));
            }
        } else if (Set.class.isAssignableFrom(accessor.getReturnType())) {
            final Set leftValue = transformer.transformRight2Left((Class<BinaryRule<Set, JsonValue>>) (Object) Set2JsonArray.class, rightValue);
            final Method mutator = this.getMutator(accessor);
            if (null == mutator) {
                final Set lv = RT.wrap(() -> (Set) accessor.invoke(left));
                lv.addAll(leftValue);
            } else {
                RT.wrap(() -> mutator.invoke(left, leftValue));
            }
        } else if (Map.class.isAssignableFrom(accessor.getReturnType())) {
            final Map leftValue = transformer.transformRight2Left((Class<BinaryRule<Map, JsonObject>>) (Object) Map2JsonObject.class, rightValue.asObject());
            final Method mutator = this.getMutator(accessor);
            if (null == mutator) {
                final Map lv = RT.wrap(() -> (Map) accessor.invoke(left));
                lv.putAll(leftValue);
            } else {
                RT.wrap(() -> mutator.invoke(left, leftValue));
            }
        } else {
            final Object leftValue = transformer.transformRight2Left((Class<BinaryRule<Object, JsonValue>>) (Object) Object2JsonValue.class, rightValue);
            RT.wrap(() -> this.getMutator(accessor).invoke(left, leftValue));
        }
    }

    @Override
    public boolean isValidForLeft2Right(final DatatypeClass left) {
        if (null == left) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isValidForRight2Left(final ClassMetadata right) {
        if (null == right) {
            return false;
        }
        return true; // null != right.get("$class");
    }

    @Override
    public boolean isAMatch(final DatatypeClass left, final ClassMetadata right, final BinaryTransformer transformer) {
        final String n_left = left.getClass().getName();
        final String n_right = right.getString("$class", "<Undefined>"); // should never be undefined due to isValid check
        return Objects.equals(n_left, n_right);
    }

    @Override
    public ClassMetadata constructLeft2Right(final DatatypeClass left, final BinaryTransformer transformer) {
        final PackageMetadata pmd = this.getJdoMd().newPackageMetadata(left.getPackageName());
        final ClassMetadata right = pmd.newClassMetadata(left.getName());

        for (final DatatypeProperty m : left.getIdentityProperty) {
            final Object value = RT.wrap(() -> m.invoke(left));
            final JsonValue memberValue = transformer.transformLeft2Right((Class<BinaryRule<Object, JsonValue>>) (Object) Object2JsonValue.class, value);
            final Reference refAnn = m.getAnnotation(Reference.class);
            if (null == refAnn) {
                right.add(this.getMemberName(m), memberValue);
            } else {
                final JsonObject reference = this.getReferenceTo(value, transformer);
                right.add(this.getMemberName(m), reference);
            }
        }

        return right;
    }

    @Override
    public DatatypeClass constructRight2Left(final ClassMetadata right, final BinaryTransformer transformer) {
        return null;
    }

    @Override
    public void updateLeft2Right(final DatatypeClass left, final ClassMetadata right, final BinaryTransformer transformer) {

        for (final DatatypeProperty dp : left.getPropertyIdentity()) {
        }
        for (final DatatypeProperty dp : left.getPropertyReference()) {
        }
        for (final DatatypeProperty dp : left.getPropertyComposite()) {
            final String memberName = dp.getName();
            final Object value = RT.wrap(() -> m.invoke(left));
            boolean includeIt = null != value;
            if (value instanceof Collection && ((Collection) value).isEmpty()) {
                includeIt = false;
            }
            if (includeIt) {
                final Reference refAnn = m.getAnnotation(Reference.class);
                if (null == refAnn) {
                    final JsonValue memberValue = transformer.transformLeft2Right((Class<BinaryRule<Object, JsonValue>>) (Object) Object2JsonValue.class,
                            value);
                    right.add(this.getMemberName(m), memberValue);
                } else {
                    final JsonObject reference = this.getReferenceTo(value, transformer);
                    right.add(this.getMemberName(m), reference);
                }
            }
        }
    }

    @Override
    public void updateRight2Left(final DatatypeClass left, final ClassMetadata right, final BinaryTransformer transformer) {

    }

}
