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

import org.hjson.JsonValue;

import net.akehurst.transform.binary.api.BinaryTransformer;

public class Null2JsonValue extends Object2JsonValue<Void, JsonValue> {

    @Override
    public boolean isValidForLeft2Right(final Void left, final BinaryTransformer transformer) {
        return null == left;
    }

    @Override
    public boolean isValidForRight2Left(final JsonValue right, final BinaryTransformer transformer) {
        return right == JsonValue.NULL;
    }

    @Override
    public boolean isAMatch(final Void left, final JsonValue right, final BinaryTransformer transformer) {
        return null == left && right == JsonValue.NULL;
    }

    @Override
    public JsonValue constructLeft2Right(final Void left, final BinaryTransformer transformer) {
        return JsonValue.NULL;
    }

    @Override
    public Void constructRight2Left(final JsonValue right, final BinaryTransformer transformer) {
        return null;
    }

    @Override
    public void updateLeft2Right(final Void left, final JsonValue right, final BinaryTransformer transformer) {

    }

    @Override
    public void updateRight2Left(final Void left, final JsonValue right, final BinaryTransformer transformer) {

    }

}
