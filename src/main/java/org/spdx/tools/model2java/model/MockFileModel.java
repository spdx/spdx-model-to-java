package org.spdx.tools.model2java.model;

/**
 * Model for Mock Files in the unit test directory
 */
public class MockFileModel extends BaseModel {
    private String versionSuffix;
    private String specVersion;

    public void setVersionSuffix(String versionSuffix) {
        this.versionSuffix = versionSuffix;
    }

    public String getVersionSuffix() {
        return this.versionSuffix;
    }

    public void setSpecVersion(String specVersion) {
        this.specVersion = specVersion;
    }

    public String getSpecVersion() {
        return this.specVersion;
    }
}
