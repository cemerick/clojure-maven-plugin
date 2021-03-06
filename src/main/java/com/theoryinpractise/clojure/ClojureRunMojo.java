/*
 * Copyright (c) Mark Derricutt 2010.
 *
 * The use and distribution terms for this software are covered by the Eclipse Public License 1.0
 * (http://opensource.org/licenses/eclipse-1.0.php) which can be found in the file epl-v10.html
 * at the root of this distribution.
 *
 * By using this software in any fashion, you are agreeing to be bound by the terms of this license.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.theoryinpractise.clojure;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @goal run
 * @requiresDependencyResolution compile
 */
public class ClojureRunMojo extends AbstractClojureCompilerMojo {

    /**
     * The main clojure script to run
     *
     * @parameter expression="${clojure.script}"
     * @required
     */
    private String script;

    /**
     * Additional scripts to run
     *
     * @parameter
     */
    private String[] scripts;

    /**
     * args specified on the command line.
     *
     * @parameter expression="${clojure.args}"
     */
    private String args;

    /**
     * Returns either a path to a temp file that loads all of the provided scripts,
     * or simply returns the singular <code>script</code> String (which therefore allows
     * for @ classpath-loading paths to be passed in as a script).
     *
     * If multiple scripts are defined, they must all exist; otherwise an exception is thrown.
     */
    private static String mergeScripts (String script, String[] scripts) throws MojoExecutionException {
        if (script == null || script.trim().equals("")) {
            throw new MojoExecutionException("<script> is undefined");
        }
        if (scripts == null) {
            return script;
        } else if (scripts.length == 0) {
            throw new MojoExecutionException("<scripts> is defined but has no <script> entries");
        }

        List<String> paths = new ArrayList<String>();
        paths.add(script);

        paths.addAll(Arrays.asList(scripts));
        for (String scriptFile : paths) {
            if (scriptFile == null || scriptFile.trim().equals("")) {
                throw new MojoExecutionException("<script> entry cannot be empty");
            }
            if (!(new File(scriptFile).exists())) {
                throw new MojoExecutionException(scriptFile + " cannot be found");
            }
        }

        try {
            File testFile = File.createTempFile("run", ".clj");
            final FileWriter writer = new FileWriter(testFile);

            for (String scriptFile : paths) {
                writer.write("(load-file \"" + scriptFile + "\")");
                writer.write(System.getProperty("line.separator"));
            }
            writer.close();
            return testFile.getPath();
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    public void execute() throws MojoExecutionException {
        String path = mergeScripts(script, scripts);

        try {
            List<String> clojureArguments = new ArrayList<String>();
            clojureArguments.add(path);

            if (args != null) {
                clojureArguments.addAll(Arrays.asList(args.split(" ")));
            }

            getLog().debug("Running clojure:run against " + path);

            callClojureWith(
                    getSourceDirectories(SourceDirectory.COMPILE),
                    outputDirectory, getRunWithClasspathElements(), "clojure.main",
                    clojureArguments.toArray(new String[clojureArguments.size()]));

        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
