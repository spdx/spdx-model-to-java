package org.spdx.tools.model2java;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import org.apache.commons.beanutils.BeanUtils;
import org.spdx.tools.model2java.model.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates Java code based on a collection of <code>SpecVersionContainer</code>s.
 */
public class JavaCodeGenerator {

    public static final String VERSION_SUFFIX = "v3";

    private final List<SpecVersionContainer> specVersions;
    private final List<String> warnings = new ArrayList<>();

    /**
     * Class to generate Java code from a list of SPDX specifications
     * @param specVersions SpecVersionContainer which contains the model files for a given version of an SPDX Spec
     */
    public JavaCodeGenerator(List<SpecVersionContainer> specVersions) {
        Objects.requireNonNull(specVersions);
        if (specVersions.isEmpty()) {
            throw new RuntimeException("No spec versions provided");
        }
        this.specVersions = specVersions;
        Collections.sort(this.specVersions);
        specVersions.forEach(sv -> warnings.addAll(sv.getWarnings()));
    }

    /**
     * @param classModel Model file containing class information
     * @return the class name qualified with the package name
     */
    private String toQualifiedName(BaseClassModel classModel) {
        Objects.requireNonNull(classModel.getPkgName());
        Objects.requireNonNull(classModel.getClassName());
        return classModel.getPkgName() + "." + classModel.getClassName();
    }

    /**
     * @param enumModel Model file containing enumeration information
     * @return the class name qualified with the package name
     */
    private String toQualifiedName(EnumModel enumModel) {
        Objects.requireNonNull(enumModel.getPkgName());
        Objects.requireNonNull(enumModel.getName());
        return enumModel.getPkgName() + "." + enumModel.getName();
    }


    /**
     * Merges individuals across different versions
     * @return all individuals from all spec versions
     */
    private List<IndividualClassModel> mergeIndividuals() {
        // Start with the latest version
        List<IndividualClassModel> retval = new ArrayList<>(this.specVersions.get(this.specVersions.size()-1).getIndividuals());
        Set<String> addedIndividuals = new HashSet<>();
        for (IndividualClassModel individual : retval) {
            addedIndividuals.add(toQualifiedName(individual));
        }
        for (int i = this.specVersions.size()-2; i >= 0; i--) {
            List<IndividualClassModel> individualsToMerge = this.specVersions.get(i).getIndividuals();
            for (IndividualClassModel individual : individualsToMerge) {
                String qualifiedClassName = toQualifiedName(individual);
                if (!addedIndividuals.contains(qualifiedClassName)) {
                    addedIndividuals.add(qualifiedClassName);
                    retval.add(individual);
                    warnings.add(String.format("Latest spec version does not contain the individual %s from spec version %s",
                            qualifiedClassName, this.specVersions.get(i).getSpecVersion()));
                }
            }
        }
        return retval;
    }

    /**
     * Merge all enums across versions filling in the <code>enumClassToVersionMissingValue</code>
     * @param enumClassToVersionMissingValues List of any missing values for an enum class by spec version
     * @return all enums merged
     */
    private List<EnumModel> mergeEnums(Map<String, Map<String, List<String>>> enumClassToVersionMissingValues) {
        List<EnumModel> retval = new ArrayList<>(this.specVersions.get(this.specVersions.size()-1).getEnums());
        Map<String, List<String>> qualifiedNameToValues = new HashMap<>();
        for (EnumModel enumModel:retval) {
            qualifiedNameToValues.put(toQualifiedName(enumModel), enumModel.getEnumValues());
        }
        for (int i = this.specVersions.size()-2; i >= 0; i--) {
            List<EnumModel> enumsToMerge = this.specVersions.get(i).getEnums();
            for (EnumModel enumModelToMerge:enumsToMerge) {
                String qualifiedEnumName = toQualifiedName(enumModelToMerge);
                if (!qualifiedNameToValues.containsKey(qualifiedEnumName)) {
                    retval.add(enumModelToMerge);
                    qualifiedNameToValues.put(qualifiedEnumName, enumModelToMerge.getEnumValues());
                    warnings.add(String.format("Latest spec version does not contain the enum %s from spec version %s",
                            qualifiedEnumName, this.specVersions.get(i).getSpecVersion()));
                } else {
                    // Check for any missing values - need to remove the last character since it can be a comma or a semicolon
                    List<String> versionValues = enumModelToMerge.getEnumValues().stream()
                            .map(s -> s.substring(0, s.length() - 1))
                            .collect(Collectors.toList());
                    List<String> addedValues = qualifiedNameToValues.get(qualifiedEnumName).stream()
                            .map(s -> s.substring(0, s.length() - 1))
                            .collect(Collectors.toList());
                    addedValues.removeAll(versionValues);
                    if (!addedValues.isEmpty()) {
                        enumClassToVersionMissingValues.putIfAbsent(qualifiedEnumName, new HashMap<>());
                        enumClassToVersionMissingValues.get(qualifiedEnumName).put(this.specVersions.get(i).getSpecVersion(), addedValues);
                    }
                    // TODO: We could check if there are any items removed and add a warning
                }
            }
        }
        return retval;
    }

    /**
     * Merge all java classes across versions
     * @param enumClassToVersionMissingValues List of any missing values for an enum class by spec version
     * @return all java classes merged
     */
    List<JavaClassModel> mergeJavaClassModels(Map<String, Map<String, List<String>>> enumClassToVersionMissingValues) {
        List<JavaClassModel> retval = new ArrayList<>(this.specVersions.get(this.specVersions.size()-1).getJavaClasses());
        Map<String, Map<String, List<String>>> javaClassToVersionMissingProperties = new HashMap<>();
        Map<String, List<String>> qualifiedNameToProperties = new HashMap<>();
        for (JavaClassModel javaClassModel:retval) {
            qualifiedNameToProperties.put(toQualifiedName(javaClassModel), collectAllPropertyNames(javaClassModel));
        }
        for (int i = this.specVersions.size()-2; i >= 0; i--) {
            List<JavaClassModel> javaClassesToMerge = this.specVersions.get(i).getJavaClasses();
            for (JavaClassModel javaClassModel:javaClassesToMerge) {
                String qualifiedClassName = toQualifiedName(javaClassModel);
                if (!qualifiedNameToProperties.containsKey(qualifiedClassName)) {
                    retval.add(javaClassModel);
                    qualifiedNameToProperties.put(qualifiedClassName, collectAllPropertyNames(javaClassModel));
                    warnings.add(String.format("Latest spec version does not contain the Java class %s from spec version %s",
                            qualifiedClassName, this.specVersions.get(i).getSpecVersion()));
                } else {
                    List<String> versionProperties = collectAllPropertyNames(javaClassModel);
                    List<String> addedProperties = new ArrayList<>(qualifiedNameToProperties.get(qualifiedClassName));
                    addedProperties.removeAll(versionProperties);
                    if (!addedProperties.isEmpty()) {
                        javaClassToVersionMissingProperties.putIfAbsent(qualifiedClassName, new HashMap<>());
                        javaClassToVersionMissingProperties.get(qualifiedClassName).put(this.specVersions.get(i).getSpecVersion(), addedProperties);
                    }
                }
            }
        }
        // TODO: Add the missing enums and properties to the JavaClassClass
        return retval;
    }

    /**
     * Merges external java classes across different versions
     * @return all external java classes from all spec versions
     */
    List<JavaClassModel> mergeExternalJavaClassModels() {
        List<JavaClassModel> retval = new ArrayList<>(this.specVersions.get(this.specVersions.size()-1).getExternalJavaClasses());
        List<String> externalClassNames = retval.stream().map(this::toQualifiedName).collect(Collectors.toList());
        for (int i = this.specVersions.size()-2; i >= 0; i--) {
            List<JavaClassModel> externalJavaClassesToMerge = this.specVersions.get(i).getExternalJavaClasses();
            for (JavaClassModel externalJavaClassModel:externalJavaClassesToMerge) {
                String qualifiedClassName = toQualifiedName(externalJavaClassModel);
                if (!externalClassNames.contains(qualifiedClassName)) {
                    retval.add(externalJavaClassModel);
                    warnings.add(String.format("Latest spec version does not contain the External Java class %s from spec version %s",
                            qualifiedClassName, this.specVersions.get(i).getSpecVersion()));
                }
            }
        }
        return retval;
    }

    /**
     * Merges unit tests across different versions
     * @return all unit tests from all spec versions
     */
    private List<UnitTestModel> mergeUnitTestModels() {
        List<UnitTestModel> retval = this.specVersions.get(this.specVersions.size()-1).getUnitTestClasses();
        List<String> unitTestNames = retval.stream().map(this::toQualifiedName).collect(Collectors.toList());
        for (int i = this.specVersions.size()-2; i >= 0; i--) {
            List<UnitTestModel> unitTestClassesToMerge = this.specVersions.get(i).getUnitTestClasses();
            for (UnitTestModel unitTestModel:unitTestClassesToMerge) {
                String qualifiedClassName = toQualifiedName(unitTestModel);
                if (!unitTestNames.contains(qualifiedClassName)) {
                    retval.add(unitTestModel);
                    warnings.add(String.format("Latest spec version does not contain the Unit Test %s from spec version %s",
                            qualifiedClassName, this.specVersions.get(i).getSpecVersion()));
                }
            }
        }
        return retval;
    }

    /**
     * Merges model class factories across different versions
     * @return model class factories from all spec versions
     * @throws InvocationTargetException on errors copying property values between model objects
     * @throws IllegalAccessException on errors copying property values between model objects
     */
    private ModelClassFactoryModel mergeModelClassFactories() throws InvocationTargetException, IllegalAccessException {
        ModelClassFactoryModel retval = new ModelClassFactoryModel();
        BeanUtils.copyProperties(retval, this.specVersions.get(this.specVersions.size()-1).getModelClassFactoryModel());
        for (int i = this.specVersions.size()-2; i >= 0; i--) {
            ModelClassFactoryModel modelClassFactoryToMerge = this.specVersions.get(i).getModelClassFactoryModel();
            List<TypeToClassModel> missingTypeToClasses = new ArrayList<>(modelClassFactoryToMerge.getTypeToClass());
            missingTypeToClasses.removeAll(retval.getTypeToClass());
            if (!missingTypeToClasses.isEmpty()) {
                for (TypeToClassModel missingTypeToClass:missingTypeToClasses) {
                    retval.getTypeToClass().add(missingTypeToClass);
                    warnings.add(String.format("Latest spec version does not contain the TypeToClass %s from spec version %s",
                            missingTypeToClass.getClassConstant(), this.specVersions.get(i).getSpecVersion()));
                }
            }
        }
        return retval;
    }

    /**
     * Merges model objects across different versions
     * @return model objects from all spec versions
     * @throws InvocationTargetException on errors copying property values between model objects
     * @throws IllegalAccessException on errors copying property values between model objects
     */
    private ModelObjectModel mergeModelObjects() throws InvocationTargetException, IllegalAccessException {
        ModelObjectModel retval = new ModelObjectModel();
        BeanUtils.copyProperties(retval, this.specVersions.get(this.specVersions.size()-1).getModelObjectModel());
        for (int i = this.specVersions.size()-2; i >= 0; i--) {
            ModelObjectModel modelObjectToMerge = this.specVersions.get(i).getModelObjectModel();
            List<String> missingCreateBuilders = new ArrayList<>(modelObjectToMerge.getCreateBuilder());
            missingCreateBuilders.removeAll(retval.getCreateBuilder());
            if (!missingCreateBuilders.isEmpty()) {
                for (String missingCreateBuilder:missingCreateBuilders) {
                    retval.getCreateBuilder().add(missingCreateBuilder);
                    warnings.add(String.format("Latest spec version does not contain the CreateBuilder %s from spec version %s",
                            missingCreateBuilder, this.specVersions.get(i).getSpecVersion()));
                }
            }
            List<String> missingImports = new ArrayList<>(modelObjectToMerge.getImports());
            missingImports.removeAll(retval.getImports());
            if (!missingImports.isEmpty()) {
                for (String missingImport:missingImports) {
                    retval.getImports().add(missingImport);
                    warnings.add(String.format("Latest spec version does not contain the ModelObject import %s from spec version %s",
                            missingImport, this.specVersions.get(i).getSpecVersion()));
                }
            }
        }
        return retval;
    }

    /**
     * Merges enum factory models across different versions
     * @return enum factory models from all spec versions
     * @throws InvocationTargetException on errors copying property values between model objects
     * @throws IllegalAccessException on errors copying property values between model objects
     */
    private EnumFactoryModel mergeEnumFactories() throws InvocationTargetException, IllegalAccessException {
        EnumFactoryModel retval = new EnumFactoryModel();
        BeanUtils.copyProperties(retval, this.specVersions.get(this.specVersions.size()-1).getEnumFactoryModel());
        List<String> qualifiedEnumNames = retval.getEnumClasses().stream().map(this::toQualifiedName).collect(Collectors.toList());
        for (int i = this.specVersions.size()-2; i >= 0; i--) {
            EnumFactoryModel enumFactoryToMerge = this.specVersions.get(i).getEnumFactoryModel();
            List<String> missingEnumNames = enumFactoryToMerge.getEnumClasses().stream().map(this::toQualifiedName).collect(Collectors.toList());
            missingEnumNames.removeAll(qualifiedEnumNames);
            if (!missingEnumNames.isEmpty()) {
                for (EnumModel enumModel:enumFactoryToMerge.getEnumClasses()) {
                    if (missingEnumNames.contains(toQualifiedName(enumModel))) {
                        retval.getEnumClasses().add(enumModel);
                        warnings.add(String.format("Latest spec version does not contain the Enum %s from spec version %s",
                                toQualifiedName(enumModel), this.specVersions.get(i).getSpecVersion()));
                    }
                }
            }
            List<String> missingImports = new ArrayList<>(enumFactoryToMerge.getImports());
            missingImports.removeAll(retval.getImports());
            if (!missingImports.isEmpty()) {
                for (String missingImport:missingImports) {
                    retval.getImports().add(missingImport);
                    warnings.add(String.format("Latest spec version does not contain the EnumFactory import %s from spec version %s",
                            missingImport, this.specVersions.get(i).getSpecVersion()));
                }
            }
        }
        return retval;
    }

    /**
     * Merges test value generators across different versions
     * @return test value generators from all spec versions
     * @throws InvocationTargetException on errors copying property values between model objects
     * @throws IllegalAccessException on errors copying property values between model objects
     * @exception ShaclToJavaException when the unable to merge properties
     */
    private TestValuesGeneratorModel mergeTestValuesGenerator() throws InvocationTargetException, IllegalAccessException, ShaclToJavaException {
        TestValuesGeneratorModel retval = new TestValuesGeneratorModel();
        BeanUtils.copyProperties(retval, this.specVersions.get(this.specVersions.size()-1).getTestValuesGeneratorModel());
        for (int i = this.specVersions.size()-2; i >= 0; i--) {
            TestValuesGeneratorModel testValuesGeneratorModelToMerge = this.specVersions.get(i).getTestValuesGeneratorModel();
            List<String> missingPropertyConstantNames = mergeProperties(testValuesGeneratorModelToMerge.getBooleanProperties(),
                    retval.getBooleanProperties());
            missingPropertyConstantNames.addAll(mergeProperties(testValuesGeneratorModelToMerge.getElementProperties(),
                    retval.getElementProperties()));
            missingPropertyConstantNames.addAll(mergeProperties(testValuesGeneratorModelToMerge.getDoubleProperties(),
                    retval.getDoubleProperties()));
            missingPropertyConstantNames.addAll(mergeProperties(testValuesGeneratorModelToMerge.getEnumerationProperties(),
                    retval.getEnumerationProperties()));
            missingPropertyConstantNames.addAll(mergeProperties(testValuesGeneratorModelToMerge.getIntegerProperties(),
                    retval.getIntegerProperties()));
            missingPropertyConstantNames.addAll(mergeProperties(testValuesGeneratorModelToMerge.getObjectProperties(),
                    retval.getObjectProperties()));
            missingPropertyConstantNames.addAll(mergeProperties(testValuesGeneratorModelToMerge.getStringProperties(),
                    retval.getStringProperties()));
            missingPropertyConstantNames.addAll(mergeProperties(testValuesGeneratorModelToMerge.getExtendableLicenseProperties(),
                    retval.getExtendableLicenseProperties()));
            missingPropertyConstantNames.addAll(mergeProperties(testValuesGeneratorModelToMerge.getLicenseAdditionProperties(),
                    retval.getLicenseAdditionProperties()));
            missingPropertyConstantNames.addAll(mergeProperties(testValuesGeneratorModelToMerge.getAnyLicenseInfoProperties(),
                    retval.getAnyLicenseInfoProperties()));
            missingPropertyConstantNames.addAll(mergeProperties(testValuesGeneratorModelToMerge.getEnumPropertyValueCollection(),
                    retval.getEnumPropertyValueCollection()));
            missingPropertyConstantNames.addAll(mergeProperties(testValuesGeneratorModelToMerge.getObjectPropertyValueCollection(),
                    retval.getObjectPropertyValueCollection()));
            missingPropertyConstantNames.addAll(mergeProperties(testValuesGeneratorModelToMerge.getObjectPropertyValueSet(),
                    retval.getObjectPropertyValueSet()));
            missingPropertyConstantNames.addAll(mergeProperties(testValuesGeneratorModelToMerge.getStringCollection(),
                    retval.getStringCollection()));

            for (String missingPropertyConstantName:missingPropertyConstantNames) {
                warnings.add(String.format("Latest spec version does not contain the test value property constant %s from spec version %s",
                        missingPropertyConstantName, this.specVersions.get(i).getSpecVersion()));
            }
            List<String> missingImports = new ArrayList<>(testValuesGeneratorModelToMerge.getImports());
            missingImports.removeAll(retval.getImports());
            if (!missingImports.isEmpty()) {
                for (String missingImport:missingImports) {
                    retval.getImports().add(missingImport);
                    warnings.add(String.format("Latest spec version does not contain the TestValuesFactory import %s from spec version %s",
                            missingImport, this.specVersions.get(i).getSpecVersion()));
                }
            }
        }
        return retval;
    }

    /**
     * Merge any additional properties in the fromProperties to the toProperties
     * @param fromProperties properties to merge from
     * @param toProperties resultant properties including all properties in both lists
     * @return list of property constant names that were added
     * @exception ShaclToJavaException when the unable to merge properties
     */
    private List<String> mergeProperties(List<PropertyModel> fromProperties, List<PropertyModel> toProperties) throws ShaclToJavaException {
        if (Objects.isNull(fromProperties)) {
            return new ArrayList<>();
        }
        if (Objects.isNull(toProperties) && !fromProperties.isEmpty()) {
            throw new ShaclToJavaException("Can not merge properties int a null property list");
        }
        List<String> missingPropertyConstants = fromProperties.stream().map(PropertyModel::getPropertyConstant)
                .collect(Collectors.toList());
        List<String> currentPropertyConstants = toProperties.stream().map(PropertyModel::getPropertyConstant)
                .collect(Collectors.toList());
        missingPropertyConstants.removeAll(currentPropertyConstants);
        if (!missingPropertyConstants.isEmpty()) {
            for (PropertyModel prop : fromProperties) {
                if (missingPropertyConstants.contains(prop.getPropertyConstant())) {
                    toProperties.add(prop);
                }
            }
        }
        return missingPropertyConstants;
    }

    /**
     * Merges individual factories across different versions
     * @return individual factory model with merged data from all spec versions
     * @throws InvocationTargetException on errors copying property values between model objects
     * @throws IllegalAccessException on errors copying property values between model objects
     */
    private IndividualFactoryModel mergeIndividualFactories() throws InvocationTargetException, IllegalAccessException {
        IndividualFactoryModel retval = new IndividualFactoryModel();
        BeanUtils.copyProperties(retval, this.specVersions.get(this.specVersions.size()-1).getIndividualFactoryModel());
        List<String> individualClassNames = retval.getIndividuals().stream().map(IndividualModel::getClassName).collect(Collectors.toList());
        for (int i = this.specVersions.size()-2; i >= 0; i--) {
            IndividualFactoryModel individualFactoryToMerge = this.specVersions.get(i).getIndividualFactoryModel();
            List<String> missingIndividualClassNames = individualFactoryToMerge.getIndividuals()
                    .stream().map(IndividualModel::getClassName).collect(Collectors.toList());
            missingIndividualClassNames.removeAll(individualClassNames);
            if (!missingIndividualClassNames.isEmpty()) {
                for (IndividualModel individual:individualFactoryToMerge.getIndividuals()) {
                    if (missingIndividualClassNames.contains(individual.getClassName())) {
                        individualClassNames.add(individual.getClassName());
                        retval.getIndividuals().add(individual);
                        warnings.add(String.format("Latest spec version does not contain the Individual Class %s from spec version %s",
                                individual.getClassName(), this.specVersions.get(i).getSpecVersion()));
                    }
                }
            }
            List<String> missingImports = new ArrayList<>(individualFactoryToMerge.getImports());
            missingImports.removeAll(retval.getImports());
            if (!missingImports.isEmpty()) {
                for (String missingImport:missingImports) {
                    retval.getImports().add(missingImport);
                    warnings.add(String.format("Latest spec version does not contain the IndividualFactory import %s from spec version %s",
                            missingImport, this.specVersions.get(i).getSpecVersion()));
                }
            }
        }
        return retval;
    }

    /**
     * Merges constant models across different versions
     * @return constants model with merged data from all spec versions
     * @throws InvocationTargetException on errors copying property values between model objects
     * @throws IllegalAccessException on errors copying property values between model objects
     */
    private ConstantsModel mergeConstantsModels() throws InvocationTargetException, IllegalAccessException {
        ConstantsModel retval = new ConstantsModel();
        BeanUtils.copyProperties(retval, this.specVersions.get(this.specVersions.size()-1).getConstantsModel());
        boolean addedClassConstant = false;
        for (int i = this.specVersions.size()-2; i >= 0; i--) {
            ConstantsModel constantsToMerge = this.specVersions.get(i).getConstantsModel();
            // merge namespaces
            List<NamespaceModel> missingNamespaces = new ArrayList<>(constantsToMerge.getNamespaces());
            missingNamespaces.removeAll(retval.getNamespaces());
            if (!missingNamespaces.isEmpty()) {
                for (NamespaceModel missingNameSpace:missingNamespaces) {
                    retval.getNamespaces().add(missingNameSpace);
                    warnings.add(String.format("Latest spec version does not contain the NameSpace %s from spec version %s",
                            missingNameSpace.getNamespaceName(), this.specVersions.get(i).getSpecVersion()));
                }
            }
            List<String> missingClassConstantDefinitions = new ArrayList<>(constantsToMerge.getClassConstantDefinitions());
            missingClassConstantDefinitions.removeAll(retval.getClassConstantDefinitions());
            if (!missingClassConstantDefinitions.isEmpty()) {
                for (String missingClassConstantDefinition:missingClassConstantDefinitions) {
                    retval.getClassConstantDefinitions().add(missingClassConstantDefinition);
                    warnings.add(String.format("Latest spec version does not contain the class constant definition  '%s' from spec version %s",
                            missingClassConstantDefinition, this.specVersions.get(i).getSpecVersion()));
                }
            }
            List<String> missingClassConstants = new ArrayList<>(constantsToMerge.getClassConstants());
            missingClassConstants.removeAll(retval.getClassConstants());
            if (!missingClassConstants.isEmpty()) {
                addedClassConstant = true;
                for (String missingClassConstant:missingClassConstants) {
                    retval.getClassConstants().add(missingClassConstant);
                    warnings.add(String.format("Latest spec version does not contain the class constant '%s' from spec version %s",
                            missingClassConstant, this.specVersions.get(i).getSpecVersion()));
                }
            }
            if (addedClassConstant) {
                // Need to regenerate the allClassConstants
                retval.setAllClassConstants(SpecVersionContainer.buildClassConstant(retval.getClassConstants()));
            }
        }
        return retval;
    }

    private SpdxModelInfoModel mergeSpdxModelInfos() {
        SpdxModelInfoModel retval = this.specVersions.get(this.specVersions.size()-1).getSpdxModelInfoModel();
        List<String> supportedVersions = new ArrayList<>();
        supportedVersions.add("\"" + retval.getVersionSemVer() + "\"");
        for (int i = this.specVersions.size()-2; i >= 0; i--) {
            supportedVersions.add("\"" + this.specVersions.get(i).getSpdxModelInfoModel().getVersionSemVer() + "\"");
        }
        retval.setSupportedVersions(String.join(",", supportedVersions));
        return retval;
    }

    /**
     * Collect all the property names used in the Java Class model
     * @param javaClassModel Java class model containing lists of properties by type
     * @return consolidated list of all property names
     */
    private List<String> collectAllPropertyNames(JavaClassModel javaClassModel) {
        List<String> retval = new ArrayList<>();
        for (PropertyModel propertyModel: javaClassModel.getBooleanProperties()) {
            retval.add(propertyModel.getPropertyName());
        }
        for (PropertyModel propertyModel: javaClassModel.getElementProperties()) {
            retval.add(propertyModel.getPropertyName());
        }
        for (PropertyModel propertyModel: javaClassModel.getDoubleProperties()) {
            retval.add(propertyModel.getPropertyName());
        }
        for (PropertyModel propertyModel: javaClassModel.getEnumerationProperties()) {
            retval.add(propertyModel.getPropertyName());
        }
        for (PropertyModel propertyModel: javaClassModel.getIntegerProperties()) {
            retval.add(propertyModel.getPropertyName());
        }
        for (PropertyModel propertyModel: javaClassModel.getObjectProperties()) {
            retval.add(propertyModel.getPropertyName());
        }
        for (PropertyModel propertyModel: javaClassModel.getAnyLicenseInfoProperties()) {
            retval.add(propertyModel.getPropertyName());
        }
        for (PropertyModel propertyModel: javaClassModel.getEnumPropertyValueCollection()) {
            retval.add(propertyModel.getPropertyName());
        }
        for (PropertyModel propertyModel: javaClassModel.getLicenseAdditionProperties()) {
            retval.add(propertyModel.getPropertyName());
        }
        for (PropertyModel propertyModel: javaClassModel.getExtendableLicenseProperties()) {
            retval.add(propertyModel.getPropertyName());
        }
        for (PropertyModel propertyModel: javaClassModel.getStringProperties()) {
            retval.add(propertyModel.getPropertyName());
        }
        for (PropertyModel propertyModel: javaClassModel.getObjectPropertyValueCollection()) {
            retval.add(propertyModel.getPropertyName());
        }
        for (PropertyModel propertyModel: javaClassModel.getStringCollection()) {
            retval.add(propertyModel.getPropertyName());
        }
        for (PropertyModel propertyModel: javaClassModel.getObjectPropertyValueSet()) {
            retval.add(propertyModel.getPropertyName());
        }
        for (PropertyModel propertyModel: javaClassModel.getEnumPropertyValueCollection()) {
            retval.add(propertyModel.getPropertyName());
        }
        return retval;
    }


    /**
     * Generates source and test files and store them in the dir
     * @param dir Directory to hold the java source
     * @return list of warnings - if empty, all files were generated successfully
     * @throws IOException for any issues storing the files
     * @throws ShaclToJavaException errors in the ontology
     * @throws InvocationTargetException on errors copying property values between model objects
     * @throws IllegalAccessException on errors copying property values between model objects
     */
    public List<String> generate(File dir) throws IOException, ShaclToJavaException, InvocationTargetException, IllegalAccessException {

        for (IndividualClassModel individual : mergeIndividuals()) {
            File sourceFile = createJavaSourceFile(individual.getIndividualUri(), individual.getClassName(), dir);
            writeMustacheFile(ShaclToJavaConstants.INDIVIDUAL_CLASS_TEMPLATE, sourceFile, individual);
        }

        Map<String, Map<String, List<String>>> enumClassToVersionMissingValue = new HashMap<>();
        for (EnumModel enumModel:mergeEnums(enumClassToVersionMissingValue)) {
            File sourceFile = createJavaSourceFile(enumModel.getClassUri(), enumModel.getName(), dir);
            writeMustacheFile(ShaclToJavaConstants.ENUM_CLASS_TEMPLATE, sourceFile, enumModel);
        }
        for (JavaClassModel javaClassModel:mergeJavaClassModels(enumClassToVersionMissingValue)) {
            File sourceFile = createJavaSourceFile(javaClassModel.getClassUri(), javaClassModel.getClassName(), dir);
            writeMustacheFile(ShaclToJavaConstants.JAVA_CLASS_TEMPLATE, sourceFile, javaClassModel);
        }
        for (JavaClassModel externalJavaClassModel:mergeExternalJavaClassModels()) {
            File sourceFile = createExternalJavaSourceFile(externalJavaClassModel.getClassUri(),
                    externalJavaClassModel.getClassName(), dir);
            writeMustacheFile(ShaclToJavaConstants.EXTERNAL_JAVA_CLASS_TEMPLATE, sourceFile, externalJavaClassModel);
        }
        for (UnitTestModel unitTestModel:mergeUnitTestModels()) {
            File unitTestFile = createUnitTestFile(unitTestModel.getClassUri(), unitTestModel.getClassName(), dir);
            writeMustacheFile(ShaclToJavaConstants.UNIT_TEST_TEMPLATE, unitTestFile, unitTestModel);
        }
        Path topLevelSourcePath = dir.toPath().resolve("src").resolve("main").resolve("java")
                .resolve("org").resolve("spdx").resolve("library")
                .resolve("model").resolve(VERSION_SUFFIX);
        Files.createDirectories(topLevelSourcePath);
        File constantsFile = topLevelSourcePath.resolve("SpdxConstantsV3.java").toFile();
        writeMustacheFile(ShaclToJavaConstants.SPDX_CONSTANTS_TEMPLATE, constantsFile, mergeConstantsModels());
        File enumFactoryFile = topLevelSourcePath.resolve("SpdxEnumFactory.java").toFile();
        writeMustacheFile(ShaclToJavaConstants.ENUM_FACTORY_TEMPLATE, enumFactoryFile, mergeEnumFactories());
        File modelClassFactoryFile = topLevelSourcePath.resolve("SpdxModelClassFactoryV3.java").toFile();
        writeMustacheFile(ShaclToJavaConstants.MODEL_CLASS_FACTORY_TEMPLATE, modelClassFactoryFile, mergeModelClassFactories());
        File modelObjectFile = topLevelSourcePath.resolve("ModelObjectV3.java").toFile();
        writeMustacheFile(ShaclToJavaConstants.BASE_MODEL_OBJECT_TEMPLATE, modelObjectFile, mergeModelObjects());
        SpdxModelInfoModel spdxModelInfoModel = mergeSpdxModelInfos();
        File spdxModelInfoFile = topLevelSourcePath.resolve(String.format("SpdxModelInfo%s.java", spdxModelInfoModel.getClassSuffix())).toFile();
        writeMustacheFile(ShaclToJavaConstants.MODEL_INFO_TEMPLATE, spdxModelInfoFile, spdxModelInfoModel);
        File packageInfoFile = topLevelSourcePath.resolve("package-info.java").toFile();
        writeMustacheFile(ShaclToJavaConstants.PACKAGE_INFO_TEMPLATE, packageInfoFile,
                this.specVersions.get(this.specVersions.size()-1).getPackageInfoModel());
        File pomFile = dir.toPath().resolve("pom.xml").toFile();
        writeMustacheFile(ShaclToJavaConstants.POM_TEMPLATE, pomFile, new HashMap<>());
        File individualsFile = topLevelSourcePath.resolve("SpdxIndividualFactory.java").toFile();
        writeMustacheFile(ShaclToJavaConstants.INDIVIDUALS_FACTORY_TEMPLATE, individualsFile, mergeIndividualFactories());
        Path testPath = dir.toPath().resolve("src").resolve("test").resolve("java").resolve("org")
                .resolve("spdx").resolve("library").resolve("model").resolve(VERSION_SUFFIX);
        Files.createDirectories(testPath);
       File testValuesGeneratorFile = testPath.resolve("TestValuesGenerator.java").toFile();
       //TODO: Replace with merged test values generator
        writeMustacheFile(ShaclToJavaConstants.TEST_VALUES_GENERATOR_TEMPLATE, testValuesGeneratorFile,
                mergeTestValuesGenerator());
        MockFileModel mockFileModel = this.specVersions.get(this.specVersions.size()-1).getMockFileModel();
        File mockModelStoreFile = testPath.resolve("MockModelStore.java").toFile();
        writeMustacheFile(ShaclToJavaConstants.MOCK_MODEL_STORE_TEMPLATE, mockModelStoreFile, mockFileModel);
        File mockCopyManager = testPath.resolve("MockCopyManager.java").toFile();
        writeMustacheFile(ShaclToJavaConstants.MOCK_COPY_MANAGER_TEMPLATE, mockCopyManager, mockFileModel);
        File unitTestHelper = testPath.resolve("UnitTestHelper.java").toFile();
        writeMustacheFile(ShaclToJavaConstants.UNIT_TEST_HELPER_TEMPLATE, unitTestHelper, mockFileModel);
        File testModelInfoFile = testPath.resolve("TestSpdxModelInfo.java").toFile();
        writeMustacheFile(ShaclToJavaConstants.TEST_MODEL_INFO_TEMPLATE, testModelInfoFile, mockFileModel);
        Path licensePackagePath = dir.toPath().resolve("src").resolve("main").resolve("java").resolve("org")
                .resolve("spdx").resolve("library").resolve("model").resolve(VERSION_SUFFIX)
                .resolve("simplelicensing");
        Files.createDirectories(licensePackagePath);
        File file = licensePackagePath.resolve("InvalidLicenseExpression.java").toFile();
        writeMustacheFile(ShaclToJavaConstants.INVALID_LICENSE_EXPRESSION_TEMPLATE, file,
                this.specVersions.get(this.specVersions.size()-1).getInvalidLicenseExpressionModel());
        return warnings;
    }

    /**
     * @param classUri URI for the class
     * @param dir directory to hold the file
     * @param className Name of the class
     * @return the created file
     * @throws IOException on IO errors
     */
    private File createUnitTestFile(String classUri, String className, File dir) throws IOException {
        Path path = dir.toPath().resolve("src").resolve("test").resolve("java").resolve("org")
                .resolve("spdx").resolve("library").resolve("model").resolve(VERSION_SUFFIX);
        String[] parts = classUri.substring(ShaclToJavaConstants.SPDX_URI_PREFIX.length()).split("/");
        // [0] is version, [1] is "terms"
        for (int i = 2; i < parts.length-1; i++) {
            path = path.resolve(parts[i].toLowerCase());
        }
        Files.createDirectories(path);
        File retval = path.resolve(className + "Test.java").toFile();
        if (!retval.createNewFile()) {
            throw new IOException("IO Error creating new file");
        }
        return retval;
    }

    /**
     * @param classUri URI for the class
     * @param dir directory to hold the file
     * @param className Name of the class
     * @return the created file
     * @throws IOException on IO errors
     */
    private File createExternalJavaSourceFile(String classUri, String className, File dir) throws IOException {
        Path path = dir.toPath().resolve("src").resolve("main").resolve("java").resolve("org")
                .resolve("spdx").resolve("library").resolve("model").resolve(VERSION_SUFFIX);
        String[] parts = classUri.substring(ShaclToJavaConstants.SPDX_URI_PREFIX.length()).split("/");
        // [0] is version, [1] is "terms"
        for (int i = 2; i < parts.length-1; i++) {
            path = path.resolve(parts[i].toLowerCase());
        }
        Files.createDirectories(path);
        File retval = path.resolve("External" + className + ".java").toFile();
        if (!retval.createNewFile()) {
            throw new IOException("IO Error creating new file");
        }
        return retval;
    }

    /**
     * @param classUri URI for the class
     * @param dir directory to hold the file
     * @param className Name of the class
     * @return the created file
     * @throws IOException on IO errors
     */
    private File createJavaSourceFile(String classUri, String className, File dir) throws IOException {
        Path path = dir.toPath().resolve("src").resolve("main").resolve("java").resolve("org")
                .resolve("spdx").resolve("library").resolve("model").resolve(VERSION_SUFFIX);
        String[] parts = classUri.substring(ShaclToJavaConstants.SPDX_URI_PREFIX.length()).split("/");
        // [0] is version, [1] is "terms"
        for (int i = 2; i < parts.length-1; i++) {
            path = path.resolve(parts[i].toLowerCase());
        }
        Files.createDirectories(path);
        File retval = path.resolve(className + ".java").toFile();
        if (!retval.createNewFile()) {
            throw new IOException("IO Error creating new file");
        }
        return retval;
    }

    /**
     * Writes a file using a Mustache template
     * @param templateName Name of Mustache template
     * @param file File to write to
     * @param mustacheObject Object to used to fill in the Mustache template
     * @throws IOException on I/O error writing to file
     */
    private void writeMustacheFile(String templateName, File file, Object mustacheObject) throws IOException {
        String templateDirName = ShaclToJavaConstants.TEMPLATE_ROOT_PATH;
        File templateDirectoryRoot = new File(templateDirName);
        if (!(templateDirectoryRoot.exists() && templateDirectoryRoot.isDirectory())) {
            templateDirName = ShaclToJavaConstants.TEMPLATE_CLASS_PATH;
        }
        DefaultMustacheFactory builder = new DefaultMustacheFactory(templateDirName);
        Mustache mustache = builder.compile(templateName);
        try (FileOutputStream stream = new FileOutputStream(file); OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
            mustache.execute(writer, mustacheObject);
        }
    }
}
