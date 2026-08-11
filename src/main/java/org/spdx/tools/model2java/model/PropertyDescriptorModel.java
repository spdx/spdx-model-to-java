package org.spdx.tools.model2java.model;

/**
 * Property descriptors used in the Java constants model
 */
public class PropertyDescriptorModel extends BaseModel {
    private String propertyConstantName;
    private String propertyConstantValue;

    public String getPropertyConstantName() {
        return propertyConstantName;
    }
    public void setPropertyConstantName(String propertyConstantName) {
        this.propertyConstantName = propertyConstantName;
    }
    public String getPropertyConstantValue() {
        return propertyConstantValue;
    }
    public void setPropertyConstantValue(String propertyConstantValue) {
        this.propertyConstantValue = propertyConstantValue;
    }
}
