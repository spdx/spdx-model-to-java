package org.spdx.tools.model2java;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import org.spdx.tools.model2java.model.BaseClassModel;
import org.spdx.tools.model2java.model.IndividualClassModel;
import org.spdx.tools.model2java.model.JavaClassModel;
import org.spdx.tools.model2java.model.UnitTestModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Generates Java code based on a collection of <code>SpecVersionContainer</code>s.
 */
public class JavaCodeGenerator {

    public static final String VERSION_SUFFIX = "v3";

    private final List<SpecVersionContainer> specVersions;
    private final List<String> warnings = new ArrayList<>();

    public JavaCodeGenerator(List<SpecVersionContainer> specVersions) {
        Objects.requireNonNull(specVersions);
        if (specVersions.isEmpty()) {
            throw new RuntimeException("No spec versions provided");
        }
        this.specVersions = specVersions;
        Collections.sort(this.specVersions);
    }

    private String toQualifiedName(BaseClassModel classModel) {
        Objects.requireNonNull(classModel.getPkgName());
        Objects.requireNonNull(classModel.getClassName());
        return classModel.getPkgName() + "." + classModel.getClassName();
    }


    private List<IndividualClassModel> mergeIndividuals() {
        // Start with the lastest version
        List<IndividualClassModel> retval = this.specVersions.get(this.specVersions.size()-1).getIndividuals();
        Set<String> addedIndividuals = new HashSet<>();
        for (IndividualClassModel individual : retval) {
            addedIndividuals.add(toQualifiedName(individual));
        }
        for (int i = this.specVersions.size()-2; i >= 0; i--) {
            List<IndividualClassModel> individualsToMerge = this.specVersions.get(i).getIndividuals();
            for (IndividualClassModel individual : individualsToMerge) {
                String qualifiedClassName = toQualifiedName(individual);
                if (!addedIndividuals.contains(qualifiedClassName)) {
                    addedIndividuals.add(qualifiedClassName);
                    retval.add(individual);
                    warnings.add(String.format("Latest spec version does not contain the individual %s from spec version %s",
                            qualifiedClassName, this.specVersions.get(i).getSpecVersion()));
                }
            }
        }
        return retval;
    }

    /**
     * Generates source and test files and store them in the dir
     * @param dir Directory to hold the java source
     * @return list of warnings - if empty, all files were generated successfully
     * @throws IOException for any issues storing the files
     * @throws ShaclToJavaException errors in the ontology
     */
    public List<String> generate(File dir) throws IOException, ShaclToJavaException {

        List<IndividualClassModel> individuals = mergeIndividuals();
        for (IndividualClassModel individual : individuals) {
            File sourceFile = createJavaSourceFile((String)individual.getIndividualUri(), individual.getClassName(), dir);
            writeMustacheFile(ShaclToJavaConstants.INDIVIDUAL_CLASS_TEMPLATE, sourceFile, individual);
        }



        return warnings;
    }

    /**
     * @param classUri URI for the class
     * @param dir directory to hold the file
     * @return the created file
     * @throws IOException on IO errors
     */
    private File createJavaSourceFile(String classUri, String className, File dir) throws IOException {
        Path path = dir.toPath().resolve("src").resolve("main").resolve("java").resolve("org")
                .resolve("spdx").resolve("library").resolve("model").resolve(VERSION_SUFFIX);
        String[] parts = classUri.substring(ShaclToJavaConstants.SPDX_URI_PREFIX.length()).split("/");
        // [0] is version, [1] is "terms"
        for (int i = 2; i < parts.length-1; i++) {
            path = path.resolve(parts[i].toLowerCase());
        }
        Files.createDirectories(path);
        File retval = path.resolve(className + ".java").toFile();
        if (!retval.createNewFile()) {
            throw new IOException("IO Error creating new file");
        }
        return retval;
    }

    private void writeMustacheFile(String templateName, File file, Object mustacheMap) throws IOException {
        String templateDirName = ShaclToJavaConstants.TEMPLATE_ROOT_PATH;
        File templateDirectoryRoot = new File(templateDirName);
        if (!(templateDirectoryRoot.exists() && templateDirectoryRoot.isDirectory())) {
            templateDirName = ShaclToJavaConstants.TEMPLATE_CLASS_PATH;
        }
        DefaultMustacheFactory builder = new DefaultMustacheFactory(templateDirName);
        Mustache mustache = builder.compile(templateName);
        FileOutputStream stream = null;
        OutputStreamWriter writer = null;
        try {
            stream = new FileOutputStream(file);
            writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
            mustache.execute(writer, mustacheMap);
        } finally {
            if (writer != null) {
                writer.close();
            }
            if (stream != null) {
                stream.close();
            }
        }
    }


    private void generateUnitTest(String classUri, UnitTestModel stringObjectMap) {
        //TODO: Implement
    }

    private void generateExternalJavaClass(String classUri, JavaClassModel stringObjectMap) {
        //TODO: Implement
    }


    private void generateJavaClass(String classUri, JavaClassModel stringObjectMap) {
        //TODO: Implement
    }

}
