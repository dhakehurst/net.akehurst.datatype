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

package net.akehurst.datatype.transform.json.rule;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jooq.lambda.Unchecked;
import org.json.JSONObject;

import net.akehurst.datatype.annotation.Datatype;
import net.akehurst.datatype.annotation.Identity;
import net.akehurst.datatype.annotation.Query;
import net.akehurst.datatype.annotation.Reference;
import net.akehurst.transform.binary.api.BinaryRule;
import net.akehurst.transform.binary.api.BinaryTransformer;

/**
 * The class for the LHS objects must be annotated with @Datatype.
 * Constructor arguments for the LHS object must correspond to an accessor annotated with @Identity, declared in the corresponding order to the constructor arguments.
 * The RHS object will contain an additional member named "_class" that contains the full class name of the transformed LHS object.
 *
 */
public class Datatype2HJsonObject extends JavaValue2JsonValue<Object, JSONObject> implements BinaryRule<Object, JSONObject> {

    private String getMemberName(final Method accessor) {
        return accessor.getName().substring(3, 4).toLowerCase() + accessor.getName().substring(4);
    }

    private Method getMutator(final Method accessor) {
        final String muName = "set" + accessor.getName().substring(4);
        final Class<?> type = accessor.getReturnType();
        return Unchecked.supplier(() -> accessor.getDeclaringClass().getMethod(muName, type)).get();
    }

    Object resolve(final JSONObject node, final List<String> reference) {

        return null;
    }

    private JSONObject createReferenceValue(final Object referedToObject, final BinaryTransformer transformer) {
        final JSONObject reference = new JSONObject();
        reference.reference.put("$ref", refStr);
        return reference;
    }

    @Override
    public boolean isValidForLeft2Right(final Object left) {
        if (null == left) {
            return false;
        }
        final Datatype ann = left.getClass().getAnnotation(Datatype.class);
        return null != ann;
    }

    @Override
    public boolean isValidForRight2Left(final JSONObject right) {
        if (null == right) {
            return false;
        }
        return null != right.get("_class");
    }

    @Override
    public boolean isAMatch(final Object left, final JSONObject right, final BinaryTransformer transformer) {
        final String n_left = left.getClass().getName();
        final String n_right = right.getString("_class", "<Undefined>"); // should never be undefined due to isValid check
        return Objects.equals(n_left, n_right);
    }

    @Override
    public JSONObject constructLeft2Right(final Object left, final BinaryTransformer transformer) {
        final JSONObject right = new JSONObject();
        right.add("_class", left.getClass().getName());

        for (final Method m : left.getClass().getMethods()) {
            final Identity idAnn = m.getAnnotation(Identity.class);
            if (null != idAnn) {
                final Object value = Unchecked.supplier(() -> m.invoke(left)).get();
                final Reference refAnn = m.getAnnotation(Reference.class);
                if (null == refAnn) {
                    final JsonValue memberValue = transformer.transformLeft2Right((Class<BinaryRule<Object, JsonValue>>) (Object) JavaValue2JsonValue.class,
                            value);
                    right.add(this.getMemberName(m), memberValue);
                } else {
                    final JsonObject reference = this.getReferenceFor(value, transformer);
                    right.add(this.getMemberName(m), reference);
                }
            }
        }

        return right;
    }

    @Override
    public Object constructRight2Left(final JSONObject right, final BinaryTransformer transformer) {
        final String className = right.optString("_class", "<Undefined>"); // should never be undefined due to isValid check

        final List<Class<?>> parameterTypes = new ArrayList<>();
        final List<Object> initargs = new ArrayList<>();

        final Class<?> leftClass = Unchecked.supplier(() -> Class.forName(className)).get();
        for (final Method m : leftClass.getMethods()) {
            final Identity idAnn = m.getAnnotation(Identity.class);
            if (m.getDeclaringClass() != Object.class && null != idAnn) {
                parameterTypes.add(m.getReturnType());
                final Object mv = right.get(this.getMemberName(m));
                final Object v = transformer.transformRight2Left((Class<BinaryRule<Object, Object>>) (Object) JavaValue2JsonValue.class, mv);
                initargs.add(v);
            }
        }

        final Object left = Unchecked.supplier(() -> {
            final Constructor<?> cons = Class.forName(className).getConstructor(parameterTypes.toArray(new Class<?>[parameterTypes.size()]));
            return cons.newInstance(initargs.toArray(new Object[initargs.size()]));
        }).get();

        return left;
    }

    @Override
    public void updateLeft2Right(final Object left, final JSONObject right, final BinaryTransformer transformer) {
        for (final Method m : left.getClass().getMethods()) {
            final Query queryAnn = m.getAnnotation(Query.class);
            final Identity idAnn = m.getAnnotation(Identity.class);
            // TODO: may need to change this '&& 0==m.getParameters().length' to support getters with args for e.g. maps!
            if (m.getDeclaringClass() != Object.class && null == queryAnn && null == idAnn && 0 == m.getParameters().length && m.getName().startsWith("get")) {
                final String memberName = this.getMemberName(m);
                final Object value = Unchecked.supplier(() -> m.invoke(left)).get();
                if (null != value) {
                    final Reference refAnn = m.getAnnotation(Reference.class);
                    if (null == refAnn) {
                        final Object memberValue = transformer.transformLeft2Right((Class<BinaryRule<Object, Object>>) (Object) JavaValue2JsonValue.class,
                                value);
                        right.put(this.getMemberName(m), memberValue);
                    } else {
                        final JsonObject reference = this.getReferenceFor(value, transformer);
                        right.add(this.getMemberName(m), reference);
                    }
                }
            }
        }
    }

    @Override
    public void updateRight2Left(final Object left, final JSONObject right, final BinaryTransformer transformer) {
        for (final Method m : left.getClass().getMethods()) {
            final Query queryAnn = m.getAnnotation(Query.class);
            final Identity idAnn = m.getAnnotation(Identity.class);
            if (null == queryAnn && null == idAnn && 0 == m.getParameters().length && m.getName().startsWith("get")) {
                final String memberName = this.getMemberName(m);
                final JsonValue memberValue = right.get(memberName);
                if (null != memberValue) {
                    final Object value = transformer.transformRight2Left((Class<BinaryRule<Object, JsonValue>>) (Object) JavaValue2JsonValue.class,
                            memberValue);
                    Unchecked.runnable(() -> this.getMutator(m).invoke(left, value));
                }
            }
        }

    }

}
