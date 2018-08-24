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

import java.util.Objects;

import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.hjson.Stringify;
import org.jooq.lambda.Seq;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.akehurst.datatype.transform.hjson.data.AddressBook;
import net.akehurst.datatype.transform.hjson.data.Contact;
import net.akehurst.datatype.transform.hjson.data.Person;

public class test_HJsonTransformer {

    private HJsonTransformer sut;

    @Before
    public void setup() {
        this.sut = new HJsonTransformerDefault();
    }

    @Test
    public void simple_toHJson() {

        final Person datatype = new Person("Fred", "Blogs");

        final JsonObject hjson = this.sut.toHJson(datatype).asObject();

        Assert.assertEquals(datatype.getClass().getName(), hjson.get("$class").asString());
        Assert.assertEquals(datatype.getFirstname(), hjson.get("firstname").asString());
        Assert.assertEquals(datatype.getLastname(), hjson.get("lastname").asString());
    }

    @Test
    public void simple_toDatatype() {

        final JsonObject hjson = new JsonObject();
        hjson.add("$class", Person.class.getName());
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

        Assert.assertEquals(datatype.getClass().getName(), hjson.get("$class").asString());
        Assert.assertEquals(datatype.getFirstname(), hjson.get("firstname").asString());
        for (int i = 0; i < hjson.get("othernames").asObject().get("elements").asArray().size(); ++i) {
            final String othername = datatype.getOthernames().get(i);
            final String hj = hjson.get("othernames").asObject().get("elements").asArray().get(i).asString();
            Assert.assertEquals(othername, hj);
        }
        Assert.assertEquals(datatype.getLastname(), hjson.get("lastname").asString());
    }

    @Test
    public void withList_toDatatype() {

        final JsonObject hjson = new JsonObject();
        hjson.add("$class", Person.class.getName());
        hjson.add("firstname", "Fred");
        hjson.add("lastname", "Blogs");
        final JsonObject jsonOther = new JsonObject();
        jsonOther.add("$class", "Set");
        final JsonArray elements = new JsonArray();
        jsonOther.add("elements", elements);
        hjson.add("othernames", jsonOther);
        elements.add(JsonValue.valueOf("Jim"));

        final Person datatype = this.sut.toDatatype(hjson);

        Assert.assertEquals(hjson.get("$class").asString(), datatype.getClass().getName());
        Assert.assertEquals(hjson.get("firstname").asString(), datatype.getFirstname());
        for (int i = 0; i < datatype.getOthernames().size(); ++i) {
            final String othername = datatype.getOthernames().get(i);
            final String hj = hjson.get("othernames").asArray().get(i).asString();
            Assert.assertEquals(hj, othername);
        }
        Assert.assertEquals(hjson.get("lastname").asString(), datatype.getLastname());
    }

    @Test
    public void withReference_toHJson() {

        final AddressBook datatype = new AddressBook();
        final Person p1 = new Person("Fred", "Blogs");
        final Contact c1 = new Contact("Fred");
        datatype.getContacts().add(c1);
        c1.setPerson(p1);
        final Person p2 = new Person("Jane", "Doe");
        p1.setInRelationshipWith(p2);
        final Contact c2 = new Contact("Jane");
        datatype.getContacts().add(c2);
        c2.setPerson(p2);

        final JsonObject hjson = this.sut.toHJson(datatype).asObject();

        System.out.println(hjson.toString(Stringify.FORMATTED));

        Assert.assertEquals(datatype.getClass().getName(), hjson.get("$class").asString());
        Assert.assertEquals(p1.getFirstname(), hjson.get("contacts").asArray().get(0).asObject().get("person").asObject().get("firstname").asString());
        Assert.assertEquals("#/contacts/1/person",
                hjson.get("contacts").asArray().get(0).asObject().get("person").asObject().get("inRelationshipWith").asObject().get("$ref").asString());
    }

    @Test
    public void withReference_toDatatype() {

        final JsonObject p1 = new JsonObject();
        p1.add("$class", Person.class.getName());
        p1.add("firstname", "Fred");
        p1.add("lastname", "Blogs");
        final JsonObject ref = new JsonObject();
        ref.add("$ref", "#/contacts/1/person");
        p1.add("inRelationshipWith", ref);
        final JsonObject c1 = new JsonObject();
        c1.add("$class", Contact.class.getName());
        c1.add("alias", "Fred");
        c1.add("person", p1);

        final JsonObject p2 = new JsonObject();
        p2.add("$class", Person.class.getName());
        p2.add("firstname", "Jane");
        p2.add("lastname", "Doe");
        final JsonObject c2 = new JsonObject();
        c2.add("$class", Contact.class.getName());
        c2.add("alias", "Jane");
        c2.add("person", p2);

        final JsonObject hjson = new JsonObject();
        hjson.add("$class", AddressBook.class.getName());
        final JsonObject contacts = new JsonObject();
        contacts.add("$class", "Set");
        final JsonArray elements = new JsonArray();
        contacts.add("elements", elements);
        hjson.add("contacts", contacts);
        elements.add(c1);
        elements.add(c2);

        final AddressBook datatype = this.sut.toDatatype(hjson);

        System.out.println(hjson.toString(Stringify.FORMATTED));

        Assert.assertEquals(hjson.get("$class").asString(), datatype.getClass().getName());
        Assert.assertEquals(2, datatype.getContacts().size());
        Assert.assertEquals(p2.get("firstname").asString(), Seq.seq(datatype.getContacts()).findFirst(c -> Objects.equals("Fred", c.getPerson().getFirstname()))
                .get().getPerson().getInRelationshipWith().getFirstname());
    }
}
