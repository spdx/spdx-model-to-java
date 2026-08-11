package org.spdx.tools.model2java;

import junit.framework.TestCase;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.spdx.tools.model2java.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

public class SpecVersionContainerTest extends TestCase {

    static final String MODEL_DIR_PATH = "testResources";
    static final String MODEL_FILE_PATH = MODEL_DIR_PATH + File.separator + "spdx-model-3-1.ttl";

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSpdxVersionContainer() throws IOException, ShaclToJavaException {
        try (InputStream is = new FileInputStream(MODEL_FILE_PATH)) {
            OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
            model.read(is, "", "Turtle");
            SpecVersionContainer svc = new SpecVersionContainer(model, "3.1.0");
            assertTrue(svc.getWarnings().isEmpty());
            List<IndividualClassModel> individuals = svc.getIndividuals();
            List<IndividualClassModel> noAssertionLicenses = individuals.stream().filter(individual -> {
                return individual.getClassName().equals("NoAssertionLicense");
            }).collect(Collectors.toList());
            assertEquals(1, noAssertionLicenses.size());
            List<EnumModel> enums = svc.getEnums();
            assertFalse(enums.isEmpty());
            List<EnumModel> hashAlgorithms = enums.stream().filter(e -> {
                return e.getName().equals("HashAlgorithm");
            }).collect(Collectors.toList());
            assertEquals(1, hashAlgorithms.size());
            assertTrue(hashAlgorithms.get(0).getEnumValues().contains("ADLER32(\"adler32\"),"));
            List<JavaClassModel> javaClasses =  svc.getJavaClasses();
            assertFalse(javaClasses.isEmpty());
            List<JavaClassModel> personClasses = javaClasses.stream().filter(javaClass -> {
                return javaClass.getClassName().equals("Person");
            }).collect(Collectors.toList());
            assertEquals(1, personClasses.size());
            assertEquals("Agent", personClasses.get(0).getSuperClass());
            assertFalse(personClasses.get(0).getObjectProperties().isEmpty());
            List<JavaClassModel> externalJavaClasses = svc.getExternalJavaClasses();
            assertFalse(externalJavaClasses.isEmpty());
            List<JavaClassModel> locationClasses = externalJavaClasses.stream().filter(externalClass -> {
                return externalClass.getClassName().equals("PhysicalLocation");
            }).collect(Collectors.toList());
            assertEquals(1, locationClasses.size());
            List<UnitTestModel> unitTestClasses = svc.getUnitTestClasses();
            assertFalse(unitTestClasses.isEmpty());
            List<UnitTestModel> lifecycleScopeRelationships = unitTestClasses.stream().filter(unitTestClass -> {
                return unitTestClass.getClassName().equals("LifecycleScopedRelationship");
            }).collect(Collectors.toList());
            assertEquals(1, lifecycleScopeRelationships.size());
            List<PropertyModel> enumProps = lifecycleScopeRelationships.get(0).getEnumerationProperties();
            List<PropertyModel> scopeTypes = enumProps.stream().filter(enumProp -> {
                return enumProp.getType().equals("LifecycleScopeType");
            }).collect(Collectors.toList());
            assertEquals(1, scopeTypes.size());
            ConstantsModel constantsModel = svc.getConstantsModel();
            assertFalse(constantsModel.getAllClassConstants().isEmpty());
            assertFalse(constantsModel.getNamespaces().isEmpty());
            EnumFactoryModel enumFactoryModel = svc.getEnumFactoryModel();
            assertFalse(enumFactoryModel.getEnumClasses().isEmpty());
            assertFalse(enumFactoryModel.getImports().isEmpty());
            ModelClassFactoryModel modelClassFactoryModel = svc.getModelClassFactoryModel();
            assertFalse(modelClassFactoryModel.getTypeToClass().isEmpty());
            ModelObjectModel modelObjectModel = svc.getModelObjectModel();
            assertFalse(modelObjectModel.getCreateBuilder().isEmpty());
            SpdxModelInfoModel spdxModelInfoModel = svc.getSpdxModelInfoModel();
            assertEquals(JavaCodeGenerator.VERSION_SUFFIX, spdxModelInfoModel.getVersionSuffix());
            PackageInfoModel packageInfoModel = svc.getPackageInfoModel();
            assertEquals(JavaCodeGenerator.VERSION_SUFFIX, packageInfoModel.getVersionSuffix());
            IndividualFactoryModel individualFactoryModel = svc.getIndividualFactoryModel();
            assertFalse(individualFactoryModel.getIndividuals().isEmpty());
            MockFileModel mockFileModel = svc.getMockFileModel();
            assertEquals(JavaCodeGenerator.VERSION_SUFFIX, mockFileModel.getVersionSuffix());
            InvalidLicenseExpressionModel invalidLicenseExpressionModel = svc.getInvalidLicenseExpressionModel();
            assertEquals(JavaCodeGenerator.VERSION_SUFFIX, invalidLicenseExpressionModel.getVersionSuffix());
        }
    }
}
