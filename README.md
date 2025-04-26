# spdx-model-to-java

Generates Java source files from the [SPDX spec version 3+][spdx-spec]
suitable for inclusion in the SPDX Java Library

[spdx-spec]: https://spdx.dev/use/specifications/

## Usage CLI

To run the utility as a command line interface, execute the main method `ShaclToJavaCli` with 2 parameters:

- model file in turtle format
- output directory

## Usage Library

To use the code as a library, the main entry point is the `ShaclToJava` class which takes a single parameter of the SPDX Ontology model.

The `generate(dir)` method will generate the Java files in the `dir` directory.

The API documentation is available at: <https://spdx.github.io/spdx-model-to-java>

## Development Status

This is a utility specifically built and tested for use in the SPDX Java Library.  
It is relatively stable for that purpose.

There are still a few items marked as TODO in the code and some of the generated code will produce warnings in Java linter (e.g. unused import statements).
