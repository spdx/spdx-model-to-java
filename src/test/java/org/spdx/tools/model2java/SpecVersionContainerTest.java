package org.spdx.tools.model2java;

import junit.framework.TestCase;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.spdx.tools.model2java.model.EnumModel;
import org.spdx.tools.model2java.model.IndividualClassModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

public class SpecVersionContainerTest extends TestCase {

    static final String MODEL_DIR_PATH = "testResources";
    static final String MODEL_FILE_PATH = MODEL_DIR_PATH + File.separator + "spdx-model-3-1.ttl";

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSpdxVersionContainer() throws IOException, ShaclToJavaException {
        try (InputStream is = new FileInputStream(MODEL_FILE_PATH)) {
            OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
            model.read(is, "", "Turtle");
            SpecVersionContainer svc = new SpecVersionContainer(model, "3.1.0");
            assertTrue(svc.getWarnings().isEmpty());
            List<IndividualClassModel> individuals = svc.getIndividuals();
            List<IndividualClassModel> noAssertionLicenses = individuals.stream().filter(individual -> {
                return individual.getClassName().equals("NoAssertionLicense");
            }).collect(Collectors.toList());
            assertEquals(1, noAssertionLicenses.size());
            List<EnumModel> enums = svc.getEnums();
            assertFalse(enums.isEmpty());
            List<EnumModel> hashAlgorithms = enums.stream().filter(e -> {
                return e.getName().equals("HashAlgorithm");
            }).collect(Collectors.toList());
            assertEquals(1, hashAlgorithms.size());
            assertTrue(hashAlgorithms.get(0).getEnumValues().contains("ADLER32(\"adler32\"),"));
        }
    }
}
