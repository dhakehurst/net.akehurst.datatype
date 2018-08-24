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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import net.akehurst.transform.binary.api.BinaryRule;
import net.akehurst.transform.binary.api.BinaryTransformer;

public class Map2JsonObject extends Object2JsonValue<Map<Object, Object>, JsonObject> {

    @Override
    public boolean isValidForLeft2Right(final Map<Object, Object> left) {
        return null != left;
    }

    @Override
    public boolean isValidForRight2Left(final JsonObject right) {
        return null != right && Objects.equals("Map", right.getString("$type", ""));
    }

    @Override
    public boolean isAMatch(final Map<Object, Object> left, final JsonObject right, final BinaryTransformer transformer) {
        return true;
    }

    @Override
    public JsonObject constructLeft2Right(final Map<Object, Object> left, final BinaryTransformer transformer) {
        final JsonObject right = new JsonObject();
        right.add("$type", "Map");
        return right;
    }

    @Override
    public Map<Object, Object> constructRight2Left(final JsonObject right, final BinaryTransformer transformer) {
        return new HashMap<>();
    }

    @Override
    public void updateLeft2Right(final Map<Object, Object> left, final JsonObject right, final BinaryTransformer transformer) {
        final JsonArray elements = new JsonArray();
        right.add("$elements", elements);
        for (final Map.Entry<Object, Object> me : left.entrySet()) {
            final JsonValue jk = transformer.transformLeft2Right((Class<BinaryRule<Object, JsonValue>>) (Object) Object2JsonValue.class, me.getKey());
            final JsonValue jv = transformer.transformLeft2Right((Class<BinaryRule<Object, JsonValue>>) (Object) Object2JsonValue.class, me.getValue());
            final JsonArray entry = new JsonArray();
            entry.add(jk);
            entry.add(jv);
            elements.add(entry);
        }

    }

    @Override
    public void updateRight2Left(final Map<Object, Object> left, final JsonObject right, final BinaryTransformer transformer) {

    }

}
