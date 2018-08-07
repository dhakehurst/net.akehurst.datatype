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

import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.akehurst.datatype.transform.json.data.Person;

public class test_HJsonTransformer {

    private JsonTransformer sut;

    @Before
    public void setup() {
        this.sut = new JsonTransformerDefault();
    }

    @Test
    public void simple_toHJson() {

        final Person datatype = new Person("Fred", "Blogs");

        final JsonObject hjson = this.sut.toHJson(datatype).asObject();

        Assert.assertEquals(datatype.getClass().getName(), hjson.get("_class").asString());
        Assert.assertEquals(datatype.getFirstname(), hjson.get("firstname").asString());
        Assert.assertEquals(datatype.getLastname(), hjson.get("lastname").asString());
    }

    @Test
    public void simple_toDatatype() {

        final JsonObject hjson = new JsonObject();
        hjson.add("_class", Person.class.getName());
        hjson.add("firstname", "Fred");
        hjson.add("lastname", "Blogs");

        final Person datatype = this.sut.toDatatype(hjson);

        Assert.assertEquals(hjson.get("_class").asString(), datatype.getClass().getName());
        Assert.assertEquals(hjson.get("firstname").asString(), datatype.getFirstname());
        Assert.assertEquals(hjson.get("lastname").asString(), datatype.getLastname());
    }

    @Test
    public void withList_toHJson() {

        final Person datatype = new Person("Fred", "Blogs");
        datatype.getOthernames().add("Jim");

        final JsonObject hjson = this.sut.toHJson(datatype).asObject();

        Assert.assertEquals(datatype.getClass().getName(), hjson.get("_class").asString());
        Assert.assertEquals(datatype.getFirstname(), hjson.get("firstname").asString());
        for (int i = 0; i < hjson.get("othernames").asArray().size(); ++i) {
            final String othername = datatype.getOthernames().get(i);
            final String hj = hjson.get("othernames").asArray().get(i).asString();
            Assert.assertEquals(othername, hj);
        }
        Assert.assertEquals(datatype.getLastname(), hjson.get("lastname").asString());
    }

    @Test
    public void withList_toDatatype() {

        final JsonObject hjson = new JsonObject();
        hjson.add("_class", Person.class.getName());
        hjson.add("firstname", "Fred");
        hjson.add("lastname", "Blogs");
        final JsonArray jsonOther = new JsonArray();
        hjson.add("othernames", jsonOther);
        jsonOther.add(JsonValue.valueOf("Jim"));

        final Person datatype = this.sut.toDatatype(hjson);

        Assert.assertEquals(hjson.get("_class").asString(), datatype.getClass().getName());
        Assert.assertEquals(hjson.get("firstname").asString(), datatype.getFirstname());
        for (int i = 0; i < datatype.getOthernames().size(); ++i) {
            final String othername = datatype.getOthernames().get(i);
            final String hj = hjson.get("othernames").asArray().get(i).asString();
            Assert.assertEquals(hj, othername);
        }
        Assert.assertEquals(hjson.get("lastname").asString(), datatype.getLastname());
    }
}
