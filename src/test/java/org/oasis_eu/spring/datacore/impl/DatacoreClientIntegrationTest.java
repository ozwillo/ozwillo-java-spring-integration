package org.oasis_eu.spring.datacore.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.oasis_eu.spring.datacore.DatacoreClient;
import org.oasis_eu.spring.datacore.model.DCOperator;
import org.oasis_eu.spring.datacore.model.DCQueryParameters;
import org.oasis_eu.spring.datacore.model.DCResource;
import org.oasis_eu.spring.datacore.model.DCResult;
import org.oasis_eu.spring.datacore.model.DCResultType;
import org.oasis_eu.spring.test.IntegrationTest;
import org.oasis_eu.spring.test.TestConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * User: schambon
 * Date: 1/3/14
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfiguration.class}, loader = AnnotationConfigContextLoader.class)
@Category(IntegrationTest.class)
public class DatacoreClientIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatacoreClientIntegrationTest.class);

    @Autowired
    private DatacoreClient client;

    @Value("${application.geoarea.project:geo_0}")
    private String project;

    @Test
    public void testCRUDOnDatacore() {
        String iri = UUID.randomUUID().toString();

        // create

        DCResource resource = new DCResource();
        resource.setBaseUri("http://data.oasis-eu.org/dc/type");
        resource.setType("citizenkin.procedure.electoral_roll_registration");
        resource.setIri(iri);

        resource.getValues().put("nom_de_famille", new DCResource.StringValue("POUTINE"));
        resource.getValues().put("prenom", new DCResource.StringValue("VLADIMIR"));
        resource.getValues().put("sexe", new DCResource.StringValue("M"));
        resource.getValues().put("date_de_naissance", new DCResource.StringValue("1954-12-25"));
        resource.getValues().put("commune_de_naissance", new DCResource.StringValue("PETROGRAD"));
        resource.getValues().put("code_departement_de_naissance", new DCResource.StringValue("99"));
        resource.getValues().put("type_de_demande", new DCResource.StringValue("premiere_demande"));
        resource.getValues().put("adresse1", new DCResource.StringValue("Palais du Président"));
        resource.getValues().put("code_postal", new DCResource.StringValue("99045"));
        resource.getValues().put("commune", new DCResource.StringValue("MOSCOU"));
        resource.getValues().put("justificatif_identite", new DCResource.StringValue(UUID.randomUUID().toString()));
        List<DCResource.Value> values = new ArrayList<>();
        values.add(new DCResource.StringValue(UUID.randomUUID().toString()));
        resource.getValues().put("justificatifs_domicile", new DCResource.ArrayValue(values));


        DCResult dcResult = client.saveResource(project, resource);
        if (! dcResult.getType().equals(DCResultType.SUCCESS)) {
            for (String s : dcResult.getErrorMessages()) {
                LOGGER.error(s);
            }
        }
        DCResource result = dcResult.getResource();

        assertEquals(resource.getUri(), result.getUri());
        assertEquals(0, result.getVersion()); // la version a été créée


        // find
        List<DCResource> find = client.findResources(project,"citizenkin.procedure.electoral_roll_registration",
                new DCQueryParameters("nom_de_famille", DCOperator.EQ, "POUTINE")
                        .and("prenom", DCOperator.EQ, "VLADIMIR"),
                0, 10);
        assertFalse(find.isEmpty());

        // read
        DCResource read = client.getResource(project,"citizenkin.procedure.electoral_roll_registration", iri).getResource();
        assertNotNull(read);


        // update after read
        read.getValues().put("commune_de_naissance", new DCResource.StringValue("SAINT-PÉTERSBOURG"));

        client.updateResource(project,read);

        read = client.getResource(project,"citizenkin.procedure.electoral_roll_registration", iri).getResource();
        assertNotNull(read);
        assertEquals("SAINT-PÉTERSBOURG", read.getValues().get("commune_de_naissance").asString());
        assertEquals(1, read.getVersion()); // version incrémentée

        // optimistic locking failure
        read.setVersion(0);
        assertEquals(DCResultType.CONFLICT, client.updateResource(project,read).getType());

        // delete
        read.setVersion(1);
        client.deleteResource(project,read);

        assertEquals(DCResultType.NOT_FOUND, client.getResource(project,"citizenkin.procedure.electoral_roll_registration", iri).getType());

    }
}
