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

package net.akehurst.datatype.transform.hjson.rule;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

import net.akehurst.datatype.annotation.Datatype;
import net.akehurst.datatype.annotation.Identity;
import net.akehurst.datatype.annotation.Query;
import net.akehurst.datatype.annotation.Reference;
import net.akehurst.datatype.transform.hjson.HJsonTransformerDefault;
import net.akehurst.transform.binary.api.BinaryRule;
import net.akehurst.transform.binary.api.BinaryTransformer;

/**
 * The class for the LHS objects must be annotated with @Datatype.
 * Constructor arguments for the LHS object must correspond to an accessor annotated with @Identity, declared in the corresponding order to the constructor arguments.
 * The RHS object will contain an additional member named "_class" that contains the full class name of the transformed LHS object.
 *
 */
public class Datatype2HJsonObject extends Object2JsonValue<Object, JsonObject> implements BinaryRule<Object, JsonObject> {

    private String getMemberName(final Method accessor) {
        return accessor.getName().substring(3, 4).toLowerCase() + accessor.getName().substring(4);
    }

    private Method getMutator(final Method accessor) {
        final String muName = "set" + accessor.getName().substring(3);
        final Class<?> type = accessor.getReturnType();
        return RT.wrap(() -> accessor.getDeclaringClass().getMethod(muName, type));
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
            } else if (null != from.getClass().getAnnotation(Datatype.class)) { // treat from as an Object
                for (final Method m : from.getClass().getMethods()) {
                    final Query queryAnn = m.getAnnotation(Query.class);
                    final Reference refAnn = m.getAnnotation(Reference.class);
                    if (m.getDeclaringClass() != Object.class && null == queryAnn && null == refAnn && 0 == m.getParameters().length
                            && m.getName().startsWith("get")) {
                        final Object value = RT.wrap(() -> m.invoke(from));
                        final List<String> path = this.createPath(value, to);
                        if (null != path) {
                            path.add(0, this.getMemberName(m));
                            return path;
                        }
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
            reference.add("$ref", refStr);
            return reference;
        } else {
            final JsonObject reference = new JsonObject();
            final String refStr = "#/" + Seq.seq(path).toString("/");
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
                final JsonValue v = from.asObject().get(head);
                return this.resolveReference(tail, v);
            } else {
                return null;
            }
        }
    }

    private JsonValue resolveReference(final JsonObject referenceObject, final BinaryTransformer transformer) {
        final List<String> path = Arrays.asList(referenceObject.get("$ref").asString().substring(2).split("/"));
        final HJsonTransformerDefault hjt = (HJsonTransformerDefault) transformer;
        return this.resolveReference(path, hjt.getHJsonRoot());
    }

    private boolean isDatatype(final Class<?> cls) {
        final Datatype ann = cls.getAnnotation(Datatype.class);
        if (null == ann) {
            // check interfaces, superclasses are included because @Datatype is marked as @Inherited
            for (final Class<?> intf : cls.getInterfaces()) {
                if (this.isDatatype(intf)) {
                    return true;
                }
            }
            // check interfaces of superclass
            return this.isDatatype(cls.getSuperclass());
        } else {
            return true;
        }
    }

    private boolean isIdentityAccessor(final Method m) {
        boolean res = true;
        res &= null != m.getDeclaringClass().getAnnotation(Datatype.class);
        res &= null != m.getAnnotation(Identity.class);
        return res;
    }

    private boolean isPropertyAccessor(final Method m) {
        return false;
    }

    @Override
    public boolean isValidForLeft2Right(final Object left) {
        if (null == left) {
            return false;
        }
        return this.isDatatype(left.getClass());
    }

    @Override
    public boolean isValidForRight2Left(final JsonObject right) {
        if (null == right) {
            return false;
        }
        return null != right.get("$class");
    }

    @Override
    public boolean isAMatch(final Object left, final JsonObject right, final BinaryTransformer transformer) {
        final String n_left = left.getClass().getName();
        final String n_right = right.getString("$class", "<Undefined>"); // should never be undefined due to isValid check
        return Objects.equals(n_left, n_right);
    }

    @Override
    public JsonObject constructLeft2Right(final Object left, final BinaryTransformer transformer) {
        final JsonObject right = new JsonObject();
        right.add("$class", left.getClass().getName());

        for (final Method m : left.getClass().getMethods()) {
            final Identity idAnn = m.getAnnotation(Identity.class);
            if (null != idAnn) {
                final Object value = RT.wrap(() -> m.invoke(left));
                final JsonValue memberValue = transformer.transformLeft2Right((Class<BinaryRule<Object, JsonValue>>) (Object) Object2JsonValue.class, value);
                final Reference refAnn = m.getAnnotation(Reference.class);
                if (null == refAnn) {
                    right.add(this.getMemberName(m), memberValue);
                } else {
                    final JsonObject reference = this.getReferenceTo(memberValue, transformer);
                    right.add(this.getMemberName(m), reference);
                }
            }
        }

        return right;
    }

    @Override
    public Object constructRight2Left(final JsonObject right, final BinaryTransformer transformer) {
        final String className = right.getString("$class", "<Undefined>"); // should never be undefined due to isValid check

        final List<Class<?>> parameterTypes = new ArrayList<>();
        final List<Object> initargs = new ArrayList<>();

        final Class<?> leftClass = RT.wrap(() -> Class.forName(className));
        for (final Method m : leftClass.getMethods()) {
            final Identity idAnn = m.getAnnotation(Identity.class);
            if (m.getDeclaringClass() != Object.class && null != idAnn && null != m.getDeclaringClass().getAnnotation(Datatype.class)) {
                parameterTypes.add(m.getReturnType());
                final JsonValue mv = right.get(this.getMemberName(m));
                final Reference refAnn = m.getAnnotation(Reference.class);
                if (null == refAnn) { // not a reference
                    final Object v = transformer.transformRight2Left((Class<BinaryRule<Object, JsonValue>>) (Object) Object2JsonValue.class, mv);
                    initargs.add(v);
                } else {
                    final JsonValue rv = this.resolveReference(mv.asObject(), transformer);
                }
            }
        }

        final Object left = RT.wrap(() -> {
            final Constructor<?> cons = Class.forName(className).getConstructor(parameterTypes.toArray(new Class<?>[parameterTypes.size()]));
            return cons.newInstance(initargs.toArray(new Object[initargs.size()]));
        });

        return left;
    }

    @Override
    public void updateLeft2Right(final Object left, final JsonObject right, final BinaryTransformer transformer) {
        for (final Method m : left.getClass().getMethods()) {
            final Query queryAnn = m.getAnnotation(Query.class);
            final Identity idAnn = m.getAnnotation(Identity.class);
            // TODO: may need to change this '&& 0==m.getParameters().length' to support getters with args for e.g. maps!
            if (m.getDeclaringClass() != Object.class && null != m.getDeclaringClass().getAnnotation(Datatype.class) && null == queryAnn && null == idAnn
                    && 0 == m.getParameters().length && m.getName().startsWith("get")) {
                final String memberName = this.getMemberName(m);
                final Object value = RT.wrap(() -> m.invoke(left));
                if (null != value) {
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
    }

    @Override
    public void updateRight2Left(final Object left, final JsonObject right, final BinaryTransformer transformer) {
        for (final Method m : left.getClass().getMethods()) {
            final Query queryAnn = m.getAnnotation(Query.class);
            final Identity idAnn = m.getAnnotation(Identity.class);
            if (null == queryAnn && null == idAnn && null != m.getDeclaringClass().getAnnotation(Datatype.class) && 0 == m.getParameters().length
                    && m.getName().startsWith("get")) {
                final String memberName = this.getMemberName(m);
                final JsonValue memberValue = right.get(memberName);
                if (null != memberValue) {
                    final Reference refAnn = m.getAnnotation(Reference.class);
                    if (null == refAnn) { // not a reference
                        if (List.class.isAssignableFrom(m.getReturnType())) {
                            final Object value = transformer.transformRight2Left((Class<BinaryRule<List, JsonValue>>) (Object) List2JsonArray.class,
                                    memberValue);
                            RT.wrap(() -> this.getMutator(m).invoke(left, value));
                        } else if (Set.class.isAssignableFrom(m.getReturnType())) {
                            final Object value = transformer.transformRight2Left((Class<BinaryRule<Set, JsonValue>>) (Object) Set2JsonArray.class, memberValue);
                            RT.wrap(() -> this.getMutator(m).invoke(left, value));
                        } else if (Map.class.isAssignableFrom(m.getReturnType())) {
                            final Object value = transformer.transformRight2Left((Class<BinaryRule<Map, JsonObject>>) (Object) Map2JsonObject.class,
                                    memberValue.asObject());
                            RT.wrap(() -> this.getMutator(m).invoke(left, value));
                        } else {
                            final Object value = transformer.transformRight2Left((Class<BinaryRule<Object, JsonValue>>) (Object) Object2JsonValue.class,
                                    memberValue);
                            RT.wrap(() -> this.getMutator(m).invoke(left, value));
                        }
                    } else {
                        final JsonValue rv = this.resolveReference(memberValue.asObject(), transformer);
                        if (List.class.isAssignableFrom(m.getReturnType())) {
                            final Object value = transformer.transformRight2Left((Class<BinaryRule<List, JsonValue>>) (Object) List2JsonArray.class, rv);
                            RT.wrap(() -> this.getMutator(m).invoke(left, value));
                        } else if (Set.class.isAssignableFrom(m.getReturnType())) {
                            final Object value = transformer.transformRight2Left((Class<BinaryRule<Set, JsonValue>>) (Object) Set2JsonArray.class, rv);
                            RT.wrap(() -> this.getMutator(m).invoke(left, value));
                        } else if (Map.class.isAssignableFrom(m.getReturnType())) {
                            final Object value = transformer.transformRight2Left((Class<BinaryRule<Map, JsonObject>>) (Object) Map2JsonObject.class,
                                    rv.asObject());
                            RT.wrap(() -> this.getMutator(m).invoke(left, value));
                        } else {
                            final Object value = transformer.transformRight2Left((Class<BinaryRule<Object, JsonValue>>) (Object) Object2JsonValue.class, rv);
                            RT.wrap(() -> this.getMutator(m).invoke(left, value));
                        }
                    }
                }
            }
        }

    }

}
