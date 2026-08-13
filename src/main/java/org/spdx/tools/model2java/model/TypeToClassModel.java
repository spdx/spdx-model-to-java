package org.spdx.tools.model2java.model;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TypeToClassModel)) {
            return false;
        }
        return Objects.equals(((TypeToClassModel) o).getClassConstant(), this.getClassConstant()) &&
                Objects.equals(((TypeToClassModel) o).getClassPath(), this.getClassPath());
    }

    @Override
    public int hashCode() {
        String s1 = Objects.isNull(this.getClassConstant()) ? "" : this.getClassConstant();
        String s2 = Objects.isNull(this.getClassPath()) ? "" : this.getClassPath();
        return s1.hashCode() ^ s2.hashCode();
    }
}
