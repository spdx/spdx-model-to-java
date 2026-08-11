package org.spdx.tools.model2java.model;

import java.util.List;

/**
 * Model object backing the enum mustache template.
 */
public class EnumModel extends BaseModel {
    private String year;
    private String pkgName;
    private String classComment;
    private String name;
    private String classUri;
    private List<String> enumValues;

    public String getYear() {
        return year;
    }
    public void setYear(String year) {
        this.year = year;
    }
    public String getPkgName() {
        return pkgName;
    }
    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
    }
    public String getClassComment() {
        return classComment;
    }
    public void setClassComment(String classComment) {
        this.classComment = classComment;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getClassUri() {
        return classUri;
    }
    public void setClassUri(String classUri) {
        this.classUri = classUri;
    }
    public List<String> getEnumValues() {
        return enumValues;
    }
    public void setEnumValues(List<String> enumValues) {
        this.enumValues = enumValues;
    }
}