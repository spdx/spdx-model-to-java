package org.spdx.tools.model2java.model;

/**
 * Model for the InvalidLicenseExpression class
 */
public class InvalidLicenseExpressionModel extends BaseModel {
    private String versionSuffix;
    private String versionSemVer;

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
}
