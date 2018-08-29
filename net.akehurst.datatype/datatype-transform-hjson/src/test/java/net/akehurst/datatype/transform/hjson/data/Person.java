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

package net.akehurst.datatype.transform.hjson.data;

import java.util.ArrayList;
import java.util.List;

import net.akehurst.datatype.annotation.Datatype;
import net.akehurst.datatype.annotation.Identity;
import net.akehurst.datatype.annotation.Reference;

@Datatype
public class Person {

    private final String firstname;
    private final List<String> othernames;
    private final String lastname;
    private Person inRelationshipWith;
    private final List<Person> friends;

    public Person(final String firstname, final String lastname) {
        this.firstname = firstname;
        this.othernames = new ArrayList<>();
        this.lastname = lastname;
        this.friends = new ArrayList<>();
    }

    @Identity(0)
    public String getFirstname() {
        return this.firstname;
    }

    public List<String> getOthernames() {
        return this.othernames;
    }

    @Identity(1)
    public String getLastname() {
        return this.lastname;
    }

    @Reference
    public Person getInRelationshipWith() {
        return this.inRelationshipWith;
    }

    public void setInRelationshipWith(final Person value) {
        this.inRelationshipWith = value;
    }

    @Reference
    public List<Person> getFriends() {
        return this.friends;
    }

}
