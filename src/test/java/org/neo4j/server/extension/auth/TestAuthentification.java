/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.server.extension.auth;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.server.configuration.EmbeddedServerConfigurator;
import org.neo4j.server.configuration.ThirdPartyJaxRsPackage;
import org.neo4j.test.ImpermanentGraphDatabase;

import javax.ws.rs.core.MultivaluedMap;
import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * @author tbaum
 * @since 31.05.11 21:11
 */
public class TestAuthentification {
    private WrappingNeoServerBootstrapper testBootstrapper;

    @Before
    public void setUp() throws Exception {

        ImpermanentGraphDatabase db = new ImpermanentGraphDatabase();

        EmbeddedServerConfigurator config = new EmbeddedServerConfigurator(db);
        config.configuration().setProperty("org.neo4j.server.credentials", "neo4j:master");
        config.getThirdpartyJaxRsClasses().add(new ThirdPartyJaxRsPackage("org.neo4j.server.extension.auth", "/admin"));
        testBootstrapper = new WrappingNeoServerBootstrapper(db, config);
        testBootstrapper.start();
    }

    @After
    public void tearDown() {
        testBootstrapper.stop();
    }

    private void delete(final File dir) {
        dir.deleteOnExit();
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                delete(file);
            } else {
                file.deleteOnExit();
            }
        }
    }

    @Test public void expecting401() throws IOException, InterruptedException {
        Client client = Client.create();


        try {
            client.resource("http://localhost:7474/").get(String.class);
            fail();
        } catch (UniformInterfaceException e) {
            assertEquals("expecting responsecode 401", 401, e.getResponse().getStatus());
        }

        try {
            client.resource("http://localhost:7474/db/data").get(String.class);
            fail();
        } catch (UniformInterfaceException e) {
            assertEquals("expecting responsecode 401", 401, e.getResponse().getStatus());
        }


        try {
            client.resource("http://localhost:7474/admin/add-user-ro").get(String.class);
            fail();
        } catch (UniformInterfaceException e) {
            assertEquals("expecting responsecode 401", 401, e.getResponse().getStatus());
        }


        try {
            client.resource("http://localhost:7474/admin/add-user-ro").get(String.class);
            fail();
        } catch (UniformInterfaceException e) {
            assertEquals("expecting responsecode 401", 401, e.getResponse().getStatus());
        }

        try {
            client.resource("http://localhost:7474/admin/add-user-rw").get(String.class);
            fail();
        } catch (UniformInterfaceException e) {
            assertEquals("expecting responsecode 401", 401, e.getResponse().getStatus());
        }

        try {
            client.resource("http://localhost:7474/admin/remove-user").get(String.class);
            fail();
        } catch (UniformInterfaceException e) {
            assertEquals("expecting responsecode 401", 401, e.getResponse().getStatus());
        }
    }

    @Test public void addRoAndRemoveUserTest() throws IOException, InterruptedException {
        Client adminClient = Client.create();
        adminClient.addFilter(new HTTPBasicAuthFilter("neo4j", "master"));

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("user", "test:pass");

        assertEquals("OK", adminClient.resource("http://localhost:7474/admin/add-user-ro").post(String.class, formData));


        Client client = Client.create();
        client.addFilter(new HTTPBasicAuthFilter("test", "pass"));

        client.resource("http://localhost:7474/").accept("application/json").get(String.class);
        client.resource("http://localhost:7474/db/data").get(String.class);

        try {
            client.resource("http://localhost:7474/db/data/node").post(String.class);
            fail();
        } catch (UniformInterfaceException e) {
            assertEquals("expecting responsecode 401", 401, e.getResponse().getStatus());
        }

        assertEquals("OK", adminClient.resource("http://localhost:7474/admin/remove-user").post(String.class, formData));

        try {
            client.resource("http://localhost:7474/db/data/node").get(String.class);
            fail();
        } catch (UniformInterfaceException e) {
            assertEquals("expecting responsecode 401", 401, e.getResponse().getStatus());
        }

        try {
            client.resource("http://localhost:7474/db/data").get(String.class);
            fail();
        } catch (UniformInterfaceException e) {
            assertEquals("expecting responsecode 401", 401, e.getResponse().getStatus());
        }
    }

    @Test public void addRwAndRemoveUserTest() throws IOException, InterruptedException {
        Client adminClient = Client.create();
        adminClient.addFilter(new HTTPBasicAuthFilter("neo4j", "master"));

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("user", "test:pass");

        assertEquals("OK", adminClient.resource("http://localhost:7474/admin/add-user-rw").post(String.class, formData));


        Client client = Client.create();
        client.addFilter(new HTTPBasicAuthFilter("test", "pass"));

        client.resource("http://localhost:7474/").accept("application/json").get(String.class);
        client.resource("http://localhost:7474/db/data").get(String.class);

        client.resource("http://localhost:7474/db/data/node").post(String.class);


        assertEquals("OK", adminClient.resource("http://localhost:7474/admin/remove-user").post(String.class, formData));


        try {
            client.resource("http://localhost:7474/").get(String.class);
            fail();
        } catch (UniformInterfaceException e) {
            assertEquals("expecting responsecode 401", 401, e.getResponse().getStatus());
        }

        try {
            client.resource("http://localhost:7474/db/data").get(String.class);
            fail();
        } catch (UniformInterfaceException e) {
            assertEquals("expecting responsecode 401", 401, e.getResponse().getStatus());
        }
    }
}