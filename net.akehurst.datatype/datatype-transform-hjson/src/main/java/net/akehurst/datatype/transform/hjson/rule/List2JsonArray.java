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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import net.akehurst.transform.binary.api.BinaryRule;
import net.akehurst.transform.binary.api.BinaryTransformer;

public class List2JsonArray extends Object2JsonValue<List<Object>, JsonObject> {

    @Override
    public boolean isValidForLeft2Right(final List<Object> left, final BinaryTransformer transformer) {
        return null != left;
    }

    @Override
    public boolean isValidForRight2Left(final JsonObject right, final BinaryTransformer transformer) {
        return null != right && Objects.equals("List", right.getString("$type", ""));
    }

    @Override
    public boolean isAMatch(final List<Object> left, final JsonObject right, final BinaryTransformer transformer) {
        return true;
    }

    @Override
    public JsonObject constructLeft2Right(final List<Object> left, final BinaryTransformer transformer) {
        final JsonObject right = new JsonObject();
        right.add("$type", "List");
        return right;
    }

    @Override
    public List<Object> constructRight2Left(final JsonObject right, final BinaryTransformer transformer) {
        return new ArrayList<>();
    }

    @Override
    public void updateLeft2Right(final List<Object> left, final JsonObject right, final BinaryTransformer transformer) {
        final JsonArray elements = new JsonArray();
        right.add("$elements", elements);
        for (final Object value : left) {
            final JsonValue jv = transformer.transformLeft2Right((Class<BinaryRule<Object, JsonValue>>) (Object) Object2JsonValue.class, value);
            elements.add(jv);
        }

    }

    @Override
    public void updateRight2Left(final List<Object> left, final JsonObject right, final BinaryTransformer transformer) {
        for (final JsonValue jv : right.get("$elements").asArray()) {
            final Object o = transformer.transformRight2Left((Class<BinaryRule<Object, JsonValue>>) (Object) Object2JsonValue.class, jv);
            left.add(o);
        }
    }

}
