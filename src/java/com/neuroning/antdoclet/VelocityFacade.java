package com.neuroning.antdoclet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;

/**
 * A "facade" to the Velocity template engine
 * 
 *  @author Fernando Dobladez <dobladez@gmail.com>
 */
public class VelocityFacade {

  
    private VelocityEngine velocity;
    private File outputDir;
    private Context context;
    
    /**
     * @param outputDir directory for output
     */
    public VelocityFacade(File outputDir, String templatesDir) throws Exception 
    {
      initVelocityEngine(templatesDir);
      this.outputDir = outputDir;
      this.context = new VelocityContext();
    }
    
    /**
     * Create and initialize a VelocityEngine 
     */
    private void initVelocityEngine(String templatesDir) throws Exception 
    {
      velocity = new VelocityEngine();
      velocity.setProperty("resource.loader", "file, class");
      velocity.setProperty( "file.resource.loader.path", templatesDir != null ? ".,"+templatesDir : "."); // default "file" loader      
      velocity.init();
     }
    
    public File getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(File outdir) {
        this.outputDir = outdir;
    }

    /**
     * Get a Writer to the specified File
     * @param file a File
     */
    protected FileWriter getFileWriter( String fileName ) throws IOException { 
        File file = new File( getOutputDir(), fileName );
        file.getParentFile().mkdirs();
        return new FileWriter( file );
    }

    
    /**
     * Get the evaluation-context used by this generator
     */
    public Context getContext() {
        return context;
    }

    /**
     * Add something to the Generator's evaluation-context
     */
    public void setAttribute( String key, Object value ) {
        context.put( key, value );
    }

    /**
     * Evaluate a template.
     * @param templateName the name of the template
     * @param writer output destination
     * @param context merge context
     */
    void merge( String templateName, Writer writer, Context context )
    {
        try {
            Template template = this.velocity.getTemplate( templateName );
            template.merge( context, writer );
            writer.flush();
            
        } catch (MethodInvocationException e) {
            Throwable cause = e.getWrappedThrowable();
 
            if (cause == null) {
                cause = e;
            }
            throw new RuntimeException( "Error invoking $" + e.getReferenceName() + 
                                          "." + e.getMethodName() + "() in \"" +
                                          templateName + "\"", 
                                          cause );
        } catch (Exception e) {
            throw new RuntimeException( "Error parsing \"" + templateName + "\"", 
                                          e );
        }
    }

    /**
     * Evaluate a Velocity template.
     * @param templateName name of the template
     * @param writer output destination
     */
    public void eval( String templateName, Writer writer )
        throws IOException
    {
        merge( templateName, writer, getContext() );
    }

    /**
     * Evaluate a Velocity template.
     * @param templateName name of the template
     * @param fileName name of output file
     */
    public void eval( String templateName, String fileName )
        throws IOException
    {
        FileWriter writer = getFileWriter(fileName);
        eval( templateName, writer );
        writer.close();
    }
    
    public Object create(String clazz) throws Exception
    {
            return Class.forName( clazz ).newInstance();
    }
    
    
    
}
