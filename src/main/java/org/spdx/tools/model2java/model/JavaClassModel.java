package org.spdx.tools.model2java.model;

import com.fasterxml.jackson.databind.annotation.JsonAppend;

import java.util.List;

public class JavaClassModel extends BaseClassModel {

    private boolean isAbstract;
    private boolean verifySuperclass;
    private boolean hasCreationInfo;
    private boolean compareUsingProperties;
    private List<PropertyModel> compareProperties;
    private String toString;
    private String equalsHashOverride;

    public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    public boolean isAbstract() {
        return this.isAbstract;
    }

    public void setVerifySuperclass(boolean verifySuperclass) {
        this.verifySuperclass = verifySuperclass;
    }

    public boolean isVerifySuperclass() {
        return this.verifySuperclass;
    }

    public void setHasCreationInfo(boolean hasCreationInfo) {
        this.hasCreationInfo = hasCreationInfo;
    }

    public boolean isHasCreationInfo() {
        return this.hasCreationInfo;
    }

    public void setCompareUsingProperties(boolean compareUsingProperties) {
        this.compareUsingProperties = compareUsingProperties;
    }

    public boolean isCompareUsingProperties() {
        return this.compareUsingProperties;
    }

    public void setCompareProperties(List<PropertyModel> compareProperties) {
        this.compareProperties = compareProperties;
    }

    public List<PropertyModel> getCompareProperties() {
        return this.compareProperties;
    }

    public void setToString(String toString) {
        this.toString = toString;
    }

    public String getToString() {
        return this.toString;
    }

    public void setEqualsHashOverride(String equalsHashOverride) {
        this.equalsHashOverride = equalsHashOverride;
    }

    public String getEqualsHashOverride() {
        return this.equalsHashOverride;
    }

}
