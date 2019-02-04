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

import java.time.Instant;
import java.util.Objects;

import org.hjson.JsonObject;

import net.akehurst.transform.binary.api.BinaryTransformer;

public class Instant2JsonValue extends Object2JsonValue<Instant, JsonObject> {

    @Override
    public boolean isValidForLeft2Right(final Instant left, final BinaryTransformer transformer) {
        return true;
    }

    @Override
    public boolean isValidForRight2Left(final JsonObject right, final BinaryTransformer transformer) {
        return Objects.equals("Instant", right.getString("$type", ""));
    }

    @Override
    public boolean isAMatch(final Instant left, final JsonObject right, final BinaryTransformer transformer) {
        return Objects.equals(left, Instant.parse(right.getString("$value", "")));
    }

    @Override
    public JsonObject constructLeft2Right(final Instant left, final BinaryTransformer transformer) {
        final JsonObject right = new JsonObject();
        right.add("$type", "Instant");
        return right;
    }

    @Override
    public Instant constructRight2Left(final JsonObject right, final BinaryTransformer transformer) {
        return Instant.parse(right.getString("$value", ""));
    }

    @Override
    public void updateLeft2Right(final Instant left, final JsonObject right, final BinaryTransformer transformer) {
        right.add("$value", left.toString());
    }

    @Override
    public void updateRight2Left(final Instant left, final JsonObject right, final BinaryTransformer transformer) {

    }

}
