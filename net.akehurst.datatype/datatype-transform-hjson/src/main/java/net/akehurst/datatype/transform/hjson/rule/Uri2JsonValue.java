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

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

import org.hjson.JsonObject;

import net.akehurst.transform.binary.api.BinaryTransformer;

public class Uri2JsonValue extends Object2JsonValue<URI, JsonObject> {

    @Override
    public boolean isValidForLeft2Right(final URI left) {
        return true;
    }

    @Override
    public boolean isValidForRight2Left(final JsonObject right) {
        return Objects.equals("URI", right.getString("$type", ""));
    }

    @Override
    public boolean isAMatch(final URI left, final JsonObject right, final BinaryTransformer transformer) {
        return Objects.equals(left, Instant.parse(right.getString("$value", "")));
    }

    @Override
    public JsonObject constructLeft2Right(final URI left, final BinaryTransformer transformer) {
        final JsonObject right = new JsonObject();
        right.add("$type", "URI");
        return right;
    }

    @Override
    public URI constructRight2Left(final JsonObject right, final BinaryTransformer transformer) {
        return URI.create(right.getString("$value", ""));
    }

    @Override
    public void updateLeft2Right(final URI left, final JsonObject right, final BinaryTransformer transformer) {
        right.add("$value", left.toString());
    }

    @Override
    public void updateRight2Left(final URI left, final JsonObject right, final BinaryTransformer transformer) {

    }

}
