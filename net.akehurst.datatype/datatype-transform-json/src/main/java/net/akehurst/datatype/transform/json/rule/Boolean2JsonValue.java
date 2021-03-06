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

import java.util.Objects;

import org.hjson.JsonValue;

import net.akehurst.transform.binary.api.BinaryTransformer;

public class Boolean2JsonValue extends JavaValue2JsonValue<Boolean, JsonValue> {

    @Override
    public boolean isValidForLeft2Right(final Boolean left) {
        return true;
    }

    @Override
    public boolean isValidForRight2Left(final JsonValue right) {
        return right.isBoolean();
    }

    @Override
    public boolean isAMatch(final Boolean left, final JsonValue right, final BinaryTransformer transformer) {
        return Objects.equals(left, right.asBoolean());
    }

    @Override
    public JsonValue constructLeft2Right(final Boolean left, final BinaryTransformer transformer) {
        return JsonValue.valueOf(left);
    }

    @Override
    public Boolean constructRight2Left(final JsonValue right, final BinaryTransformer transformer) {
        return right.asBoolean();
    }

    @Override
    public void updateLeft2Right(final Boolean left, final JsonValue right, final BinaryTransformer transformer) {

    }

    @Override
    public void updateRight2Left(final Boolean left, final JsonValue right, final BinaryTransformer transformer) {

    }

}
