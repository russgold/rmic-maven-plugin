package org.codehaus.mojo.rmic;

import java.io.File;
import java.net.URL;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SourceMapping;

public class RmicTestCase extends AbstractMojoTestCase
{
    private static File DEFAULT_PROJECT_OUTPUT_DIRECTORY = new File( "target/classes" ).getAbsoluteFile();
    private static File DEFAULT_RMIC_OUTPUT_DIRECTORY = new File( "target/rmi-classes" ).getAbsoluteFile();
    private static final ArrayList EMPTY_LIST = new ArrayList();

    private static Set DEFAULT_INCLUDES = newSet( "**/*" );
    private static Set DEFAULT_EXCLUDES = newSet( "**/*_Stub.class" );

    private static Set INCLUDES_A = newSet( "a/b/*" );
    private static Set INCLUDES_B = newSet( "c/d/*" );

    private static Set newSet( String contents )
    {
        HashSet result = new HashSet();
        result.add( contents );
        return result;
    }

    private static List invocations = new ArrayList();

    private final TestDependencies dependencies = new TestDependencies();
    private final RmicMojo mojo = new RmicMojo( dependencies );
    private final ScannableFileSystem fileSystem = new ScannableFileSystem();

    public void setUp() throws Exception
    {
        super.setUp();
        setVariableValueToObject( mojo, "rmiCompiler", new TestCompiler() );
        setSourceDirectory( DEFAULT_PROJECT_OUTPUT_DIRECTORY );
        setTargetDirectory( DEFAULT_RMIC_OUTPUT_DIRECTORY );
        setCompileClasspathElements( EMPTY_LIST );
        mojo.setLog( new TestLog() );
        invocations.clear();
    }

    public void test_withDefaultConfiguration_shouldProcessRemoteClassesOnly() throws MojoExecutionException
    {
        defineDefaultScan();

        mojo.execute();

        assertEquals( 1, invocations.size() );
        Invocation invocation = (Invocation) invocations.get( 0 );
        invocation.assertArgumentsFound( "a.b.RemoteClass1", "a.b.RemoteClass2" );
        invocation.assertArgumentsNotFound( "a.b.RemoteInterface", "a.b.NonRmicClass" );
        invocation.assertArgumentSequenceFound( "-classpath", DEFAULT_PROJECT_OUTPUT_DIRECTORY );
        invocation.assertArgumentSequenceFound( "-d", DEFAULT_RMIC_OUTPUT_DIRECTORY );
    }

    private void defineDefaultScan()
    {
        Set scanResults = new HashSet();
        scanResults.add( defineNonRemoteClass( "a.b.NonRmicClass" ) );
        scanResults.add( defineRemoteInterface( "a.b.RemoteInterface" ) );
        scanResults.add( defineRemoteClass( "a.b.RemoteClass1" ) );
        scanResults.add( defineRemoteClass( "a.b.RemoteClass2" ) );
        fileSystem.defineExpectedScan( DEFAULT_PROJECT_OUTPUT_DIRECTORY, DEFAULT_INCLUDES, DEFAULT_EXCLUDES, scanResults );
    }

    public void test_createClasspath() throws MojoExecutionException, NoSuchFieldException, IllegalAccessException
    {
        defineDefaultScan();
        setCompileClasspathElements( new ArrayList( Arrays.asList( new String[]{"xy"} ) ) );

        mojo.execute();

        assertEquals( 1, invocations.size() );
        Invocation invocation = (Invocation) invocations.get( 0 );
        invocation.assertArgumentSequenceFound( "-classpath",
                "xy" + File.pathSeparator + DEFAULT_PROJECT_OUTPUT_DIRECTORY );
    }

    public void test_withIiopEnabled_shouldProcessRemoteClassesAndInterfaces()
            throws MojoExecutionException, NoSuchFieldException, IllegalAccessException
    {
        defineDefaultScan();

        mojo.setIiop( true );
        mojo.execute();

        assertEquals( "Number of invocations", 1, invocations.size() );
        Invocation invocation = (Invocation) invocations.get( 0 );
        invocation.assertArgumentsFound( "a.b.RemoteClass1", "a.b.RemoteClass2", "a.b.RemoteInterface" );
        invocation.assertArgumentNotFound( "a.b.NonRmicClass" );
        invocation.assertArgumentFound( "-iiop" );
    }

    public void test_withIiopEnabled_maySpecifyNoLocalStubs()
            throws MojoExecutionException, NoSuchFieldException, IllegalAccessException
    {
        defineDefaultScan();

        mojo.setIiop( true );
        mojo.setNoLocalStubs( true );
        mojo.execute();

        assertEquals( "Number of invocations", 1, invocations.size() );
        Invocation invocation = (Invocation) invocations.get( 0 );
        invocation.assertArgumentFound( "-iiop" );
        invocation.assertArgumentFound( "-nolocalstubs" );
    }

    // this is an invalid test, since it combines mojo-options and sources
    public void IGNORE_test_withSourcesSpecified_combineOptions() throws Exception
    {
        defineDefaultScan();

        Source source = addNewSource();
        mojo.setIiop( true );
        source.setNoLocalStubs( true );
        mojo.execute();

        assertEquals( "Number of invocations", 1, invocations.size() );
        Invocation invocation = (Invocation) invocations.get( 0 );
        invocation.assertArgumentFound( "-iiop" );
        invocation.assertArgumentFound( "-nolocalstubs" );
    }

    public void test_withIDLSpecifiedSetIDLSwitch() throws Exception
    {
        defineDefaultScan();

        Source source = addNewSource();
        source.setIdl( true );
        source.setNoValueMethods( true );
        mojo.execute();

        assertEquals( "Number of invocations", 1, invocations.size() );
        Invocation invocation = (Invocation) invocations.get( 0 );
        invocation.assertArgumentFound( "-idl" );
        invocation.assertArgumentFound( "-noValueMethods" );
    }

    public void test_withKeepAndWarnSpecifiedSetSwitches() throws Exception
    {
        defineDefaultScan();

        Source source = addNewSource();
        source.setKeep( true );
        source.setNowarn( true );
        mojo.execute();

        assertEquals( "Number of invocations", 1, invocations.size() );
        Invocation invocation = (Invocation) invocations.get( 0 );
        invocation.assertArgumentFound( "-keep" );
        invocation.assertArgumentFound( "-nowarn" );
    }

    public void test_withVerboseAndVersionSpecifiedSetSwitches() throws Exception
    {
        defineDefaultScan();

//        Source source = addNewSource();
//        enableVerbose( source );
        mojo.setVerbose( true );
        mojo.setVersion( "1.2" );
        mojo.execute();

        assertEquals( "Number of invocations", 1, invocations.size() );
        Invocation invocation = (Invocation) invocations.get( 0 );
        invocation.assertArgumentFound( "-verbose" );
        invocation.assertArgumentFound( "-v1.2" );
    }

    public void test_withPoaSpecifiedAndNotIiop_throwException() throws Exception
    {
        defineDefaultScan();

        mojo.setPoa( true );
        try
        {
            mojo.execute();
            fail( "No exception thrown" );
        }
        catch ( MojoExecutionException e )
        {
            // expect this
        }
    }

    public void test_withPoaSpecifiedSetSwitches() throws Exception
    {
        defineDefaultScan();

        mojo.setIiop( true );
        mojo.setPoa( true );
        mojo.execute();

        assertEquals( "Number of invocations", 1, invocations.size() );
        Invocation invocation = (Invocation) invocations.get( 0 );
        invocation.assertArgumentFound( "-poa" );
    }

    // this is an invalid test, since it combines mojo-options and sources
    public void IGNORE_test_withTwoSourcesInvokeTwice() throws Exception
    {
        Set scanResults1 = new HashSet();
        scanResults1.add( defineNonRemoteClass( "a.b.NonRmicClass" ) );
        scanResults1.add( defineRemoteInterface( "a.b.RemoteInterface" ) );
        scanResults1.add( defineRemoteClass( "a.b.RemoteClass1" ) );
        scanResults1.add( defineRemoteClass( "a.b.RemoteClass2" ) );
        Set scanResults2 = new HashSet();
        scanResults2.add( defineNonRemoteClass( "c.d.NonRmicClass" ) );
        scanResults2.add( defineRemoteInterface( "c.d.RemoteInterface" ) );
        scanResults2.add( defineRemoteClass( "c.d.RemoteClass1" ) );
        scanResults2.add( defineRemoteClass( "c.d.RemoteClass2" ) );
        fileSystem.defineExpectedScan( DEFAULT_PROJECT_OUTPUT_DIRECTORY, INCLUDES_A, DEFAULT_EXCLUDES, scanResults1 );
        fileSystem.defineExpectedScan( DEFAULT_PROJECT_OUTPUT_DIRECTORY, INCLUDES_B, DEFAULT_EXCLUDES, scanResults2 );

        Source source1 = addNewSource();
        defineIncludes( source1, INCLUDES_A );

        Source source2 = addNewSource();
        defineIncludes( source2, INCLUDES_B );
        source2.setPoa( true );
        mojo.setIiop( true );
        mojo.execute();

        assertEquals( "Number of invocations", 2, invocations.size() );
        Invocation invocation1 = (Invocation) invocations.get( 0 );
        invocation1.assertArgumentsFound( "a.b.RemoteClass1", "a.b.RemoteClass2" );
        invocation1.assertArgumentsNotFound( "c.d.RemoteClass1", "-poa" );

        Invocation invocation2 = (Invocation) invocations.get( 1 );
        invocation2.assertArgumentsFound( "c.d.RemoteClass1", "c.d.RemoteClass2" );
        invocation2.assertArgumentsNotFound( "a.b.RemoteClass1", "a.b.RemoteClass2" );
        invocation2.assertArgumentFound( "-poa" );
    }

    private File defineNonRemoteClass( String className )
    {
        return defineClassFile( DEFAULT_PROJECT_OUTPUT_DIRECTORY, className, NonRmicClass.class );
    }

    private File defineRemoteClass( String className )
    {
        return defineClassFile( DEFAULT_PROJECT_OUTPUT_DIRECTORY, className, RmicClass.class );
    }

    private File defineRemoteInterface( String className )
    {
        return defineClassFile( DEFAULT_PROJECT_OUTPUT_DIRECTORY, className, RmicInterface.class );
    }

    private File defineClassFile( File root, String className, Class aClass )
    {
        return fileSystem.defineFile( new File( root, toClassFileName( className ) ), aClass );
    }

    private String toClassFileName( String className )
    {
        return className.replace( '.', '/' ) + ".class";
    }

    private Source addNewSource() throws NoSuchFieldException, IllegalAccessException
    {
        List sources = mojo.getSources();
        if ( sources == null )
        {
            sources = new ArrayList();
            setVariableValueToObject( mojo, "sources", sources );
        }
        Source source = new Source();
        sources.add( source );
        return source;
    }

    private void setSourceDirectory( File directory ) throws NoSuchFieldException, IllegalAccessException
    {
        setVariableValueToObject( mojo, "classesDirectory", directory );
    }

    private void defineIncludes( Source config, Set includes ) throws NoSuchFieldException,
                                                                          IllegalAccessException
    {
        setVariableValueToObject( config, "includes", includes );
    }

    private void setTargetDirectory( File directory ) throws NoSuchFieldException, IllegalAccessException
    {
        setVariableValueToObject( mojo, "outputDirectory", directory );
    }

    private void setCompileClasspathElements( List elements ) throws NoSuchFieldException, IllegalAccessException
    {
        setVariableValueToObject( mojo, "projectCompileClasspathElements", elements );
    }


    static class Invocation
    {
        private List arguments;

        Invocation( String[] arguments )
        {
            this.arguments = Arrays.asList( arguments );
        }

        private void assertArgumentSequenceFound( Object argument1, Object argument2 )
        {
            assertTrue( "Did not find sequence:" + argument1 + "," + argument2, contains( argument1.toString(),
                    argument2.toString() ) );
        }

        private void assertArgumentNotFound( String argument )
        {
            assertFalse( contains( argument ) );
        }

        private void assertArgumentsNotFound( String arg1, String arg2 )
        {
            assertArgumentNotFound( arg1 );
            assertArgumentNotFound( arg2 );
        }

        private void assertArgumentFound( String argument )
        {
            assertTrue( "Did not find: " + argument, contains( argument ) );
        }

        private void assertArgumentsFound( String arg1, String arg2 )
        {
            assertArgumentFound( arg1 );
            assertArgumentFound( arg2 );
        }

        private void assertArgumentsFound( String arg1, String arg2, String arg3 )
        {
            assertArgumentFound( arg1 );
            assertArgumentFound( arg2 );
            assertArgumentFound( arg3 );
        }

        boolean contains( String argument )
        {
            return arguments.contains( argument );
        }

        boolean contains( String argument1, String argument2 )
        {
            int i = arguments.indexOf( argument1 );
            return i >= 0 && i < arguments.size() - 1 && arguments.get( i + 1 ).equals( argument2 );
        }

        public String toString()
        {
            return "Invocation{" +
                    "arguments=" + arguments +
                    '}';
        }
    }

    static class ExpectedScan
    {
        private File root;
        private Set includes;
        private Set excludes;
        private Set results;

        ExpectedScan( File root, Set includes, Set excludes, Set results )
        {
            this.root = root;
            this.includes = includes;
            this.excludes = excludes;
            this.results = results;
        }

        boolean isExpectedScan( File root, Set includes, Set excludes )
        {
            return root.equals( this.root ) && includes.equals( this.includes ) && excludes.equals( this.excludes );
        }
    }

    class ScannableFileSystem
    {
        Map files = new HashMap();
        List scans = new ArrayList();

        private Set getFiles( File root, Set includes, Set excludes )
        {
            for ( Iterator i = scans.iterator(); i.hasNext(); )
            {
                ExpectedScan scan = (ExpectedScan) i.next();
                if ( scan.isExpectedScan( root, includes, excludes ) )
                {
                    return scan.results;
                }
            }
            fail( "Unexpected call: scanner.getIncludesSources( " + root + ", " + includes + ", " + excludes + ")" );
            return null;
        }


        private Object getFileContents( File file )
        {
            return files.get( file );
        }

        private File defineFile( File file, Object contents )
        {
            files.put( file.getAbsoluteFile(), contents );
            return file;
        }

        public void defineExpectedScan( File root, Set includes, Set excludes, Set scanResults )
        {
            scans.add( new ExpectedScan( root, includes, excludes, scanResults ) );
        }
    }

    static class TestCompiler extends SunRmiCompiler
    {
        @Override
        protected void compileInProcess( String[] args, CompilerConfiguration config )
            throws CompilerException
        {
            invocations.add( new Invocation( args ) );
        }
    }

    class TestScanner implements SourceInclusionScanner
    {
        private Set includes;
        private Set excludes;
        private List mappings = new ArrayList();

        TestScanner( Set includes, Set excludes )
        {
            this.includes = includes;
            this.excludes = excludes;
        }

        public void addSourceMapping( SourceMapping sourceMapping )
        {
            mappings.add( sourceMapping );
        }

        public Set getIncludedSources( File source, File target ) throws InclusionScanException
        {
            return fileSystem.getFiles( source, includes, excludes );
        }
    }

    class TestDependencies implements AbstractRmiMojo.DependenciesFacade
    {
        private URL[] classpathUrls;

        public boolean fileExists( File includeFile )
        {
            return false;
        }

        public SourceInclusionScanner createSourceInclusionScanner( int staleMillis, Set includes, Set excludes )
        {
            return new TestScanner( includes, excludes );
        }

        public Class loadClass( String className ) throws ClassNotFoundException
        {
            for ( int i = 0; i < classpathUrls.length; i++ )
            {
                Object object = fileSystem.getFileContents( new File( toFile( classpathUrls[i] ), toClassFileName( className ) ) );
                if ( object instanceof Class )
                {
                    return (Class) object;
                }
            }
            throw new ClassNotFoundException( className );
        }

        private File toFile( URL url )
        {
            return new File( url.getPath() );
        }

        public void defineUrlClassLoader( URL[] classpathUrls )
        {
            this.classpathUrls = classpathUrls;
        }
    }

    static class NonRmicClass
    {
    }

    static class RmicClass implements Remote
    {
    }

    static interface RmicInterface extends Remote
    {
    }


    static class TestLog implements org.apache.maven.plugin.logging.Log
    {
        public boolean isDebugEnabled()
        {
            return false;
        }

        public void debug( CharSequence charSequence )
        {
        }

        public void debug( CharSequence charSequence, Throwable throwable )
        {
        }

        public void debug( Throwable throwable )
        {
        }

        public boolean isInfoEnabled()
        {
            return false;
        }

        public void info( CharSequence charSequence )
        {
        }

        public void info( CharSequence charSequence, Throwable throwable )
        {
        }

        public void info( Throwable throwable )
        {
        }

        public boolean isWarnEnabled()
        {
            return false;
        }

        public void warn( CharSequence charSequence )
        {
        }

        public void warn( CharSequence charSequence, Throwable throwable )
        {
        }

        public void warn( Throwable throwable )
        {
        }

        public boolean isErrorEnabled()
        {
            return false;
        }

        public void error( CharSequence charSequence )
        {
        }

        public void error( CharSequence charSequence, Throwable throwable )
        {
        }

        public void error( Throwable throwable )
        {
        }
    }


}
