/* *********************************************************************
 * ECE351 
 * Department of Electrical and Computer Engineering 
 * University of Waterloo 
 * Term: Fall 2021 (1219)
 *
 * The base version of this file is the intellectual property of the
 * University of Waterloo. Redistribution is prohibited.
 *
 * By pushing changes to this file I affirm that I am the author of
 * all changes. I affirm that I have complied with the course
 * collaboration policy and have not plagiarized my work. 
 *
 * I understand that redistributing this file might expose me to
 * disciplinary action under UW Policy 71. I understand that Policy 71
 * allows for retroactive modification of my final grade in a course.
 * For example, if I post my solutions to these labs on GitHub after I
 * finish ECE351, and a future student plagiarizes them, then I too
 * could be found guilty of plagiarism. Consequently, my final grade
 * in ECE351 could be retroactively lowered. This might require that I
 * repeat ECE351, which in turn might delay my graduation.
 *
 * https://uwaterloo.ca/secretariat-general-counsel/policies-procedures-guidelines/policy-71
 * 
 * ********************************************************************/

package ece351.w.rdescent;

import ece351.util.Lexer;

public final class WRecursiveDescentRecognizer {
    private final Lexer lexer;

    public WRecursiveDescentRecognizer(final Lexer lexer) {
        this.lexer = lexer;
    }

    public static void recognize(final String input) {
    	final WRecursiveDescentRecognizer r = new WRecursiveDescentRecognizer(new Lexer(input));
        r.recognize();
    }

    /**
     * Throws an exception to reject.
     */
    public void recognize() {
        program();
    }

    /**
     * What is the termination condition of the loop in program()?
     * Will this condition be met if the waveform() method does nothing?
     */
    public void program() {
        waveform();
        while (!lexer.inspectEOF()) {
            waveform();
        }
        lexer.consumeEOF();
    }

    public void waveform() {
        // TODO: longer code snippet
        //this is checking to see if the first element is the pin name
        if(lexer.inspectID()){
            lexer.consumeID();
        }

        // //looking for a semicolon
        if(lexer.inspect(":")){
            lexer.consume(":");
        }

        // //looking for a sequence of 0 or 1's
        while (!lexer.inspect(";")) {
            //if we have zero consume zero
    		if (lexer.inspect("0")) {
        		lexer.consume("0");
        	}
            //since its not a zero that means it has to be a 1 thus we consume a 1
            else{
                lexer.consume("1");
            }
    	}

        //look for the last character at end the recognizing
        //if it fails it will through an error
        lexer.consume(";");
    }
}
