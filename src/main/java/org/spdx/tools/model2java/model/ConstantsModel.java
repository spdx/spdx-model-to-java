package org.spdx.tools.model2java.model;

import java.util.List;

/**
 * Model for the constants files
 */
public class ConstantsModel extends BaseModel {
    private String versionSuffix;
    private List<NamespaceModel> namespaces;
    private List<String> classConstantDefinitions;
    private String allClassConstants;
    private String versionSemVer;

    public String getVersionSuffix() {
        return versionSuffix;
    }
    public void setVersionSuffix(String versionSuffix) {
        this.versionSuffix = versionSuffix;
    }
    public List<NamespaceModel> getNamespaces() {
        return namespaces;
    }
    public void setNamespaces(List<NamespaceModel> namespaces) {
        this.namespaces = namespaces;
    }
    public List<String> getClassConstantDefinitions() {
        return classConstantDefinitions;
    }
    public void setClassConstantDefinitions(List<String> classConstantDefinitions) {
        this.classConstantDefinitions = classConstantDefinitions;
    }
    public String getAllClassConstants() {
        return allClassConstants;
    }
    public void setAllClassConstants(String allClassConstants) {
        this.allClassConstants = allClassConstants;
    }
    public String getVersionSemVer() {
        return versionSemVer;
    }
    public void setVersionSemVer(String versionSemVer) {
        this.versionSemVer = versionSemVer;
    }
}
