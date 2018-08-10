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

package net.akehurst.datatype.transform.hjson;

import org.hjson.JsonValue;

import net.akehurst.datatype.transform.hjson.rule.Boolean2JsonValue;
import net.akehurst.datatype.transform.hjson.rule.Datatype2HJsonObject;
import net.akehurst.datatype.transform.hjson.rule.Enum2JsonValue;
import net.akehurst.datatype.transform.hjson.rule.Instant2JsonValue;
import net.akehurst.datatype.transform.hjson.rule.Integer2JsonValue;
import net.akehurst.datatype.transform.hjson.rule.List2JsonArray;
import net.akehurst.datatype.transform.hjson.rule.Long2JsonValue;
import net.akehurst.datatype.transform.hjson.rule.Map2JsonObject;
import net.akehurst.datatype.transform.hjson.rule.Object2JsonValue;
import net.akehurst.datatype.transform.hjson.rule.Set2JsonArray;
import net.akehurst.datatype.transform.hjson.rule.String2JsonValue;
import net.akehurst.transform.binary.api.BinaryRule;
import net.akehurst.transform.binary.basic.BinaryTransformerBasic;

public class HJsonTransformerDefault extends BinaryTransformerBasic implements HJsonTransformer {

    private Object javaRoot;
    private JsonValue hjsonRoot;

    public HJsonTransformerDefault() {
        super.registerRule((Class<BinaryRule<Object, JsonValue>>) (Object) Object2JsonValue.class);
        super.registerRule(String2JsonValue.class);
        super.registerRule(Integer2JsonValue.class);
        super.registerRule(Long2JsonValue.class);
        super.registerRule(Boolean2JsonValue.class);
        super.registerRule(Enum2JsonValue.class);
        super.registerRule(Instant2JsonValue.class);
        super.registerRule(List2JsonArray.class);
        super.registerRule(Set2JsonArray.class);
        super.registerRule(Map2JsonObject.class);
        super.registerRule(Datatype2HJsonObject.class);
    }

    public JsonValue getHJsonRoot() {
        return this.hjsonRoot;
    }

    public void setHJsonRoot(final JsonValue value) {
        this.hjsonRoot = value;
    }

    public Object getJavaRoot() {
        return this.javaRoot;
    }

    public void setJavaRoot(final Object value) {
        this.javaRoot = value;
    }

    @Override
    public JsonValue toHJson(final Object datatype) {
        this.setJavaRoot(datatype);
        final JsonValue hjson = this.transformLeft2Right((Class<BinaryRule<Object, JsonValue>>) (Object) Object2JsonValue.class, datatype);
        return hjson;
    }

    @Override
    public <T> T toDatatype(final Class<T> class_, final JsonValue hjson) {
        this.setHJsonRoot(hjson);
        final Object datatype = this.transformRight2Left((Class<BinaryRule<Object, JsonValue>>) (Object) Object2JsonValue.class, hjson);
        return (T) datatype;
    }

    @Override
    public <T> T toDatatype(final JsonValue hjson) {
        this.setHJsonRoot(hjson);
        final Object datatype = this.transformRight2Left((Class<BinaryRule<Object, JsonValue>>) (Object) Object2JsonValue.class, hjson);
        return (T) datatype;
    }

}