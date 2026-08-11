package org.spdx.tools.model2java.model;

import java.util.List;

public class BaseClassModel extends BaseModel {
    private String className;
    private String classProfile;
    private List<PropertyModel> elementProperties;
    private List<PropertyModel> objectProperties;
    private List<PropertyModel> anyLicenseInfoProperties;
    private List<PropertyModel> licenseAdditionProperties;
    private List<PropertyModel> extendableLicenseProperties;
    private List<PropertyModel> enumerationProperties;
    private List<PropertyModel> booleanProperties;
    private List<PropertyModel> integerProperties;
    private List<PropertyModel> doubleProperties;
    private List<PropertyModel> stringProperties;
    private List<PropertyModel> objectPropertyValueCollection;
    private List<PropertyModel> stringCollection;
    private List<PropertyModel> objectPropertyValueSet;
    private List<PropertyModel> enumPropertyValueCollection;
    private boolean suppressUnchecked;
    private String year;
    private String pkgName;
    private String classComments;
    private String superClass;
    private String[] imports;
    private String toStringName;

    public String getClassName() {
        return className;
    }
    public void setClassName(String className) {
        this.className = className;
    }
    public String getClassProfile() {
        return classProfile;
    }
    public void setClassProfile(String classProfile) {
        this.classProfile = classProfile;
    }
    public List<PropertyModel> getElementProperties() {
        return elementProperties;
    }
    public void setElementProperties(List<PropertyModel> elementProperties) {
        this.elementProperties = elementProperties;
    }
    public List<PropertyModel> getObjectProperties() {
        return objectProperties;
    }
    public void setObjectProperties(List<PropertyModel> objectProperties) {
        this.objectProperties = objectProperties;
    }
    public List<PropertyModel> getAnyLicenseInfoProperties() {
        return anyLicenseInfoProperties;
    }
    public void setAnyLicenseInfoProperties(List<PropertyModel> anyLicenseInfoProperties) {
        this.anyLicenseInfoProperties = anyLicenseInfoProperties;
    }
    public List<PropertyModel> getLicenseAdditionProperties() {
        return licenseAdditionProperties;
    }
    public void setLicenseAdditionProperties(List<PropertyModel> licenseAdditionProperties) {
        this.licenseAdditionProperties = licenseAdditionProperties;
    }
    public List<PropertyModel> getExtendableLicenseProperties() {
        return extendableLicenseProperties;
    }
    public void setExtendableLicenseProperties(List<PropertyModel> extendableLicenseProperties) {
        this.extendableLicenseProperties = extendableLicenseProperties;
    }
    public List<PropertyModel> getEnumerationProperties() {
        return enumerationProperties;
    }
    public void setEnumerationProperties(List<PropertyModel> enumerationProperties) {
        this.enumerationProperties = enumerationProperties;
    }
    public List<PropertyModel> getBooleanProperties() {
        return booleanProperties;
    }
    public void setBooleanProperties(List<PropertyModel> booleanProperties) {
        this.booleanProperties = booleanProperties;
    }
    public List<PropertyModel> getIntegerProperties() {
        return integerProperties;
    }
    public void setIntegerProperties(List<PropertyModel> integerProperties) {
        this.integerProperties = integerProperties;
    }
    public List<PropertyModel> getDoubleProperties() {
        return doubleProperties;
    }
    public void setDoubleProperties(List<PropertyModel> doubleProperties) {
        this.doubleProperties = doubleProperties;
    }
    public List<PropertyModel> getStringProperties() {
        return stringProperties;
    }
    public void setStringProperties(List<PropertyModel> stringProperties) {
        this.stringProperties = stringProperties;
    }
    public List<PropertyModel> getObjectPropertyValueCollection() {
        return objectPropertyValueCollection;
    }
    public void setObjectPropertyValueCollection(List<PropertyModel> objectPropertyValueCollection) {
        this.objectPropertyValueCollection = objectPropertyValueCollection;
    }
    public List<PropertyModel> getStringCollection() {
        return stringCollection;
    }
    public void setStringCollection(List<PropertyModel> stringCollection) {
        this.stringCollection = stringCollection;
    }
    public List<PropertyModel> getObjectPropertyValueSet() {
        return objectPropertyValueSet;
    }
    public void setObjectPropertyValueSet(List<PropertyModel> objectPropertyValueSet) {
        this.objectPropertyValueSet = objectPropertyValueSet;
    }
    public List<PropertyModel> getEnumPropertyValueCollection() {
        return enumPropertyValueCollection;
    }
    public void setEnumPropertyValueCollection(List<PropertyModel> enumPropertyValueCollection) {
        this.enumPropertyValueCollection = enumPropertyValueCollection;
    }
    public boolean isSuppressUnchecked() {
        return suppressUnchecked;
    }
    public void setSuppressUnchecked(boolean suppressUnchecked) {
        this.suppressUnchecked = suppressUnchecked;
    }
    public String getYear() {
        return year;
    }
    public void setYear(String year) {
        this.year = year;
    }
    public String getPkgName() {
        return pkgName;
    }
    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
    }
    public String getClassComments() {
        return classComments;
    }
    public void setClassComments(String classComments) {
        this.classComments = classComments;
    }
    public String getSuperClass() {
        return superClass;
    }
    public void setSuperClass(String superClass) {
        this.superClass = superClass;
    }
    public String[] getImports() {
        return imports;
    }
    public void setImports(String[] imports) {
        this.imports = imports;
    }
    public String getToStringName() {
        return toStringName;
    }
    public void setToStringName(String toStringName) {
        this.toStringName = toStringName;
    }
}
