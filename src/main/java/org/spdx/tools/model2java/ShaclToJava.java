/**
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2023 Source Auditor Inc.
 */
package org.spdx.tools.model2java;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.engine.constraint.SparqlConstraint;
import org.apache.jena.shacl.parser.Constraint;
import org.apache.jena.shacl.parser.PropertyShape;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.util.iterator.ExtendedIterator;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;

/**
 * Generates Java source class files for the SPDX Java Library from the RDF Owl Document generated from the spec model
 * 
 * @author Gary O'Neall
 *
 */
public class ShaclToJava {
	
	OntModel model = null;
	Shapes shapes = null;
	Map<Node, Shape> shapeMap = null;
	
	Set<String> enumClassUris = new HashSet<>(); // Set of enum URI's
	Map<String, List<String>> classUriToIndividualUris = new HashMap<>(); // set of all non-enum individual URI's
	Set<String> propertyUrisForConstants = new HashSet<>(); // Map of property URI's to be included in the SPDX Constants file
	Set<String> enumerationTypes = new HashSet<>(); // Set of URI's for enumeration types
	Set<String> anyLicenseInfoTypes = new HashSet<>(); // Set of URI's for AnyLicenseInfo types
	Set<String> licenseAdditionTypes = new HashSet<>(); // Set of URI's for LicenseAddition types
	Set<String> extendableLicenseTypes = new HashSet<>(); // Set of URI's for LicenseAddition types
	Set<String> elementTypes = new HashSet<>(); // Set of URI's for Element types
	Set<String> stringTypes = new HashSet<>(); // set of classes which subtype from String
	Map<String, String> uriToClassName = new HashMap<>();
	Map<String, String> uriToPropertyName = new HashMap<>();
	List<Individual> allIndividuals;
	List<OntClass> allClasses;
	List<DatatypeProperty> allDataProperties;
	List<ObjectProperty> allObjectProperties;
	List<Resource> objectIndividuals;
	String versionSemVer;
	String versionSuffix;
	
	public enum PropertyType {
		ELEMENT,
		ANY_LICENSE_INFO,
		OBJECT,
		ENUM,
		BOOLEAN,
		INTEGER,
		STRING,
		OBJECT_COLLECTION,
		STRING_COLLECTION,
		OBJECT_SET, ENUM_COLLECTION,
		LICENSE_ADDITION,
		EXTENDABLE_LICENSE
	}
	
	
	public enum SuperclassRequired {
		YES, // All superclasses have this as a required property
		NO,	 // None of the superclasses have this as a required property
		BOTH, // Some superclasses have this as a required property and other superclasses do not
		NONE  // The property is not referenced in any of the superclasses
	}

	static String YEAR;
	
	static {
		Date date = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy");
		YEAR = dateFormat.format(date);
	}
	
	
	/**
	 * @param model model to use to generate the java files
	 */
	public ShaclToJava(OntModel model) {
		this.model = model;
		String spdxUri = model.getNsPrefixURI("spdx");
		versionSemVer = spdxUri.substring("https://spdx.org/rdf/".length());
		versionSemVer = versionSemVer.substring(0, versionSemVer.indexOf('/'));
		versionSuffix = "v" + versionSemVer.replaceAll("\\.", "_");
		shapes = Shapes.parse(model);
		shapeMap = shapes.getShapeMap();
		allIndividuals = model.listIndividuals().toList();
		allClasses = model.listClasses().toList();
		allDataProperties = model.listDatatypeProperties().toList();
		allObjectProperties = model.listObjectProperties().toList();
		objectIndividuals = new ArrayList<>();
		Query asIndividualQuery = QueryFactory.create(String.format(
				"select ?oi where {?oi <%s> <%s>}", ShaclToJavaConstants.TYPE_PRED, ShaclToJavaConstants.NAMED_INDIVIDUAL));
		try (QueryExecution qexec = QueryExecutionFactory.create(asIndividualQuery, model)) {
			ResultSet results = qexec.execSelect();
			for ( ; results.hasNext() ; ) {
				QuerySolution soln = results.nextSolution();
				objectIndividuals.add(soln.getResource("oi"));
			}
		}
		collectTypeInformation();
		collectRelationshipRestrictions();
		collectNameMappings();
	}
	
	/**
	 * Updates the uriNameMap and nameUriMap with the name and URI based on maintaining
	 * unique names.  If the name is not unique, the "core" profile gets the short name,
	 * otherwise the name is created by prepending the profile.
	 * @param uri Uri
	 * @param uriNameMap Map of URI's to names
	 * @param nameUriMap Map of names to URI
	 */
	private void createUriNameMapping(String uri, Map<String, String> uriNameMap, 
			Map<String, String> nameUriMap) {
		String name = uriToName(uri);
		if (uri.equals(nameUriMap.get(name))) {
			return; // already there
		} else if (nameUriMap.containsKey(name)) {
			String profile = uriToProfile(uri);
			if ("Core".equalsIgnoreCase(profile)) {
				// replace the existing short name
				String otherUri = nameUriMap.get(name);
				String otherProfile = uriToProfile(otherUri);
				String otherName = otherProfile + name.substring(0, 1).toUpperCase() + name.substring(1);
				uriNameMap.put(otherUri, otherName);
				nameUriMap.put(otherName, otherUri);
				uriNameMap.put(uri, name);
				nameUriMap.put(name, uri);
			} else {
				// use the profile in the name
				String nameWithProfile = profile + name.substring(0, 1).toUpperCase() + name.substring(1);
				uriNameMap.put(uri, nameWithProfile);
				nameUriMap.put(nameWithProfile, uri);
			}
		} else {
			uriNameMap.put(uri, name);
			nameUriMap.put(name, uri);
		}
	}
	
	/**
	 * Create the mapings from the URI's to class names and property names
	 */
	private void collectNameMappings() {
		uriToClassName.clear();
		Map<String, String> classNameToUri = new HashMap<>();
		for (OntClass ontClass:allClasses) {
			createUriNameMapping(ontClass.getURI(), uriToClassName, classNameToUri);
		}
		for (List<String> individualUris:classUriToIndividualUris.values()) {
			for (String individualUri:individualUris) {
				createUriNameMapping(individualUri, uriToClassName, classNameToUri);
			}
		}
		uriToPropertyName.clear();
		Map<String, String> propertyNameToUri = new HashMap<>();
		for (DatatypeProperty prop:allDataProperties) {
			createUriNameMapping(prop.getURI(), uriToPropertyName, propertyNameToUri);
		}
		for (ObjectProperty prop:allObjectProperties) {
			createUriNameMapping(prop.getURI(), uriToPropertyName, propertyNameToUri);
		}
	}

	/**
	 * Generates source and test files and store them in the dir
	 * @param dir Directory to hold the java source
	 * @return list of warnings - if empty, all files were generated successfully
	 * @throws IOException for any issues storing the files
	 * @throws ShaclToJavaException errors in the ontology
	 */
	public List<String> generate(File dir) throws IOException, ShaclToJavaException {
		List<String> warnings = new ArrayList<>();
		List<String> classUris = new ArrayList<>();
		List<Map<String, Object>> enumMustacheMaps = new ArrayList<>();
		List<String> createBuilderList = new ArrayList<>();
		Map<String, Map<String, Object>> javaClassMaps = new HashMap<>();
		Map<String, Map<String, Object>> unitTestMaps = new HashMap<>();
		Map<PropertyType, Map<String, Map<String, Object>>> allPropertiesInUse = new HashMap<>();
		allClasses.forEach(ontClass -> {
			String classUri = ontClass.getURI();
			if (classUri.startsWith("http://spdx.invalid.")) {
				return;
			}
			String comment = ontClass.getComment(null);
			classUris.add(classUri);
			String name = uriToClassName.get(classUri);
			Shape classShape = shapeMap.get(ontClass.asNode());
			Map<String, PropertyShape> propertyShapes = new HashMap<>();
			
			if (Objects.nonNull(classShape)) {
				for (PropertyShape ps : classShape.getPropertyShapes()) {
					propertyShapes.put(ps.getPath().toString(), ps);
				}
			}
			List<OntClass> subClasses = new ArrayList<>();
			ontClass.listSubClasses().forEach(oc -> {
				subClasses.add(oc);
			});
			List<OntClass> superClasses = new ArrayList<>();
			if (elementTypes.contains(classUri) ||
					licenseAdditionTypes.contains(classUri) ||
					extendableLicenseTypes.contains(classUri) ||
					anyLicenseInfoTypes.contains(classUri)) {
				String externalClassUri = classUriToExternalClassUri(classUri);
				classUris.add(externalClassUri);
				String externalClassName = "External" + uriToName(classUri);
				this.uriToClassName.put(externalClassUri, externalClassName);
			}
			addAllSuperClasses(ontClass, superClasses);
			
			for (OntClass superClass : superClasses) {
				Shape superClassShape = shapeMap.get(superClass.asNode());
				if (Objects.nonNull(superClassShape)) {
					for (PropertyShape ps : superClassShape.getPropertyShapes()) {
						
						if (!propertyShapes.containsKey(ps.getPath().toString())) {
							propertyShapes.put(ps.getPath().toString(), ps);
						}
					}
				}
			}
			
			String superClassUri = superClasses.isEmpty() ? null : superClasses.get(0).getURI();
			try {
				if (this.classUriToIndividualUris.containsKey(ontClass.getURI())) {
					// Generate the individuals
					for (String individualUri:this.classUriToIndividualUris.get(ontClass.getURI())) {
						try {
							generateIndividualClass(dir, individualUri, uriToClassName.get(individualUri),
									new ArrayList<>(propertyShapes.values()), getIndividualComment(individualUri),
									classUri, classShape, superClasses);
						} catch (ShaclToJavaException e) {
							warnings.add("Error generating Individual Java class for "+individualUri+":" + e.getMessage());
						}
					}
				}
				if (isEnumClass(ontClass)) {
					enumMustacheMaps.add(generateJavaEnum(dir, classUri, name, allIndividuals, comment));
				} else if (!stringTypes.contains(classUri)) { // TODO: we may want to handle String subtypes in the future
					try {
						boolean isAbstract = isAbstract(ontClass);
						String createString = fillMustachMapsForClass(classUri, name, new ArrayList<>(propertyShapes.values()),
								classShape, comment, superClassUri, superClasses, isAbstract, 
								javaClassMaps, unitTestMaps, allPropertiesInUse);
						if (!isAbstract) {
							createBuilderList.add(createString);
						}
						generateJavaClass(dir, classUri, javaClassMaps.get(classUri));
						if (elementTypes.contains(classUri)) {
							generateExternalJavaClass(dir, classUri, javaClassMaps.get(classUri));
						}
						if (!isAbstract) {
							generateUnitTest(dir, classUri, unitTestMaps.get(classUri));
						}
					} catch (ShaclToJavaException e) {
						warnings.add("Error generating Java class for "+name+":" + e.getMessage());
					}
				}
			} catch (IOException e) {
				warnings.add("I/O Error generating Java class for "+name+":" + e.getMessage());
			}
		});
		generateTestValueGenerator(dir, allPropertiesInUse, unitTestMaps);
		generateSpdxConstants(dir, classUris);
		generateEnumFactory(dir, enumMustacheMaps);
		generateModelClassFactory(dir, classUris);
		generateModelObject(dir, createBuilderList, classUris);
		generateSpdxModelInfo(dir);
		generatePackageInfo(dir);
		generatePomFile(dir);
		generateIndividualFactory(dir);
		//TODO: Get the version from the SHACL file
		generateMockFiles(dir);
		return warnings;
	}

	/**
	 * @param classUri class URI
	 * @return URI for a class which is the external form of the classUri
	 */
	private String classUriToExternalClassUri(String classUri) {
		String nameSpaceUri = uriToNamespaceUri(classUri);
		String className = classUri.substring(nameSpaceUri.length()+1);
		return nameSpaceUri + "/External" + className;
	}


	/**
	 * @param dir directory for the source files
	 * @param allPropertiesInUse Map of all properties
	 * @param unitTestMaps all unit test maps
	 * @throws IOException on IO error writing file
	 */
	private void generateTestValueGenerator(File dir,
			Map<PropertyType, Map<String, Map<String, Object>>> allPropertiesInUse,
			Map<String, Map<String, Object>> unitTestMaps) throws IOException {
		Map<String, Object> mustacheMap = new HashMap<>();
		mustacheMap.put("versionSuffix", versionSuffix);
		mustacheMap.put("versionSemVer", versionSemVer);
		Set<String> requiredImports = new HashSet<>();
		for (Entry<PropertyType, Map<String, Map<String, Object>>> entry:allPropertiesInUse.entrySet()) {
			List<Map<String, Object>> propertiesForType = new ArrayList<>();
			for (Map<String, Object> propertyMap:entry.getValue().values()) {
				propertiesForType.add(propertyMap);
				String typeUri = (String)propertyMap.get("typeUri");
				if (PropertyType.ENUM.equals(entry.getKey()) || PropertyType.OBJECT.equals(entry.getKey()) ||
								PropertyType.OBJECT_COLLECTION.equals(entry.getKey()) || 
								PropertyType.ENUM_COLLECTION.equals(entry.getKey()) || 
								PropertyType.OBJECT_SET.equals(entry.getKey()) ||
								PropertyType.ANY_LICENSE_INFO.equals(entry.getKey()) ||
								PropertyType.LICENSE_ADDITION.equals(entry.getKey()) ||
								PropertyType.EXTENDABLE_LICENSE.equals(entry.getKey()) ||
								PropertyType.ELEMENT.equals(entry.getKey())) {				
					requiredImports.add("import "+uriToPkg(typeUri) + "." + uriToClassName.get(typeUri) +";");
				}
			}
			String propTypeStr = "";
			switch (entry.getKey()) {
				case ELEMENT: propTypeStr = "elementProperties"; break;
				case OBJECT: propTypeStr = "objectProperties"; break;
				case LICENSE_ADDITION: propTypeStr = "licenseAdditionProperties"; break;
				case EXTENDABLE_LICENSE: propTypeStr = "extendableLicenseProperties"; break;
				case ANY_LICENSE_INFO: propTypeStr = "anyLicenseInfoProperties"; break;
				case ENUM: propTypeStr = "enumerationProperties"; break;
				case BOOLEAN: propTypeStr = "booleanProperties"; break;
				case INTEGER: propTypeStr = "integerProperties"; break;
				case STRING: propTypeStr = "stringProperties"; break;
				case OBJECT_COLLECTION: propTypeStr = "objectPropertyValueCollection"; break;
				case STRING_COLLECTION: propTypeStr = "stringCollection"; break;
				case OBJECT_SET: propTypeStr = "objectPropertyValueSet"; break;
				case ENUM_COLLECTION: propTypeStr = "enumPropertyValueCollection"; break;
				default: throw new RuntimeException("Unknown prop type: "+entry.getKey());
			}
			mustacheMap.put(propTypeStr, propertiesForType);
		}
		
		List<Map<String, Object>> classesMaps = new ArrayList<>();
		for (Entry<String, Map<String, Object>> entry:unitTestMaps.entrySet()) {
			classesMaps.add(entry.getValue());
			boolean isAbstract = (Boolean)entry.getValue().get("abstract");
			if (isAbstract) {
				requiredImports.add("import "+uriToPkg(entry.getKey()) + "." + uriToClassName.get(entry.getKey()) + ";");
			}
			requiredImports.add("import "+uriToPkg(entry.getKey()) + "." + uriToClassName.get(entry.getKey()) +
					"." + uriToClassName.get(entry.getKey()) + "Builder;");
		}
		mustacheMap.put("classesForBuilders", classesMaps);
		requiredImports.add("import java.util.Arrays;");
		requiredImports.add("import org.spdx.core.IModelCopyManager;");
		requiredImports.add("import org.spdx.core.InvalidSPDXAnalysisException;");
		requiredImports.add("import org.spdx.core.ModelRegistry;");
		requiredImports.add("import org.spdx.library.model."+versionSuffix+".core.CreationInfo;");
		requiredImports.add("import org.spdx.library.model."+versionSuffix+".core.RelationshipCompleteness;");
		requiredImports.add("import org.spdx.library.model."+versionSuffix+".core.RelationshipType;");
		requiredImports.add("import org.spdx.library.model."+versionSuffix+".core.Agent.AgentBuilder;");
		requiredImports.add("import org.spdx.storage.IModelStore;");
		requiredImports.add("import org.spdx.storage.IModelStore.IdType;");
		requiredImports.add("import java.util.List;");
		requiredImports.add("import java.util.Objects;");
		requiredImports.add("import java.util.Collection;");
		requiredImports.add("import java.util.Map;");
		requiredImports.add("import java.util.HashMap;");
		List<String> importList = new ArrayList<String>(requiredImports);
		Collections.sort(importList);
		mustacheMap.put("imports", importList);
		Path path = dir.toPath().resolve("src").resolve("test").resolve("java").resolve("org")
				.resolve("spdx").resolve("library").resolve("model").resolve(versionSuffix);
		Files.createDirectories(path);
		File testValuesGeneratorFile = path.resolve("TestValuesGenerator.java").toFile();
		testValuesGeneratorFile.createNewFile();
		writeMustacheFile(ShaclToJavaConstants.TEST_VALUES_GENERATOR_TEMPLATE, testValuesGeneratorFile, mustacheMap);
	}

	/**
	 * Generates the test mock files
	 * @param dir
	 * @throws IOException 
	 */
	private void generateMockFiles(File dir) throws IOException {
		Path path = dir.toPath().resolve("src").resolve("test").resolve("java").resolve("org")
				.resolve("spdx").resolve("library").resolve("model").resolve(versionSuffix);
		Map<String, Object> mustacheMap = new HashMap<>();
		mustacheMap.put("versionSuffix", versionSuffix);
		mustacheMap.put("specVersion", versionSemVer);
		Files.createDirectories(path);
		File mockModelStoreFile = path.resolve("MockModelStore.java").toFile();
		mockModelStoreFile.createNewFile();
		writeMustacheFile(ShaclToJavaConstants.MOCK_MODEL_STORE_TEMPLATE, mockModelStoreFile, mustacheMap);
		File mockCopyManager = path.resolve("MockCopyManager.java").toFile();
		mockCopyManager.createNewFile();
		writeMustacheFile(ShaclToJavaConstants.MOCK_COPY_MANAGER_TEMPLATE, mockCopyManager, mustacheMap);
		File unitTestHelper = path.resolve("UnitTestHelper.java").toFile();
		unitTestHelper.createNewFile();
		writeMustacheFile(ShaclToJavaConstants.UNIT_TEST_HELPER_TEMPLATE, unitTestHelper, mustacheMap);
		File testModelInfoFile = path.resolve("TestSpdxModelInfo.java").toFile();
		testModelInfoFile.createNewFile();
		writeMustacheFile(ShaclToJavaConstants.TEST_MODEL_INFO_TEMPLATE, testModelInfoFile, mustacheMap);
	}

	/**
	 * @param individualUri
	 * @return
	 */
	private String getIndividualComment(String individualUri) {
		Resource individualResource = model.getResource(individualUri);
		Statement stmt = individualResource.getProperty(model.getProperty(ShaclToJavaConstants.COMMENT_URI));
		if (Objects.isNull(stmt) || !stmt.getObject().isLiteral()) {
			return "";
		}
		return stmt.getObject().asLiteral().getString();
	}

	/**
	 * @param dir
	 * @throws IOException 
	 */
	private void generatePomFile(File dir) throws IOException {
		Path path = dir.toPath();
		Files.createDirectories(path);
		File file = path.resolve("pom.xml").toFile();
		file.createNewFile();
		writeMustacheFile(ShaclToJavaConstants.POM_TEMPLATE, file, new HashMap<>());
	}

	/**
	 * @param dir
	 * @throws IOException 
	 */
	private void generatePackageInfo(File dir) throws IOException {
		Path path = dir.toPath().resolve("src").resolve("main").resolve("java").resolve("org")
				.resolve("spdx").resolve("library").resolve("model").resolve(versionSuffix);
		Files.createDirectories(path);
		File file = path.resolve("package-info.java").toFile();
		file.createNewFile();
		Map<String, Object> mustacheMap = new HashMap<>();
		mustacheMap.put("versionSuffix", versionSuffix);
		mustacheMap.put("versionSemVer", versionSemVer);
		writeMustacheFile(ShaclToJavaConstants.PACKAGE_INFO_TEMPLATE, file, mustacheMap);
	}

	/**
	 * @param dir
	 * @throws IOException 
	 */
	private void generateSpdxModelInfo(File dir) throws IOException {
		Path path = dir.toPath().resolve("src").resolve("main").resolve("java").resolve("org")
				.resolve("spdx").resolve("library").resolve("model").resolve(versionSuffix);
		Files.createDirectories(path);
		File file = path.resolve("SpdxModelInfoV3_0.java").toFile();
		file.createNewFile();
		Map<String, Object> mustacheMap = new HashMap<>();
		mustacheMap.put("versionSuffix", versionSuffix);
		mustacheMap.put("versionSemVer", versionSemVer);
		writeMustacheFile(ShaclToJavaConstants.MODEL_INFO_TEMPLATE, file, mustacheMap);
	}

	/**
	 * @param dir
	 * @param createBuilderList
	 * @param classUris
	 * @throws IOException 
	 */
	private void generateModelObject(File dir, List<String> createBuilderList, List<String> classUris) throws IOException {
		Path path = dir.toPath().resolve("src").resolve("main").resolve("java").resolve("org")
				.resolve("spdx").resolve("library").resolve("model").resolve(versionSuffix);
		Files.createDirectories(path);
		File file = path.resolve("ModelObjectV3.java").toFile();
		file.createNewFile();
		Map<String, Object> mustacheMap = new HashMap<>();
		mustacheMap.put("createBuilder", createBuilderList);
		mustacheMap.put("versionSuffix", versionSuffix);
		mustacheMap.put("versionSemVer", versionSemVer);
		List<String> imports = new ArrayList<>();
		for (String classUri:classUris) {
			//TODO: Don't add abstract classes
			if (!enumClassUris.contains(classUri) && !enumerationTypes.contains(classUri)) {
				imports.add("import "+uriToPkg(classUri) + "." + uriToClassName.get(classUri) +";");
			}
		}
		imports.add("import org.spdx.library.model."+versionSuffix+".core.ProfileIdentifierType;");
		Collections.sort(imports);
		mustacheMap.put("imports", imports);
		writeMustacheFile(ShaclToJavaConstants.BASE_MODEL_OBJECT_TEMPLATE, file, mustacheMap);
	}
	
	/**
	 * Collect all relationship restrictions
	 */
	private void collectRelationshipRestrictions() {
		for (Shape shape:shapes) {
			if (shape.isNodeShape()) {
				for (Constraint constraint:shape.getConstraints()) {
					if (constraint instanceof SparqlConstraint) {
						SparqlConstraint sparqlConstraint = (SparqlConstraint)constraint;
						Element queryPattern = sparqlConstraint.getQuery().getQueryPattern();
						if (queryPattern instanceof ElementGroup && ((ElementGroup)queryPattern).size() == 1) {
							// TODO: Implement
						}
					}
				}
			}

		}
	}

	/**
	 * Generates the Enum Factory file
	 * @param dir source directory for the factory file
	 * @param enumMustacheMaps list of mustache maps for the enum classes
	 * @throws IOException thrown if any IO errors occurs
	 */
	private void generateEnumFactory(File dir,
			List<Map<String, Object>> enumMustacheMaps) throws IOException {
		Map<String, Object> mustacheMap = new HashMap<>();
		mustacheMap.put("enumClasses", enumMustacheMaps);
		Set<String> pkgs = new HashSet<>();
		for (Map<String, Object> map:enumMustacheMaps) {
			pkgs.add((String)map.get("pkgName") + "." + (String)map.get("name"));
		}
		List<String> imports = new ArrayList<>();
		for (String pkg:pkgs) {
			imports.add("import "+pkg+";");
		}
		Collections.sort(imports);
		mustacheMap.put("imports", imports);
		mustacheMap.put("versionSuffix", versionSuffix);
		Path path = dir.toPath().resolve("src").resolve("main").resolve("java").resolve("org")
				.resolve("spdx").resolve("library").resolve("model").resolve(versionSuffix);
		Files.createDirectories(path);
		File enumFactoryFile = path.resolve("SpdxEnumFactory.java").toFile();
		enumFactoryFile.createNewFile();	
		writeMustacheFile(ShaclToJavaConstants.ENUM_FACTORY_TEMPLATE, enumFactoryFile, mustacheMap);
	}
	
	/**
	 * Generates the SPDX Individual factory file
	 * @param dir source directory for the factory file
	 * @param individualMustacheMaps list of mustache maps for the individual vocabulariess
	 * @throws IOException thrown if any IO errors occurs
	 */
	private void generateIndividualFactory(File dir) throws IOException {
		Map<String, Object> mustacheMap = new HashMap<>();
		mustacheMap.put("versionSuffix", versionSuffix);
		List<Map<String, String>> individualMustacheMaps = new ArrayList<>();
		List<String> imports = new ArrayList<>();
		for (List<String> individuals:this.classUriToIndividualUris.values()) {
			for (String individualUri:individuals) {
				String className = uriToClassName.get(individualUri);
				String pkg = uriToPkg(individualUri);
				Map<String, String> individualMustacheMap = new HashMap<>();
				individualMustacheMap.put("individualUri", individualUri);
				individualMustacheMap.put("className", className);
				individualMustacheMaps.add(individualMustacheMap);
				String importStr = "import "+pkg+"."+className + ";";
				if (!imports.contains(importStr)) {
					imports.add(importStr);
				}
			}
		}
		Collections.sort(imports);
		mustacheMap.put("imports", imports);
		mustacheMap.put("individuals", individualMustacheMaps);
		Path path = dir.toPath().resolve("src").resolve("main").resolve("java").resolve("org")
				.resolve("spdx").resolve("library").resolve("model").resolve(versionSuffix);
		Files.createDirectories(path);
		File individualsFile = path.resolve("SpdxIndividualFactory.java").toFile();
		individualsFile.createNewFile();	
		writeMustacheFile(ShaclToJavaConstants.INDIVIDUALS_FACTORY_TEMPLATE, individualsFile, mustacheMap);
	}

	/**
	 * Generates the SPDX Constants file
	 * @param dir source directory for the constants file
	 * @param classUris list of all class URIs
	 * @throws IOException thrown if any IO errors occurs
	 */
	private void generateSpdxConstants(File dir, List<String> classUris) throws IOException {
		Map<String, Set<String>> namespaceToPropUri = new HashMap<>();
		for (String propUri:propertyUrisForConstants) {
			String nameSpaceUri = this.uriToNamespaceUri(propUri);
			Set<String> propUriSet = namespaceToPropUri.get(nameSpaceUri);
			if (Objects.isNull(propUriSet)) {
				propUriSet = new HashSet<>();
				namespaceToPropUri.put(nameSpaceUri, propUriSet);
			}
			propUriSet.add(propUri);
		}
		Map<String, Object> mustacheMap = new HashMap<>();
		mustacheMap.put("versionSuffix", versionSuffix);
		List<Map<String, Object>> namespaceMustacheList = new ArrayList<>();
		List<String> namespaceUris = new ArrayList<String>(namespaceToPropUri.keySet());
		Collections.sort(namespaceUris);
		for (String namespaceUri:namespaceUris) {
			Map<String, Object> namespaceMustacheMap = new HashMap<>();
			String namespaceName = uriToName(namespaceUri);
			namespaceMustacheMap.put("namespaceName", namespaceName);
			String namespaceConstantName = camelCaseToConstCase(namespaceName) + "_NAMESPACE";
			namespaceMustacheMap.put("namespaceConstantName", namespaceConstantName);
			namespaceMustacheMap.put("namespaceUri", namespaceUri);
			List<String> propertyUris = new ArrayList<>(namespaceToPropUri.get(namespaceUri));
			Collections.sort(propertyUris);
			List<Map<String, Object>> propMustacheList = new ArrayList<>();
			for (String propUri:propertyUris) {
				Map<String, Object> propMustacheMap = new HashMap<>();
				String propertyName = uriToPropertyName.get(propUri);
				String propertyConstantName = propertyNameToPropertyConstant(propertyName, namespaceName);
				propMustacheMap.put("propertyConstantName", propertyConstantName);
				propMustacheMap.put("propertyConstantValue", propertyName);
				propMustacheList.add(propMustacheMap);
			}
			namespaceMustacheMap.put("propertyDescriptors", propMustacheList);
			namespaceMustacheList.add(namespaceMustacheMap);
		}
		mustacheMap.put("namespaces", namespaceMustacheList);
		List<String> classConstantDefinitions = new ArrayList<>();
		List<String> classConstants = new ArrayList<>();
		for (String classUri:classUris) {
			String className = uriToClassName.get(classUri);
			String profile = uriToProfile(classUri);
			String constName = camelCaseToConstCase(profile) + "_" + camelCaseToConstCase(className);
			classConstantDefinitions.add("static final String " + constName + " = \"" + profile + "." + className + "\";");
			classConstants.add(constName);
		}
		// Add class constants for the individuals
		for (List<String> individualUris:this.classUriToIndividualUris.values()) {
			for (String individualUri:individualUris) {
				String className = uriToClassName.get(individualUri);
				String profile = uriToProfile(individualUri);
				String constName = camelCaseToConstCase(profile) + "_" + camelCaseToConstCase(className);
				classConstantDefinitions.add("static final String " + constName + " = \"" + profile + "." + className + "\";");
				classConstants.add(constName);
			}
		}
		// Add in constants for the external classes
		classConstantDefinitions.add("static final String " + "EXTERNAL_ELEMENT" + " = \"Core.ExternalElement\";");
		classConstants.add("EXTERNAL_ELEMENT");
		
		classConstantDefinitions.add("static final String " + "EXTERNAL_CUSTOM_LICENSE" + " = \"ExpandedLicensing.ExternalCustomLicense\";");
		classConstants.add("EXTERNAL_CUSTOM_LICENSE");
		
		classConstantDefinitions.add("static final String " + "EXTERNAL_CUSTOM_LICENSE_ADDITION" + " = \"ExpandedLicensing.ExternalCustomLicenseAddition\";");
		classConstants.add("EXTERNAL_CUSTOM_LICENSE_ADDITION");
		
		StringBuilder classConstantString = new StringBuilder("static final String[] ALL_SPDX_CLASSES = {");
		int lineLen = classConstantString.length();
		if (classConstants.size() > 0) {
			classConstantString.append(classConstants.get(0));
			for (int i = 1; i < classConstants.size(); i++) {
				classConstantString.append(", ");
				lineLen = lineLen + 2;
				if (lineLen > 70) {
					classConstantString.append("\n\t\t\t");
					lineLen = 0;
				}
				classConstantString.append(classConstants.get(i));
				lineLen = lineLen + classConstants.get(i).length();
			}
		}
		classConstantString.append("};");
		mustacheMap.put("classConstantDefinitions", classConstantDefinitions);
		mustacheMap.put("allClassConstants", classConstantString.toString());
		mustacheMap.put("versionSemVer", versionSemVer);
		Path path = dir.toPath().resolve("src").resolve("main").resolve("java").resolve("org")
				.resolve("spdx").resolve("library").resolve("model").resolve(versionSuffix);
		Files.createDirectories(path);
		File constantsFile = path.resolve("SpdxConstantsV3.java").toFile();
		constantsFile.createNewFile();	
		writeMustacheFile(ShaclToJavaConstants.SPDX_CONSTANTS_TEMPLATE, constantsFile, mustacheMap);
	}
	
	/**
	 * Generates the SPDX Model Class Factory source file
	 * @param dir source directory for the constants file
	 * @param classUris list of all class URIs
	 * @throws IOException thrown if any IO errors occurs
	 */
	private void generateModelClassFactory(File dir, List<String> classUris) throws IOException {		
		Map<String, Object> mustacheMap = new HashMap<>();	
		mustacheMap.put("versionSuffix", versionSuffix);
		List<Map<String, String>> typeToClasses = new ArrayList<>();
		for (String classUri:classUris) {
			String className = uriToClassName.get(classUri);
			String profile = uriToProfile(classUri);
			String packageName = uriToPkg(classUri);
			String classConstant = camelCaseToConstCase(profile) + "_" + camelCaseToConstCase(className);
			String classPath = packageName + "." + className;
			Map<String, String> typeToClassMap = new HashMap<>();
			typeToClassMap.put("classConstant", classConstant);
			typeToClassMap.put("classPath", classPath);
			typeToClasses.add(typeToClassMap);
		}
		
		// Add individual types
		for (List<String> individualUris:this.classUriToIndividualUris.values()) {
			for (String individualUri:individualUris) {
				String className = uriToClassName.get(individualUri);
				String profile = uriToProfile(individualUri);
				String packageName = uriToPkg(individualUri);
				String classConstant = camelCaseToConstCase(profile) + "_" + camelCaseToConstCase(className);
				String classPath = packageName + "." + className;
				Map<String, String> typeToClassMap = new HashMap<>();
				typeToClassMap.put("classConstant", classConstant);
				typeToClassMap.put("classPath", classPath);
				typeToClasses.add(typeToClassMap);
			}
		}
		
		mustacheMap.put("typeToClass", typeToClasses);
		Path path = dir.toPath().resolve("src").resolve("main").resolve("java").resolve("org")
				.resolve("spdx").resolve("library").resolve("model").resolve(versionSuffix);
		Files.createDirectories(path);
		File modelClassFactoryFile = path.resolve("SpdxModelClassFactoryV3.java").toFile();
		modelClassFactoryFile.createNewFile();	
		writeMustacheFile(ShaclToJavaConstants.MODEL_CLASS_FACTORY_TEMPLATE, modelClassFactoryFile, mustacheMap);
	}
	
	private String mustacheToString(String templateName, Map<String, Object> mustacheMap) throws IOException {
		String templateDirName = ShaclToJavaConstants.TEMPLATE_ROOT_PATH;
		File templateDirectoryRoot = new File(templateDirName);
		if (!(templateDirectoryRoot.exists() && templateDirectoryRoot.isDirectory())) {
			templateDirName = ShaclToJavaConstants.TEMPLATE_CLASS_PATH;
		}
		DefaultMustacheFactory builder = new DefaultMustacheFactory(templateDirName);
		Mustache mustache = builder.compile(templateName);
		StringWriter writer = new StringWriter();
		try {
			mustache.execute(writer, mustacheMap);
			return writer.toString();
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}
	
	private void writeMustacheFile(String templateName, File file, Map<String, Object> mustacheMap) throws IOException {
		String templateDirName = ShaclToJavaConstants.TEMPLATE_ROOT_PATH;
		File templateDirectoryRoot = new File(templateDirName);
		if (!(templateDirectoryRoot.exists() && templateDirectoryRoot.isDirectory())) {
			templateDirName = ShaclToJavaConstants.TEMPLATE_CLASS_PATH;
		}
		DefaultMustacheFactory builder = new DefaultMustacheFactory(templateDirName);
		Mustache mustache = builder.compile(templateName);
		FileOutputStream stream = null;
		OutputStreamWriter writer = null;
		try {
			stream = new FileOutputStream(file);
			writer = new OutputStreamWriter(stream, "UTF-8");
	        mustache.execute(writer, mustacheMap);
		} finally {
			if (writer != null) {
				writer.close();
			}
			if (stream != null) {
				stream.close();
			}
		}
	}

	/**
	 * Collect type information into the field sets for enum types, anylicenseinfo types, and elements types.  also fills in the enum class URIs
	 */
	private void collectTypeInformation() {
		for (Resource individual:objectIndividuals) {
			boolean hasLabel = false;
			String individualClassUri = null;
			String rangeUri = null;
			StmtIterator propertyIter = individual.listProperties();
			for ( ; propertyIter.hasNext() ; ) {
				Statement stmt = propertyIter.next();
				if (stmt.getPredicate().getURI().equals(ShaclToJavaConstants.TYPE_PRED) && stmt.getObject().isURIResource() &&
						!stmt.getObject().asResource().getURI().equals(ShaclToJavaConstants.NAMED_INDIVIDUAL)) {
					individualClassUri = stmt.getObject().asResource().getURI();
				}
				if (stmt.getPredicate().getURI().equals(ShaclToJavaConstants.RANGE_URI) && stmt.getObject().isURIResource()) {
					rangeUri = stmt.getObject().asResource().getURI();
				}
				if (stmt.getPredicate().getURI().equals(ShaclToJavaConstants.LABEL_URI)) {
					hasLabel = true;
				}
			}
			if (hasLabel && Objects.nonNull(individualClassUri)) {
				// TODO: This is a bit of a hack, maybe there is a better way to see if this is a class
				this.enumClassUris.add(individualClassUri);
			} else if (Objects.nonNull(rangeUri)) {
				List<String> individualsForRange = classUriToIndividualUris.get(rangeUri);
				if (Objects.isNull(individualsForRange)) {
					individualsForRange = new ArrayList<>();
					classUriToIndividualUris.put(rangeUri, individualsForRange);
				}
				individualsForRange.add(individual.getURI());
			}
		}
		
//		for (Individual individual:allIndividuals) {
//			if (Objects.nonNull(individual.getLabel(null))) {
//				// TODO: This is a bit of a hack, maybe there is a better way to see if this is a class
//				this.enumClassUris.add(individual.getOntClass(true).getURI());
//			} else {
//				this.individuals.add(individual);
//			}
//		}
		allClasses.forEach(ontClass -> {
			List<OntClass> superClasses = new ArrayList<>();
			addAllSuperClasses(ontClass, superClasses);
			if (isEnumClass(ontClass)) {
				enumerationTypes.add(ontClass.getURI());
			} else if (isStringClass(ontClass, superClasses)) {
				stringTypes.add(ontClass.getURI());
			} else {
				if (isLicenseAdditionClass(ontClass, superClasses)) {
					licenseAdditionTypes.add(ontClass.getURI());
				}
				if (isExtendableLicenseClass(ontClass, superClasses)) {
					extendableLicenseTypes.add(ontClass.getURI());
				}
				if (isAnyLicenseInfoClass(ontClass, superClasses)) {
					anyLicenseInfoTypes.add(ontClass.getURI());
				}
				if (isElementClass(ontClass, superClasses)) {
					elementTypes.add(ontClass.getURI());
				}
			} 
		});
	}
	
	/**
	 * @param classTypeRestriction class restriction if any
	 * @param dataTypeRestriction data restriction if any
	 * @return The URI for the type of a property based on it's range and restrictions
	 * @throws ShaclToJavaException 
	 */
	private String getTypeUri(@Nullable Node classTypeRestriction,
			@Nullable Node dataTypeRestriction) throws ShaclToJavaException {
		// precedence - class restrictions, data restriction
		if (Objects.nonNull(classTypeRestriction) && Objects.nonNull(classTypeRestriction.getURI())) {
			return classTypeRestriction.getURI();
		} else if (Objects.nonNull(dataTypeRestriction) && Objects.nonNull(dataTypeRestriction.getURI())) {
			if (dataTypeRestriction.getURI().startsWith(ShaclToJavaConstants.SPDX_URI_PREFIX)) {
				//TODO: Currently, all data type restrictions have a base type of String
				// Once the spec parser produces the type information for data types, this should be updated
				// to use the specified base type and the pattern
				return ShaclToJavaConstants.STRING_TYPE;
			} else {
				return dataTypeRestriction.getURI();
			}
		} else {
			throw new ShaclToJavaException("Unable to determine type URI");
		}
	}
	
	/**
	 * @param classTypeRestriction class restriction if any
	 * @param dataTypeRestriction data restriction if any
	 * @param minRestriction minimum cardinality restriction if any
	 * @param maxRextriction maximum cardinality restriction if any
	 * @return the property type based on the range and restrictions
	 * @throws ShaclToJavaException 
	 */
	private PropertyType determinePropertyType(@Nullable Node classTypeRestriction,
			@Nullable Node dataTypeRestriction, @Nullable Integer minRestriction, @Nullable Integer maxRestriction) throws ShaclToJavaException {
		String typeUri = getTypeUri(classTypeRestriction, dataTypeRestriction);
		if (enumerationTypes.contains(typeUri)) {
			if (Objects.isNull(maxRestriction) || maxRestriction > 1) {
				return PropertyType.ENUM_COLLECTION;
			} else {
				return PropertyType.ENUM;
			}
		} else if (ShaclToJavaConstants.BOOLEAN_TYPE.equals(typeUri)) {
			return PropertyType.BOOLEAN;
		} else if  (ShaclToJavaConstants.INTEGER_TYPES.contains(typeUri)) {
			return PropertyType.INTEGER;
		} else if  (ShaclToJavaConstants.STRING_TYPE.equals(typeUri) || ShaclToJavaConstants.DATE_TIME_TYPE.equals(typeUri) ||
				ShaclToJavaConstants.ANY_URI_TYPE.equals(typeUri) || stringTypes.contains(typeUri)) {
			if (Objects.isNull(maxRestriction) || maxRestriction > 1) {
				return PropertyType.STRING_COLLECTION;
			} else {
				return PropertyType.STRING;
			}
			// If we get here, we're dealing with objects
		} else if (ShaclToJavaConstants.SET_TYPE_SUFFIXES.contains(typeUri.substring(typeUri.lastIndexOf("/terms/")))) {
			return PropertyType.OBJECT_SET;
		} else if (Objects.isNull(maxRestriction) || maxRestriction > 1) {
			return PropertyType.OBJECT_COLLECTION;
		} else if (licenseAdditionTypes.contains(typeUri)) {
			return PropertyType.LICENSE_ADDITION;
		} else if (extendableLicenseTypes.contains(typeUri)) {
			return PropertyType.EXTENDABLE_LICENSE;
		} else if (anyLicenseInfoTypes.contains(typeUri)) {
			return PropertyType.ANY_LICENSE_INFO;
//		} else if (elementTypes.contains(typeUri)) {
//			return PropertyType.ELEMENT;
		} else {
			return PropertyType.OBJECT;
		}
	}
	
	/**
	 * Add super classes including transitive superclasses to the superClasses list.
	 * The classes will be in the order of the closest superclass to the ontClass
	 * @param ontClass class to add superClasses for
	 * @param superClasses
	 */
	private void addAllSuperClasses(OntClass ontClass,
			List<OntClass> superClasses) {
		ontClass.listSuperClasses().forEach(superClass -> {
			superClasses.add(superClass);
			addAllSuperClasses(superClass, superClasses);
		});
	}

	/**
	 * @param ontClass class
	 * @param superClasses list of all superclasses for the class
	 * @return true if the class is an Element or a subclass of String
	 */
	private boolean isStringClass(OntClass ontClass,
			List<OntClass> superClasses) {
		if (ShaclToJavaConstants.STRING_TYPE.equals(ontClass.getURI())) {
			return true;
		}
		for (OntClass superClass:superClasses) {
			if (ShaclToJavaConstants.STRING_TYPE.equals(superClass.getURI())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param ontClass class
	 * @param superClasses list of all superclasses for the class
	 * @return true if the class is an Element or a subclass of Element
	 */
	private boolean isElementClass(OntClass ontClass,
			List<OntClass> superClasses) {
		if (ontClass.getURI().endsWith(ShaclToJavaConstants.ELEMENT_TYPE_SUFFIX)) {
			return true;
		}
		for (OntClass superClass:superClasses) {
			if (superClass.getURI().endsWith(ShaclToJavaConstants.ELEMENT_TYPE_SUFFIX)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param ontClass class
	 * @param superClasses list of all superclasses for the class
	 * @return true if the class is an AnyLicenseInfo or a subclass of AnyLicenseInfo
	 */
	private boolean isAnyLicenseInfoClass(OntClass ontClass,
			List<OntClass> superClasses) {
		if (ontClass.getURI().endsWith(ShaclToJavaConstants.ELEMENT_TYPE_ANY_LICENSE_INFO_SUFFIX)) {
			return true;
		}
		// We don't include superclasses for AnyLicenseInfo types
		return false;
	}
	
	/**
	 * @param ontClass class
	 * @param superClasses list of all superclasses for the class
	 * @return true if the class is an AnyLicenseInfo or a subclass of AnyLicenseInfo
	 */
	private boolean isExtendableLicenseClass(OntClass ontClass,
			List<OntClass> superClasses) {
		if (ontClass.getURI().endsWith(ShaclToJavaConstants.ELEMENT_TYPE_EXTENDABLE_LICENSE_SUFFIX)) {
			return true;
		}
		// We don't include superclasses for AnyLicenseInfo types
		return false;
	}
	
	/**
	 * @param ontClass class
	 * @param superClasses list of all superclasses for the class
	 * @return true if the class is an AnyLicenseInfo or a subclass of AnyLicenseInfo
	 */
	private boolean isLicenseAdditionClass(OntClass ontClass,
			List<OntClass> superClasses) {
		if (ontClass.getURI().endsWith(ShaclToJavaConstants.ELEMENT_TYPE_LICENSE_ADDITION_SUFFIX)) {
			return true;
		}
		// We don't include superclasses for AnyLicenseInfo types
		return false;
	}

	/**
	 * @param ontClass class
	 * @return true if the class is an enumeration
	 */
	private boolean isEnumClass(OntClass ontClass) {
		if (ontClass.isEnumeratedClass()) {
			return true;
		}
		//TODO: Switch to enums to remove this hack
		@SuppressWarnings("unused")
		List<OntProperty> properties = ontClass.listDeclaredProperties().toList();
		return this.enumClassUris.contains(ontClass.getURI());
	}
	
	/**
	 * Fills in the maps needed for generating the test values generator, java class, and unit test files
	 * @param classUri URI for the class
	 * @param name local name for the class
	 * @param propertyShapes properties for the class
	 * @param classShape Shape for the class
	 * @param comment Description of the class
	 * @param superClassUri URI of the superclass (if any)
	 * @param superClasses all superclasses for the class
	 * @param abstractClass if true, the class is abstract
	 * @param javaClassMaps Map of OntClass to the mustache map for generating the java classes
	 * @param unitTestMaps Map of the OntClass to the mustachMap for generating the unit test classes
	 * @return Code to create the Java object to be appended to the model object source file
	 * @throws IOException 
	 * @throws ShaclToJavaException 
	 */
	private String fillMustachMapsForClass(String classUri, String name,
			List<PropertyShape> propertyShapes, Shape classShape, String comment, 
			@Nullable String superClassUri, List<OntClass> superClasses,
			boolean abstractClass, Map<String, Map<String, Object>> javaClassMaps,
			Map<String, Map<String, Object>> unitTestMaps,
			Map<PropertyType, Map<String, Map<String, Object>>> allPropertiesInUse) throws IOException, ShaclToJavaException {
		String pkgName = uriToPkg(classUri);
		
		Set<String> requiredImports = new HashSet<>();
		Map<String, Object> javaClassMap = new HashMap<>();
		
		javaClassMap.put("abstract", abstractClass);
		javaClassMap.put("className", name);
		javaClassMap.put("classProfile", uriToProfile(classUri));
		Map<PropertyType, List<Map<String, Object>>> propertyMap = findProperties(propertyShapes, classShape, 
				requiredImports, propertyUrisForConstants, classUri, superClasses);
		for (Entry<PropertyType, List<Map<String, Object>>> entry:propertyMap.entrySet()) {
			for (Map<String, Object> propMap:entry.getValue()) {
				Map<String, Map<String, Object>> allPropertiesForType = allPropertiesInUse.get(entry.getKey());
				if (Objects.isNull(allPropertiesForType)) {
					allPropertiesForType = new HashMap<>();
					allPropertiesInUse.put(entry.getKey(), allPropertiesForType);
				}
				String propertyUri = (String)propMap.get("uri");
				if (!allPropertiesForType.containsKey(propertyUri)) {
					allPropertiesForType.put(propertyUri, propMap);
				}
			}
		}
		int numProperties = 0;
		for (List<Map<String, Object>> props:propertyMap.values()) {
			numProperties += props.size();
		}
		if (numProperties > 0) {
			requiredImports.add("import org.spdx.library.model."+versionSuffix+".SpdxConstantsV3;");
			requiredImports.add("import java.util.Optional;");
		}
		javaClassMap.put("elementProperties", propertyMap.get(PropertyType.ELEMENT));
		javaClassMap.put("objectProperties", propertyMap.get(PropertyType.OBJECT));
		javaClassMap.put("anyLicenseInfoProperties", propertyMap.get(PropertyType.ANY_LICENSE_INFO));
		javaClassMap.put("licenseAdditionProperties", propertyMap.get(PropertyType.LICENSE_ADDITION));
		javaClassMap.put("extendableLicenseProperties", propertyMap.get(PropertyType.EXTENDABLE_LICENSE));
		javaClassMap.put("enumerationProperties", propertyMap.get(PropertyType.ENUM));
		javaClassMap.put("booleanProperties", propertyMap.get(PropertyType.BOOLEAN));
		javaClassMap.put("integerProperties", propertyMap.get(PropertyType.INTEGER));
		javaClassMap.put("stringProperties", propertyMap.get(PropertyType.STRING));
		javaClassMap.put("objectPropertyValueCollection", propertyMap.get(PropertyType.OBJECT_COLLECTION));
		javaClassMap.put("stringCollection", propertyMap.get(PropertyType.STRING_COLLECTION));
		javaClassMap.put("objectPropertyValueSet", propertyMap.get(PropertyType.OBJECT_SET));
		javaClassMap.put("enumPropertyValueCollection", propertyMap.get(PropertyType.ENUM_COLLECTION));
		javaClassMap.put("suppressUnchecked", !(propertyMap.get(PropertyType.OBJECT_COLLECTION).isEmpty() &&
				propertyMap.get(PropertyType.OBJECT_SET).isEmpty() &&
				propertyMap.get(PropertyType.STRING_COLLECTION).isEmpty()));
		javaClassMap.put("year", YEAR);
		javaClassMap.put("pkgName", pkgName);
		javaClassMap.put("classComments", toClassComment(comment));
		String superClass = getSuperClass(superClassUri, requiredImports, classUri);
		javaClassMap.put("superClass", superClass);
		javaClassMap.put("verifySuperclass", superClass != "ModelObjectV3");
		if (!this.uriToNamespaceUri(classUri).endsWith("Core")) {
			requiredImports.add("import org.spdx.library.model."+versionSuffix+".core.ProfileIdentifierType;");
		}
		boolean hasCreationInfo = false;
		for (Map<String, Object> property:propertyMap.get(PropertyType.OBJECT)) {
			if (property.get("propertyName").equals("creationInfo")) {
				hasCreationInfo = true;
				break;
			}
		}
		javaClassMap.put("hasCreationInfo", hasCreationInfo);
		if (hasCreationInfo && !"Element".equals(name)) {
			requiredImports.add("import org.spdx.library.model."+versionSuffix+".core.Element;");
			requiredImports.add("import org.spdx.library.model."+versionSuffix+".core.CreationInfo;");
		}
		String equalsHashOverride = getEqualsHashOverride(classUri, superClasses, requiredImports);
		List<String> imports = buildImports(new ArrayList<String>(requiredImports));
		javaClassMap.put("imports", imports.toArray(new String[imports.size()]));
		//TODO: Implement
		javaClassMap.put("compareUsingProperties", false); // use properties to implement compareTo
		javaClassMap.put("compareProperties", new ArrayList<Map<String, Object>>()); // List of property mustache maps to use in compare
		String toStringString = generateToString(classUri, superClasses, propertyMap, requiredImports);
		if (Objects.nonNull(toStringString)) {
			javaClassMap.put("toString", toStringString); // use properties to implement toString
		}
		if (Objects.nonNull(equalsHashOverride)) {
			javaClassMap.put("equalsHashOverride", equalsHashOverride);
		}
		javaClassMaps.put(classUri, javaClassMap);
		// make a copy of the java class map
		Map<String, Object> unitTestMap = new HashMap<>();
		for (Entry<String, Object> entry:javaClassMap.entrySet()) {
			unitTestMap.put(entry.getKey(), entry.getValue());
		}
		requiredImports.add(String.format("import %s.%s.%sBuilder;", pkgName, name, name));
		requiredImports.add("import junit.framework.TestCase;");
		requiredImports.add("import org.spdx.library.model."+versionSuffix+".MockCopyManager;");
		requiredImports.add("import org.spdx.library.model."+versionSuffix+".MockModelStore;");
		requiredImports.add("import org.spdx.library.model."+versionSuffix+".UnitTestHelper;");
		requiredImports.add("import org.spdx.library.model."+versionSuffix+".core.Agent.AgentBuilder;");
		requiredImports.add("import java.util.Arrays;");
		requiredImports.add("import org.spdx.core.ModelRegistry;");
		requiredImports.add("import org.spdx.library.model."+versionSuffix+".SpdxModelInfoV3_0;");
		requiredImports.add("import org.spdx.library.model."+versionSuffix+".TestValuesGenerator;");
		imports = buildImports(new ArrayList<String>(requiredImports));
		unitTestMap.put("imports", imports.toArray(new String[imports.size()]));
		unitTestMaps.put(classUri, unitTestMap);
		return mustacheToString(ShaclToJavaConstants.CREATE_CLASS_TEMPLATE, javaClassMap);
	}
	
	/**
	 * @param dir top level directory for code
	 * @param classUri URI for the class
	 * @param mustacheMap mustache map for template
	 * @throws IOException 
	 */
	private void generateUnitTest(File dir, String classUri,
			Map<String, Object> mustacheMap) throws IOException {
		File unitTestFile = createUnitTestFile(classUri, dir);
		writeMustacheFile(ShaclToJavaConstants.UNIT_TEST_TEMPLATE, unitTestFile, mustacheMap);
	}

	/**
	 * @param dir top level directory for code
	 * @param classUri URI for the class
	 * @param mustacheMap mustache map for template
	 * @throws IOException 
	 */
	private void generateJavaClass(File dir, String classUri,
			Map<String, Object> mustacheMap) throws IOException {
		File sourceFile = createJavaSourceFile(classUri, dir);
		writeMustacheFile(ShaclToJavaConstants.JAVA_CLASS_TEMPLATE, sourceFile, mustacheMap);
	}
	
	/**
	 * @param dir top level directory for code
	 * @param classUri URI for the class the external class is based on
	 * @param mustacheMap mustache map for template
	 * @throws IOException 
	 */
	private void generateExternalJavaClass(File dir, String classUri,
			Map<String, Object> mustacheMap) throws IOException {
		File sourceFile = createExternalJavaSourceFile(classUri, dir);
		writeMustacheFile(ShaclToJavaConstants.EXTERNAL_JAVA_CLASS_TEMPLATE, sourceFile, mustacheMap);
	}
	
	/**
	 * @param classUri
	 * @param superClasses
	 * @param propertyMap
	 * @param requiredImports
	 * @return toString override method
	 * @throws IOException 
	 */
	private String generateToString(String classUri,
			List<OntClass> superClasses,
			Map<PropertyType, List<Map<String, Object>>> propertyMap,
			Set<String> requiredImports) throws IOException {
		if (classUri.endsWith("ExpandedLicensing/WithAdditionOperator")) {
			Map<String, Object> mustacheMap = new HashMap<>();
			mustacheMap.put("className", uriToClassName.get(classUri));
			String subjectAdditionPropertyName = uriToPropertyName.get("https://spdx.org/rdf/"+versionSemVer+"/terms/ExpandedLicensing/subjectAddition");
			String subjectLicenseGetter = "get" + subjectAdditionPropertyName.substring(0, 1).toUpperCase() + subjectAdditionPropertyName.substring(1);
			mustacheMap.put("subjectAdditionGetter", subjectLicenseGetter);
			String subjectExtendablePropertyName = uriToPropertyName.get("https://spdx.org/rdf/"+versionSemVer+"/terms/ExpandedLicensing/subjectExtendableLicense");
			String extendableLicenseGetter = "get" + subjectExtendablePropertyName.substring(0, 1).toUpperCase() + subjectExtendablePropertyName.substring(1);
			mustacheMap.put("extendableLicenseGetter", extendableLicenseGetter);
			return mustacheToString(ShaclToJavaConstants.WITH_OPERATOR_TO_STRING_TEMPLATE, mustacheMap);
		} else if (classUri.endsWith("ExpandedLicensing/OrLaterOperator")) {
			Map<String, Object> mustacheMap = new HashMap<>();
			mustacheMap.put("className", uriToClassName.get(classUri));
			String subjectPropertyName = uriToPropertyName.get("https://spdx.org/rdf/"+versionSemVer+"/terms/ExpandedLicensing/subjectLicense");
			String subjectLicenseGetter = "get" + subjectPropertyName.substring(0, 1).toUpperCase() + subjectPropertyName.substring(1);
			mustacheMap.put("subjectLicenseGetter", subjectLicenseGetter);
			return mustacheToString(ShaclToJavaConstants.OR_LATER_TO_STRING_TEMPLATE, mustacheMap);
		} else if (classUri.endsWith("ExpandedLicensing/ListedLicense") ||
				classUri.endsWith("ExpandedLicensing/ListedLicenseException")) {
			return "\t\treturn this.getObjectUri().substring(SpdxConstantsV3.SPDX_LISTED_LICENSE_NAMESPACE.length());";
		} else if (classUri.endsWith("ExpandedLicensing/CustomLicense") ||
				classUri.endsWith("ExpandedLicensing/CustomLicenseAddition")) {
			return "if (this.getObjectUri().contains(\"LicenseRef-\")) {\n"
					+ "\t\t\treturn (this.getObjectUri().substring(this.getObjectUri().lastIndexOf(\"LicenseRef-\")));\n"
					+ "\t\t} else {\n"
					+ "\t\t\treturn this.getObjectUri();\n"
					+ "\t\t}";
		} else if (classUri.endsWith("ExpandedLicensing/ConjunctiveLicenseSet") ||
				classUri.endsWith("ExpandedLicensing/DisjunctiveLicenseSet")) {
			Map<String, Object> mustacheMap = new HashMap<>();
			String licenseMemberPropName = uriToPropertyName.get("https://spdx.org/rdf/"+versionSemVer+"/terms/ExpandedLicensing/member") + "s";
			String licenseMemberGetter = "get" + licenseMemberPropName.substring(0, 1).toUpperCase() + licenseMemberPropName.substring(1);
			mustacheMap.put("licenseMembersGetter", licenseMemberGetter);
			mustacheMap.put("operator", 
					classUri.endsWith("ExpandedLicensing/DisjunctiveLicenseSet") ? "OR" : "AND");
			return mustacheToString(ShaclToJavaConstants.LICENSE_SET_TO_STRING_TEMPLATE, mustacheMap);
		}  else if (classUri.endsWith("Core/Element")) {
			Map<String, Object> mustacheMap = new HashMap<>();
			String nameProp = uriToPropertyName.get("https://spdx.org/rdf/"+versionSemVer+"/terms/Core/name");
			String nameGetter = "get" + nameProp.substring(0, 1).toUpperCase() + nameProp.substring(1);
			mustacheMap.put("nameGetter", nameGetter);
			return mustacheToString(ShaclToJavaConstants.ELEMENT_TO_STRING_TEMPLATE, mustacheMap);
		}
		boolean elementSubclass = false;
		for (OntClass superClass:superClasses) {
			if (superClass.getURI().endsWith("Core/Element")) {
				elementSubclass = true;
				break;
			}
		}
		if (elementSubclass) {
			return "\t\treturn super.toString();";
		} else {
			return null;
		}
	}

	/**
	 * @param classUri URI of the class
	 * @param requiredImports 
	 * @return code for equals and hashcode if the Java class should override
	 * null if it should not be override
	 * @throws IOException 
	 */
	private @Nullable String getEqualsHashOverride(String classUri, List<OntClass> superClasses, Set<String> requiredImports) throws IOException {
		// License classes need to override equals so that the license sets work properly
		// NOTE: This needs to be checked first since licenses are subclasses of elements
		if (classUri.endsWith("ExpandedLicensing/ConjunctiveLicenseSet")) {
			Map<String, Object> mustacheMap = new HashMap<>();
			mustacheMap.put("className", uriToClassName.get(classUri));
			mustacheMap.put("primeNumber", "1381");
			requiredImports.add("import java.util.HashSet;");
			requiredImports.add("import java.util.Iterator;");
			String licenseMemberPropName = uriToPropertyName.get("https://spdx.org/rdf/"+versionSemVer+"/terms/ExpandedLicensing/member") + "s";
			String licenseMemberGetter = "get" + licenseMemberPropName.substring(0, 1).toUpperCase() + licenseMemberPropName.substring(1);
			mustacheMap.put("licenseMembersGetter", licenseMemberGetter);
			return mustacheToString(ShaclToJavaConstants.LICENSE_SET_EQUALS_OVERRIDE_TEMPLATE, mustacheMap);
		}
		if (classUri.endsWith("ExpandedLicensing/DisjunctiveLicenseSet")) {
			Map<String, Object> mustacheMap = new HashMap<>();
			mustacheMap.put("className", uriToClassName.get(classUri));
			mustacheMap.put("primeNumber", "41");
			requiredImports.add("import java.util.HashSet;");
			requiredImports.add("import java.util.Iterator;");
			String licenseMemberPropName = uriToPropertyName.get("https://spdx.org/rdf/"+versionSemVer+"/terms/ExpandedLicensing/member") + "s";
			String licenseMemberGetter = "get" + licenseMemberPropName.substring(0, 1).toUpperCase() + licenseMemberPropName.substring(1);
			mustacheMap.put("licenseMembersGetter", licenseMemberGetter);
			return mustacheToString(ShaclToJavaConstants.LICENSE_SET_EQUALS_OVERRIDE_TEMPLATE, mustacheMap);
		}
		if (classUri.endsWith("ExpandedLicensing/OrLaterOperator")) {
			Map<String, Object> mustacheMap = new HashMap<>();
			mustacheMap.put("className", uriToClassName.get(classUri));
			String subjectPropertyName = uriToPropertyName.get("https://spdx.org/rdf/"+versionSemVer+"/terms/ExpandedLicensing/subjectLicense");
			String subjectLicenseGetter = "get" + subjectPropertyName.substring(0, 1).toUpperCase() + subjectPropertyName.substring(1);
			mustacheMap.put("subjectLicenseGetter", subjectLicenseGetter);
			return mustacheToString(ShaclToJavaConstants.OR_LATER_EQUALS_OVERRIDE_TEMPLATE, mustacheMap);
		}
		if (classUri.endsWith("ExpandedLicensing/WithAdditionOperator")) {
			Map<String, Object> mustacheMap = new HashMap<>();
			mustacheMap.put("className", uriToClassName.get(classUri));
			String subjectAdditionPropertyName = uriToPropertyName.get("https://spdx.org/rdf/"+versionSemVer+"/terms/ExpandedLicensing/subjectAddition");
			String subjectLicenseGetter = "get" + subjectAdditionPropertyName.substring(0, 1).toUpperCase() + subjectAdditionPropertyName.substring(1);
			mustacheMap.put("subjectAdditionGetter", subjectLicenseGetter);
			String subjectExtendablePropertyName = uriToPropertyName.get("https://spdx.org/rdf/"+versionSemVer+"/terms/ExpandedLicensing/subjectExtendableLicense");
			String extendableLicenseGetter = "get" + subjectExtendablePropertyName.substring(0, 1).toUpperCase() + subjectExtendablePropertyName.substring(1);
			mustacheMap.put("extendableLicenseGetter", extendableLicenseGetter);
			return mustacheToString(ShaclToJavaConstants.WITH_EQUALS_OVERRIDE_TEMPLATE, mustacheMap);
		}
		return null;
	}

	/**
	 * @param dir Directory to store the source files in
	 * @param individualUri URI for the individual
	 * @param name local name for the individual
	 * @param propertyShapes properties for the individual inherited from superclasses
	 * @param comment Description of the individual
	 * @param superClassUri URI of the superclass
	 * @param superClassShape shape for the supper class
	 * @param superClasses all superclasses for the class
	 * @throws IOException 
	 * @throws ShaclToJavaException 
	 */
	private void generateIndividualClass(File dir, String individualUri, String name,
			List<PropertyShape> propertyShapes, String comment, 
			String superClassUri, Shape superClassShape, List<OntClass> superClasses) throws IOException, ShaclToJavaException {
		String pkgName = uriToPkg(individualUri);
		File sourceFile = createJavaSourceFile(individualUri, dir);
		Set<String> requiredImports = new HashSet<>();
		Map<String, Object> mustacheMap = new HashMap<>();
		mustacheMap.put("individualUri", individualUri);
		mustacheMap.put("className", name);
		mustacheMap.put("classProfile", uriToProfile(individualUri));
		Map<PropertyType, List<Map<String, Object>>> propertyMap = findProperties(propertyShapes, superClassShape, 
				requiredImports, propertyUrisForConstants, superClassUri, superClasses);
		int numProperties = 0;
		for (List<Map<String, Object>> props:propertyMap.values()) {
			numProperties += props.size();
		}
		if (numProperties > 0) {
			requiredImports.add("import org.spdx.library.model."+versionSuffix+".SpdxConstantsV3;");
			requiredImports.add("import java.util.Optional;");
		}
		mustacheMap.put("elementProperties", propertyMap.get(PropertyType.ELEMENT));
		mustacheMap.put("objectProperties", propertyMap.get(PropertyType.OBJECT));
		mustacheMap.put("anyLicenseInfoProperties", propertyMap.get(PropertyType.ANY_LICENSE_INFO));
		mustacheMap.put("licenseAdditionProperties", propertyMap.get(PropertyType.LICENSE_ADDITION));
		mustacheMap.put("extendableLicenseProperties", propertyMap.get(PropertyType.EXTENDABLE_LICENSE));
		mustacheMap.put("enumerationProperties", propertyMap.get(PropertyType.ENUM));
		mustacheMap.put("booleanProperties", propertyMap.get(PropertyType.BOOLEAN));
		mustacheMap.put("integerProperties", propertyMap.get(PropertyType.INTEGER));
		mustacheMap.put("stringProperties", propertyMap.get(PropertyType.STRING));
		mustacheMap.put("objectPropertyValueCollection", propertyMap.get(PropertyType.OBJECT_COLLECTION));
		mustacheMap.put("stringCollection", propertyMap.get(PropertyType.STRING_COLLECTION));
		mustacheMap.put("objectPropertyValueSet", propertyMap.get(PropertyType.OBJECT_SET));
		mustacheMap.put("enumPropertyValueCollection", propertyMap.get(PropertyType.ENUM_COLLECTION));
		mustacheMap.put("suppressUnchecked", !(propertyMap.get(PropertyType.OBJECT_COLLECTION).isEmpty() &&
				propertyMap.get(PropertyType.OBJECT_SET).isEmpty() &&
				propertyMap.get(PropertyType.STRING_COLLECTION).isEmpty()));
		mustacheMap.put("year", YEAR);
		mustacheMap.put("pkgName", pkgName);
		
		mustacheMap.put("classComments", toClassComment(comment));
		String superClass = getSuperClass(superClassUri, requiredImports, individualUri);
		mustacheMap.put("superClass", superClass);
		requiredImports.add("import org.spdx.storage.NullModelStore;");
		List<String> imports = buildImports(new ArrayList<String>(requiredImports));
		mustacheMap.put("imports", imports.toArray(new String[imports.size()]));
		mustacheMap.put("toStringName", name.startsWith("NoAssertion") ? "NOASSERTION" :
			name.startsWith("None") ? "NONE" : name);
		writeMustacheFile(ShaclToJavaConstants.INDIVIDUAL_CLASS_TEMPLATE, sourceFile, mustacheMap);
	}
	

	/**
	 * @param ontClass shape of class
	 * @return true if the classShape represents an abstract class
	 */
	private boolean isAbstract(OntClass ontClass) {
		if (Objects.isNull(ontClass)) {
			return false;
		}
		ExtendedIterator<Resource> iter = ontClass.listRDFTypes(true);
		while (iter.hasNext()) {
			if (ShaclToJavaConstants.ABSTRACT_TYPE_URI.equals(iter.next().getURI())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param propertyShapes direct ontology properties
	 * @param classShape Shape for the class containing the properties
	 * @param requiredImport set of required imports for this class - updated with any additional imports
	 * @param propertyUrisForConstants set of URI's for any properties - updated with any additional values
	 * @param classUri URI for the class containing the properties
	 * @param superClasses all superclasses for the class
	 * @return map of mustache strings to properties for any properties returning a type of Element
	 * @throws ShaclToJavaException 
	 */
	private Map<PropertyType, List<Map<String, Object>>> findProperties(List<PropertyShape> propertyShapes, Shape classShape, 
			Set<String> requiredImports, Set<String> propertyUrisForConstants, String classUri, List<OntClass> superClasses) throws ShaclToJavaException {
		Map<PropertyType, List<Map<String, Object>>> retval = new HashMap<>();
		for (PropertyType value:PropertyType.values()) {
			retval.put(value, new ArrayList<Map<String, Object>>());
		}
		for (PropertyShape propertyShape:propertyShapes) {
			if (!Objects.isNull(propertyShape)) {
				Map<String, Object> mustacheMap = propertyToMustachMap(propertyShape, requiredImports, 
						propertyUrisForConstants, classUri, superClasses);
				retval.get(mustacheMap.get("propertyType")).add(mustacheMap);
			}
		}
		return retval;
	}

	/**
	 * Maps a property to mustache map adding any required import strings and adding any required constant strings
	 * @param classShape Shape of the class using the property
	 * @param requiredImport set of required imports for this class - updated with any additional imports
	 * @param propertyUrisForConstants set of URI's for any properties - updated with any additional values
	 * @param classUri URI of the class using the property
	 * @param superClasses all superclasses for the class
	 * @return map of Mustache strings to values for a give ontology property
	 * @throws ShaclToJavaException 
	 */
	private Map<String, Object> propertyToMustachMap(PropertyShape propertyShape,
			Set<String> requiredImports, Set<String> propertyUrisForConstants, String classUri, List<OntClass> superClasses) throws ShaclToJavaException {
		Map<String, Object> retval = new HashMap<>();
		String nameSpace = uriToNamespaceUri(classUri);
		String propertyUri = propertyShape.getPath().toString().replaceAll("<", "").replaceAll(">", "");
		
		String name = uriToPropertyName.get(propertyUri);
		if (Objects.isNull(name)) {
			// This is a special case if a property is not defined in SHACL as either
			// an ObjectProperty or an DataProperty - e.g. core:extension
			name = uriToName(propertyUri);
			uriToPropertyName.put(propertyUri, name);
		}
		retval.put("propertyName", name);
		retval.put("propertyNameUpper", camelCaseToConstCase(name));
		String getSetName = name.substring(0, 1).toUpperCase() + name.substring(1);
		retval.put("getter", "get" + getSetName);
		retval.put("setter", "set" + getSetName);
		retval.put("adder", "add" + getSetName);
		retval.put("addAller", "addAll" + getSetName);
		retval.put("isCreationInfo", "creationInfo".equals(name));
		
		Integer minCardinality = null;
		Integer maxCardinality = null;
		Integer minStringLength = null;
		Integer maxStringLength = null;
		String pattern = null;
		Node classRestriction = null;
		Node dataTypeRestriction = null;
		
		for (Constraint constraint:propertyShape.getConstraints()) {
			ConstraintCollector collector = new ConstraintCollector();
			constraint.visit(collector);
			if (collector.getMinCardinality() != null) {
				if (minCardinality == null || minCardinality > collector.getMinCardinality()) {
					minCardinality = collector.getMinCardinality();
				}
			}
			if (collector.getMaxCardinality() != null) {
				if (maxCardinality == null || maxCardinality > collector.getMaxCardinality()) {
					maxCardinality = collector.getMaxCardinality();
				}
			}
			if (collector.getPattern() != null) {
				pattern = collector.getPattern();
			}
			if (collector.getStrMinLengh() != null) {
				if (minStringLength == null || minStringLength > collector.getStrMaxLenght()) {
					minStringLength = collector.getStrMinLengh();
				}
			}
			if (collector.getStrMaxLenght() != null) {
				if (maxStringLength == null || maxStringLength < collector.getStrMaxLenght()) {
					maxStringLength = collector.getStrMaxLenght();
				}
			}
			if (collector.getDataType() != null) {
				dataTypeRestriction = collector.getDataType();
			} else if (collector.getExpectedClass() != null) {
				classRestriction = collector.getExpectedClass();
			}
		}
		String propertySuffix = propertyUri.substring(propertyUri.lastIndexOf("/terms/"));
		PropertyType propertyType = ShaclToJavaConstants.SET_PROPERTY_SUFFIXES.contains(propertySuffix) ? PropertyType.OBJECT_SET : determinePropertyType(classRestriction, dataTypeRestriction, 
				minCardinality, maxCardinality);
		if (PropertyType.OBJECT_COLLECTION.equals(propertyType) || PropertyType.STRING_COLLECTION.equals(propertyType) ||
				PropertyType.ENUM_COLLECTION.equals(propertyType)) {
			requiredImports.add("import java.util.Collection;");
			requiredImports.add("import java.util.Collections;");
			requiredImports.add("import java.util.Objects;");
		}
 		retval.put("propertyType", propertyType);
		String typeUri = getTypeUri(classRestriction, dataTypeRestriction);
		String type;
		if (ShaclToJavaConstants.BOOLEAN_TYPE.equals(typeUri)) {
			type = "Boolean";
		} else if (ShaclToJavaConstants.STRING_TYPE.equals(typeUri) || ShaclToJavaConstants.DATE_TIME_TYPE.equals(typeUri)) {
			type = "String";
		} else if (ShaclToJavaConstants.INTEGER_TYPES.contains(typeUri)) {
			type = "Integer";
		} else {
			type = uriToClassName.get(typeUri);
			if (!typeUri.startsWith(nameSpace) && 
					(PropertyType.ENUM.equals(propertyType) || PropertyType.OBJECT.equals(propertyType) ||
							PropertyType.OBJECT_COLLECTION.equals(propertyType) || 
							PropertyType.ENUM_COLLECTION.equals(propertyType) || 
							PropertyType.OBJECT_SET.equals(propertyType) ||
							PropertyType.ANY_LICENSE_INFO.equals(propertyType) ||
							PropertyType.LICENSE_ADDITION.equals(propertyType) ||
							PropertyType.EXTENDABLE_LICENSE.equals(propertyType) ||
							PropertyType.ELEMENT.equals(propertyType))) {				
				requiredImports.add("import "+uriToPkg(typeUri) + "." + uriToClassName.get(typeUri) +";");
			}
		}
		retval.put("typeUri", typeUri);
		retval.put("type", type);
		boolean required = minCardinality != null && minCardinality > 0;
		if (required) {
			requiredImports.add("import java.util.Collections;");
			requiredImports.add("import java.util.Arrays;");
			requiredImports.add("import java.util.Objects;");
		}
		retval.put("required", required);
		
		String profileIdentifierType = namespaceToProfileIdentifierType(nameSpace);
		retval.put("requiredProfiles",  profileIdentifierType);
		String classNamespace = uriToNamespaceUri(classUri);
		boolean inSuperClass = inSuperClass(superClasses, propertyShape);
		retval.put("superSetter", inSuperClass);
		SuperclassRequired superRequired = inSuperClass ? determineSuperRequired(superClasses, propertyShape) :
			SuperclassRequired.NONE;
		boolean nonOptional = required && nameSpace.equals(classNamespace) && 
				(SuperclassRequired.YES.equals(superRequired) || SuperclassRequired.NONE.equals(superRequired)); // we can't override an optional
		retval.put("nonOptional", nonOptional);
		boolean hasConstraint = required;
		if (Objects.nonNull(pattern)) {
			retval.put("pattern", pattern);
			hasConstraint = true;
		}
		if (Objects.nonNull(minStringLength)) {
			retval.put("min", minStringLength.toString());
			hasConstraint = true;
		} else if (ShaclToJavaConstants.XSD_NON_NEGATIVE_INTEGER.equals(typeUri)) {
			retval.put("min", "0");
			hasConstraint = true;
		} else if (ShaclToJavaConstants.XSD_POSITIVE_INTEGER.equals(typeUri)) {
			retval.put("min", "1");
			hasConstraint = true;
		}
		if (Objects.nonNull(maxStringLength)) {
			retval.put("max", maxStringLength.toString());
			hasConstraint = true;
		}
		if (Objects.nonNull(pattern)) {
			requiredImports.add("import java.util.regex.Pattern;");
			retval.put("pattern", StringEscapeUtils.escapeJava(pattern));
		}
		retval.put("hasConstraint", hasConstraint);
		retval.put("uri", propertyUri);
		String propNameSpace = uriToNamespaceUri(propertyUri);
		propNameSpace = propNameSpace.substring(propNameSpace.lastIndexOf('/')+1);
		String propConstant = propertyNameToPropertyConstant(name, propNameSpace);
		retval.put("propertyConstant", propConstant);
		propertyUrisForConstants.add(propertyUri);
		if ("specVersion".equals(name)) {
			retval.put("isSpecVersion", true); // special case that the spec version is set in the CoreModelObject in addition to the store
		}
		return retval;
	}
	
	/**
	 * @param superClasses superclasses to search for required fields
	 * @param propertyShape property shape for the property
	 * @return true if the property is present in one of the superclasses
	 */
	private boolean inSuperClass(List<OntClass> superClasses,
			PropertyShape propertyShape) {
		for (OntClass ontClass:superClasses) {
			Shape classShape = shapeMap.get(ontClass.asNode());
			if (Objects.nonNull(classShape)) {
				for (Iterator<PropertyShape> propIter = classShape.getPropertyShapes().iterator(); propIter.hasNext();) {
					PropertyShape superPropertyShape = propIter.next();
					if (propertyShape.getPath().equalTo(superPropertyShape.getPath(), null)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * @param superClasses superclasses to search for required fields
	 * @param propertyShape property shape for the property
	 * @return the SuperClassRequired balue based on the constraints
	 */
	private SuperclassRequired determineSuperRequired(List<OntClass> superClasses,
			PropertyShape propertyShape) {
		SuperclassRequired retval = SuperclassRequired.NONE;
		for (OntClass ontClass:superClasses) {
			Shape classShape = shapeMap.get(ontClass.asNode());
			if (Objects.nonNull(classShape)) {
				for (Iterator<PropertyShape> propIter = classShape.getPropertyShapes().iterator(); propIter.hasNext();) {
					PropertyShape superPropertyShape = propIter.next();
					if (propertyShape.getPath().equalTo(superPropertyShape.getPath(), null)) {
						Integer minCardinality = null;
						for (Constraint constraint:superPropertyShape.getConstraints()) {
							ConstraintCollector collector = new ConstraintCollector();
							constraint.visit(collector);
							if (collector.getMinCardinality() != null) {
								if (minCardinality == null || minCardinality > collector.getMinCardinality()) {
									minCardinality = collector.getMinCardinality();
								}
							}
						}
						if (Objects.nonNull(minCardinality) && minCardinality > 0) {
							// required
							if (SuperclassRequired.NONE.equals(retval)) {
								retval = SuperclassRequired.YES;
							} else if (SuperclassRequired.NO.equals(retval)) {
								retval = SuperclassRequired.BOTH;
							}
						} else {
							// not required
							if (SuperclassRequired.NONE.equals(retval)) {
								retval = SuperclassRequired.NO;
							} else if (SuperclassRequired.YES.equals(retval)) {
								retval = SuperclassRequired.BOTH;
							}
						}
					}
				}
			}
		}
		return retval;
	}

	/**
	 * @param nameSpace
	 * @return the ProfileIdentifierType string associated with the namespace
	 */
	private String namespaceToProfileIdentifierType(String nameSpace) {
		return "ProfileIdentifierType." + uriToName(nameSpace).replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
	}

	/**
	 * @param uri URI used for classes and properties
	 * @return namespace portion of the URI
	 */
	private String uriToNamespaceUri(String uri) {
		return uri.substring(0, uri.lastIndexOf('/'));
	}
	
	/**
	 * @param uri URI used for classes and properties
	 * @return profile name (last segment of the namespace)
	 */
	private String uriToProfile(String uri) {
		String namespace = uriToNamespaceUri(uri);
		return uriToName(namespace);
	}
	
	/**
	 * @param uri URI used for classes and properties
	 * @return the name of the class or property
	 */
	private String uriToName(String uri) {
		String retval;
		if (uri.contains("#")) {
			retval = uri.substring(uri.lastIndexOf('#') + 1);
		} else if (uri.contains("/")) {
			retval = uri.substring(uri.lastIndexOf('/') + 1);
		} else {
			retval = uri;
		}
		return ShaclToJavaConstants.RESERVED_JAVA_WORDS.containsKey(retval) ? ShaclToJavaConstants.RESERVED_JAVA_WORDS.get(retval) : retval;
	}
	
	/**
	 * @param superClassUri URI for the superclass
	 * @param requiredImport set of required imports - updated if the superClass adds a new import statement
	 * @param classUri the URI for the class with the superClass
	 * @return superClass for the class
	 */
	private String getSuperClass(String superClassUri, Set<String> requiredImports, String classUri) {
		if (Objects.isNull(superClassUri) || ShaclToJavaConstants.OWL_THING_URI.equals(superClassUri)) {
			return "ModelObjectV3";
		}
		String classNameSpace = uriToNamespaceUri(classUri);
		if (!superClassUri.startsWith(classNameSpace)) {
			requiredImports.add("import " + uriToPkg(superClassUri) + "." + uriToClassName.get(superClassUri) + ";");
		}
		return uriToClassName.get(superClassUri);
	}

	/**
	 * @return a list of import statements appropriate for the class
	 */
	private List<String> buildImports(List<String> localImports) {
		List<String> retval = new ArrayList<>();
		retval.add("import javax.annotation.Nullable;");
		retval.add("");
		retval.add("import java.util.ArrayList;");
		retval.add("import java.util.List;");
		retval.add("import java.util.Set;");
		retval.add("");
		retval.add("import org.spdx.core.CoreModelObject;");
		retval.add("import org.spdx.core.DefaultModelStore;");
		retval.add("import org.spdx.core.InvalidSPDXAnalysisException;");
		retval.add("import org.spdx.core.IModelCopyManager;");
		retval.add("import org.spdx.core.IndividualUriValue;");
		retval.add("import org.spdx.library.model."+versionSuffix+".ModelObjectV3;");
		retval.add("import org.spdx.storage.IModelStore;");
		retval.add("import org.spdx.storage.IModelStore.IdType;");
		retval.add("import org.spdx.storage.IModelStore.IModelStoreLock;");
		retval.add("");
		Collections.sort(localImports);
		for (String localImport:localImports) {
			retval.add(localImport);
		}
		return retval;
	}
	

	/**
	 * @param dir Directory to store the source files in
	 * @param classUri URI for the enum
	 * @param name local name for the enum
	 * @param allIndividuals individual values from the model
	 * @param comment Description of the enum
	 * @return mustacheMap for the java enum properties
	 * @throws IOException I/O error writing the file
	 */
	private Map<String, Object> generateJavaEnum(File dir, String classUri, String name,
			List<Individual> allIndividuals, String comment) throws IOException {
		Map<String, Object> mustacheMap = new HashMap<>();
		mustacheMap.put("year", YEAR);
		mustacheMap.put("pkgName", uriToPkg(classUri));
		mustacheMap.put("classComment", toClassComment(comment));
		mustacheMap.put("name", name);
		mustacheMap.put("classUri", classUri);
		List<String> enumValues = new ArrayList<>();
		String lastEnumValue = null;
		for (Individual individual:allIndividuals) {
			if (individual.hasRDFType(classUri)) {
				String enumName = camelCaseToConstCase(individual.getLocalName());
				if (Objects.nonNull(lastEnumValue)) {
					enumValues.add(lastEnumValue + ",");
				}
				lastEnumValue = enumName + "(\"" + individual.getLocalName() + "\")";
			}
		}
		if (Objects.nonNull(lastEnumValue)) {
			enumValues.add(lastEnumValue + ";");
		}
		mustacheMap.put("enumValues", enumValues);
		File sourceFile = createJavaSourceFile(classUri, dir);
		writeMustacheFile(ShaclToJavaConstants.ENUM_CLASS_TEMPLATE, sourceFile, mustacheMap);
		return mustacheMap;
	}
	
	/**
	 * Convert a camelCase String into a string matching the Java constant string conventions
	 * @param camel input string
	 * @return an all upper case string with underscore separators
	 */
	private String camelCaseToConstCase(String camel) {
		if (camel.isEmpty()) {
			return camel;
		}
		StringBuilder retval = new StringBuilder();
		retval.append(Character.toUpperCase(camel.charAt(0)));
		for (int i = 1; i < camel.length(); i++) {
			char ch = camel.charAt(i);
			if (!Character.isLowerCase(ch) && Character.isAlphabetic(ch)) {
				retval.append('_');
			}
			if (ch == '-') {
				retval.append('_');
			} else {
				retval.append(Character.toUpperCase(ch));
			}
		}
		return retval.toString();
	}
	
	/**
	 * Convert the prop name to the name of the constant used for the PropertyDescriptor
	 * @param propName name of the property
	 * @param namespaceName name of the namespace
	 * @return property constant name
	 */
	private String propertyNameToPropertyConstant(String propName, String namespaceName) {
		return   "PROP_" + camelCaseToConstCase(propName);
	}

	/**
	 * @param comment from model documentation
	 * @return text formatted for a class comment
	 */
	private String toClassComment(String comment) {
		StringBuilder sb = new StringBuilder("/**\n");
		sb.append(" * DO NOT EDIT - this file is generated by the Owl to Java Utility \n");
		sb.append(" * See: https://github.com/spdx/tools-java \n");
		sb.append(" * \n");
		String[] tokens = comment.split("\\s+");
		int i = 0;
		while (i < tokens.length) {
			int len = 4;
			sb.append(" * ");
			while (len < ShaclToJavaConstants.COMMENT_LINE_LEN && i < tokens.length) {
				len += tokens[i].length();
				sb.append(tokens[i++].trim());
				sb.append(' ');
			}
			sb.append("\n");
		}
		sb.append(" */");
		return sb.toString();
	}

	/**
	 * @param classUri URI for the class under test
	 * @param dir directory to hold the file
	 * @return the created file
	 * @throws IOException 
	 */
	private File createUnitTestFile(String classUri, File dir) throws IOException {		
		Path path = dir.toPath().resolve("src").resolve("test").resolve("java").resolve("org")
				.resolve("spdx").resolve("library").resolve("model").resolve(versionSuffix);
		String[] parts = classUri.substring(ShaclToJavaConstants.SPDX_URI_PREFIX.length()).split("/");
		for (int i = 2; i < parts.length-1; i++) {
			path = path.resolve(parts[i].toLowerCase());
		}
		Files.createDirectories(path);
		String fileName = uriToClassName.get(classUri)+"Test";
		File retval = path.resolve(fileName + ".java").toFile();
		retval.createNewFile();
		return retval;
	}

	/**
	 * @param classUri URI for the class
	 * @param dir directory to hold the file
	 * @return the created file
	 * @throws IOException 
	 */
	private File createJavaSourceFile(String classUri, File dir) throws IOException {		
		Path path = dir.toPath().resolve("src").resolve("main").resolve("java").resolve("org")
				.resolve("spdx").resolve("library").resolve("model").resolve(versionSuffix);
		String[] parts = classUri.substring(ShaclToJavaConstants.SPDX_URI_PREFIX.length()).split("/");
		// [0] is version, [1] is "terms"
		for (int i = 2; i < parts.length-1; i++) {
			path = path.resolve(parts[i].toLowerCase());
		}
		Files.createDirectories(path);
		String fileName = uriToClassName.get(classUri);
		File retval = path.resolve(fileName + ".java").toFile();
		retval.createNewFile();
		return retval;
	}
	
	/**
	 * @param classUri URI for the non-external class
	 * @param dir directory to hold the file
	 * @return the created file
	 * @throws IOException 
	 */
	private File createExternalJavaSourceFile(String classUri, File dir) throws IOException {		
		Path path = dir.toPath().resolve("src").resolve("main").resolve("java").resolve("org")
				.resolve("spdx").resolve("library").resolve("model").resolve(versionSuffix);
		String[] parts = classUri.substring(ShaclToJavaConstants.SPDX_URI_PREFIX.length()).split("/");
		// [0] is version, [1] is "terms"
		for (int i = 2; i < parts.length-1; i++) {
			path = path.resolve(parts[i].toLowerCase());
		}
		Files.createDirectories(path);
		String fileName = "External" + uriToClassName.get(classUri);
		File retval = path.resolve(fileName + ".java").toFile();
		retval.createNewFile();
		return retval;
	}

	/**
	 * @param classUri
	 * @return
	 */
	private String uriToPkg(String classUri) {
		String[] parts = classUri.substring(ShaclToJavaConstants.SPDX_URI_PREFIX.length()).split("/");
		StringBuilder sb = new StringBuilder("org.spdx.library.model.");
		sb.append(versionSuffix);
		for (int i = 2; i < parts.length-1; i++) {
			sb.append(".");
			sb.append(parts[i].toLowerCase());
		}
		return sb.toString();
	}

}
