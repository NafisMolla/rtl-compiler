# RTL-compiler

### Description of the `WParboiledParser` File

The `WParboiledParser` class is designed to parse W language programs using the Parboiled parsing library for Java. This parser extends `BaseParser351` and is annotated with `@BuildParseTree` to build a parse tree. The main method reads input from a file specified in the command-line arguments and processes it using the `WParboiledParser`. The `parse` method constructs an abstract syntax tree (AST) for a W program from the given input text, primarily for debugging purposes. The top-level production in the grammar is defined by the `Program` method, which starts parsing by pushing a new `WProgram` onto the stack and processing one or more waveforms until the end of input (EOI). Each line of the input W file represents a pin in the circuit, processed by the `Waveform` method, which extracts the pin name and bit string, creating a `Waveform` object and appending it to the `WProgram`. The `Name` method defines the grammar for a pin name, which is a sequence of letters, digits, or underscores, while the `Letter` method matches any alphabetical character. The `BitString` method defines the grammar for a sequence of values (bits) for a pin, and the `Bit` method matches a single bit (0 or 1) and appends it to the current `Waveform`. Additionally, the file includes utility methods for class loading (`findLoadedClass` and `loadClass`) using Java's MethodHandles API, showcasing advanced features and flexibility in dynamic class management. The file begins with a header comment emphasizing the importance of adhering to academic integrity policies and the potential consequences of policy violations.


## Notable features
### Description of the `elaborate` and `desugar` Functions

The `elaborate` function processes and transforms an abstract syntax tree (AST) of a hardware description program. It starts by converting command-line arguments into a `VProgram` using the `desugar` function. This `VProgram` is then recursively elaborated by initializing an `Elaborator` instance, which calls the `elaborateit` method to process the AST. During this transformation, the `elaborateit` method constructs a new `VProgram` by iterating over `DesignUnit`s, mapping inputs and outputs, and modifying internal signals. Helper methods such as `expandProcessComponent`, `changeIfVars`, and `changeStatementVars` are used to handle different types of statements within the architecture, ensuring that all components and signals are correctly updated and consistent.

The `desugar` function simplifies the program’s AST by expanding syntactic sugar into more fundamental constructs. This involves converting complex syntactic constructs into simpler ones, making the program easier to process and elaborate. By breaking down the initial program representation into a more basic form, the `desugar` function sets the stage for the `elaborate` function to perform its detailed processing. Together, these functions enable the creation of a detailed, lower-level representation of the program, suitable for further analysis or execution.


### Description of the `Splitter` Class

The `Splitter` class is a process splitter designed to transform a `VProgram` by dividing processes with `if/else` statements into multiple processes, ensuring each `if/else` statement assigns values to only one pin. The main method begins by desugaring the input program and then elaborating it to create a more detailed abstract syntax tree (AST). The core functionality is in the `splitit` method, which iterates over design units and their statements, splitting any processes containing `if/else` statements. If a process is identified, it further breaks down the `if/else` statements into separate processes using the `splitIfElseStatement` helper method. This method checks if the output variables of `if` and `else` bodies are the same, builds a sensitivity list from used variables in expressions, and creates new processes with updated statements. The resulting list of processes is then appended to the design unit. The class also overrides the `visitVar` method to track used variables in expressions, which helps in building sensitivity lists. Other visit methods for various expression types are provided as no-ops, maintaining the structure of the AST without modifications. The `Splitter` class ultimately ensures a well-structured, split representation of processes in the `VProgram`, facilitating more efficient analysis and execution of the hardware description.



### Description of the `Synthesizer` Class

The `Synthesizer` class translates VHDL to F, focusing on transforming VHDL programs into an equivalent F representation. The process starts with the `main` method, which desugars the input program and then splits it using the `Splitter` class. The `synthesize` method is then called to perform the translation. The core functionality resides in the `synthesizeit` method, which iterates over design units and their statements, handling both `Process` and non-`Process` statements. For `Process` statements, it further examines sequential statements and, if they include `IfElseStatement`s, it uses the `implication` method to transform them into F assignment statements. 

The `implication` function is central to this transformation. It begins with error checking to ensure that each `IfElseStatement` has exactly one assignment statement in both the if-body and else-body, and that the output variables in both bodies are identical. If these conditions are not met, it throws an `IllegalArgumentException`. Once validated, the function constructs a new `FProgram` by incrementing a condition counter and creating a new condition variable with a unique name. This condition variable is assigned the result of the condition expression from the `IfElseStatement`.

The function then creates the logical expressions for the if and else branches. For the if branch, it creates an `AndExpr` that combines the condition variable with the expression from the if-body. For the else branch, it creates an `AndExpr` that combines the negation of the condition variable with the expression from the else-body. These two expressions are then combined into an `OrExpr`, representing the overall conditional logic.

Finally, the `implication` function forms a new `AssignmentStatement` using the combined logical expression and appends it to the `FProgram`. This process ensures that the conditional logic of the VHDL `IfElseStatement` is correctly translated into the F language, preserving the intended behavior of the original VHDL program. The `Synthesizer` class, through the `implication` function, effectively converts VHDL constructs into their F equivalents, ensuring a smooth transition between these two hardware description languages.
