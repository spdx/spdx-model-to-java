/**
 * SPDX-FileCopyrightText: Copyright (c) 2023 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
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

import junit.framework.TestCase;

/**
 * @author Gary O'Neall
 */
public class ShaclToJavaTest extends TestCase {
	
	static final String MODEL_FILE_PATH = "testResources" + File.separator + "spdx-model.ttl";

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testConvertToJava() throws IOException, ShaclToJavaException {
		ShaclToJava otj;
		File tempDir = Files.createTempDirectory("spdx_test").toFile();
		try {
			try (InputStream is = new FileInputStream(MODEL_FILE_PATH)) {
				OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
				model.read(is, "", "Turtle");
				otj = new ShaclToJava(model);
				List<String> warnings = otj.generate(tempDir);
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
