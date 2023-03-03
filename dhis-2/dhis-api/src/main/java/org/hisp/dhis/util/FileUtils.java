/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
public class FileUtils
{
    private FileUtils()
    {
        throw new IllegalStateException( "Utility class" );
    }

    /**
     * Reads given resource file as a string.
     *
     * @param fileName path to the resource file
     * @return the file's contents
     * @throws IOException if read fails for any reason
     */
    public static String getResourceFileAsString( String fileName )
        throws IOException
    {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try ( InputStream is = classLoader.getResourceAsStream( fileName ) )
        {
            if ( is == null )
                return null;

            try ( InputStreamReader isr = new InputStreamReader( is );
                BufferedReader reader = new BufferedReader( isr ) )
            {
                return reader.lines().collect( Collectors.joining( System.lineSeparator() ) );
            }
        }
    }

    /**
     * Deletes the given file if it exists.
     *
     * @param file the file to delete
     */
    public static void cleanUp( File file )
    {
        if ( file == null )
        {
            return;
        }

        try
        {
            Files.deleteIfExists( file.toPath() );
        }
        catch ( IOException e )
        {
            log.warn( String.format( "Temporary file '%s' could not be deleted.", file.toPath() ), e );
        }
    }
}