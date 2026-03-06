# spdx-model-to-java

Generates Java source files from the [SPDX spec version 3+][spdx-spec]
suitable for inclusion in the SPDX Java Library

[spdx-spec]: https://spdx.dev/use/specifications/

## Usage CLI

To run the utility as a command line interface, execute the main method `ShaclToJavaCli` with 2 parameters:

- input directory of model files in turtle format - there should be one model file per version to be generated
- output directory

## Usage Library

To use the code as a library, the main entry point is the `ShaclToJava` class which takes two parameters:

- SPDX Ontology model for the code to be generated
- SPDX Ontology model for the previous version of the model - this will provide the proper class hierarchy for compatibility

The `generate(dir)` method will generate the Java files in the `dir` directory.

The API documentation is available at: <https://spdx.github.io/spdx-model-to-java>

## Development Status

This is a utility specifically built and tested for use in the SPDX Java Library.  
It is relatively stable for that purpose.

There are still a few items marked as TODO in the code and some of the generated code will produce warnings in Java linter (e.g. unused import statements).
