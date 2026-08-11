package org.spdx.tools.model2java.model;

import java.util.List;

/**
 * Model for the IndividualFactory class
 */
public class IndividualFactoryModel extends BaseModel {
    private String versionSuffix;
    private List<IndividualModel> individuals;
    private List<String> imports;

    public void setVersionSuffix(String versionSuffix) {
        this.versionSuffix = versionSuffix;
    }

    public String getVersionSuffix() {
        return this.versionSuffix;
    }

    public void setIndividuals(List<IndividualModel> individuals) {
        this.individuals = individuals;
    }

    public List<IndividualModel> getIndividuals() {
        return this.individuals;
    }

    public void setImports(List<String> imports) {
        this.imports = imports;
    }

    public List<String> getImports() {
        return this.imports;
    }
}
