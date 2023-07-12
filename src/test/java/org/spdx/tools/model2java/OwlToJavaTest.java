/**
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2023 Source Auditor Inc.
 */
package org.spdx.tools.model2java;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.ws.commons.schema.XmlSchemaSerializer.XmlSchemaSerializerException;

import junit.framework.TestCase;

/**
 * @author gary
 *
 */
public class OwlToJavaTest extends TestCase {
	
	static final String MODEL_FILE_PATH = "testResources" + File.separator + "model.ttl";

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testConvertToJava() throws IOException, XmlSchemaSerializerException, OwlToJavaException {
		OwlToJava otj = null;
		File tempDir = Files.createTempDirectory("spdx_test").toFile();
		try {
			try (InputStream is = new FileInputStream(new File(MODEL_FILE_PATH))) {
				OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
				model.read(is, "", "Turtle");
				otj = new OwlToJava(model);
				List<String> warnings = otj.generate(tempDir);
				assertTrue(warnings.isEmpty());
				Path aIPath = tempDir.toPath().resolve("src")
					.resolve("main")
					.resolve("java")
					.resolve("org")
					.resolve("spdx")
					.resolve("library")
					.resolve("model")
					.resolve("AI");
				File classFile = aIPath.resolve("AIPackage.java").toFile();
				File enumFile = aIPath.resolve("PresenceType.java").toFile();
				assertTrue(classFile.exists());
				assertTrue(classFile.isFile());
				assertTrue(enumFile.exists());
				assertTrue(enumFile.isFile());
			}
		} finally {
			assertTrue(deleteDirectory(tempDir));
		}
	}

	/**
	 * @param tempDir
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
