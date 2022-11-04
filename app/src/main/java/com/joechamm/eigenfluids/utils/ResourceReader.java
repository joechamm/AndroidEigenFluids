/*
 * MIT License
 *
 * Copyright (c) 2022 Joseph Cunningham
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
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

package com.joechamm.eigenfluids.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.res.Resources;

public class ResourceReader {

    public static String readTextFileFromResource (
            Context context, int resourceId
    ) {
        StringBuilder body = new StringBuilder ();

        try {
            InputStream inputStream = context.getResources ().openRawResource ( resourceId );
            InputStreamReader inputStreamReader = new InputStreamReader ( inputStream );
            BufferedReader bufferedReader = new BufferedReader ( inputStreamReader );

            String nextLine;

            while ( ( nextLine = bufferedReader.readLine () ) != null ) {
                body.append ( nextLine );
                body.append ( '\n' );
            }
        } catch ( IOException e ) {
            throw new RuntimeException ( "Could not open resource: " + resourceId, e );
        } catch ( Resources.NotFoundException nfe ) {
            throw new RuntimeException ( "Resource not found: " + resourceId, nfe );
        }

        return body.toString ();
    }

}
