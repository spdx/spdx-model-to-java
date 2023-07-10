/**
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2023 Source Auditor Inc.
 */
package org.spdx.tools.model2java;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.Statement;
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
		
		private Integer min = null;
		private Integer max = null;
		private String pattern = null;
		private Node dataType = null;
		private Integer strMinLengh = null;
		private Integer strMaxLenght = null;
		private Node expectedClass = null;
		

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
			min = constraint.getMinCount();
		}

		@Override
		public void visit(MaxCount constraint) {
			max = constraint.getMaxCount();
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
			// ignore
			
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
		public Integer getMin() {
			return min;
		}

		/**
		 * @return the max
		 */
		public Integer getMax() {
			return max;
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
		
	}
	
	static final String SPDX_URI_PREFIX = "https://spdx.org/rdf/";
	static final String INDENT = "\t";
	private static final int COMMENT_LINE_LEN = 72;
	private static final String BOOLEAN_TYPE = "http://www.w3.org/2001/XMLSchema#boolean";
	private static final String STRING_TYPE = "http://www.w3.org/2001/XMLSchema#string";
	private static final String ELEMENT_TYPE_URI = "https://spdx.org/rdf/Core/Element";
	private static final String ELEMENT_TYPE_ANY_LICENSE_INFO = "https://spdx.org/rdf/Licensing/AnyLicenseInfo";
	private static final String DATE_TIME_TYPE = "https://spdx.org/rdf/Core/DateTime";
	private static final String ANY_URI_TYPE = "http://www.w3.org/2001/XMLSchema#anyURI";
	private static final String OWL_THING_URI = "http://www.w3.org/2002/07/owl#Thing";
	
	static final String TEMPLATE_CLASS_PATH = "resources" + "/" + "javaTemplates";
	static final String TEMPLATE_ROOT_PATH = "resources" + File.separator + "javaTemplates";
	private static final String JAVA_CLASS_TEMPLATE = "ModelObjectTemplate.txt";
	private static final String ENUM_CLASS_TEMPLATE = "EnumTemplate.txt";
	private static final String SPDX_CONSTANTS_TEMPLATE = "SpdxConstantsTemplate.txt";
	
	private static Set<String> INTEGER_TYPES = new HashSet<>();
	static {
		INTEGER_TYPES.add("http://www.w3.org/2001/XMLSchema#positiveInteger");
		INTEGER_TYPES.add("http://www.w3.org/2001/XMLSchema#decimal");
		INTEGER_TYPES.add("http://www.w3.org/2001/XMLSchema#nonNegativeInteger");
		//TODO: Add other types - needs research
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
		//TODO: Add any SET_TYPE_URIS here
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
		collectTypeInformation(allClasses, allIndividuals);
		allClasses.forEach(ontClass -> {
			String comment = ontClass.getComment(null);
			String classUri = ontClass.getURI();
			String name = ontClass.getLocalName();
			if (RESERVED_JAVA_WORDS.containsKey(name)) {
				name = RESERVED_JAVA_WORDS.get(name);
			}
			Shape classShape = shapeMap.get(ontClass.asNode());
			List<Statement> props = new ArrayList<>();
			ontClass.listProperties().forEach(stmt -> {
				props.add(stmt);
			});
			List<OntClass> subClasses = new ArrayList<>();
			ontClass.listSubClasses().forEach(oc -> {
				subClasses.add(oc);
			});
			List<OntClass> superClasses = new ArrayList<>();
			addAllSuperClasses(ontClass, superClasses);
			List<OntProperty> properties = ontClass.listDeclaredProperties(true).toList();
			String superClassUri = superClasses.isEmpty() ? null : superClasses.get(0).getURI();
			try {
				//TODO: Handle individual classes
				if (isEnumClass(ontClass)) {
					generateJavaEnum(dir, classUri, name, allIndividuals, comment);
				} else if (!stringTypes.contains(classUri)) { // TODO: we may want to handle String subtypes in the future
					try {
						generateJavaClass(dir, classUri, name, properties, classShape, comment, superClassUri);
					} catch (OwlToJavaException e) {
						warnings.add("Error generating Java class for "+name+":" + e.getMessage());
					}
				}
			} catch (IOException e) {
				warnings.add("I/O Error generating Java class for "+name+":" + e.getMessage());
			}
		});
		generateSpdxConstants(dir);
		return warnings;
	}

	/**
	 * Generates the SPDX Constants file
	 * @param dir source directory for the constants file
	 * @throws IOException thrown if any IO errors occurs
	 */
	private void generateSpdxConstants(File dir) throws IOException {
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
				String propertyName = uriToName(propUri);
				String propertyConstantName = propertyNameToPropertyConstant(propertyName, namespaceName);
				propMustacheMap.put("propertyConstantName", propertyConstantName);
				propMustacheMap.put("propertyConstantValue", propertyName);
				propMustacheList.add(propMustacheMap);
			}
			namespaceMustacheMap.put("propertyDescriptors", propMustacheList);
			namespaceMustacheList.add(namespaceMustacheMap);
		}
		mustacheMap.put("namespaces", namespaceMustacheList);
		Path path = dir.toPath().resolve("src").resolve("main").resolve("java").resolve("org")
				.resolve("spdx").resolve("library");
		Files.createDirectories(path);
		File constantsFile = path.resolve("SpdxConstants.java").toFile();
		constantsFile.createNewFile();	
		writeMustacheFile(SPDX_CONSTANTS_TEMPLATE, constantsFile, mustacheMap);
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
	 * @throws OwlToJavaException 
	 */
	private void collectTypeInformation(List<OntClass> allClasses, List<Individual> allIndividuals) throws OwlToJavaException {
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
	 * @param range range for the property if any
	 * @param classTypeRestriction class restriction if any
	 * @param dataTypeRestriction data restriction if any
	 * @return The URI for the type of a property based on it's range and restrictions
	 */
	private String getTypeUri(@Nullable OntResource range, @Nullable Node classTypeRestriction,
			@Nullable Node dataTypeRestriction) {
		// precedence - range, class restrictions, data restriction
		String typeUri = range == null ? "" : range.getURI() == null ? "" : range.getURI();
		if (typeUri.isEmpty()) {
			typeUri = classTypeRestriction == null ? "" : classTypeRestriction.getURI() == null ? "" : classTypeRestriction.getURI();
		}
		if (typeUri.isEmpty()) {
			typeUri = dataTypeRestriction == null ? "" : dataTypeRestriction.getURI() == null ? "" : dataTypeRestriction.getURI();
		}
		return typeUri;
	}
	
	/**
	 * @param range range for the property if any
	 * @param classTypeRestriction class restriction if any
	 * @param dataTypeRestriction data restriction if any
	 * @param minRestriction minimum cardinality restriction if any
	 * @param maxRextriction maximum cardinality restriction if any
	 * @return the property type based on the range and restrictions
	 */
	private PropertyType determinePropertyType(@Nullable OntResource range, @Nullable Node classTypeRestriction,
			@Nullable Node dataTypeRestriction, @Nullable Integer minRestriction, @Nullable Integer maxRestriction) {
		String typeUri = getTypeUri(range, classTypeRestriction, dataTypeRestriction);
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
			//TODO: Add in specific types and type checking for DATE_TIME_TYPE and ANY_URI_TYPE
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
		for (OntClass superClass:superClasses) {
			if (ELEMENT_TYPE_ANY_LICENSE_INFO.equals(superClass.getURI())) {
				return true;
			}
		}
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
		List<OntProperty> properties = ontClass.listDeclaredProperties().toList();
		return this.enumClassUris.contains(ontClass.getURI());
	}
	
	/**
	 * @param dir Directory to store the source files in
	 * @param classUri URI for the class
	 * @param name local name for the class
	 * @param properties properties for the class
	 * @param classShape Shape for the class
	 * @param comment Description of the class
	 * @param superClassUri URI of the superclass (if any)
	 * @throws IOException 
	 * @throws OwlToJavaException 
	 */
	private void generateJavaClass(File dir, String classUri, String name,
			List<OntProperty> properties, Shape classShape, String comment, 
			@Nullable String superClassUri) throws IOException, OwlToJavaException {
		String pkgName = uriToPkg(classUri);
		File sourceFile = createJavaSourceFile(classUri, dir);
		Set<String> requiredImports = new HashSet<>();
		Map<String, Object> mustacheMap = new HashMap<>();
		mustacheMap.put("className", name);
		mustacheMap.put("classProfile", uriToProfile(classUri));
		Map<PropertyType, List<Map<String, Object>>> propertyMap = findProperties(properties, classShape, 
				requiredImports, propertyUrisForConstants, classUri);
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
		mustacheMap.put("year", "2023"); // TODO - use actual year
		mustacheMap.put("pkgName", pkgName);
		
		mustacheMap.put("classComments", toClassComment(comment));
		String superClass = getSuperClass(superClassUri, requiredImports, classUri);
		mustacheMap.put("superClass", superClass);
		mustacheMap.put("verifySuperclass", superClass != "ModelObject");
		List<String> imports = buildImports(new ArrayList<String>(requiredImports));
		mustacheMap.put("imports", imports.toArray(new String[imports.size()]));
		//TODO: Implement
		mustacheMap.put("compareUsingProperties", false); // use properties to implement compareTo
		mustacheMap.put("compareProperties", new ArrayList<Map<String, Object>>()); // List of property mustache maps to use in compare
		//TODO: Implement
		mustacheMap.put("usePropertiesForToString", false); // use properties to implement toString
		mustacheMap.put("toStringProperties", new ArrayList<Map<String, Object>>()); // List of property mustache maps to use in compare
		//TODO: Figure out how to handle version specific verify
		//TODO: Add builder to template
		
		writeMustacheFile(JAVA_CLASS_TEMPLATE, sourceFile, mustacheMap);
	}
	

	/**
	 * @param properties direct ontology properties
	 * @param classShape Shape for the class containing the properties
	 * @param requiredImport set of required imports for this class - updated with any additional imports
	 * @param propertyUrisForConstants set of URI's for any properties - updated with any additional values
	 * @param classUri URI for the class containing the properties
	 * @return map of mustache strings to properties for any properties returning a type of Element
	 * @throws OwlToJavaException 
	 */
	private Map<PropertyType, List<Map<String, Object>>> findProperties(List<OntProperty> properties, Shape classShape, 
			Set<String> requiredImports, Set<String> propertyUrisForConstants, String classUri) throws OwlToJavaException {
		Map<PropertyType, List<Map<String, Object>>> retval = new HashMap<>();
		for (PropertyType value:PropertyType.values()) {
			retval.put(value, new ArrayList<Map<String, Object>>());
		}
		Map<String, PropertyShape> propShapeMap = new HashMap<>();
		for (PropertyShape propShape:classShape.getPropertyShapes()) {
			String propUri = propShape.getPath().toString().replaceAll("<", "").replaceAll(">", "");
			propShapeMap.put(propUri, propShape);
		}
		for (OntProperty property:properties) {
			PropertyShape propertyShape = propShapeMap.get(property.getURI());
			if (!Objects.isNull(propertyShape)) {
				Map<String, Object> mustacheMap = propertyToMustachMap(property, propertyShape, requiredImports, propertyUrisForConstants, classUri);
				retval.get(mustacheMap.get("propertyType")).add(mustacheMap);
			}
		}
		return retval;
	}

	/**
	 * Maps a property to mustache map adding any required import strings and adding any required constant strings
	 * @param property Property to convert to Mustache appropriate map
	 * @param classShape Shape of the class using the property
	 * @param requiredImport set of required imports for this class - updated with any additional imports
	 * @param propertyUrisForConstants set of URI's for any properties - updated with any additional values
	 * @param classUri URI of the class using the property
	 * @return map of Mustache strings to values for a give ontology property
	 * @throws OwlToJavaException 
	 */
	private Map<String, Object> propertyToMustachMap(OntProperty property, Shape propertyShape,
			Set<String> requiredImports, Set<String> propertyUrisForConstants, String classUri) throws OwlToJavaException {
		//TODO: Implement
		Map<String, Object> retval = new HashMap<>();
		String nameSpace = uriToNamespaceUri(classUri);
		String name = property.getLocalName();
		if (RESERVED_JAVA_WORDS.containsKey(name)) {
			name = RESERVED_JAVA_WORDS.get(name);
		}
		retval.put("propertyName", name);
		String getSetName = name.substring(0, 1).toUpperCase() + name.substring(1);
		retval.put("getter", "get" + getSetName);
		retval.put("setter", "set" + getSetName);
		
		Integer min = null;
		Integer max = null;
		String pattern = null;
		Node classRestriction = null;
		Node dataTypeRestriction = null;
		
		for (Constraint constraint:propertyShape.getConstraints()) {
			ConstraintCollector collector = new ConstraintCollector();
			constraint.visit(collector);
			if (collector.getMin() != null) {
				if (min == null || min > collector.getMin()) {
					min = collector.getMin();
				}
			}
			if (collector.getMax() != null) {
				if (max == null || max > collector.getMax()) {
					max = collector.getMax();
				}
			}
			if (collector.getPattern() != null) {
				pattern = collector.getPattern();
			}
			
			if (collector.getDataType() != null) {
				dataTypeRestriction = collector.getDataType();
			} else if (collector.getExpectedClass() != null) {
				classRestriction = collector.getExpectedClass();
			}
		}

		List<? extends OntResource> ranges = property.listRange().toList();
		if (ranges.size() != 1) {
			throw new OwlToJavaException("Ambiguous or missing type for property "+property.getLocalName());
		}
		OntResource rangeResource = ranges.get(0);
		PropertyType propertyType = determinePropertyType(rangeResource, classRestriction, dataTypeRestriction, min, max);
		if (PropertyType.OBJECT_COLLECTION.equals(propertyType) || PropertyType.STRING_COLLECTION.equals(propertyType)) {
			requiredImports.add("import java.util.Collection;");
		}
 		retval.put("propertyType", propertyType);
		String typeUri = getTypeUri(rangeResource, classRestriction, dataTypeRestriction);
		String type;
		if (BOOLEAN_TYPE.equals(typeUri)) {
			type = "boolean";
		} else if (STRING_TYPE.equals(typeUri) || DATE_TIME_TYPE.equals(typeUri)) {
			type = "String";
		} else if (INTEGER_TYPES.contains(typeUri)) {
			type = "int";
		} else {
			type = uriToName(typeUri);
			if (!typeUri.startsWith(nameSpace) && 
					(PropertyType.ENUM.equals(propertyType) || PropertyType.OBJECT.equals(propertyType) ||
							PropertyType.OBJECT_COLLECTION.equals(propertyType) || PropertyType.OBJECT_SET.equals(propertyType))) {				
				requiredImports.add("import "+uriToPkg(typeUri) + "." + uriToName(typeUri) +";");
			}
		}
		retval.put("type", type);
		retval.put("required", min != null && min > 0);
		//TODO: Add any additional profile restrictions
		retval.put("requiredProfiles",  namespaceToProfileIdentifierType(nameSpace));
		if (Objects.nonNull(pattern)) {
			retval.put("pattern", pattern);
		}
		if (Objects.nonNull(min)) {
			retval.put("min", min.toString());
		}
		if (Objects.nonNull(max)) {
			retval.put("max", max.toString());
		}
		if (Objects.nonNull(pattern)) {
			requiredImports.add("import java.util.regex.Pattern;");
			retval.put("pattern", StringEscapeUtils.escapeJava(pattern));
		}
		retval.put("uri", property.getURI());
		String propNameSpace = uriToNamespaceUri(property.getURI());
		propNameSpace = propNameSpace.substring(propNameSpace.lastIndexOf('/')+1);
		String propConstant = propertyNameToPropertyConstant(name, propNameSpace);
		retval.put("propertyConstant", propConstant);
		propertyUrisForConstants.add(property.getURI());
		return retval;
	}

	/**
	 * @param nameSpace
	 * @return the ProfileIdentifierType string associated with the namespace
	 */
	private String namespaceToProfileIdentifierType(String nameSpace) {
		return "ProfileIdentifierType." + uriToName(nameSpace).toUpperCase();
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
	 * @param superClassUri URI for the superclass
	 * @param requiredImport set of required imports - updated if the superClass adds a new import statement
	 * @param classUri the URI for the class with the superClass
	 * @return superClass for the class
	 */
	private String getSuperClass(String superClassUri, Set<String> requiredImports, String classUri) {
		if (Objects.isNull(superClassUri) || OWL_THING_URI.equals(superClassUri)) {
			return "ModelObject";
		}
		String classNameSpace = uriToNamespaceUri(classUri);
		if (!superClassUri.startsWith(classNameSpace)) {
			requiredImports.add("import " + uriToPkg(superClassUri) + "." + uriToName(superClassUri) + ";");
		}
		return uriToName(superClassUri);
	}

	/**
	 * @return a list of import statements appropriate for the class
	 */
	private List<String> buildImports(List<String> localImports) {
		List<String> retval = new ArrayList<>();
		retval.add("import java.util.ArrayList;");
		retval.add("import java.util.Arrays;");
		retval.add("import java.util.Collections;");
		retval.add("import java.util.List;");
		retval.add("import java.util.Optional;");
		retval.add("import java.util.Set;");
		retval.add("");
		retval.add("import org.spdx.library.DefaultModelStore;");
		retval.add("import org.spdx.library.InvalidSPDXAnalysisException;");
		retval.add("import org.spdx.library.ModelCopyManager;");
		retval.add("import org.spdx.library.SpdxConstants;");
		retval.add("import org.spdx.library.model.ModelObject;");
		retval.add("import org.spdx.storage.IModelStore;");
		retval.add("import org.spdx.storage.IModelStore.IdType;");
		retval.add("");
		Collections.sort(localImports);
		for (String localImport:localImports) {
			retval.add(localImport);
		}
		return retval;
	}

	/**
	 * @param dir Directory to store the source files in
	 * @param classUri URI for the class
	 * @param name local name for the class
	 * @param properties properties for the class
	 * @param comment Description of the class
	 * @throws IOException 
	 */
	private void generateUnitTest(File dir, String classUri, String name,
			List<OntProperty> properties, String comment) throws IOException {
		//TODO: Implement
	}
	

	/**
	 * @param dir Directory to store the source files in
	 * @param classUri URI for the enum
	 * @param name local name for the enum
	 * @param allIndividuals individual values from the model
	 * @param comment Description of the enum
	 * @throws IOException 
	 */
	private void generateJavaEnum(File dir, String classUri, String name,
			List<Individual> allIndividuals, String comment) throws IOException {
		Map<String, Object> mustacheMap = new HashMap<>();
		mustacheMap.put("year", "2023"); // TODO: Implement the actual year
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
			if (!Character.isLowerCase(ch)) {
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
		return   camelCaseToConstCase(namespaceName) + "_PROP_" + camelCaseToConstCase(propName);
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
	 * @param enumToModelName entry mapping an enum value to the model name
	 * @param writer 
	 */
	private void writeEnumEntry(Entry<String, String> enumToModelName, PrintWriter writer) {
		writer.write(enumToModelName.getKey());
		writer.write("(\"");
		writer.write(enumToModelName.getValue());
		writer.write("\")");
	}

	/**
	 * @param writer
	 */
	private void writeFileHeader(PrintWriter writer) {
		writer.println("/**");
		writer.println(" * Copyright (c) 2019 Source Auditor Inc.");
		writer.println("");
		writer.println(" * SPDX-License-Identifier: Apache-2.0");
		writer.println("");
		writer.println(" */");
	}

	/**
	 * @param classUri URI for the class
	 * @param dir directory to hold the file
	 * @return the created file
	 * @throws IOException 
	 */
	private File createJavaSourceFile(String classUri, File dir) throws IOException {		
		Path path = dir.toPath().resolve("src").resolve("main").resolve("java").resolve("org")
				.resolve("spdx").resolve("library").resolve("model");
		String[] parts = classUri.substring(SPDX_URI_PREFIX.length()).split("/");
		for (int i = 0; i < parts.length-1; i++) {
			path = path.resolve(parts[i].toLowerCase());
		}
		Files.createDirectories(path);
		File retval = path.resolve(parts[parts.length-1] + ".java").toFile();
		retval.createNewFile();
		return retval;
	}

	/**
	 * @param classUri
	 * @return
	 */
	private String uriToPkg(String classUri) {
		String[] parts = classUri.substring(SPDX_URI_PREFIX.length()).split("/");
		StringBuilder sb = new StringBuilder("org.spdx.library.model");
		for (int i = 0; i < parts.length-1; i++) {
			sb.append(".");
			sb.append(parts[i].toLowerCase());
		}
		return sb.toString();
	}

}
