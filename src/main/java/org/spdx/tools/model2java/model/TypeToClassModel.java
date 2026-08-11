package org.spdx.tools.model2java.model;

/**
 * Holds a type to class mapping for the ModelClassFactory model
 */
public class TypeToClassModel extends BaseModel {
    private String classConstant;
    private String classPath;

    public String getClassConstant() {
        return classConstant;
    }
    public void setClassConstant(String classConstant) {
        this.classConstant = classConstant;
    }
    public String getClassPath() {
        return classPath;
    }
    public void setClassPath(String classPath) {
        this.classPath = classPath;
    }
}
