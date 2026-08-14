package org.spdx.tools.model2java;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.ontology.*;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.engine.constraint.HasValueConstraint;
import org.apache.jena.shacl.engine.constraint.ShNot;
import org.apache.jena.shacl.engine.constraint.SparqlConstraint;
import org.apache.jena.shacl.parser.Constraint;
import org.apache.jena.shacl.parser.PropertyShape;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.spdx.tools.model2java.model.*;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.spdx.tools.model2java.ShaclToJavaConstants.RESERVED_JAVA_WORDS;
import static org.spdx.tools.model2java.ShaclToJavaConstants.TYPE_PRED;

/**
 * Holds RDF model information about a specific SPDX specification version
 */
public class SpecVersionContainer implements Comparable<SpecVersionContainer> {

    OntModel model;
    Shapes shapes;
    Map<Node, Shape> shapeMap;

    final List<String> warnings = new ArrayList<>();

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
    String spdxNamespace;

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

    final List<IndividualClassModel> individuals = new ArrayList<>();
    final List<EnumModel> enums = new ArrayList<>();
    final List<JavaClassModel> javaClasses = new ArrayList<>();
    final List<JavaClassModel> externalJavaClasses = new ArrayList<>();
    final List<UnitTestModel> unitTestClasses = new ArrayList<>();
    final ConstantsModel constantsModel = new ConstantsModel();
    final EnumFactoryModel enumFactoryModel = new EnumFactoryModel();
    final ModelClassFactoryModel modelClassFactoryModel = new ModelClassFactoryModel();
    final ModelObjectModel modelObjectModel = new ModelObjectModel();
    final SpdxModelInfoModel spdxModelInfoModel = new SpdxModelInfoModel();
    final PackageInfoModel packageInfoModel = new PackageInfoModel();
    final IndividualFactoryModel individualFactoryModel = new IndividualFactoryModel();
    final MockFileModel mockFileModel = new MockFileModel();
    final InvalidLicenseExpressionModel invalidLicenseExpressionModel = new InvalidLicenseExpressionModel();
    final TestValuesGeneratorModel testValuesGeneratorModel = new TestValuesGeneratorModel();

    /**
     * Create a container for model information related to a specific version of an SPDX specification
     * @param model Ontology model representing the spec version
     * @param specVersion Version of the specification
     */
    SpecVersionContainer(OntModel model, String specVersion) {
        this.model = model;
        shapes = Shapes.parse(model);
        shapeMap = shapes.getShapeMap();
        spdxNamespace = model.getNsPrefixURI("spdx");
        versionSemVer = specVersion;
        allIndividuals = model.listIndividuals().toList();
        allClasses = model.listClasses().toList();
        allDataProperties = model.listDatatypeProperties().toList();
        allObjectProperties = model.listObjectProperties().toList();
        objectIndividuals = new ArrayList<>();
        Query asIndividualQuery = QueryFactory.create(String.format(
                "select ?oi where {?oi <%s> <%s>}", TYPE_PRED, ShaclToJavaConstants.NAMED_INDIVIDUAL));
        try (QueryExecution qexec = QueryExecutionFactory.create(asIndividualQuery, model)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();
                objectIndividuals.add(solution.getResource("oi"));
            }
        }
        collectTypeInformation();
        collectRelationshipRestrictions();
        collectNameMappings();
        buildModel();
    }

    /**
     * Collect type information into the field sets for enum types, anyLicenseInfo types, and elements types.  also fills in the enum class URIs
     */
    private void collectTypeInformation() {
        for (Resource individual:objectIndividuals) {
            String individualClassUri = null;
            StmtIterator propertyIter = individual.listProperties();
            while (propertyIter.hasNext()) {
                Statement stmt = propertyIter.next();
                if (stmt.getPredicate().getURI().equals(TYPE_PRED) && stmt.getObject().isURIResource() &&
                        !stmt.getObject().asResource().getURI().equals(ShaclToJavaConstants.NAMED_INDIVIDUAL)) {
                    individualClassUri = stmt.getObject().asResource().getURI();
                }
            }
            OntClass individualClass = model.getOntClass(individualClassUri);
            List<OntClass> superClasses = new ArrayList<>();
            addAllSuperClasses(individualClass, superClasses);
            boolean elementSubClass = false;
            for (OntClass superClass : superClasses) {
                if (superClass.getURI().endsWith("/Element")) {
                    elementSubClass = true;
                    break;
                }
            }
            if (elementSubClass) {
                // TODO: This is a bit of a hack, maybe there is a better way to see if this is not an enum
                List<String> individualsForRange = classUriToIndividualUris.get(individualClassUri);
                if (Objects.isNull(individualsForRange)) {
                    individualsForRange = new ArrayList<>();
                    classUriToIndividualUris.put(individualClassUri, individualsForRange);
                }
                individualsForRange.add(individual.getURI());
            } else {
                this.enumClassUris.add(individualClassUri);
            }
        }

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
     * Add super classes including transitive superclasses to the superClasses list.
     * The classes will be in the order of the closest superclass to the ontClass
     * @param ontClass class to add superClasses for
     * @param superClasses resultant list of superclasses - list elements are added by this method
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
        return ontClass.getURI().endsWith(ShaclToJavaConstants.ELEMENT_TYPE_ANY_LICENSE_INFO_SUFFIX);
        // We don't include superclasses for AnyLicenseInfo types
    }

    /**
     * @param ontClass class
     * @param superClasses list of all superclasses for the class
     * @return true if the class is an AnyLicenseInfo or a subclass of AnyLicenseInfo
     */
    private boolean isExtendableLicenseClass(OntClass ontClass,
                                             List<OntClass> superClasses) {
        return ontClass.getURI().endsWith(ShaclToJavaConstants.ELEMENT_TYPE_EXTENDABLE_LICENSE_SUFFIX);
        // We don't include superclasses for AnyLicenseInfo types
    }

    /**
     * @param ontClass class
     * @param superClasses list of all superclasses for the class
     * @return true if the class is an AnyLicenseInfo or a subclass of AnyLicenseInfo
     */
    private boolean isLicenseAdditionClass(OntClass ontClass,
                                           List<OntClass> superClasses) {
        return ontClass.getURI().endsWith(ShaclToJavaConstants.ELEMENT_TYPE_LICENSE_ADDITION_SUFFIX);
        // We don't include superclasses for AnyLicenseInfo types
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
        //TODO: See if we can remove the above line - I think it is here just for debugging
        return this.enumClassUris.contains(ontClass.getURI());
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
                            // TODO: Implement shape constraint shacl - requires quite a bit of effort
                        }
                    }
                }
            }

        }
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
        }
        if (nameUriMap.containsKey(name)) {
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
        return ShaclToJavaConstants.RESERVED_JAVA_WORDS.getOrDefault(retval, retval);
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
     * Extracts the profile name from a URI for a class or property
     * @param uri URI used for classes and properties
     * @return profile name (last segment of the namespace)
     */
    private String uriToProfile(String uri) {
        String namespace = uriToNamespaceUri(uri);
        return uriToName(namespace);
    }

    /**
     * Converts a URI to a package name
     * @param classUri URI for a class
     * @return Java package name for the class
     */
    private String uriToPkg(String classUri) {
        String[] parts = classUri.substring(ShaclToJavaConstants.SPDX_URI_PREFIX.length()).split("/");
        StringBuilder sb = new StringBuilder("org.spdx.library.model.");
        sb.append(JavaCodeGenerator.VERSION_SUFFIX);
        for (int i = 2; i < parts.length-1; i++) {
            sb.append(".");
            sb.append(parts[i].toLowerCase());
        }
        return sb.toString();
    }

    /**
     * @param uri URI used for classes and properties
     * @return namespace portion of the URI
     */
    private String uriToNamespaceUri(String uri) {
        return uri.substring(0, uri.lastIndexOf('/'));
    }

    /**
     * @param nameSpace namespace to convert
     * @return the ProfileIdentifierType string associated with the namespace
     */
    private String namespaceToProfileIdentifierType(String nameSpace) {
        return "ProfileIdentifierType." + uriToName(nameSpace).replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
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
                for (PropertyShape superPropertyShape : classShape.getPropertyShapes()) {
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
                for (PropertyShape superPropertyShape : classShape.getPropertyShapes()) {
                    if (propertyShape.getPath().equalTo(superPropertyShape.getPath(), null)) {
                        Integer minCardinality = null;
                        for (Constraint constraint : superPropertyShape.getConstraints()) {
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
     * Create the mappings from the URI's to class names and property names
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
     * @param classUri class URI
     * @return URI for a class which is the external form of the classUri
     */
    private String classUriToExternalClassUri(String classUri) {
        String nameSpaceUri = uriToNamespaceUri(classUri);
        String className = classUri.substring(nameSpaceUri.length()+1);
        return nameSpaceUri + "/External" + className;
    }

    /**
     * @param individualUri URI for the individual
     * @return Comment from the ONTModel for the individual - empty string if not present
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
     * @param ontClass shape of class
     * @return true if the classShape represents an abstract class
     */
    private boolean isAbstract(OntClass ontClass) {
        if (Objects.isNull(ontClass)) {
            return false;
        }
        Shape classShape = shapeMap.get(ontClass.asNode());
        if (Objects.nonNull(classShape)) {
            for (PropertyShape propertyShape : classShape.getPropertyShapes()) {
                String propertyUri = propertyShape.getPath().toString().replaceAll("<", "").replaceAll(">", "");
                if (TYPE_PRED.equals(propertyUri)) {
                    for (Constraint constraint : propertyShape.getConstraints()) {
                        ConstraintCollector collector = new ConstraintCollector();
                        constraint.visit(collector);
                        ShNot notConstraint = collector.getNotConstraint();
                        if (Objects.nonNull(notConstraint) && Objects.nonNull(notConstraint.getOther())) {
                            for (Constraint otherConstraint : notConstraint.getOther().getConstraints()) {
                                if (Objects.nonNull(otherConstraint) && otherConstraint instanceof HasValueConstraint &&
                                        ontClass.getURI().equals(((HasValueConstraint) otherConstraint).getValue().getURI())) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Builds the model files filling in the information from the OntModel
     */
    private void buildModel() {
        List<String> classUris = new ArrayList<>();
        List<String> createBuilderList = new ArrayList<>();
        Map<String, JavaClassModel> javaClassModels = new HashMap<>();
        Map<String, UnitTestModel> unitTestModels = new HashMap<>();
        Map<PropertyModel.PropertyType, Map<String, PropertyModel>> allPropertiesInUse = new HashMap<>();
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
                            this.individuals.add(buildIndividualClass(individualUri, uriToClassName.get(individualUri),
                                    new ArrayList<>(propertyShapes.values()), getIndividualComment(individualUri),
                                    classUri, classShape, superClasses));
                        } catch (ShaclToJavaException e) {
                            warnings.add("Error generating Individual Java class for "+individualUri+":" + e.getMessage());
                        }
                    }
                }
                if (isEnumClass(ontClass)) {
                    enums.add(buildJavaEnum(classUri, name, allIndividuals, comment));
                } else if (!stringTypes.contains(classUri)) { // TODO: we may want to handle String subtypes in the future
                    try {
                        boolean isAbstract = isAbstract(ontClass);
                        String createString = buildDataForClass(classUri, name, new ArrayList<>(propertyShapes.values()),
                                classShape, comment, superClassUri, superClasses, isAbstract,
                                javaClassModels, unitTestModels, allPropertiesInUse);
                        if (!isAbstract) {
                            createBuilderList.add(createString);
                        }
                        javaClasses.add(javaClassModels.get(classUri));
                        if (elementTypes.contains(classUri)) {
                            externalJavaClasses.add(javaClassModels.get(classUri));
                        }
                        if (!isAbstract) {
                            unitTestClasses.add(unitTestModels.get(classUri));
                        }
                    } catch (ShaclToJavaException e) {
                        warnings.add("Error generating Java class for "+name+":" + e.getMessage());
                    }
                }
            } catch (IOException e) {
                warnings.add("I/O Error generating Java class for "+name+":" + e.getMessage());
            }
        });
        buildTestValueGenerator(allPropertiesInUse, unitTestModels);
        buildSpdxConstants(classUris);
        buildEnumFactory(enums);
        buildModelClassFactory(classUris);
        buildModelObject(createBuilderList, classUris);
        buildSpdxModelInfo();
        buildPackageInfo();
        buildIndividualFactory();
        buildMockFiles();
        buildInvalidLicenseExpression();
    }


    private void buildInvalidLicenseExpression() {
        invalidLicenseExpressionModel.setVersionSuffix(JavaCodeGenerator.VERSION_SUFFIX);
        invalidLicenseExpressionModel.setVersionSemVer(versionSemVer);
    }

    private void buildMockFiles() {
        mockFileModel.setVersionSuffix(JavaCodeGenerator.VERSION_SUFFIX);
        mockFileModel.setSpecVersion(versionSemVer);
    }

    private void buildIndividualFactory() {
        individualFactoryModel.setVersionSuffix(JavaCodeGenerator.VERSION_SUFFIX);
        List<IndividualModel> individualModels = new ArrayList<>();
        List<String> imports = new ArrayList<>();
        for (List<String> individuals:this.classUriToIndividualUris.values()) {
            for (String individualUri:individuals) {
                String className = uriToClassName.get(individualUri);
                String pkg = uriToPkg(individualUri);
                IndividualModel individualModel = new IndividualModel();
                individualModel.setIndividualUri(individualUri);
                individualModel.setClassName(className);
                individualModels.add(individualModel);
                String importStr = "import "+pkg+"."+className + ";";
                if (!imports.contains(importStr)) {
                    imports.add(importStr);
                }
            }
        }
        Collections.sort(imports);
        individualFactoryModel.setIndividuals(individualModels);
        individualFactoryModel.setImports(imports);
    }

    private void buildPackageInfo() {
        packageInfoModel.setVersionSuffix(JavaCodeGenerator.VERSION_SUFFIX);
        packageInfoModel.setVersionSemVer(versionSemVer);
    }

    private void buildSpdxModelInfo() {
        spdxModelInfoModel.setClassSuffix("3.0.1".equals(versionSemVer) ? "V3_0" : "V3");// for backwards compatibility
        spdxModelInfoModel.setVersionSuffix(JavaCodeGenerator.VERSION_SUFFIX);
        spdxModelInfoModel.setVersionSemVer(versionSemVer);
    }

    /**
     * Builds the ModelObjectModel
     * @param createBuilderList list of createBuilder strings
     * @param classUris list of all class URIs
     */
    private void buildModelObject(List<String> createBuilderList, List<String> classUris) {
        modelObjectModel.setCreateBuilder(createBuilderList);
        modelObjectModel.setVersionSuffix(JavaCodeGenerator.VERSION_SUFFIX);
        modelObjectModel.setVersionSemVer(versionSemVer);
        List<String> imports = new ArrayList<>();
        for (String classUri:classUris) {
            //TODO: Don't add abstract classes
            if (!enumClassUris.contains(classUri) && !enumerationTypes.contains(classUri)) {
                imports.add("import "+uriToPkg(classUri) + "." + uriToClassName.get(classUri) +";");
            }
        }
        imports.add("import org.spdx.library.model."+JavaCodeGenerator.VERSION_SUFFIX+".core.ProfileIdentifierType;");
        Collections.sort(imports);
        modelObjectModel.setImports(imports);
    }

    /**
     * Builds the ModelClassFactory
     * @param classUris URIs for all the classes
     */
    private void buildModelClassFactory(List<String> classUris) {
        modelClassFactoryModel.setVersionSuffix(JavaCodeGenerator.VERSION_SUFFIX);
        List<TypeToClassModel> typeToClasses = new ArrayList<>();
        for (String classUri:classUris) {
            String className = uriToClassName.get(classUri);
            String profile = uriToProfile(classUri);
            String packageName = uriToPkg(classUri);
            String classConstant = camelCaseToConstCase(profile) + "_" + camelCaseToConstCase(className);
            String classPath = packageName + "." + className;
            TypeToClassModel typeToClassModel = new TypeToClassModel();
            typeToClassModel.setClassConstant(classConstant);
            typeToClassModel.setClassPath(classPath);
            typeToClasses.add(typeToClassModel);
        }

        // Add individual types
        for (List<String> individualUris:this.classUriToIndividualUris.values()) {
            for (String individualUri:individualUris) {
                String className = uriToClassName.get(individualUri);
                String profile = uriToProfile(individualUri);
                String packageName = uriToPkg(individualUri);
                String classConstant = camelCaseToConstCase(profile) + "_" + camelCaseToConstCase(className);
                String classPath = packageName + "." + className;
                TypeToClassModel typeToClassModel = new TypeToClassModel();
                typeToClassModel.setClassConstant(classConstant);
                typeToClassModel.setClassPath(classPath);
                typeToClasses.add(typeToClassModel);
            }
        }
        modelClassFactoryModel.setTypeToClass(typeToClasses);
    }

    /**
     * Builds the Enum factory model
     * @param enumModels list of enum models
     */
    private void buildEnumFactory(List<EnumModel> enumModels) {
        enumFactoryModel.setEnumClasses(enumModels);
        Set<String> pkgs = new HashSet<>();
        for (EnumModel enumModel:enumModels) {
            pkgs.add(enumModel.getPkgName() + "." + enumModel.getName());
        }
        List<String> imports = new ArrayList<>();
        for (String pkg:pkgs) {
            imports.add("import "+pkg+";");
        }
        Collections.sort(imports);
        enumFactoryModel.setImports(imports);
        enumFactoryModel.setVersionSuffix(JavaCodeGenerator.VERSION_SUFFIX);
    }

    /**
     * Builds the spdxConstants model
     * @param classUris URIs for all the classes
     */
    private void buildSpdxConstants(List<String> classUris) {
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
        constantsModel.setVersionSuffix(JavaCodeGenerator.VERSION_SUFFIX);
        List<NamespaceModel> namespaceList = new ArrayList<>();
        List<String> namespaceUris = new ArrayList<>(namespaceToPropUri.keySet());
        Collections.sort(namespaceUris);
        for (String namespaceUri:namespaceUris) {
            NamespaceModel namespaceModel = new NamespaceModel();
            String namespaceName = uriToName(namespaceUri);
            namespaceModel.setNamespaceName(namespaceName);
            String namespaceConstantName = camelCaseToConstCase(namespaceName) + "_NAMESPACE";
            namespaceModel.setNamespaceConstantName(namespaceConstantName);
            namespaceModel.setNamespaceUri(namespaceUri);
            List<String> propertyUris = new ArrayList<>(namespaceToPropUri.get(namespaceUri));
            Collections.sort(propertyUris);
            List<PropertyDescriptorModel> propList = new ArrayList<>();
            for (String propUri:propertyUris) {
                PropertyDescriptorModel propModel = new PropertyDescriptorModel();

                String propertyConstantName = propertyNameToPropertyConstant(uriToPropertyName.get(propUri), namespaceName);
                propModel.setPropertyConstantName(propertyConstantName);
                String propertyName = uriToName(propUri);
                String uriPropName = propNameToUriPropName(propertyName);
                propModel.setPropertyConstantValue(uriPropName);
                propList.add(propModel);
            }
            namespaceModel.setPropertyDescriptors(propList);
            namespaceList.add(namespaceModel);
        }
        constantsModel.setNamespaces(namespaceList);
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

        String classConstantString = buildClassConstant(classConstants);
        constantsModel.setClassConstants(classConstants);
        constantsModel.setClassConstantDefinitions(classConstantDefinitions);
        constantsModel.setAllClassConstants(classConstantString);
        constantsModel.setVersionSemVer(versionSemVer);
    }

    /**
     * Buidl a class constant string from a list of classes
     * @param classConstants class constant strings
     * @return a class constant string
     */
    public static String buildClassConstant(List<String> classConstants) {
        StringBuilder classConstantString = new StringBuilder("static final String[] ALL_SPDX_CLASSES = {");
        int lineLen = classConstantString.length();
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
        classConstantString.append("};");
        return classConstantString.toString();
    }

    /**
     * Convert the property name used in the Java class to the URI version of the property name
     * @param propertyName used in the class
     * @return URI version of the property name which may be a Java reserved word
     */
    private static String propNameToUriPropName(String propertyName) {
        String uriPropName = null;
        if (RESERVED_JAVA_WORDS.containsValue(propertyName)) {
            for (Map.Entry<String, String> entry:RESERVED_JAVA_WORDS.entrySet()) {
                if (entry.getValue().equals(propertyName)) {
                    uriPropName = entry.getKey();
                    break;
                }
            }
        }
        if (Objects.isNull(uriPropName)) {
            uriPropName = propertyName;
        }
        return uriPropName;
    }

    private void buildTestValueGenerator(Map<PropertyModel.PropertyType, Map<String, PropertyModel>> allPropertiesInUse, Map<String, UnitTestModel> unitTestMaps) {
        testValuesGeneratorModel.setVersionSuffix(JavaCodeGenerator.VERSION_SUFFIX);
        testValuesGeneratorModel.setVersionSemVer(versionSemVer);
        Set<String> requiredImports = new HashSet<>();
        for (Map.Entry<PropertyModel.PropertyType, Map<String, PropertyModel>> entry:allPropertiesInUse.entrySet()) {
            List<PropertyModel> propertiesForType = new ArrayList<>();
            for (PropertyModel propertyMap:entry.getValue().values()) {
                propertiesForType.add(propertyMap);
                String typeUri = propertyMap.getTypeUri();
                if (PropertyModel.PropertyType.ENUM.equals(entry.getKey()) || PropertyModel.PropertyType.OBJECT.equals(entry.getKey()) ||
                        PropertyModel.PropertyType.OBJECT_COLLECTION.equals(entry.getKey()) ||
                        PropertyModel.PropertyType.ENUM_COLLECTION.equals(entry.getKey()) ||
                        PropertyModel.PropertyType.OBJECT_SET.equals(entry.getKey()) ||
                        PropertyModel.PropertyType.ANY_LICENSE_INFO.equals(entry.getKey()) ||
                        PropertyModel.PropertyType.LICENSE_ADDITION.equals(entry.getKey()) ||
                        PropertyModel.PropertyType.EXTENDABLE_LICENSE.equals(entry.getKey()) ||
                        PropertyModel.PropertyType.ELEMENT.equals(entry.getKey())) {
                    requiredImports.add("import "+uriToPkg(typeUri) + "." + uriToClassName.get(typeUri) +";");
                }
            }
            switch (entry.getKey()) {
                case ELEMENT: testValuesGeneratorModel.setElementProperties(propertiesForType); break;
                case OBJECT: testValuesGeneratorModel.setObjectProperties(propertiesForType); break;
                case LICENSE_ADDITION: testValuesGeneratorModel.setLicenseAdditionProperties(propertiesForType); break;
                case EXTENDABLE_LICENSE: testValuesGeneratorModel.setExtendableLicenseProperties(propertiesForType); break;
                case ANY_LICENSE_INFO: testValuesGeneratorModel.setAnyLicenseInfoProperties(propertiesForType); break;
                case ENUM: testValuesGeneratorModel.setEnumerationProperties(propertiesForType); break;
                case BOOLEAN: testValuesGeneratorModel.setBooleanProperties(propertiesForType); break;
                case INTEGER: testValuesGeneratorModel.setIntegerProperties(propertiesForType); break;
                case DOUBLE: testValuesGeneratorModel.setDoubleProperties(propertiesForType); break;
                case STRING: testValuesGeneratorModel.setStringProperties(propertiesForType); break;
                case OBJECT_COLLECTION: testValuesGeneratorModel.setObjectPropertyValueCollection(propertiesForType); break;
                case STRING_COLLECTION: testValuesGeneratorModel.setStringCollection(propertiesForType); break;
                case OBJECT_SET: testValuesGeneratorModel.setObjectPropertyValueSet(propertiesForType); break;
                case ENUM_COLLECTION: testValuesGeneratorModel.setEnumPropertyValueCollection(propertiesForType); break;
                default: throw new RuntimeException("Unknown prop type: "+entry.getKey());
            }
        }

        List<UnitTestModel> classesMaps = new ArrayList<>();
        for (Map.Entry<String, UnitTestModel> entry:unitTestMaps.entrySet()) {
            classesMaps.add(entry.getValue());
            boolean isAbstract = entry.getValue().isAbstract();
            if (isAbstract) {
                requiredImports.add("import "+uriToPkg(entry.getKey()) + "." + uriToClassName.get(entry.getKey()) + ";");
            }
            requiredImports.add("import "+uriToPkg(entry.getKey()) + "." + uriToClassName.get(entry.getKey()) +
                    "." + uriToClassName.get(entry.getKey()) + "Builder;");
        }
        testValuesGeneratorModel.setClassesForBuilders(classesMaps);
        requiredImports.add("import java.util.Arrays;");
        requiredImports.add("import org.spdx.core.IModelCopyManager;");
        requiredImports.add("import org.spdx.core.InvalidSPDXAnalysisException;");
        requiredImports.add("import org.spdx.core.ModelRegistry;");
        requiredImports.add("import org.spdx.library.model."+JavaCodeGenerator.VERSION_SUFFIX+".core.CreationInfo;");
        requiredImports.add("import org.spdx.library.model."+JavaCodeGenerator.VERSION_SUFFIX+".core.RelationshipCompleteness;");
        requiredImports.add("import org.spdx.library.model."+JavaCodeGenerator.VERSION_SUFFIX+".core.RelationshipType;");
        requiredImports.add("import org.spdx.library.model."+JavaCodeGenerator.VERSION_SUFFIX+".core.Agent.AgentBuilder;");
        requiredImports.add("import org.spdx.storage.IModelStore;");
        requiredImports.add("import org.spdx.storage.IModelStore.IdType;");
        requiredImports.add("import java.util.List;");
        requiredImports.add("import java.util.Objects;");
        requiredImports.add("import java.util.Collection;");
        requiredImports.add("import java.util.Map;");
        requiredImports.add("import java.util.HashMap;");
        List<String> importList = new ArrayList<>(requiredImports);
        Collections.sort(importList);
        testValuesGeneratorModel.setImports(importList);
    }

    private String buildDataForClass(String classUri, String name, ArrayList<PropertyShape> propertyShapes,
                                     Shape classShape, String comment, String superClassUri,
                                     List<OntClass> superClasses, boolean isAbstract,
                                     Map<String, JavaClassModel> javaClassModels, Map<String, UnitTestModel>
                                             unitTestModels, Map<PropertyModel.PropertyType, Map<String, PropertyModel>>
                                             allPropertiesInUse) throws ShaclToJavaException {
        String pkgName = uriToPkg(classUri);

        Set<String> requiredImports = new HashSet<>();
        JavaClassModel javaClassModel = new JavaClassModel();
        javaClassModel.setClassUri(classUri);
        javaClassModel.setAbstract(isAbstract);
        javaClassModel.setClassName(name);
        javaClassModel.setClassProfile(uriToProfile(classUri));
        Map<PropertyModel.PropertyType, List<PropertyModel>> propertyMap = findProperties(propertyShapes, classShape,
                requiredImports, propertyUrisForConstants, classUri, superClasses);
        for (Map.Entry<PropertyModel.PropertyType, List<PropertyModel>> entry:propertyMap.entrySet()) {
            for (PropertyModel propertyModel:entry.getValue()) {
                Map<String, PropertyModel> allPropertiesForType = allPropertiesInUse.get(entry.getKey());
                if (Objects.isNull(allPropertiesForType)) {
                    allPropertiesForType = new HashMap<>();
                    allPropertiesInUse.put(entry.getKey(), allPropertiesForType);
                }
                String propertyUri = propertyModel.getUri();
                if (!allPropertiesForType.containsKey(propertyUri)) {
                    allPropertiesForType.put(propertyUri, propertyModel);
                }
            }
        }
        int numProperties = 0;
        for (List<PropertyModel> props:propertyMap.values()) {
            numProperties += props.size();
        }
        requiredImports.add("import org.spdx.library.model."+JavaCodeGenerator.VERSION_SUFFIX+".SpdxConstantsV3;");
        if (numProperties > 0) {
            requiredImports.add("import java.util.Optional;");
        }
        javaClassModel.setElementProperties(propertyMap.get(PropertyModel.PropertyType.ELEMENT));
        javaClassModel.setObjectProperties(propertyMap.get(PropertyModel.PropertyType.OBJECT));
        javaClassModel.setAnyLicenseInfoProperties(propertyMap.get(PropertyModel.PropertyType.ANY_LICENSE_INFO));
        javaClassModel.setLicenseAdditionProperties(propertyMap.get(PropertyModel.PropertyType.LICENSE_ADDITION));
        javaClassModel.setExtendableLicenseProperties(propertyMap.get(PropertyModel.PropertyType.EXTENDABLE_LICENSE));
        javaClassModel.setEnumerationProperties(propertyMap.get(PropertyModel.PropertyType.ENUM));
        javaClassModel.setBooleanProperties(propertyMap.get(PropertyModel.PropertyType.BOOLEAN));
        javaClassModel.setIntegerProperties(propertyMap.get(PropertyModel.PropertyType.INTEGER));
        javaClassModel.setDoubleProperties(propertyMap.get(PropertyModel.PropertyType.DOUBLE));
        javaClassModel.setStringProperties(propertyMap.get(PropertyModel.PropertyType.STRING));
        javaClassModel.setObjectPropertyValueCollection(propertyMap.get(PropertyModel.PropertyType.OBJECT_COLLECTION));
        javaClassModel.setStringCollection(propertyMap.get(PropertyModel.PropertyType.STRING_COLLECTION));
        javaClassModel.setObjectPropertyValueSet(propertyMap.get(PropertyModel.PropertyType.OBJECT_SET));
        javaClassModel.setEnumPropertyValueCollection(propertyMap.get(PropertyModel.PropertyType.ENUM_COLLECTION));
        javaClassModel.setSuppressUnchecked(!(propertyMap.get(PropertyModel.PropertyType.OBJECT_COLLECTION).isEmpty() &&
                propertyMap.get(PropertyModel.PropertyType.OBJECT_SET).isEmpty() &&
                propertyMap.get(PropertyModel.PropertyType.STRING_COLLECTION).isEmpty()));
        javaClassModel.setYear(YEAR);
        javaClassModel.setPkgName(pkgName);
        javaClassModel.setClassComments(toClassComment(comment));
        String superClass = getSuperClass(superClassUri, requiredImports, classUri);
        javaClassModel.setSuperClass(superClass);
        javaClassModel.setVerifySuperclass(!superClass.equals("ModelObjectV3"));
        if (!this.uriToNamespaceUri(classUri).endsWith("Core")) {
            requiredImports.add("import org.spdx.library.model."+JavaCodeGenerator.VERSION_SUFFIX+".core.ProfileIdentifierType;");
        }
        boolean hasCreationInfo = false;
        for (PropertyModel property:propertyMap.get(PropertyModel.PropertyType.OBJECT)) {
            if (property.getPropertyName().equals("creationInfo")) {
                hasCreationInfo = true;
                break;
            }
        }
        javaClassModel.setHasCreationInfo(hasCreationInfo);
        if (hasCreationInfo && !"Element".equals(name)) {
            requiredImports.add("import org.spdx.library.model."+JavaCodeGenerator.VERSION_SUFFIX+".core.Element;");
            requiredImports.add("import org.spdx.library.model."+JavaCodeGenerator.VERSION_SUFFIX+".core.CreationInfo;");
        }
        String equalsHashOverride;
        try {
            equalsHashOverride = getEqualsHashOverride(classUri, requiredImports);
        } catch(IOException ex) {
            throw new ShaclToJavaException("I/O error converting Mustache template for hashOverride", ex);
        }
        List<String> imports = buildImports(new ArrayList<>(requiredImports));
        javaClassModel.setImports(imports.toArray(new String[0]));
        //TODO: Implement
        javaClassModel.setCompareUsingProperties(false); // use properties to implement compareTo
        javaClassModel.setCompareProperties(new ArrayList<>()); // List of property models to use in compare
        String toStringString;
        try {
            toStringString = generateToString(classUri, superClasses);
        } catch (IOException e) {
            throw new ShaclToJavaException("I/O error converting Mustache template for toString", e);
        }
        if (Objects.nonNull(toStringString)) {
            javaClassModel.setToString(toStringString); // use properties to implement toString
        }
        if (Objects.nonNull(equalsHashOverride)) {
            javaClassModel.setEqualsHashOverride(equalsHashOverride);
        }
        javaClassModels.put(classUri, javaClassModel);

        // make a copy of the java class model
        UnitTestModel unitTestModel = new UnitTestModel();
        try {
            BeanUtils.copyProperties(unitTestModel, javaClassModel);
        } catch (IllegalAccessException e) {
            throw new ShaclToJavaException("Error accessing java class model when copying properties", e);
        } catch (InvocationTargetException e) {
            throw new ShaclToJavaException("Error copying java class properties", e);
        }
        requiredImports.add(String.format("import %s.%s.%sBuilder;", pkgName, name, name));
        requiredImports.add("import junit.framework.TestCase;");
        requiredImports.add("import org.spdx.library.model."+JavaCodeGenerator.VERSION_SUFFIX+".MockCopyManager;");
        requiredImports.add("import org.spdx.library.model."+JavaCodeGenerator.VERSION_SUFFIX+".MockModelStore;");
        requiredImports.add("import org.spdx.library.model."+JavaCodeGenerator.VERSION_SUFFIX+".UnitTestHelper;");
        requiredImports.add("import org.spdx.library.model."+JavaCodeGenerator.VERSION_SUFFIX+".core.Agent.AgentBuilder;");
        requiredImports.add("import java.util.Arrays;");
        requiredImports.add("import org.spdx.core.ModelRegistry;");
        requiredImports.add("import org.spdx.library.model."+JavaCodeGenerator.VERSION_SUFFIX+".TestValuesGenerator;");
        imports = buildImports(new ArrayList<>(requiredImports));
        unitTestModel.setImports(imports.toArray(new String[0]));
        unitTestModels.put(classUri, unitTestModel);
        try {
            return mustacheToString(ShaclToJavaConstants.CREATE_CLASS_TEMPLATE, javaClassModel);
        } catch (IOException e) {
            throw new ShaclToJavaException("IO error converting to the create class string", e);
        }
    }

    /**
     * @param classUri URI for the class
     * @param superClasses list of superclasses
     * @return toString override method
     * @throws IOException on error converting using Mustache
     */
    private String generateToString(String classUri,
                                    List<OntClass> superClasses) throws IOException {
        if (classUri.endsWith("ExpandedLicensing/WithAdditionOperator")) {
            Map<String, Object> mustacheMap = new HashMap<>();
            mustacheMap.put("className", uriToClassName.get(classUri));
            String subjectAdditionPropertyName = uriToPropertyName.get(spdxNamespace + "ExpandedLicensing/subjectAddition");
            String subjectLicenseGetter = "get" + subjectAdditionPropertyName.substring(0, 1).toUpperCase() + subjectAdditionPropertyName.substring(1);
            mustacheMap.put("subjectAdditionGetter", subjectLicenseGetter);
            String subjectExtendablePropertyName = uriToPropertyName.get(spdxNamespace + "ExpandedLicensing/subjectExtendableLicense");
            String extendableLicenseGetter = "get" + subjectExtendablePropertyName.substring(0, 1).toUpperCase() + subjectExtendablePropertyName.substring(1);
            mustacheMap.put("extendableLicenseGetter", extendableLicenseGetter);
            return mustacheToString(ShaclToJavaConstants.WITH_OPERATOR_TO_STRING_TEMPLATE, mustacheMap);
        } else if (classUri.endsWith("ExpandedLicensing/OrLaterOperator")) {
            Map<String, Object> mustacheMap = new HashMap<>();
            mustacheMap.put("className", uriToClassName.get(classUri));
            String subjectPropertyName = uriToPropertyName.get(spdxNamespace + "ExpandedLicensing/subjectLicense");
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
            String licenseMemberPropName = uriToPropertyName.get(spdxNamespace + "ExpandedLicensing/member") + "s";
            String licenseMemberGetter = "get" + licenseMemberPropName.substring(0, 1).toUpperCase() + licenseMemberPropName.substring(1);
            mustacheMap.put("licenseMembersGetter", licenseMemberGetter);
            mustacheMap.put("operator",
                    classUri.endsWith("ExpandedLicensing/DisjunctiveLicenseSet") ? "OR" : "AND");
            return mustacheToString(ShaclToJavaConstants.LICENSE_SET_TO_STRING_TEMPLATE, mustacheMap);
        }  else if (classUri.endsWith("Core/Element")) {
            Map<String, Object> mustacheMap = new HashMap<>();
            String nameProp = uriToPropertyName.get(spdxNamespace + "Core/name");
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
     * @param requiredImports list of required imports
     * @return code for equals and hashcode if the Java class should override
     * null if it should not be overridden
     */
    private @Nullable String getEqualsHashOverride(String classUri, Set<String> requiredImports) throws IOException {
        // License classes need to override equals so that the license sets work properly
        // NOTE: This needs to be checked first since licenses are subclasses of elements
        if (classUri.endsWith("ExpandedLicensing/ConjunctiveLicenseSet")) {
            Map<String, Object> mustacheMap = new HashMap<>();
            mustacheMap.put("className", uriToClassName.get(classUri));
            mustacheMap.put("primeNumber", "1381");
            requiredImports.add("import java.util.HashSet;");
            requiredImports.add("import java.util.Iterator;");
            String licenseMemberPropName = uriToPropertyName.get(spdxNamespace + "ExpandedLicensing/member") + "s";
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
            String licenseMemberPropName = uriToPropertyName.get(spdxNamespace + "ExpandedLicensing/member") + "s";
            String licenseMemberGetter = "get" + licenseMemberPropName.substring(0, 1).toUpperCase() + licenseMemberPropName.substring(1);
            mustacheMap.put("licenseMembersGetter", licenseMemberGetter);
            return mustacheToString(ShaclToJavaConstants.LICENSE_SET_EQUALS_OVERRIDE_TEMPLATE, mustacheMap);
        }
        if (classUri.endsWith("ExpandedLicensing/OrLaterOperator")) {
            Map<String, Object> mustacheMap = new HashMap<>();
            mustacheMap.put("className", uriToClassName.get(classUri));
            String subjectPropertyName = uriToPropertyName.get(spdxNamespace + "ExpandedLicensing/subjectLicense");
            String subjectLicenseGetter = "get" + subjectPropertyName.substring(0, 1).toUpperCase() + subjectPropertyName.substring(1);
            mustacheMap.put("subjectLicenseGetter", subjectLicenseGetter);
            return mustacheToString(ShaclToJavaConstants.OR_LATER_EQUALS_OVERRIDE_TEMPLATE, mustacheMap);
        }
        if (classUri.endsWith("ExpandedLicensing/WithAdditionOperator")) {
            Map<String, Object> mustacheMap = new HashMap<>();
            mustacheMap.put("className", uriToClassName.get(classUri));
            String subjectAdditionPropertyName = uriToPropertyName.get(spdxNamespace + "ExpandedLicensing/subjectAddition");
            String subjectLicenseGetter = "get" + subjectAdditionPropertyName.substring(0, 1).toUpperCase() + subjectAdditionPropertyName.substring(1);
            mustacheMap.put("subjectAdditionGetter", subjectLicenseGetter);
            String subjectExtendablePropertyName = uriToPropertyName.get(spdxNamespace + "ExpandedLicensing/subjectExtendableLicense");
            String extendableLicenseGetter = "get" + subjectExtendablePropertyName.substring(0, 1).toUpperCase() + subjectExtendablePropertyName.substring(1);
            mustacheMap.put("extendableLicenseGetter", extendableLicenseGetter);
            return mustacheToString(ShaclToJavaConstants.WITH_EQUALS_OVERRIDE_TEMPLATE, mustacheMap);
        }
        return null;
    }

    /**
     * Converts a map and Mustache template to a string
     * @param templateName Name of the template located in the resources directory
     * @param mustacheMap Either a map or an object for the Mustache string replacements
     * @return String result of applying the mustacheMap to the template
     * @throws IOException on I/O error in the stringWriter
     */
    private String mustacheToString(String templateName, Object mustacheMap) throws IOException {
        String templateDirName = ShaclToJavaConstants.TEMPLATE_ROOT_PATH;
        File templateDirectoryRoot = new File(templateDirName);
        if (!(templateDirectoryRoot.exists() && templateDirectoryRoot.isDirectory())) {
            templateDirName = ShaclToJavaConstants.TEMPLATE_CLASS_PATH;
        }
        DefaultMustacheFactory builder = new DefaultMustacheFactory(templateDirName);
        Mustache mustache = builder.compile(templateName);
        try (StringWriter writer = new StringWriter()) {
            mustache.execute(writer, mustacheMap);
            return writer.toString();
        }
    }

    /**
     * @param classUri URI for the enum
     * @param name local name for the enum
     * @param allIndividuals individual values from the model
     * @param comment Description of the enum
     * @return model object for the java enum properties
     */
    private EnumModel buildJavaEnum(String classUri, String name, List<Individual> allIndividuals, String comment) {
        EnumModel model = new EnumModel();
        model.setYear(YEAR);
        model.setPkgName(uriToPkg(classUri));
        model.setClassComment(toClassComment(comment));
        model.setName(name);
        model.setClassUri(classUri);
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
        model.setEnumValues(enumValues);
        return model;
    }

    /**
     * @param individualUri URI for the individual
     * @param name local name for the individual
     * @param propertyShapes properties for the individual inherited from superclasses
     * @param comment Description of the individual
     * @param superClassUri URI of the superclass
     * @param superClassShape shape for the supper class
     * @param superClasses all superclasses for the class
     * @throws ShaclToJavaException on inconsistent or unimplemented SHACL fields
     */
    private IndividualClassModel buildIndividualClass(String individualUri, String name,
                                                      List<PropertyShape> propertyShapes, String comment,
                                                      String superClassUri, Shape superClassShape, List<OntClass> superClasses) throws IOException, ShaclToJavaException {
        String pkgName = uriToPkg(individualUri);
        Set<String> requiredImports = new HashSet<>();
        IndividualClassModel model = new IndividualClassModel();
        model.setIndividualUri(individualUri);
        model.setClassName(name);
        model.setClassProfile(uriToProfile(individualUri));
        Map<PropertyModel.PropertyType, List<PropertyModel>> propertyMap = findProperties(propertyShapes, superClassShape,
                requiredImports, propertyUrisForConstants, superClassUri, superClasses);
        int numProperties = 0;
        for (List<PropertyModel> props:propertyMap.values()) {
            numProperties += props.size();
        }
        if (numProperties > 0) {
            requiredImports.add("import org.spdx.library.model."+JavaCodeGenerator.VERSION_SUFFIX+".SpdxConstantsV3;");
            requiredImports.add("import java.util.Optional;");
        }
        model.setElementProperties(propertyMap.get(PropertyModel.PropertyType.ELEMENT));
        model.setObjectProperties(propertyMap.get(PropertyModel.PropertyType.OBJECT));
        model.setAnyLicenseInfoProperties(propertyMap.get(PropertyModel.PropertyType.ANY_LICENSE_INFO));
        model.setLicenseAdditionProperties(propertyMap.get(PropertyModel.PropertyType.LICENSE_ADDITION));
        model.setExtendableLicenseProperties(propertyMap.get(PropertyModel.PropertyType.EXTENDABLE_LICENSE));
        model.setEnumerationProperties(propertyMap.get(PropertyModel.PropertyType.ENUM));
        model.setBooleanProperties(propertyMap.get(PropertyModel.PropertyType.BOOLEAN));
        model.setIntegerProperties(propertyMap.get(PropertyModel.PropertyType.INTEGER));
        model.setDoubleProperties(propertyMap.get(PropertyModel.PropertyType.DOUBLE));
        model.setStringProperties(propertyMap.get(PropertyModel.PropertyType.STRING));
        model.setObjectPropertyValueCollection(propertyMap.get(PropertyModel.PropertyType.OBJECT_COLLECTION));
        model.setStringCollection(propertyMap.get(PropertyModel.PropertyType.STRING_COLLECTION));
        model.setObjectPropertyValueSet(propertyMap.get(PropertyModel.PropertyType.OBJECT_SET));
        model.setEnumPropertyValueCollection(propertyMap.get(PropertyModel.PropertyType.ENUM_COLLECTION));
        model.setSuppressUnchecked(!(propertyMap.get(PropertyModel.PropertyType.OBJECT_COLLECTION).isEmpty() &&
                propertyMap.get(PropertyModel.PropertyType.OBJECT_SET).isEmpty() &&
                propertyMap.get(PropertyModel.PropertyType.STRING_COLLECTION).isEmpty()));
        model.setYear(YEAR);
        model.setPkgName(pkgName);

        model.setClassComments(toClassComment(comment));
        String superClass = getSuperClass(superClassUri, requiredImports, individualUri);
        model.setSuperClass(superClass);
        requiredImports.add("import org.spdx.storage.NullModelStore;");
        List<String> imports = buildImports(new ArrayList<>(requiredImports));
        model.setImports(imports.toArray(new String[0]));
        model.setToStringName(name.startsWith("NoAssertion") ? "NOASSERTION" :
                name.startsWith("None") ? "NONE" : name);
        return model;
    }

    /**
     * @param comment from model documentation
     * @return text formatted for a class comment
     */
    private String toClassComment(String comment) {
        StringBuilder sb = new StringBuilder("/**\n");
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
     * @param superClassUri URI for the superclass
     * @param requiredImports set of required imports - updated if the superClass adds a new import statement
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
     * @param propertyShapes direct ontology properties
     * @param classShape Shape for the class containing the properties
     * @param requiredImports set of required imports for this class - updated with any additional imports
     * @param propertyUrisForConstants set of URI's for any properties - updated with any additional values
     * @param classUri URI for the class containing the properties
     * @param superClasses all superclasses for the class
     * @return map of PropertyModels to PropertyTypes for any properties returning a type of Element
     * @throws ShaclToJavaException on any error processing the SHACL information
     */
    private Map<PropertyModel.PropertyType, List<PropertyModel>> findProperties(List<PropertyShape> propertyShapes, Shape classShape,
                                                                                Set<String> requiredImports, Set<String> propertyUrisForConstants,
                                                                                String classUri, List<OntClass> superClasses) throws ShaclToJavaException {
        Map<PropertyModel.PropertyType, List<PropertyModel>> retval = new HashMap<>();
        for (PropertyModel.PropertyType value: PropertyModel.PropertyType.values()) {
            retval.put(value, new ArrayList<>());
        }
        for (PropertyShape propertyShape:propertyShapes) {
            if (!Objects.isNull(propertyShape) && propertyShape.getPath().toString().contains("/terms")) {
                PropertyModel propertyModel = propertyPropertyModel(propertyShape, requiredImports,
                        propertyUrisForConstants, classUri, superClasses);
                retval.get(propertyModel.getPropertyType()).add(propertyModel);
            }
        }
        return retval;
    }

    /**
     * Maps a property to PropertyModel adding any required import strings and adding any required constant strings
     * @param requiredImports set of required imports for this class - updated with any additional imports
     * @param propertyUrisForConstants set of URI's for any properties - updated with any additional values
     * @param classUri URI of the class using the property
     * @param superClasses all superclasses for the class
     * @return PropertyModel containing values for a give ontology property
     * @throws ShaclToJavaException on any error processing the SHACL information
     */
    private PropertyModel propertyPropertyModel(PropertyShape propertyShape,
                                                Set<String> requiredImports, Set<String> propertyUrisForConstants, String classUri, List<OntClass> superClasses) throws ShaclToJavaException {
        PropertyModel retval = new PropertyModel();
        String nameSpace = uriToNamespaceUri(classUri);
        String propertyUri = propertyShape.getPath().toString().replaceAll("<", "").replaceAll(">", "");

        String name = uriToPropertyName.get(propertyUri);
        if (Objects.isNull(name)) {
            // This is a special case if a property is not defined in SHACL as either
            // an ObjectProperty or an DataProperty - e.g. core:extension
            name = uriToName(propertyUri);
            uriToPropertyName.put(propertyUri, name);
        }
        retval.setPropertyName(name);
        retval.setPropertyNameUpper(camelCaseToConstCase(name));
        String getSetName = name.substring(0, 1).toUpperCase() + name.substring(1);
        retval.setGetter("get" + getSetName);
        retval.setSetter("set" + getSetName);
        retval.setAdder("add" + getSetName);
        retval.setAddAller("addAll" + getSetName);
        retval.setCreationInfo("creationInfo".equals(name));

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
        PropertyModel.PropertyType propertyType;
        if (ShaclToJavaConstants.SET_PROPERTY_SUFFIXES.contains(propertySuffix)) {
            propertyType = PropertyModel.PropertyType.OBJECT_SET;
        } else if ("extension".equals(name)) {
            propertyType = PropertyModel.PropertyType.OBJECT_COLLECTION; // workaround for https://github.com/spdx/spec-parser/issues/207
        } else {
            propertyType = determinePropertyType(classRestriction, dataTypeRestriction,
                    minCardinality, maxCardinality);
        }
        if (PropertyModel.PropertyType.OBJECT_COLLECTION.equals(propertyType) || PropertyModel.PropertyType.STRING_COLLECTION.equals(propertyType) ||
                PropertyModel.PropertyType.ENUM_COLLECTION.equals(propertyType)) {
            requiredImports.add("import java.util.Collection;");
            requiredImports.add("import java.util.Collections;");
            requiredImports.add("import java.util.Objects;");
        }
        retval.setPropertyType(propertyType);
        String typeUri = "extension".equals(name) ?
                spdxNamespace + "Extension/Extension" : // workaround for https://github.com/spdx/spec-parser/issues/207
                getTypeUri(classRestriction, dataTypeRestriction);
        String type;
        if (ShaclToJavaConstants.BOOLEAN_TYPE.equals(typeUri)) {
            type = "Boolean";
        } else if (ShaclToJavaConstants.STRING_TYPE.equals(typeUri) || ShaclToJavaConstants.DATE_TIME_TYPE.equals(typeUri)) {
            type = "String";
        } else if (ShaclToJavaConstants.INTEGER_TYPES.contains(typeUri)) {
            type = "Integer";
        } else if (ShaclToJavaConstants.DOUBLE_TYPES.contains(typeUri)) {
            type = "Double";
        } else {
            type = uriToClassName.get(typeUri);
            if (!typeUri.startsWith(nameSpace) &&
                    (PropertyModel.PropertyType.ENUM.equals(propertyType) || PropertyModel.PropertyType.OBJECT.equals(propertyType) ||
                            PropertyModel.PropertyType.OBJECT_COLLECTION.equals(propertyType) ||
                            PropertyModel.PropertyType.ENUM_COLLECTION.equals(propertyType) ||
                            PropertyModel.PropertyType.OBJECT_SET.equals(propertyType) ||
                            PropertyModel.PropertyType.ANY_LICENSE_INFO.equals(propertyType) ||
                            PropertyModel.PropertyType.LICENSE_ADDITION.equals(propertyType) ||
                            PropertyModel.PropertyType.EXTENDABLE_LICENSE.equals(propertyType) ||
                            PropertyModel.PropertyType.ELEMENT.equals(propertyType))) {
                requiredImports.add("import "+uriToPkg(typeUri) + "." + uriToClassName.get(typeUri) +";");
            }
        }
        retval.setTypeUri(typeUri);
        retval.setType(type);
        boolean required = minCardinality != null && minCardinality > 0;
        if (required) {
            requiredImports.add("import java.util.Collections;");
            requiredImports.add("import java.util.Arrays;");
            requiredImports.add("import java.util.Objects;");
        }
        retval.setRequired(required);

        String profileIdentifierType = namespaceToProfileIdentifierType(nameSpace);
        retval.setRequiredProfiles(profileIdentifierType);
        String classNamespace = uriToNamespaceUri(classUri);
        boolean inSuperClass = inSuperClass(superClasses, propertyShape);
        retval.setSuperSetter(inSuperClass);
        SuperclassRequired superRequired = inSuperClass ? determineSuperRequired(superClasses, propertyShape) :
                SuperclassRequired.NONE;
        boolean nonOptional = required && nameSpace.equals(classNamespace) &&
                (SuperclassRequired.YES.equals(superRequired) || SuperclassRequired.NONE.equals(superRequired)); // we can't override an optional
        retval.setNonOptional(nonOptional);
        boolean hasConstraint = required;
        if (Objects.nonNull(pattern)) {
            retval.setPattern(pattern);
            hasConstraint = true;
        }
        if (Objects.nonNull(minStringLength)) {
            retval.setMin(minStringLength.toString());
            hasConstraint = true;
        } else if (ShaclToJavaConstants.XSD_NON_NEGATIVE_INTEGER.equals(typeUri)) {
            retval.setMin("0");
            hasConstraint = true;
        } else if (ShaclToJavaConstants.XSD_POSITIVE_INTEGER.equals(typeUri)) {
            retval.setMin("1");
            hasConstraint = true;
        }
        if (Objects.nonNull(maxStringLength)) {
            retval.setMax(maxStringLength.toString());
            hasConstraint = true;
        }
        if (Objects.nonNull(pattern)) {
            requiredImports.add("import java.util.regex.Pattern;");
            retval.setPattern(StringEscapeUtils.escapeJava(pattern));
        }
        retval.setHasConstraint(hasConstraint);
        retval.setUri(propertyUri);
        String propNameSpace = uriToNamespaceUri(propertyUri);
        propNameSpace = propNameSpace.substring(propNameSpace.lastIndexOf('/')+1);
        String propConstant = propertyNameToPropertyConstant(name, propNameSpace);
        retval.setPropertyConstant(propConstant);
        propertyUrisForConstants.add(propertyUri);
        if ("specVersion".equals(name)) {
            retval.setIsSpecVersion(true); // special case that the spec version is set in the CoreModelObject in addition to the store
        }
        return retval;
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
     * @param classTypeRestriction class restriction if any
     * @param dataTypeRestriction data restriction if any
     * @param minRestriction minimum cardinality restriction if any
     * @param maxRestriction maximum cardinality restriction if any
     * @return the property type based on the range and restrictions
     * @throws ShaclToJavaException on any errors processing the SHACL data
     */
    private PropertyModel.PropertyType determinePropertyType(@Nullable Node classTypeRestriction,
                                                           @Nullable Node dataTypeRestriction, @Nullable Integer minRestriction, @Nullable Integer maxRestriction) throws ShaclToJavaException {
        String typeUri = getTypeUri(classTypeRestriction, dataTypeRestriction);
        if (enumerationTypes.contains(typeUri)) {
            if (Objects.isNull(maxRestriction) || maxRestriction > 1) {
                return PropertyModel.PropertyType.ENUM_COLLECTION;
            } else {
                return PropertyModel.PropertyType.ENUM;
            }
        } else if (ShaclToJavaConstants.BOOLEAN_TYPE.equals(typeUri)) {
            return PropertyModel.PropertyType.BOOLEAN;
        } else if  (ShaclToJavaConstants.INTEGER_TYPES.contains(typeUri)) {
            return PropertyModel.PropertyType.INTEGER;
        } else if (ShaclToJavaConstants.DOUBLE_TYPES.contains(typeUri)) {
            return PropertyModel.PropertyType.DOUBLE;
        }else if  (ShaclToJavaConstants.STRING_TYPE.equals(typeUri) || ShaclToJavaConstants.DATE_TIME_TYPE.equals(typeUri) ||
                ShaclToJavaConstants.ANY_URI_TYPE.equals(typeUri) || stringTypes.contains(typeUri)) {
            if (Objects.isNull(maxRestriction) || maxRestriction > 1) {
                return PropertyModel.PropertyType.STRING_COLLECTION;
            } else {
                return PropertyModel.PropertyType.STRING;
            }
            // If we get here, we're dealing with objects
        } else if (ShaclToJavaConstants.SET_TYPE_SUFFIXES.contains(typeUri.substring(typeUri.lastIndexOf("/terms/")))) {
            return PropertyModel.PropertyType.OBJECT_SET;
        } else if (Objects.isNull(maxRestriction) || maxRestriction > 1) {
            return PropertyModel.PropertyType.OBJECT_COLLECTION;
        } else if (licenseAdditionTypes.contains(typeUri)) {
            return PropertyModel.PropertyType.LICENSE_ADDITION;
        } else if (extendableLicenseTypes.contains(typeUri)) {
            return PropertyModel.PropertyType.EXTENDABLE_LICENSE;
        } else if (anyLicenseInfoTypes.contains(typeUri)) {
            return PropertyModel.PropertyType.ANY_LICENSE_INFO;
//		} else if (elementTypes.contains(typeUri)) {
//			return PropertyType.ELEMENT;
        } else {
            return PropertyModel.PropertyType.OBJECT;
        }
    }

    /**
     * @param classTypeRestriction class restriction if any
     * @param dataTypeRestriction data restriction if any
     * @return The URI for the type of property based on its range and restrictions
     * @throws ShaclToJavaException on any errors processing the SHACL data
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
        retval.add("import org.spdx.library.model."+JavaCodeGenerator.VERSION_SUFFIX+".ModelObjectV3;");
        retval.add("import org.spdx.storage.IModelStore;");
        retval.add("import org.spdx.storage.IModelStore.IdType;");
        retval.add("import org.spdx.storage.IModelStore.IModelStoreLock;");
        retval.add("");
        Collections.sort(localImports);
        retval.addAll(localImports);
        return retval;
    }

    @Override
    public int compareTo(SpecVersionContainer o) {
        return this.getSpecVersion().compareTo(o.getSpecVersion());
    }

    public String getSpecVersion() {
        return this.versionSemVer;
    }

    public List<IndividualClassModel> getIndividuals() {
        return individuals;
    }

    public List<EnumModel> getEnums() {
        return this.enums;
    }

    public List<String> getWarnings() {
        return this.warnings;
    }

    public List<JavaClassModel> getJavaClasses() {
        return this.javaClasses;
    }

    public List<JavaClassModel> getExternalJavaClasses() {
        return this.externalJavaClasses;
    }

    public List<UnitTestModel> getUnitTestClasses() {
        return this.unitTestClasses;
    }

    public ConstantsModel getConstantsModel() {
        return constantsModel;
    }

    public EnumFactoryModel getEnumFactoryModel() {
        return this.enumFactoryModel;
    }

    public ModelClassFactoryModel getModelClassFactoryModel() {
        return this.modelClassFactoryModel;
    }

    public ModelObjectModel getModelObjectModel() {
        return this.modelObjectModel;
    }

    public SpdxModelInfoModel getSpdxModelInfoModel() {
        return this.spdxModelInfoModel;
    }

    public PackageInfoModel getPackageInfoModel() {
        return this.packageInfoModel;
    }

    public IndividualFactoryModel getIndividualFactoryModel() {
        return this.individualFactoryModel;
    }

    public MockFileModel getMockFileModel() {
        return this.mockFileModel;
    }

    public InvalidLicenseExpressionModel getInvalidLicenseExpressionModel() {
        return this.invalidLicenseExpressionModel;
    }

    public TestValuesGeneratorModel getTestValuesGeneratorModel() {
        return this.testValuesGeneratorModel;
    }
}
