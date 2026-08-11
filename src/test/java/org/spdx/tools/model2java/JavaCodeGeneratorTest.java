package org.spdx.tools.model2java;

import junit.framework.TestCase;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JavaCodeGeneratorTest  extends TestCase {
    static final String MODEL_DIR_PATH = "testResources";
    static final String MODEL_FILE_PATH = MODEL_DIR_PATH + File.separator + "spdx-model-3-0-1.ttl";
    static final String MODEL_FILE_PATH_3_1 = MODEL_DIR_PATH + File.separator + "spdx-model-3-1.ttl";

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGenerateOneModel() throws IOException, ShaclToJavaException {
        File tempDir = Files.createTempDirectory("spdx_test").toFile();
        try {
            try (InputStream is = new FileInputStream(MODEL_FILE_PATH)) {
                OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
                model.read(is, "", "Turtle");
                SpecVersionContainer svc301 = new SpecVersionContainer(model, "3.0.1");
                JavaCodeGenerator jcg = new JavaCodeGenerator(new ArrayList<>(List.of(svc301)));
                List<String> warnings = jcg.generate(tempDir);
                assertTrue(warnings.isEmpty());
                Path aIPath = tempDir.toPath().resolve("src")
                        .resolve("main")
                        .resolve("java")
                        .resolve("org")
                        .resolve("spdx")
                        .resolve("library")
                        .resolve("model")
                        .resolve("v3_0_1")
                        .resolve("ai");
                File classFile = aIPath.resolve("AIPackage.java").toFile();
                File enumFile = aIPath.resolve("SafetyRiskAssessmentType.java").toFile();
                assertTrue(classFile.exists());
                assertTrue(classFile.isFile());
                assertTrue(enumFile.exists());
                assertTrue(enumFile.isFile());
            }
        } finally {
            assertTrue(deleteDirectory(tempDir));
        }
    }

    public void testGenerateTwoModels() throws IOException, ShaclToJavaException {
        File tempDir = Files.createTempDirectory("spdx_test").toFile();
        try {
            SpecVersionContainer svc301;
            try (InputStream is = new FileInputStream(MODEL_FILE_PATH)) {
                OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
                model.read(is, "", "Turtle");
                svc301 = new SpecVersionContainer(model, "3.0.1");
            }
            SpecVersionContainer svc31;
            try (InputStream is = new FileInputStream(MODEL_FILE_PATH_3_1)) {
                OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
                model.read(is, "", "Turtle");
                svc31 = new SpecVersionContainer(model, "3.1.0");
            }
            JavaCodeGenerator jcg = new JavaCodeGenerator(new ArrayList<>(List.of(svc301, svc31)));
            List<String> warnings = jcg.generate(tempDir);
            assertTrue(warnings.isEmpty());
            Path aIPath = tempDir.toPath().resolve("src")
                    .resolve("main")
                    .resolve("java")
                    .resolve("org")
                    .resolve("spdx")
                    .resolve("library")
                    .resolve("model")
                    .resolve("v3_0_1")
                    .resolve("ai");
            File classFile = aIPath.resolve("AIPackage.java").toFile();
            File enumFile = aIPath.resolve("SafetyRiskAssessmentType.java").toFile();
            assertTrue(classFile.exists());
            assertTrue(classFile.isFile());
            assertTrue(enumFile.exists());
            assertTrue(enumFile.isFile());
        } finally {
            assertTrue(deleteDirectory(tempDir));
        }
    }

    /**
     * @param tempDir directory to delete
     */
    private boolean deleteDirectory(File tempDir) {
        File[] files = tempDir.listFiles();
        if (Objects.nonNull(files)) {
            for (File file:files) {
                deleteDirectory(file);
            }
        }
        return tempDir.delete();
    }

}
