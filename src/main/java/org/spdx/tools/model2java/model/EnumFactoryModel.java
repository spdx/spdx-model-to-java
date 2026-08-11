package org.spdx.tools.model2java.model;

import java.util.List;

public class EnumFactoryModel extends BaseModel {
    private List<EnumModel> enumClasses;
    private List<String> imports;
    private String versionSuffix;

    public void setEnumClasses(List<EnumModel> enumClasses) {
        this.enumClasses = enumClasses;
    }

    public List<EnumModel> getEnumClasses() {
        return this.enumClasses;
    }

    public void setImports(List<String> imports) {
        this.imports = imports;
    }

    public List<String> getImports() {
        return this.imports;
    }

    public void setVersionSuffix(String versionSuffix) {
        this.versionSuffix = versionSuffix;
    }

    public String getVersionSuffix() {
        return this.versionSuffix;
    }
}
