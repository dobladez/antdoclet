package com.neuroning.antdoclet;

import java.io.File;
import java.io.OutputStreamWriter;

import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.RootDoc;

/**
 * AntDoclet Main class
 * 
 * This doclet generates documentation and other deliverables from the source
 * code of ant Tasks and Types.
 * 
 * It uses template-based generation to make it easy to create new deliverables
 * or modify the ones provided.
 * 
 * @author Fernando Dobladez <dobladez@gmail.com>
 */
public class AntDoclet extends com.sun.javadoc.Doclet {

    /**
     * Processes the JavaDoc documentation.
     * 
     * @param root The root of the documentation tree.
     * @return True if processing was succesful.
     * @see com.sun.java.Doclet
     */
    public static boolean start(RootDoc root) {

        // Get some options
        String title = "My Ant Tasks";
        String[] templates = null;
        String templatesDir = ".";
        String[] outputdirs = new String[] { "."};

        String[][] options = root.options();
        for (int opt = 0; opt < options.length; opt++) {
            if (options[opt][0].equalsIgnoreCase("-doctitle")) {
                title = options[opt][1];
            } else if (options[opt][0].equalsIgnoreCase("-templates")) {
                templates = options[opt][1].split(","); // comma-separated
                                                        // filenames
            } else if (options[opt][0].equalsIgnoreCase("-templatesdir")) {
                templatesDir = options[opt][1]; // comma-separated filenames
            } else if (options[opt][0].equalsIgnoreCase("-d")) {
                outputdirs = options[opt][1].split(",");
            }
        }

        // Init Velocity-template Generator
        VelocityFacade velocity = null;
        try {
            velocity = new VelocityFacade(new File("."), templatesDir);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set global parameters to the templates
        velocity.setAttribute("velocity", velocity);
        velocity.setAttribute("title", title);
        velocity.setAttribute("antroot", new AntRoot(root));

        for (int i = 0; i < templates.length; i++) {
            try {
                if (outputdirs.length > i)
                    velocity.setOutputDir(new File(outputdirs[i]));
                velocity.eval(templates[i], new OutputStreamWriter(System.out));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    /**
     * A JavaDoc option parsing handler. This one returns the number of
     * arguments required for the given option.
     * 
     * @param option
     *            The name of the option.
     * @return The number of arguments.
     * @see com.sun.java.Doclet
     */
    public static int optionLength(String option) {
        // Check for the output option and then return that it requires two
        // arguments,
        // itself and the file name.

        if (option.equalsIgnoreCase("-output"))
            return 2;
        else if (option.equalsIgnoreCase("-doctitle"))
            return 2;
        else if (option.equalsIgnoreCase("-templates"))
            return 2;
        else if (option.equalsIgnoreCase("-templatesdir"))
            return 2;
        else if (option.equalsIgnoreCase("-d"))
            return 2;

        return 0;
    }

    /**
     * A JavaDoc option parsing handler. This one checks the validity of the
     * options.
     * 
     * @param options
     *            The two dimensional array of options.
     * @param reporter
     *            The error reporter.
     * @return True if the options are valid.
     * @see com.sun.java.Doclet
     */
    public static boolean validOptions(String options[][],
            DocErrorReporter reporter) {
        // TODO: do some actual validation of the arguments :)
        return true;
    }

    
}
