/**
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2024 Source Auditor Inc.
 */
package org.spdx.tools.model2java;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;

/**
 * Command Line Interface for the ShaclToJava utility
 * 
 * Generates Java code from a SHACL file specifically for SPDX version 3+
 * 
 * Usage: ShaclToJavaCli spdx-model.ttl outputdirectory
 * 
 * @author Gary O'Neall
 *
 */
public class ShaclToJavaCli {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println(String.format("Invalid arguments.  Expected 2 arguments, found %d arguments.", args.length));
			usage();
			System.exit(-1);
		}
		File outputdir = new File(args[1]);
		if (!outputdir.exists()) {
			System.out.println(String.format("Output directory %s does not exist.", args[1]));
			usage();
			System.exit(-1);
		}
		if (!outputdir.isDirectory()) {
			System.out.println(String.format("Output directory %s is not a directory.", args[1]));
			usage();
			System.exit(-1);
		}
		try (InputStream is = new FileInputStream(new File(args[0]))) {
			OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
			model.read(is, "", "Turtle");
			ShaclToJava s2j = new ShaclToJava(model);
			List<String> warnings = s2j.generate(outputdir);
			if (warnings.size() > 0) {
				System.out.println("Shacl2Java completed with the following warnings:");
				for (String warning:warnings) {
					System.out.print('\t');
					System.out.println(warning);
				}
				System.exit(1);
			} else {
				System.out.println("Java code generated successfully");
				System.exit(0);
			}
		} catch (IOException e) {
			System.out.println("I/O Error reading ontology file");
			usage();
			System.exit(-1);
		} catch (ShaclToJavaException e) {
			System.out.println(String.format("Error generating Java code: %s", e.getMessage()));
			usage();
			System.exit(-1);
		}
	}
	
	private static void usage() {
		System.out.println("Usage: ShaclToJavaCli spdx-model.ttl outputdirectory");
	}

}
