package org.spdx.tools.model2java.model;

public class PropertyModel extends BaseModel {

    public enum PropertyType {
        ELEMENT,
        ANY_LICENSE_INFO,
        OBJECT,
        ENUM,
        BOOLEAN,
        INTEGER,
        DOUBLE,
        STRING,
        OBJECT_COLLECTION,
        STRING_COLLECTION,
        OBJECT_SET, ENUM_COLLECTION,
        LICENSE_ADDITION,
        EXTENDABLE_LICENSE
    }

    private String propertyName;
    private String propertyNameUpper;
    private String getter;
    private String setter;
    private String adder;
    private String addAller;
    private boolean isCreationInfo;
    private PropertyType propertyType;
    private String typeUri;
    private String type;
    private boolean required;
    private String requiredProfiles;
    private boolean superSetter;
    private boolean nonOptional;
    private String pattern;
    private String min;
    private String max;
    private boolean hasConstraint;
    private String uri;
    private String propertyConstant;
    private boolean isSpecVersion;

    public String getPropertyName() {
        return propertyName;
    }
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }
    public String getPropertyNameUpper() {
        return propertyNameUpper;
    }
    public void setPropertyNameUpper(String propertyNameUpper) {
        this.propertyNameUpper = propertyNameUpper;
    }
    public String getGetter() {
        return getter;
    }
    public void setGetter(String getter) {
        this.getter = getter;
    }
    public String getSetter() {
        return setter;
    }
    public void setSetter(String setter) {
        this.setter = setter;
    }
    public String getAdder() {
        return adder;
    }
    public void setAdder(String adder) {
        this.adder = adder;
    }
    public String getAddAller() {
        return addAller;
    }
    public void setAddAller(String addAller) {
        this.addAller = addAller;
    }
    public boolean isCreationInfo() {
        return isCreationInfo;
    }
    public void setCreationInfo(boolean isCreationInfo) {
        this.isCreationInfo = isCreationInfo;
    }
    public PropertyType getPropertyType() {
        return propertyType;
    }
    public void setPropertyType(PropertyType propertyType) {
        this.propertyType = propertyType;
    }
    public String getTypeUri() {
        return typeUri;
    }
    public void setTypeUri(String typeUri) {
        this.typeUri = typeUri;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public boolean isRequired() {
        return required;
    }
    public void setRequired(boolean required) {
        this.required = required;
    }
    public String getRequiredProfiles() {
        return requiredProfiles;
    }
    public void setRequiredProfiles(String requiredProfiles) {
        this.requiredProfiles = requiredProfiles;
    }
    public boolean isSuperSetter() {
        return superSetter;
    }
    public void setSuperSetter(boolean superSetter) {
        this.superSetter = superSetter;
    }
    public boolean isNonOptional() {
        return nonOptional;
    }
    public void setNonOptional(boolean nonOptional) {
        this.nonOptional = nonOptional;
    }
    public String getPattern() {
        return pattern;
    }
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
    public String getMin() {
        return min;
    }
    public void setMin(String min) {
        this.min = min;
    }
    public String getMax() {
        return max;
    }
    public void setMax(String max) {
        this.max = max;
    }
    public boolean isHasConstraint() {
        return hasConstraint;
    }
    public void setHasConstraint(boolean hasConstraint) {
        this.hasConstraint = hasConstraint;
    }
    public String getUri() {
        return uri;
    }
    public void setUri(String uri) {
        this.uri = uri;
    }
    public String getPropertyConstant() {
        return propertyConstant;
    }
    public void setPropertyConstant(String propertyConstant) {
        this.propertyConstant = propertyConstant;
    }
    public Boolean getIsSpecVersion() {
        return isSpecVersion;
    }
    public void setIsSpecVersion(Boolean isSpecVersion) {
        this.isSpecVersion = isSpecVersion;
    }
}
