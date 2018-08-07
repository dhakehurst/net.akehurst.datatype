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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import net.akehurst.datatype.annotation.Datatype;
import net.akehurst.datatype.annotation.Identity;

@Datatype
public class Contact {

    private final String alias;
    private Person person;
    private final Map<Instant, Address> address;

    public Contact(final String alias) {
        this.alias = alias;
        this.address = new HashMap<>();
    }

    @Identity
    public String getAlias() {
        return this.alias;
    }

    public Person getPerson() {
        return this.person;
    }

    public void setPerson(final Person fullname) {
        this.person = fullname;
    }

    public Map<Instant, Address> getAddress() {
        return this.address;
    }

}
