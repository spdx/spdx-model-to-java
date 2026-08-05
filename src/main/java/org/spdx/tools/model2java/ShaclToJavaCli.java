/**
 * SPDX-FileCopyrightText: Copyright (c) 2024 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools.model2java;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;

/**
 * Command Line Interface for the ShaclToJava utility
 * <p/>
 * Generates Java code from a SHACL file specifically for SPDX version 3+
 * <p/>
 * Usage: ShaclToJavaCli inputdirectory outputdirectory
 * <p/>
 * The input directory contains model files in TTL format for all versions to be generated
 * <p/>
 * The output directory will contain generated Java code to implement the model
 * <p/>
 * @author Gary O'Neall
 */
public class ShaclToJavaCli {

	/**
	 * @param args args[0] input directory args[1] output directory
	 */
	public static void main(String[] args) {
		System.exit(run(args));
	}
	/**
	 * @param args args[0] input directory args[1] output directory
	 */
	public static int run(String[] args) {
		if (args.length != 2) {
			System.out.printf("Invalid arguments.  Expected 2 arguments, found %d arguments.%n", args.length);
			usage();
			return -1;
		}
		File outputDir = new File(args[1]);
		if (!outputDir.exists()) {
			System.out.printf("Output directory %s does not exist.%n", args[1]);
			usage();
			return -1;
		}
		if (!outputDir.isDirectory()) {
			System.out.printf("Output directory %s is not a directory.%n", args[1]);
			usage();
			return -1;
		}
		File inputDir = new File(args[0]);
		if (!inputDir.exists()) {
			System.out.printf("Input directory %s does not exist.%n", args[1]);
			usage();
			return -1;
		}
		if (!inputDir.isDirectory()) {
			System.out.printf("Input directory %s is not a directory.%n", args[1]);
			usage();
			return -1;
		}
		File[] inputFiles = inputDir.listFiles();
		if (Objects.isNull(inputFiles) || inputFiles.length < 1) {
			System.out.printf("Input directory %s contains no files.%n", args[1]);
			usage();
			return -1;
		}
		List<OntModel> models = new ArrayList<>();
		for (File f : Objects.requireNonNull(inputDir.listFiles())) {
			try (InputStream is = new FileInputStream(f)) {
				OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
				model.read(is, "", "Turtle");
				models.add(model);
			} catch (IOException e) {
				System.out.println("I/O Error reading ontology file");
				usage();
				return -1;
			}
		}
		models.sort(new Comparator<OntModel>() {
			@Override
			public int compare(OntModel o1, OntModel o2) {
				return(getSpecVersion(o1).compareTo(getSpecVersion(o2)));
			}
		});
		List<String> warnings = new ArrayList<>();
		for (int i = 0; i < models.size(); i++) {
			try {
				String specVersion = getSpecVersion(models.get(i));
				String[] specVersionParts = specVersion.split("\\.");
				String versionForPackageName = "v3_" +
						(specVersionParts.length > 1 ? specVersionParts[1] : "0");
				ShaclToJava s2j = new ShaclToJava(models.get(i), versionForPackageName, specVersion);
				warnings.addAll(s2j.generate(outputDir));
				if ("3.0.1".equals(specVersion)) {
					// generate the package version for the 3.0.1 patch version only - for compatibility
					// going forward, we will only generate minor versions
					ShaclToJava s2jdot = new ShaclToJava(models.get(i), "v3_0_1", specVersion);
					warnings.addAll(s2jdot.generate(outputDir));
				}
				if (i == models.size()-1) {
					// for the latest version, create a package for the latest version
					ShaclToJava s2jlatest = new ShaclToJava(models.get(i), "v3", specVersion);
					warnings.addAll(s2jlatest.generate(outputDir));
				}
			} catch (IOException e) {
				System.out.println("I/O Error writing output directory");
				usage();
				return -1;
			} catch (ShaclToJavaException e) {
				System.out.printf("Error generating Java code: %s%n", e.getMessage());
				usage();
				return -1;
			}
		}
		if (!warnings.isEmpty()) {
			System.out.println("Shacl2Java completed with the following warnings:");
			for (String warning:warnings) {
				System.out.print('\t');
				System.out.println(warning);
			}
			return 1;
		} else {
			System.out.println("Java code generated successfully");
			return 0;
		}
	}

	private static String getSpecVersion(OntModel model) {
		//TODO: The following is a hack to work around https://github.com/spdx/spec-parser/issues/214
		// Individual spdxOrg = model.getIndividual(model.getNsPrefixURI("ns1") + "SpdxOrganization");
		String spdxUri = null;
		try (QueryExecution query = QueryExecutionFactory.create(
				"SELECT ?agent WHERE {<" + model.getNsPrefixURI("ns1") + "SpdxOrganization> <"
						+ model.getNsPrefixURI("ns1") + "creationInfo> ?agent}", model)) {
			ResultSet result = query.execSelect();
			if (result.hasNext()) {
				QuerySolution solution = result.next();
				RDFNode agentNode = solution.get("?agent");
				if (Objects.nonNull(agentNode)) {
					spdxUri = agentNode.toString();
				}
			}

		}
		if (Objects.isNull(spdxUri)) {
			spdxUri = model.getNsPrefixURI("spdx");
		}
		//TODO: end of hack
		String versionSemVer = spdxUri.substring("https://spdx.org/rdf/".length());
		versionSemVer = versionSemVer.substring(0, versionSemVer.indexOf('/'));
		return versionSemVer;
	}
	
	private static void usage() {
		System.out.println("Usage: ShaclToJavaCli inputdirectory outputdirectory");
	}

}
