package org.spdx.tools.model2java.model;

import java.util.List;

/**
 * Model for the ModelObject class
 */
public class ModelObjectModel extends BaseModel {
    private List<String> createBuilder;
    private String versionSuffix;
    private String versionSemVer;
    private List<String> imports;

    public void setCreateBuilder(List<String> createBuilder) {
        this.createBuilder = createBuilder;
    }

    public List<String> getCreateBuilder() {
        return this.createBuilder;
    }

    public void setVersionSuffix(String versionSuffix) {
        this.versionSuffix = versionSuffix;
    }

    public String getVersionSuffix() {
        return this.versionSuffix;
    }

    public void setVersionSemVer(String versionSemVer) {
        this.versionSemVer = versionSemVer;
    }

    public String getVersionSemVer() {
        return this.versionSemVer;
    }

    public void setImports(List<String> imports) {
        this.imports = imports;
    }

    public List<String> getImports() {
        return this.imports;
    }
}
