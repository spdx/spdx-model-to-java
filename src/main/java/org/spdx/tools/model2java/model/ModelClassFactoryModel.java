package org.spdx.tools.model2java.model;

import java.util.List;

/**
 * Model for the ModelClassFactory file
 */
public class ModelClassFactoryModel extends BaseModel {
    private String versionSuffix;
    private List<TypeToClassModel> typeToClass;

    public String getVersionSuffix() {
        return versionSuffix;
    }
    public void setVersionSuffix(String versionSuffix) {
        this.versionSuffix = versionSuffix;
    }
    public List<TypeToClassModel> getTypeToClass() {
        return typeToClass;
    }
    public void setTypeToClass(List<TypeToClassModel> typeToClass) {
        this.typeToClass = typeToClass;
    }
}
