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

import org.hjson.JsonArray;
import org.hjson.JsonValue;

import net.akehurst.transform.binary.api.BinaryRule;
import net.akehurst.transform.binary.api.BinaryTransformer;

public class List2JsonArray extends Object2JsonValue<List<Object>, JsonArray> {

    @Override
    public boolean isValidForLeft2Right(final List<Object> left) {
        return null != left;
    }

    @Override
    public boolean isValidForRight2Left(final JsonArray right) {
        return null != right;
    }

    @Override
    public boolean isAMatch(final List<Object> left, final JsonArray right, final BinaryTransformer transformer) {
        return true;
    }

    @Override
    public JsonArray constructLeft2Right(final List<Object> left, final BinaryTransformer transformer) {
        return new JsonArray();
    }

    @Override
    public List<Object> constructRight2Left(final JsonArray right, final BinaryTransformer transformer) {
        return new ArrayList<>();
    }

    @Override
    public void updateLeft2Right(final List<Object> left, final JsonArray right, final BinaryTransformer transformer) {

        for (final Object value : left) {
            final JsonValue jv = transformer.transformLeft2Right((Class<BinaryRule<Object, JsonValue>>) (Object) Object2JsonValue.class, value);
            right.add(jv);
        }

    }

    @Override
    public void updateRight2Left(final List<Object> left, final JsonArray right, final BinaryTransformer transformer) {

    }

}
