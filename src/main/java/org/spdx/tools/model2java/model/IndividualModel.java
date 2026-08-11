package org.spdx.tools.model2java.model;

/**
 * Model for Individual declarations
 */
public class IndividualModel extends BaseModel {
    private String individualUri;
    private String className;

    public void setIndividualUri(String individualUri) {
        this.individualUri = individualUri;
    }

    public String getIndividualUri() {
        return this.individualUri;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassName() {
        return this.className;
    }
}
