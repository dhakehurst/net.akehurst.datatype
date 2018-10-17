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

import java.util.Objects;

import org.hjson.JsonObject;

import net.akehurst.transform.binary.api.BinaryTransformer;

public class Enum2JsonValue extends Object2JsonValue<Enum<?>, JsonObject> {

    @Override
    public boolean isValidForLeft2Right(final Enum<?> left) {
        return true;
    }

    @Override
    public boolean isValidForRight2Left(final JsonObject right) {
        return Objects.equals("Enum", right.getString("$type", ""));
    }

    @Override
    public boolean isAMatch(final Enum<?> left, final JsonObject right, final BinaryTransformer transformer) {
        return Objects.equals(left.getClass().getName() + "." + left.toString(), right.asString());
    }

    @Override
    public JsonObject constructLeft2Right(final Enum<?> left, final BinaryTransformer transformer) {
        final JsonObject right = new JsonObject();
        right.add("$type", "Enum");
        right.add("$class", left.getClass().getName());
        right.add("$value", left.toString());
        return right;
    }

    @Override
    public Enum<?> constructRight2Left(final JsonObject right, final BinaryTransformer transformer) {
        final String valueStr = right.getString("$value", "");
        final String className = right.getString("$class", "");
        final Class<?> enumType = RT.wrap(() -> Class.forName(className));
        return Enum.valueOf((Class) enumType, valueStr);
    }

    @Override
    public void updateLeft2Right(final Enum<?> left, final JsonObject right, final BinaryTransformer transformer) {

    }

    @Override
    public void updateRight2Left(final Enum<?> left, final JsonObject right, final BinaryTransformer transformer) {

    }

}
