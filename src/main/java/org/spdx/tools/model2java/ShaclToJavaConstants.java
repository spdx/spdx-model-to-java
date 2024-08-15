/**
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2024 Source Auditor Inc.
 */
package org.spdx.tools.model2java;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Gary O'Neall
 * 
 * Constants used in ShaclToJava - moved to a separate file since ShaclToJava was getting way to big!
 *
 */
public class ShaclToJavaConstants {
	
	static final String SPDX_URI_PREFIX = "https://spdx.org/rdf/";
	static final String INDENT = "\t";
	public static final int COMMENT_LINE_LEN = 72;
	public static final String TYPE_PRED = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
	public static final String COMMENT_URI = "http://www.w3.org/2000/01/rdf-schema#comment";
	public static final String NAMED_INDIVIDUAL = "http://www.w3.org/2002/07/owl#NamedIndividual";
	public static final String LABEL_URI = "http://www.w3.org/2000/01/rdf-schema#label";
	public static final String RANGE_URI = "http://www.w3.org/2000/01/rdf-schema#range";
	public static final String BOOLEAN_TYPE = "http://www.w3.org/2001/XMLSchema#boolean";
	public static final String STRING_TYPE = "http://www.w3.org/2001/XMLSchema#string";
	public static final String ELEMENT_TYPE_URI = "https://spdx.org/rdf/3.0.0/terms/Core/Element";
	public static final String ELEMENT_TYPE_ANY_LICENSE_INFO = "https://spdx.org/rdf/3.0.0/terms/Licensing/AnyLicenseInfo";
	public static final String ELEMENT_TYPE_EXTENDABLE_LICENSE = "https://spdx.org/rdf/3.0.0/terms/ExpandedLicensing/ExtendableLicense";
	public static final String ELEMENT_TYPE_LICENSE_ADDITION = "https://spdx.org/rdf/3.0.0/terms/ExpandedLicensing/LicenseAddition";
	public static final String DATE_TIME_TYPE = "http://www.w3.org/2001/XMLSchema#dateTimeStamp";
	public static final String ANY_URI_TYPE = "http://www.w3.org/2001/XMLSchema#anyURI";
	public static final String OWL_THING_URI = "http://www.w3.org/2002/07/owl#Thing";
	public static final String XSD_INTEGER = "http://www.w3.org/2001/XMLSchema#integer";
	public static final String XSD_POSITIVE_INTEGER = "http://www.w3.org/2001/XMLSchema#positiveInteger";
	public static final String XSD_NON_NEGATIVE_INTEGER = "http://www.w3.org/2001/XMLSchema#nonNegativeInteger";
	public static final String ABSTRACT_TYPE_URI = "http://spdx.invalid./AbstractClass";
	
	static final String TEMPLATE_CLASS_PATH = "resources" + "/" + "javaTemplates";
	static final String TEMPLATE_ROOT_PATH = "resources" + File.separator + "javaTemplates";
	public static final String JAVA_CLASS_TEMPLATE = "JavaClassTemplate.txt";
	public static final String ENUM_CLASS_TEMPLATE = "EnumTemplate.txt";
	public static final String SPDX_CONSTANTS_TEMPLATE = "SpdxConstantsTemplate.txt";
	public static final String UNIT_TEST_TEMPLATE = "UnitTestTemplate.txt";
	public static final String ENUM_FACTORY_TEMPLATE = "SpdxEnumFactoryTemplate.txt";
	public static final String INDIVIDUALS_FACTORY_TEMPLATE = "SpdxIndividualFactoryTemplate.txt";
	public static final String MODEL_CLASS_FACTORY_TEMPLATE = "ModelClassFactoryTemplate.txt";
	public static final String CREATE_CLASS_TEMPLATE = "CreateClassTemplate.txt";
	public static final String EXTERNAL_JAVA_CLASS_TEMPLATE = "ExternalJavaClassTemplate.txt";
	public static final String BASE_MODEL_OBJECT_TEMPLATE = "BaseModelObjectTemplate.txt";
	public static final String MODEL_INFO_TEMPLATE = "ModelInfoTemplate.txt";
	public static final String PACKAGE_INFO_TEMPLATE = "PackageInfoTemplate.txt";
	public static final String POM_TEMPLATE = "PomTemplate.txt";
	public static final String INDIVIDUAL_CLASS_TEMPLATE = "IndividualClassTemplate.txt";
	public static final String LICENSE_SET_EQUALS_OVERRIDE_TEMPLATE = "LicenseSetEqualsOverrideTemplate.txt";
	public static final String OR_LATER_EQUALS_OVERRIDE_TEMPLATE = "OrLaterEqualsOverrideTemplate.txt";
	public static final String WITH_EQUALS_OVERRIDE_TEMPLATE = "WithOperatorEqualsOverrideTemplate.txt";
	public static final String WITH_OPERATOR_TO_STRING_TEMPLATE = "WithOperatorToStringTemplate.txt";
	public static final String OR_LATER_TO_STRING_TEMPLATE = "OrLaterToStringTemplate.txt";
	public static final String LICENSE_SET_TO_STRING_TEMPLATE = "LicenseSetToStringTemplate.txt";
	public static final String ELEMENT_TO_STRING_TEMPLATE = "ElementToStringTemplate.txt";
	public static final String MOCK_MODEL_STORE_TEMPLATE = "MockModelStoreTemplate.txt";
	public static final String MOCK_COPY_MANAGER_TEMPLATE = "MockCopyManagerTemplate.txt";
	public static final String UNIT_TEST_HELPER_TEMPLATE = "UnitTestHelperTemplate.txt";
	public static final String TEST_VALUES_GENERATOR_TEMPLATE = "TestValuesGeneratorTemplate.txt";
	public static final String TEST_MODEL_INFO_TEMPLATE = "TestModelInfoTemplate.txt";
	
	public static Set<String> INTEGER_TYPES = new HashSet<>();
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
	
	public static Map<String, String> RESERVED_JAVA_WORDS = new HashMap<>();
	static {
		RESERVED_JAVA_WORDS.put("Package", "SpdxPackage");
		RESERVED_JAVA_WORDS.put("package", "spdxPackage");
		RESERVED_JAVA_WORDS.put("File", "SpdxFile");
		RESERVED_JAVA_WORDS.put("file", "spdxFile");
	}
	
	public static Set<String> SET_TYPE_URIS = new HashSet<>(); // set of URI's for types should be treated as sets
	public static Set<String> SET_PROPERTY_URIS = new HashSet<>(); // set of URI's for properties whose types should be treated as sets
	static {
		SET_PROPERTY_URIS.add("https://spdx.org/rdf/3.0.0/terms/ExpandedLicensing/member");
	}
}
