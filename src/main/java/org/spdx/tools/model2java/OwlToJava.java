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
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.Resource;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.engine.constraint.ClassConstraint;
import org.apache.jena.shacl.engine.constraint.ClosedConstraint;
import org.apache.jena.shacl.engine.constraint.ConstraintComponentSPARQL;
import org.apache.jena.shacl.engine.constraint.DatatypeConstraint;
import org.apache.jena.shacl.engine.constraint.DisjointConstraint;
import org.apache.jena.shacl.engine.constraint.EqualsConstraint;
import org.apache.jena.shacl.engine.constraint.HasValueConstraint;
import org.apache.jena.shacl.engine.constraint.InConstraint;
import org.apache.jena.shacl.engine.constraint.JLogConstraint;
import org.apache.jena.shacl.engine.constraint.JViolationConstraint;
import org.apache.jena.shacl.engine.constraint.LessThanConstraint;
import org.apache.jena.shacl.engine.constraint.LessThanOrEqualsConstraint;
import org.apache.jena.shacl.engine.constraint.MaxCount;
import org.apache.jena.shacl.engine.constraint.MinCount;
import org.apache.jena.shacl.engine.constraint.NodeKindConstraint;
import org.apache.jena.shacl.engine.constraint.PatternConstraint;
import org.apache.jena.shacl.engine.constraint.QualifiedValueShape;
import org.apache.jena.shacl.engine.constraint.ShAnd;
import org.apache.jena.shacl.engine.constraint.ShNode;
import org.apache.jena.shacl.engine.constraint.ShNot;
import org.apache.jena.shacl.engine.constraint.ShOr;
import org.apache.jena.shacl.engine.constraint.ShXone;
import org.apache.jena.shacl.engine.constraint.SparqlConstraint;
import org.apache.jena.shacl.engine.constraint.StrLanguageIn;
import org.apache.jena.shacl.engine.constraint.StrMaxLengthConstraint;
import org.apache.jena.shacl.engine.constraint.StrMinLengthConstraint;
import org.apache.jena.shacl.engine.constraint.UniqueLangConstraint;
import org.apache.jena.shacl.engine.constraint.ValueMaxExclusiveConstraint;
import org.apache.jena.shacl.engine.constraint.ValueMaxInclusiveConstraint;
import org.apache.jena.shacl.engine.constraint.ValueMinExclusiveConstraint;
import org.apache.jena.shacl.engine.constraint.ValueMinInclusiveConstraint;
import org.apache.jena.shacl.parser.Constraint;
import org.apache.jena.shacl.parser.ConstraintVisitor;
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
public class OwlToJava {
	
	class ConstraintCollector implements ConstraintVisitor {
		
		private Integer minCardinality = null;
		private Integer maxCardinality = null;
		private String pattern = null;
		private Node dataType = null;
		private Integer strMinLengh = null;
		private Integer strMaxLenght = null;
		private Node expectedClass = null;
		private SparqlConstraint sparqlConstraint = null;
		

		@Override
		public void visit(ClassConstraint constraint) {
			expectedClass = constraint.getExpectedClass();
		}

		@Override
		public void visit(DatatypeConstraint constraint) {
			dataType = constraint.getDatatype();
		}

		@Override
		public void visit(NodeKindConstraint constraint) {
			// ignore
		}

		@Override
		public void visit(MinCount constraint) {
			minCardinality = constraint.getMinCount();
		}

		@Override
		public void visit(MaxCount constraint) {
			maxCardinality = constraint.getMaxCount();
		}

		@Override
		public void visit(ValueMinExclusiveConstraint constraint) {
			// ignore
		}

		@Override
		public void visit(ValueMinInclusiveConstraint constraint) {
			// ignore
		}

		@Override
		public void visit(ValueMaxInclusiveConstraint constraint) {
			// ignore
		}

		@Override
		public void visit(ValueMaxExclusiveConstraint constraint) {
			// ignore
		}

		@Override
		public void visit(StrMinLengthConstraint constraint) {
			strMinLengh = constraint.getMinLength();
		}

		@Override
		public void visit(StrMaxLengthConstraint constraint) {
			strMaxLenght = constraint.getMaxLength();
		}

		@Override
		public void visit(PatternConstraint constraint) {
			pattern = constraint.getPattern();
		}

		@Override
		public void visit(StrLanguageIn constraint) {
			// ignore
		}

		@Override
		public void visit(UniqueLangConstraint constraint) {
			// ignore
		}

		@Override
		public void visit(EqualsConstraint constraint) {
			// ignore
		}

		@Override
		public void visit(DisjointConstraint constraint) {
			// ignore
		}

		@Override
		public void visit(LessThanConstraint constraint) {
			// ignore
		}

		@Override
		public void visit(LessThanOrEqualsConstraint constraint) {
			// ignore
			
		}

		@Override
		public void visit(ShNot constraint) {
			// ignore
			
		}

		@Override
		public void visit(ShAnd constraint) {
			// ignore
			
		}

		@Override
		public void visit(ShOr constraint) {
			// ignore
			
		}

		@Override
		public void visit(ShXone constraint) {
			// ignore
			
		}

		@Override
		public void visit(ShNode constraint) {
			// ignore
			
		}

		@Override
		public void visit(QualifiedValueShape constraint) {
			// ignore
			
		}

		@Override
		public void visit(ClosedConstraint constraint) {
			// ignore
			
		}

		@Override
		public void visit(HasValueConstraint constraint) {
			// ignore
			
		}

		@Override
		public void visit(InConstraint constraint) {
			// ignore
			
		}

		@Override
		public void visit(ConstraintComponentSPARQL constraint) {
			// ignore
			
		}

		@Override
		public void visit(SparqlConstraint constraint) {
			this.sparqlConstraint = constraint;
		}

		@Override
		public void visit(JViolationConstraint constraint) {
			// ignore
			
		}

		@Override
		public void visit(JLogConstraint constraint) {
			// ignore
			
		}

		/**
		 * @return minimum constraint
		 */
		public Integer getMinCardinality() {
			return minCardinality;
		}

		/**
		 * @return the maxCardinality
		 */
		public Integer getMaxCardinality() {
			return maxCardinality;
		}

		/**
		 * @return the pattern
		 */
		public String getPattern() {
			return pattern;
		}

		/**
		 * @return the dataType
		 */
		public Node getDataType() {
			return dataType;
		}

		/**
		 * @return the strMinLengh
		 */
		public Integer getStrMinLengh() {
			return strMinLengh;
		}

		/**
		 * @return the strMaxLenght
		 */
		public Integer getStrMaxLenght() {
			return strMaxLenght;
		}

		/**
		 * @return the expectedClass
		 */
		public Node getExpectedClass() {
			return expectedClass;
		}

		/**
		 * @return the sparqlConstraint
		 */
		public SparqlConstraint getSparqlConstraint() {
			return sparqlConstraint;
		}
	}
	
	static final String SPDX_URI_PREFIX = "https://spdx.org/rdf/";
	static final String INDENT = "\t";
	private static final int COMMENT_LINE_LEN = 72;
	private static final String BOOLEAN_TYPE = "http://www.w3.org/2001/XMLSchema#boolean";
	private static final String STRING_TYPE = "http://www.w3.org/2001/XMLSchema#string";
	private static final String ELEMENT_TYPE_URI = "https://spdx.org/rdf/Core/Element";
	private static final String ELEMENT_TYPE_ANY_LICENSE_INFO = "https://spdx.org/rdf/Licensing/AnyLicenseInfo";
	private static final String DATE_TIME_TYPE = "http://www.w3.org/2001/XMLSchema#dateTimeStamp";
	private static final String ANY_URI_TYPE = "http://www.w3.org/2001/XMLSchema#anyURI";
	private static final String OWL_THING_URI = "http://www.w3.org/2002/07/owl#Thing";
	private static final String XSD_INTEGER = "http://www.w3.org/2001/XMLSchema#integer";
	private static final String XSD_POSITIVE_INTEGER = "http://www.w3.org/2001/XMLSchema#positiveInteger";
	private static final String XSD_NON_NEGATIVE_INTEGER = "http://www.w3.org/2001/XMLSchema#nonNegativeInteger";
	private static final String ABSTRACT_TYPE_URI = "http://spdx.invalid./AbstractClass";
	
	static final String TEMPLATE_CLASS_PATH = "resources" + "/" + "javaTemplates";
	static final String TEMPLATE_ROOT_PATH = "resources" + File.separator + "javaTemplates";
	private static final String JAVA_CLASS_TEMPLATE = "JavaClassTemplate.txt";
	private static final String ENUM_CLASS_TEMPLATE = "EnumTemplate.txt";
	private static final String SPDX_CONSTANTS_TEMPLATE = "SpdxConstantsTemplate.txt";
	private static final String UNIT_TEST_TEMPLATE = "UnitTestTemplate.txt";
	private static final String ENUM_FACTORY_TEMPLATE = "SpdxEnumFactoryTemplate.txt";
	private static final String INDIVIDUALS_FACTORY_TEMPLATE = "SpdxIndividualFactoryTemplate.txt";
	private static final String MODEL_CLASS_FACTORY_TEMPLATE = "ModelClassFactoryTemplate.txt";
	private static final String CREATE_CLASS_TEMPLATE = "CreateClassTemplate.txt";
	private static final String EXTERNAL_ELEMENT_TEMPLATE = "ExternalElementTemplate.txt";
	private static final String BASE_MODEL_OBJECT_TEMPLATE = "BaseModelObjectTemplate.txt";
	private static final String MODEL_INFO_TEMPLATE = "ModelInfoTemplate.txt";
	private static final String PACKAGE_INFO_TEMPLATE = "PackageInfoTemplate.txt";
	private static final String POM_TEMPLATE = "PomTemplate.txt";
	private static Set<String> INTEGER_TYPES = new HashSet<>();
	static {
		INTEGER_TYPES.add(XSD_POSITIVE_INTEGER);
		INTEGER_TYPES.add(XSD_NON_NEGATIVE_INTEGER);
		INTEGER_TYPES.add(XSD_INTEGER);
		INTEGER_TYPES.add("http://www.w3.org/2001/XMLSchema#byte");
		INTEGER_TYPES.add("http://www.w3.org/2001/XMLSchema#int");
		INTEGER_TYPES.add("http://www.w3.org/2001/XMLSchema#long");
		INTEGER_TYPES.add("http://www.w3.org/2001/XMLSchema#negativeInteger");
		INTEGER_TYPES.add("http://www.w3.org/2001/XMLSchema#nonPositiveInteger");
		INTEGER_TYPES.add("http://www.w3.org/2001/XMLSchema#positiveInteger");
		INTEGER_TYPES.add("http://www.w3.org/2001/XMLSchema#short");
		INTEGER_TYPES.add("http://www.w3.org/2001/XMLSchema#unsignedLong");
		INTEGER_TYPES.add("http://www.w3.org/2001/XMLSchema#unsignedInt");
		INTEGER_TYPES.add("http://www.w3.org/2001/XMLSchema#unsignedShort");
		INTEGER_TYPES.add("http://www.w3.org/2001/XMLSchema#unsignedByte");
		INTEGER_TYPES.add("http://www.w3.org/2001/XMLSchema#decimal");
	}
	
	private static Map<String, String> RESERVED_JAVA_WORDS = new HashMap<>();
	static {
		RESERVED_JAVA_WORDS.put("Package", "SpdxPackage");
		RESERVED_JAVA_WORDS.put("package", "spdxPackage");
		RESERVED_JAVA_WORDS.put("File", "SpdxFile");
		RESERVED_JAVA_WORDS.put("file", "spdxFile");
	}
	
	private static Set<String> SET_TYPE_URIS = new HashSet<>(); // set of URI's for types should be treated as sets
	static {
		// Currently, there are no sets - all are treated as collections
	}
	OntModel model = null;
	Shapes shapes = null;
	Map<Node, Shape> shapeMap = null;
	
	Set<String> enumClassUris = new HashSet<>(); // Set of enum URI's
	Set<String> propertyUrisForConstants = new HashSet<>(); // Map of property URI's to be included in the SPDX Constants file
	Set<String> enumerationTypes = new HashSet<>(); // Set of URI's for enumeration types
	Set<String> anyLicenseInfoTypes = new HashSet<>(); // Set of URI's for AnyLicenseInfo types
	Set<String> elementTypes = new HashSet<>(); // Set of URI's for Elemen types
	Set<String> stringTypes = new HashSet<>(); // set of classes which subtype from String
	
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
		OBJECT_SET, ENUM_COLLECTION
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
	public OwlToJava(OntModel model) {
		this.model = model;
		shapes = Shapes.parse(model);
		shapeMap = shapes.getShapeMap();
	}
	
	/**
	 * Generates sore files and store them in the dir
	 * @param dir Directory to hold the java source
	 * @return list of warnings - if empty, all files were generated successfully
	 * @throws IOException for any issues storing the files
	 * @throws OwlToJavaException errors in the ontology
	 */
	public List<String> generate(File dir) throws IOException, OwlToJavaException {
		List<String> warnings = new ArrayList<>();
		List<Individual> allIndividuals = model.listIndividuals().toList();
		List<OntClass> allClasses = model.listClasses().toList();
		List<DatatypeProperty> allDataProperties = model.listDatatypeProperties().toList();
		collectTypeInformation(allClasses, allIndividuals, allDataProperties);
		collectRelationshipRestrictions();
		List<String> classUris = new ArrayList<>();
		List<Map<String, Object>> enumMustacheMaps = new ArrayList<>();
		List<String> createBuilderList = new ArrayList<>();
		allClasses.forEach(ontClass -> {
			String comment = ontClass.getComment(null);
			String classUri = ontClass.getURI();
			classUris.add(classUri);
			String name = uriToClassName(classUri);
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
				if (isEnumClass(ontClass)) {
					enumMustacheMaps.add(generateJavaEnum(dir, classUri, name, allIndividuals, comment));
				} else if (!stringTypes.contains(classUri)) { // TODO: we may want to handle String subtypes in the future
					try {
						boolean isAbstract = isAbstract(ontClass);
						String createString = generateJavaClass(dir, classUri, name, new ArrayList<>(propertyShapes.values()),
								classShape, comment, superClassUri, superClasses, isAbstract);
						if (!isAbstract) {
							createBuilderList.add(createString);
						}
					} catch (OwlToJavaException e) {
						warnings.add("Error generating Java class for "+name+":" + e.getMessage());
					}
				}
			} catch (IOException e) {
				warnings.add("I/O Error generating Java class for "+name+":" + e.getMessage());
			}
		});
		generateSpdxConstants(dir, classUris);
		generateEnumFactory(dir, enumMustacheMaps);
		generateModelClassFactory(dir, classUris);
		generateExternalElement(dir);
		generateModelObject(dir, createBuilderList, classUris);
		generateSpdxModelInfo(dir);
		generatePackageInfo(dir);
		generatePomFile(dir);
		//TODO: Implement Individual Maps
		generateIndividualFactory(dir, new ArrayList<Map<String, Object>>());
		return warnings;
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
		writeMustacheFile(POM_TEMPLATE, file, new HashMap<>());
	}

	/**
	 * @param dir
	 * @throws IOException 
	 */
	private void generatePackageInfo(File dir) throws IOException {
		Path path = dir.toPath().resolve("src").resolve("main").resolve("java").resolve("org")
				.resolve("spdx").resolve("library").resolve("model").resolve("v3");
		Files.createDirectories(path);
		File file = path.resolve("package-info.java").toFile();
		file.createNewFile();
		writeMustacheFile(PACKAGE_INFO_TEMPLATE, file, new HashMap<>());
	}

	/**
	 * @param dir
	 * @throws IOException 
	 */
	private void generateSpdxModelInfo(File dir) throws IOException {
		Path path = dir.toPath().resolve("src").resolve("main").resolve("java").resolve("org")
				.resolve("spdx").resolve("library").resolve("model").resolve("v3");
		Files.createDirectories(path);
		File file = path.resolve("SpdxModelInfoV3_0.java").toFile();
		file.createNewFile();
		writeMustacheFile(MODEL_INFO_TEMPLATE, file, new HashMap<>());
	}

	/**
	 * @param dir
	 * @param createBuilderList
	 * @param classUris
	 * @throws IOException 
	 */
	private void generateModelObject(File dir, List<String> createBuilderList, List<String> classUris) throws IOException {
		Path path = dir.toPath().resolve("src").resolve("main").resolve("java").resolve("org")
				.resolve("spdx").resolve("library").resolve("model").resolve("v3");
		Files.createDirectories(path);
		File file = path.resolve("ModelObjectV3.java").toFile();
		file.createNewFile();
		Map<String, Object> mustacheMap = new HashMap<>();
		mustacheMap.put("createBuilder", createBuilderList);
		List<String> imports = new ArrayList<>();
		for (String classUri:classUris) {
			//TODO: Don't add abstract classes
			if (!enumClassUris.contains(classUri) && !enumerationTypes.contains(classUri)) {
				imports.add("import "+uriToPkg(classUri) + "." + uriToClassName(classUri) +";");
			}
		}
		imports.add("import org.spdx.library.model.v3.core.ProfileIdentifierType;");
		Collections.sort(imports);
		mustacheMap.put("imports", imports);
		writeMustacheFile(BASE_MODEL_OBJECT_TEMPLATE, file, mustacheMap);
	}

	/**
	 * Generate the ExternalElement.java file
	 * @param dir top level directory for the project
	 * @throws IOException 
	 */
	private void generateExternalElement(File dir) throws IOException {
		Path path = dir.toPath().resolve("src").resolve("main").resolve("java").resolve("org")
				.resolve("spdx").resolve("library").resolve("model").resolve("v3");
		Files.createDirectories(path);
		File file = path.resolve("ExternalElement.java").toFile();
		file.createNewFile();
		writeMustacheFile(EXTERNAL_ELEMENT_TEMPLATE, file, new HashMap<>());
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
		
		Path path = dir.toPath().resolve("src").resolve("main").resolve("java").resolve("org")
				.resolve("spdx").resolve("library").resolve("model").resolve("v3");
		Files.createDirectories(path);
		File enumFactoryFile = path.resolve("SpdxEnumFactory.java").toFile();
		enumFactoryFile.createNewFile();	
		writeMustacheFile(ENUM_FACTORY_TEMPLATE, enumFactoryFile, mustacheMap);
	}
	
	/**
	 * Generates the SPDX Individual factory file
	 * @param dir source directory for the factory file
	 * @param individualMustacheMaps list of mustache maps for the individual vocabulariess
	 * @throws IOException thrown if any IO errors occurs
	 */
	private void generateIndividualFactory(File dir,
			List<Map<String, Object>> individualMustacheMaps) throws IOException {
		Map<String, Object> mustacheMap = new HashMap<>();
		mustacheMap.put("individuals", individualMustacheMaps);
		Set<String> pkgs = new HashSet<>();
		for (Map<String, Object> map:individualMustacheMaps) {
			pkgs.add((String)map.get("pkgName") + "." + (String)map.get("name"));
		}
		List<String> imports = new ArrayList<>();
		for (String pkg:pkgs) {
			imports.add("import "+pkg+";");
		}
		Collections.sort(imports);
		mustacheMap.put("imports", imports);
		
		Path path = dir.toPath().resolve("src").resolve("main").resolve("java").resolve("org")
				.resolve("spdx").resolve("library").resolve("model").resolve("v3");
		Files.createDirectories(path);
		File individualsFile = path.resolve("SpdxIndividualFactory.java").toFile();
		individualsFile.createNewFile();	
		writeMustacheFile(INDIVIDUALS_FACTORY_TEMPLATE, individualsFile, mustacheMap);
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
				String propertyName = uriToPropertyName(propUri);
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
			String className = uriToClassName(classUri);
			String profile = uriToProfile(classUri);
			String constName = camelCaseToConstCase(profile) + "_" + camelCaseToConstCase(className);
			classConstantDefinitions.add("static final String " + constName + " = \"" + profile + "." + className + "\";");
			classConstants.add(constName);
		}
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
		Path path = dir.toPath().resolve("src").resolve("main").resolve("java").resolve("org")
				.resolve("spdx").resolve("library").resolve("model").resolve("v3");
		Files.createDirectories(path);
		File constantsFile = path.resolve("SpdxConstantsV3.java").toFile();
		constantsFile.createNewFile();	
		writeMustacheFile(SPDX_CONSTANTS_TEMPLATE, constantsFile, mustacheMap);
	}
	
	/**
	 * Generates the SPDX Model Class Factory source file
	 * @param dir source directory for the constants file
	 * @param classUris list of all class URIs
	 * @throws IOException thrown if any IO errors occurs
	 */
	private void generateModelClassFactory(File dir, List<String> classUris) throws IOException {		
		Map<String, Object> mustacheMap = new HashMap<>();	
		
		List<Map<String, String>> typeToClasses = new ArrayList<>();
		for (String classUri:classUris) {
			String className = uriToClassName(classUri);
			String profile = uriToProfile(classUri);
			String packageName = uriToPkg(classUri);
			String classConstant = camelCaseToConstCase(profile) + "_" + camelCaseToConstCase(className);
			String classPath = packageName + "." + className;
			Map<String, String> typeToClassMap = new HashMap<>();
			typeToClassMap.put("classConstant", classConstant);
			typeToClassMap.put("classPath", classPath);
			typeToClasses.add(typeToClassMap);
		}
		mustacheMap.put("typeToClass", typeToClasses);
		Path path = dir.toPath().resolve("src").resolve("main").resolve("java").resolve("org")
				.resolve("spdx").resolve("library").resolve("model").resolve("v3");
		Files.createDirectories(path);
		File modelClassFactoryFile = path.resolve("SpdxModelClassFactory.java").toFile();
		modelClassFactoryFile.createNewFile();	
		writeMustacheFile(MODEL_CLASS_FACTORY_TEMPLATE, modelClassFactoryFile, mustacheMap);
	}
	
	private String mustacheToString(String templateName, Map<String, Object> mustacheMap) throws IOException {
		String templateDirName = TEMPLATE_ROOT_PATH;
		File templateDirectoryRoot = new File(templateDirName);
		if (!(templateDirectoryRoot.exists() && templateDirectoryRoot.isDirectory())) {
			templateDirName = TEMPLATE_CLASS_PATH;
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
		String templateDirName = TEMPLATE_ROOT_PATH;
		File templateDirectoryRoot = new File(templateDirName);
		if (!(templateDirectoryRoot.exists() && templateDirectoryRoot.isDirectory())) {
			templateDirName = TEMPLATE_CLASS_PATH;
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
	 * @param allObjectProperties object properties in the schema
	 * @param allDataProperties data properties in the schema
	 * @param allClasses classes in the schema
	 * @param allIndividuals Individuals to determine enum class types
	 * @param allDataProperties any data type propoerties
	 * @throws OwlToJavaException 
	 */
	private void collectTypeInformation(List<OntClass> allClasses, List<Individual> allIndividuals, List<DatatypeProperty> allDataProperties) throws OwlToJavaException {
		for (Individual individual:allIndividuals) {
			this.enumClassUris.add(individual.getOntClass(true).getURI());
		}
		allClasses.forEach(ontClass -> {
			List<OntClass> superClasses = new ArrayList<>();
			addAllSuperClasses(ontClass, superClasses);
			if (isEnumClass(ontClass)) {
				enumerationTypes.add(ontClass.getURI());
			} else if (isAnyLicenseInfoClass(ontClass, superClasses)) {
				anyLicenseInfoTypes.add(ontClass.getURI());
			} else if (isElementClass(ontClass, superClasses)) {
				elementTypes.add(ontClass.getURI());
			} else if (isStringClass(ontClass, superClasses)) {
				stringTypes.add(ontClass.getURI());
			}
		});
	}
	
	/**
	 * @param classTypeRestriction class restriction if any
	 * @param dataTypeRestriction data restriction if any
	 * @return The URI for the type of a property based on it's range and restrictions
	 * @throws OwlToJavaException 
	 */
	private String getTypeUri(@Nullable Node classTypeRestriction,
			@Nullable Node dataTypeRestriction) throws OwlToJavaException {
		// precedence - class restrictions, data restriction
		if (Objects.nonNull(classTypeRestriction) && Objects.nonNull(classTypeRestriction.getURI())) {
			return classTypeRestriction.getURI();
		} else if (Objects.nonNull(dataTypeRestriction) && Objects.nonNull(dataTypeRestriction.getURI())) {
			if (dataTypeRestriction.getURI().startsWith(SPDX_URI_PREFIX)) {
				//TODO: Currently, all data type restrictions have a base type of String
				// Once the spec parser produces the type information for data types, this should be updated
				// to use the specified base type and the pattern
				return STRING_TYPE;
			} else {
				return dataTypeRestriction.getURI();
			}
		} else {
			throw new OwlToJavaException("Unable to determine type URI");
		}
	}
	
	/**
	 * @param classTypeRestriction class restriction if any
	 * @param dataTypeRestriction data restriction if any
	 * @param minRestriction minimum cardinality restriction if any
	 * @param maxRextriction maximum cardinality restriction if any
	 * @return the property type based on the range and restrictions
	 * @throws OwlToJavaException 
	 */
	private PropertyType determinePropertyType(@Nullable Node classTypeRestriction,
			@Nullable Node dataTypeRestriction, @Nullable Integer minRestriction, @Nullable Integer maxRestriction) throws OwlToJavaException {
		String typeUri = getTypeUri(classTypeRestriction, dataTypeRestriction);
		if (enumerationTypes.contains(typeUri)) {
			if (Objects.isNull(maxRestriction) || maxRestriction > 1) {
				return PropertyType.ENUM_COLLECTION;
			} else {
				return PropertyType.ENUM;
			}
		} else if (BOOLEAN_TYPE.equals(typeUri)) {
			return PropertyType.BOOLEAN;
		} else if  (INTEGER_TYPES.contains(typeUri)) {
			return PropertyType.INTEGER;
		} else if  (STRING_TYPE.equals(typeUri) || DATE_TIME_TYPE.equals(typeUri) ||
				ANY_URI_TYPE.equals(typeUri) || stringTypes.contains(typeUri)) {
			if (Objects.isNull(maxRestriction) || maxRestriction > 1) {
				return PropertyType.STRING_COLLECTION;
			} else {
				return PropertyType.STRING;
			}
			// If we get here, we're dealing with objects
		} else if (SET_TYPE_URIS.contains(typeUri)) {
			return PropertyType.OBJECT_SET;
		} else if (Objects.isNull(maxRestriction) || maxRestriction > 1) {
			return PropertyType.OBJECT_COLLECTION;
		} else if (anyLicenseInfoTypes.contains(typeUri)) {
			return PropertyType.ANY_LICENSE_INFO;
		} else if (elementTypes.contains(typeUri)) {
			return PropertyType.ELEMENT;
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
		if (STRING_TYPE.equals(ontClass.getURI())) {
			return true;
		}
		for (OntClass superClass:superClasses) {
			if (STRING_TYPE.equals(superClass.getURI())) {
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
		if (ELEMENT_TYPE_URI.equals(ontClass.getURI())) {
			return true;
		}
		for (OntClass superClass:superClasses) {
			if (ELEMENT_TYPE_URI.equals(superClass.getURI())) {
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
		if (ELEMENT_TYPE_ANY_LICENSE_INFO.equals(ontClass.getURI())) {
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
	 * @param dir Directory to store the source files in
	 * @param classUri URI for the class
	 * @param name local name for the class
	 * @param propertyShapes properties for the class
	 * @param classShape Shape for the class
	 * @param comment Description of the class
	 * @param superClassUri URI of the superclass (if any)
	 * @param superClasses all superclasses for the class
	 * @param abstractClass if true, the class is abstract
	 * @return Code to create the Java object to be appended to the model object source file
	 * @throws IOException 
	 * @throws OwlToJavaException 
	 */
	private String generateJavaClass(File dir, String classUri, String name,
			List<PropertyShape> propertyShapes, Shape classShape, String comment, 
			@Nullable String superClassUri, List<OntClass> superClasses,
			boolean abstractClass) throws IOException, OwlToJavaException {
		String pkgName = uriToPkg(classUri);
		File sourceFile = createJavaSourceFile(classUri, dir);
		File unitTestFile = createUnitTestFile(classUri, dir);
		Set<String> requiredImports = new HashSet<>();
		Map<String, Object> mustacheMap = new HashMap<>();
		mustacheMap.put("abstract", abstractClass);
		mustacheMap.put("className", name);
		mustacheMap.put("classProfile", uriToProfile(classUri));
		Map<PropertyType, List<Map<String, Object>>> propertyMap = findProperties(propertyShapes, classShape, 
				requiredImports, propertyUrisForConstants, classUri, superClasses);
		int numProperties = 0;
		for (List<Map<String, Object>> props:propertyMap.values()) {
			numProperties += props.size();
		}
		if (numProperties > 0) {
			requiredImports.add("import org.spdx.library.model.v3.SpdxConstantsV3;");
			requiredImports.add("import java.util.Optional;");
		}
		mustacheMap.put("elementProperties", propertyMap.get(PropertyType.ELEMENT));
		mustacheMap.put("objectProperties", propertyMap.get(PropertyType.OBJECT));
		mustacheMap.put("anyLicenseInfoProperties", propertyMap.get(PropertyType.ANY_LICENSE_INFO));
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
		String superClass = getSuperClass(superClassUri, requiredImports, classUri);
		mustacheMap.put("superClass", superClass);
		mustacheMap.put("verifySuperclass", superClass != "ModelObjectV3");
		if (!this.uriToNamespaceUri(classUri).endsWith("Core")) {
			requiredImports.add("import org.spdx.library.model.v3.core.ProfileIdentifierType;");
		}
		boolean hasCreationInfo = false;
		for (Map<String, Object> property:propertyMap.get(PropertyType.OBJECT)) {
			if (property.get("propertyName").equals("creationInfo")) {
				hasCreationInfo = true;
				break;
			}
		}
		mustacheMap.put("hasCreationInfo", hasCreationInfo);
		if (hasCreationInfo && !"Element".equals(name)) {
			requiredImports.add("import org.spdx.library.model.v3.core.Element;");
			requiredImports.add("import org.spdx.library.model.v3.core.CreationInfo;");
		}
		List<String> imports = buildImports(new ArrayList<String>(requiredImports));
		mustacheMap.put("imports", imports.toArray(new String[imports.size()]));
		//TODO: Implement
		mustacheMap.put("compareUsingProperties", false); // use properties to implement compareTo
		mustacheMap.put("compareProperties", new ArrayList<Map<String, Object>>()); // List of property mustache maps to use in compare
		//TODO: Implement
		mustacheMap.put("usePropertiesForToString", false); // use properties to implement toString
		mustacheMap.put("toStringProperties", new ArrayList<Map<String, Object>>()); // List of property mustache maps to use in compare
		//TODO: Figure out how to handle version specific verify
		writeMustacheFile(JAVA_CLASS_TEMPLATE, sourceFile, mustacheMap);
		writeMustacheFile(UNIT_TEST_TEMPLATE, unitTestFile, mustacheMap);
		return mustacheToString(CREATE_CLASS_TEMPLATE, mustacheMap);
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
			if (ABSTRACT_TYPE_URI.equals(iter.next().getURI())) {
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
	 * @throws OwlToJavaException 
	 */
	private Map<PropertyType, List<Map<String, Object>>> findProperties(List<PropertyShape> propertyShapes, Shape classShape, 
			Set<String> requiredImports, Set<String> propertyUrisForConstants, String classUri, List<OntClass> superClasses) throws OwlToJavaException {
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
	 * @throws OwlToJavaException 
	 */
	private Map<String, Object> propertyToMustachMap(PropertyShape propertyShape,
			Set<String> requiredImports, Set<String> propertyUrisForConstants, String classUri, List<OntClass> superClasses) throws OwlToJavaException {
		Map<String, Object> retval = new HashMap<>();
		String nameSpace = uriToNamespaceUri(classUri);
		String propertyUri = propertyShape.getPath().toString().replaceAll("<", "").replaceAll(">", "");
		
		String name = uriToPropertyName(propertyUri);
		retval.put("propertyName", name);
		retval.put("propertyNameUpper", camelCaseToConstCase(name));
		String getSetName = name.substring(0, 1).toUpperCase() + name.substring(1);
		retval.put("getter", "get" + getSetName);
		retval.put("setter", "set" + getSetName);
		retval.put("adder", "add" + getSetName);
		retval.put("addAller", "addAll" + getSetName);
		
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
		
		PropertyType propertyType = determinePropertyType(classRestriction, dataTypeRestriction, 
				minCardinality, maxCardinality);
		if (PropertyType.OBJECT_COLLECTION.equals(propertyType) || PropertyType.STRING_COLLECTION.equals(propertyType) ||
				PropertyType.ENUM_COLLECTION.equals(propertyType)) {
			requiredImports.add("import java.util.Collection;");
			requiredImports.add("import java.util.Objects;");
		}
 		retval.put("propertyType", propertyType);
		String typeUri = getTypeUri(classRestriction, dataTypeRestriction);
		String type;
		if (BOOLEAN_TYPE.equals(typeUri)) {
			type = "Boolean";
		} else if (STRING_TYPE.equals(typeUri) || DATE_TIME_TYPE.equals(typeUri)) {
			type = "String";
		} else if (INTEGER_TYPES.contains(typeUri)) {
			type = "Integer";
		} else {
			type = uriToClassName(typeUri);
			if (!typeUri.startsWith(nameSpace) && 
					(PropertyType.ENUM.equals(propertyType) || PropertyType.OBJECT.equals(propertyType) ||
							PropertyType.OBJECT_COLLECTION.equals(propertyType) || 
							PropertyType.ENUM_COLLECTION.equals(propertyType) || 
							PropertyType.OBJECT_SET.equals(propertyType) ||
							PropertyType.ANY_LICENSE_INFO.equals(propertyType) ||
							PropertyType.ELEMENT.equals(propertyType))) {				
				requiredImports.add("import "+uriToPkg(typeUri) + "." + uriToClassName(typeUri) +";");
			}
		}
		
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
		} else if (XSD_NON_NEGATIVE_INTEGER.equals(typeUri)) {
			retval.put("min", "0");
			hasConstraint = true;
		} else if (XSD_POSITIVE_INTEGER.equals(typeUri)) {
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
		return RESERVED_JAVA_WORDS.containsKey(retval) ? RESERVED_JAVA_WORDS.get(retval) : retval;
	}
	
	/**
	 * @param uri
	 * @return the class name associated with the class URI
	 */
	private String uriToClassName(String uri) {
		String retval = uriToName(uri);
		String profile = uriToProfile(uri);
		if (!"Core".equalsIgnoreCase(profile)) {
			retval = profile + retval.substring(0, 1).toUpperCase() + retval.substring(1);
		}
		return retval;
	}

	/**
	 * @param uri
	 * @return the property name associated with the class URI
	 */
	private String uriToPropertyName(String uri) {
		String retval = uriToName(uri);
		String profile = uriToProfile(uri);
		if (!"Core".equalsIgnoreCase(profile)) {
			retval = profile + retval.substring(0, 1).toUpperCase() + retval.substring(1);
		}
		return retval;
	}
	
	/**
	 * @param superClassUri URI for the superclass
	 * @param requiredImport set of required imports - updated if the superClass adds a new import statement
	 * @param classUri the URI for the class with the superClass
	 * @return superClass for the class
	 */
	private String getSuperClass(String superClassUri, Set<String> requiredImports, String classUri) {
		if (Objects.isNull(superClassUri) || OWL_THING_URI.equals(superClassUri)) {
			return "ModelObjectV3";
		}
		String classNameSpace = uriToNamespaceUri(classUri);
		if (!superClassUri.startsWith(classNameSpace)) {
			requiredImports.add("import " + uriToPkg(superClassUri) + "." + uriToClassName(superClassUri) + ";");
		}
		return uriToClassName(superClassUri);
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
		retval.add("import org.spdx.library.model.v3.ModelObjectV3;");
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
		writeMustacheFile(ENUM_CLASS_TEMPLATE, sourceFile, mustacheMap);
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
			while (len < COMMENT_LINE_LEN && i < tokens.length) {
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
				.resolve("spdx").resolve("library").resolve("model").resolve("v3");
		String[] parts = classUri.substring(SPDX_URI_PREFIX.length()).split("/");
		for (int i = 2; i < parts.length-1; i++) {
			path = path.resolve(parts[i].toLowerCase());
		}
		Files.createDirectories(path);
		String fileName = uriToClassName(classUri);
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
				.resolve("spdx").resolve("library").resolve("model").resolve("v3");
		String[] parts = classUri.substring(SPDX_URI_PREFIX.length()).split("/");
		// [0] is version, [1] is "terms"
		for (int i = 2; i < parts.length-1; i++) {
			path = path.resolve(parts[i].toLowerCase());
		}
		Files.createDirectories(path);
		String fileName = uriToClassName(classUri);
		File retval = path.resolve(fileName + ".java").toFile();
		retval.createNewFile();
		return retval;
	}

	/**
	 * @param classUri
	 * @return
	 */
	private String uriToPkg(String classUri) {
		String[] parts = classUri.substring(SPDX_URI_PREFIX.length()).split("/");
		StringBuilder sb = new StringBuilder("org.spdx.library.model.v3");
		for (int i = 2; i < parts.length-1; i++) {
			sb.append(".");
			sb.append(parts[i].toLowerCase());
		}
		return sb.toString();
	}

}
