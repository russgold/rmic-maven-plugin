package org.codehaus.mojo.rmic;

/*
 * Copyright (c) 2012, Codehaus.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a group of class files to be processed by rmic, along with the desired options.
 */
public class Source
{

    private static final String INCLUDE_ALL = "**/*";
    private AbstractRmiMojo mojo;

    /**
     * A list of inclusions when searching for classes to compile.
     *
     * @parameter
     */
    protected Set<String> includes;

    /**
     * A list of exclusions when searching for classes to compile.
     *
     * @parameter
     */
    protected Set<String> excludes;

    /**
     * The version of the rmi protocol to which the stubs should be compiled. Valid values include 1.1, 1.2, compat. See
     * the rmic documentation for more information. If nothing is specified the underlying rmi compiler will
     * choose the default value.  For example, in sun jdk 1.5 the default is 1.2.
     *
     * @parameter
     */
    private String version;

    /**
     * Create stubs for IIOP.
     *
     * @parameter
     */
    private Boolean iiop;

    /**
     * Do not create stubs optimized for same process.
     *
     * @parameter
     */
    private Boolean noLocalStubs;

    /**
     * Create IDL.
     *
     * @parameter default-value="false"
     */
    private Boolean idl;

    /**
     * Do not generate methods for valuetypes.
     *
     * @parameter
     */
    private Boolean noValueMethods;

    /**
     * Do not delete intermediate generated source files.
     *
     * @parameter
     */
    private Boolean keep;

    /**
     * Turn off rmic warnings.
     *
     * @parameter
     */
    private Boolean nowarn;

    /**
     * Enable poa generation.
     *
     * @parameter
     */
    private Boolean poa;

    /**
     * Enable verbose output.
     *
     * @parameter
     */
    private Boolean verbose;


    public boolean isIiop()
    {
        return isSetOrDefault( iiop, mojo != null && mojo.isIiop() );
    }

    public boolean isNoLocalStubs()
    {
        return isSetOrDefault( noLocalStubs, mojo != null && mojo.isNoLocalStubs() );
    }

    public boolean isIdl()
    {
        return isSetOrDefault( idl, mojo != null && mojo.isIdl() );
    }

    public boolean isNoValueMethods()
    {
        return isSetOrDefault( noValueMethods, mojo != null && mojo.isNoValueMethods() );
    }

    public boolean isKeep()
    {
        return isSetOrDefault( keep, mojo != null && mojo.isKeep() );
    }

    public boolean isNowarn()
    {
        return isSetOrDefault( nowarn, mojo != null && mojo.isNowarn() );
    }

    public boolean isPoa()
    {
        return isSetOrDefault( poa, mojo != null && mojo.isPoa() );
    }

    public boolean isVerbose()
    {
        return isSetOrDefault( verbose, mojo != null && mojo.isVerbose() );
    }

    public String getVersion()
    {
        return version != null ? version : (mojo == null ? null : mojo.getVersion());
    }

    private static boolean isSetOrDefault( Boolean field, boolean defaultValue )
    {
        return field != null ? field.booleanValue() : defaultValue;
    }

    void setRmiMojo( AbstractRmiMojo mojo )
    {
        this.mojo = mojo;
    }

    Set<String> getIncludes()
    {
        return !isEmpty( includes ) ? includes
                : (mojo == null || isEmpty( mojo.includes )) ? createOneElementSet( INCLUDE_ALL ) : mojo.includes;
    }

    Set<String> getExcludes()
    {
        return !isEmpty( excludes ) ? excludes
                : (mojo == null || isEmpty( mojo.excludes )) ? new HashSet<String>() : mojo.excludes;
    }

    private static HashSet createOneElementSet( Object element )
    {
        return new HashSet( Arrays.asList( new Object[]{element} ) );
    }

    private static boolean isEmpty( Collection collection )
    {
        return collection == null || collection.isEmpty();
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append( "Including " ).append( getIncludes() ).append( "; excluding " ).append( getExcludes() );
        sb.append( "\nwith options: " );
        appendIfTrue( sb, isIiop(), "-iiop" );
        appendIfTrue( sb, isIiop() && isNoLocalStubs(), "-noLocalStubs" );
        appendIfTrue( sb, isIdl(), "-idl" );
        appendIfTrue( sb, isIdl() && isNoValueMethods(), "-noValueMethods" );
        appendIfTrue( sb, isKeep(), "-keep" );
        appendIfTrue( sb, isNowarn(), "-nowarn" );
        appendIfTrue( sb, isPoa(), "-poa" );

        if ( getVersion() != null )
        {
            sb.append( "-v" ).append( getVersion() );
        }
        return sb.toString();
    }

    private void appendIfTrue( StringBuffer sb, boolean condition, String option )
    {
        if ( condition )
        {
            sb.append( option ).append( ' ' );
        }
    }

    public AbstractRmiMojo getMojo()
    {
        return mojo;
    }

    public void setMojo( AbstractRmiMojo mojo )
    {
        this.mojo = mojo;
    }

    public void setIncludes( Set<String> includes )
    {
        this.includes = includes;
    }

    public void setExcludes( Set<String> excludes )
    {
        this.excludes = excludes;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public void setIiop( Boolean iiop )
    {
        this.iiop = iiop;
    }

    public void setNoLocalStubs( Boolean noLocalStubs )
    {
        this.noLocalStubs = noLocalStubs;
    }

    public void setIdl( Boolean idl )
    {
        this.idl = idl;
    }

    public void setNoValueMethods( Boolean noValueMethods )
    {
        this.noValueMethods = noValueMethods;
    }

    public void setKeep( Boolean keep )
    {
        this.keep = keep;
    }

    public void setNowarn( Boolean nowarn )
    {
        this.nowarn = nowarn;
    }

    public void setPoa( Boolean poa )
    {
        this.poa = poa;
    }

    public void setVerbose( Boolean verbose )
    {
        this.verbose = verbose;
    }

    
    
}
