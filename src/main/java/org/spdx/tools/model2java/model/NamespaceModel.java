package org.spdx.tools.model2java.model;

import java.util.List;

/**
 * Model for a single namespace in the constants file
 */
public class NamespaceModel extends BaseModel {
    private String namespaceName;
    private String namespaceConstantName;
    private String namespaceUri;
    private List<PropertyDescriptorModel> propertyDescriptors;

    public String getNamespaceName() {
        return namespaceName;
    }
    public void setNamespaceName(String namespaceName) {
        this.namespaceName = namespaceName;
    }
    public String getNamespaceConstantName() {
        return namespaceConstantName;
    }
    public void setNamespaceConstantName(String namespaceConstantName) {
        this.namespaceConstantName = namespaceConstantName;
    }
    public String getNamespaceUri() {
        return namespaceUri;
    }
    public void setNamespaceUri(String namespaceUri) {
        this.namespaceUri = namespaceUri;
    }
    public List<PropertyDescriptorModel> getPropertyDescriptors() {
        return propertyDescriptors;
    }
    public void setPropertyDescriptors(List<PropertyDescriptorModel> propertyDescriptors) {
        this.propertyDescriptors = propertyDescriptors;
    }
}
