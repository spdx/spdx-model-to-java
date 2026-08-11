package org.spdx.tools.model2java.model;

/**
 * Model for the SPDX Model Info class
 */
public class SpdxModelInfoModel extends BaseModel {
    private String versionSuffix;
    private String versionSemVer;
    private String classSuffix;

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

    public void setClassSuffix(String classSuffix) {
        this.classSuffix = classSuffix;
    }

    public String getClassSuffix() {
        return this.classSuffix;
    }
}
