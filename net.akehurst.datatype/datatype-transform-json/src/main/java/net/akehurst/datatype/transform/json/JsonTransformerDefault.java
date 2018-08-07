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

package net.akehurst.datatype.transform.json;

import org.json.JSONObject;

import net.akehurst.datatype.transform.json.rule.Boolean2JsonValue;
import net.akehurst.datatype.transform.json.rule.Datatype2HJsonObject;
import net.akehurst.datatype.transform.json.rule.Enum2JsonValue;
import net.akehurst.datatype.transform.json.rule.Instant2JsonValue;
import net.akehurst.datatype.transform.json.rule.Integer2JsonValue;
import net.akehurst.datatype.transform.json.rule.JavaValue2JsonValue;
import net.akehurst.datatype.transform.json.rule.List2JsonArray;
import net.akehurst.datatype.transform.json.rule.Long2JsonValue;
import net.akehurst.datatype.transform.json.rule.Set2JsonArray;
import net.akehurst.datatype.transform.json.rule.String2String;
import net.akehurst.transform.binary.api.BinaryRule;
import net.akehurst.transform.binary.basic.BinaryTransformerBasic;

public class JsonTransformerDefault extends BinaryTransformerBasic implements JsonTransformer {

    public JsonTransformerDefault() {
        super.registerRule((Class<BinaryRule<Object, JSONObject>>) (Object) JavaValue2JsonValue.class);
        super.registerRule(String2String.class);
        super.registerRule(Integer2JsonValue.class);
        super.registerRule(Long2JsonValue.class);
        super.registerRule(Boolean2JsonValue.class);
        super.registerRule(Enum2JsonValue.class);
        super.registerRule(Instant2JsonValue.class);
        super.registerRule(List2JsonArray.class);
        super.registerRule(Set2JsonArray.class);
        super.registerRule(Datatype2HJsonObject.class);
    }

    @Override
    public JSONObject toJson(final Object datatype) {
        final JSONObject hjson = this.transformLeft2Right((Class<BinaryRule<Object, JSONObject>>) (Object) JavaValue2JsonValue.class, datatype);
        return hjson;
    }

    @Override
    public <T> T toDatatype(final Class<T> class_, final JSONObject hjson) {
        final Object datatype = this.transformRight2Left((Class<BinaryRule<Object, JSONObject>>) (Object) JavaValue2JsonValue.class, hjson);
        return (T) datatype;
    }

    @Override
    public <T> T toDatatype(final JSONObject hjson) {
        final Object datatype = this.transformRight2Left((Class<BinaryRule<Object, JSONObject>>) (Object) JavaValue2JsonValue.class, hjson);
        return (T) datatype;
    }

}
