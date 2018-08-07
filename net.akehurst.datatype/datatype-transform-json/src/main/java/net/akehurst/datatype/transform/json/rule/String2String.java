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

import net.akehurst.transform.binary.api.BinaryTransformer;

public class String2String extends JavaValue2JsonValue<String, String> {

    @Override
    public boolean isValidForLeft2Right(final String left) {
        return true;
    }

    @Override
    public boolean isValidForRight2Left(final String right) {
        return true;
    }

    @Override
    public boolean isAMatch(final String left, final String right, final BinaryTransformer transformer) {
        return Objects.equals(left, right);
    }

    @Override
    public String constructLeft2Right(final String left, final BinaryTransformer transformer) {
        return left;
    }

    @Override
    public String constructRight2Left(final String right, final BinaryTransformer transformer) {
        return right;
    }

    @Override
    public void updateLeft2Right(final String left, final String right, final BinaryTransformer transformer) {

    }

    @Override
    public void updateRight2Left(final String left, final String right, final BinaryTransformer transformer) {

    }

}
