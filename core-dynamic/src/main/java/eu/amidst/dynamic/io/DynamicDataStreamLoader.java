/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package eu.amidst.dynamic.io;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.filereaders.DataFileReader;
import eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.datastream.filereaders.DynamicDataStreamFromFile;

import java.lang.reflect.UndeclaredThrowableException;

/**
 * This class allows to load a Dynamic Data Stream from disk.
 */
public class DynamicDataStreamLoader {

    /** Represents the class name of the different loaders available in the toolbox*/
    private static String[] loaders = { "eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataReader",
                                        "eu.amidst.core.datastream.filereaders.arffFileReader.ARFFDataFolderReader"};

    /** Represents the data file reader. */
    private static DataFileReader dataFileReader = new ARFFDataReader();  //ARFFDataFolderReader;

    /**
     * Loads a {@link DataStream} of {@link DynamicDataInstance} from a given file.
     * @param path the path of the file from which the dynamic data stream will be loaded.
     * @return a {@link DataStream} of {@link DynamicDataInstance}.
     */
    public static DataStream<DynamicDataInstance> loadFromFile(String path){
        dataFileReader = selectRightLoader(path);
        dataFileReader.loadFromFile(path);
        return new DynamicDataStreamFromFile(dataFileReader);
    }


    /**
     * Gets the suitable DataFileReader according the name of the file.
     * @param fileName, the name of the file/folder
     * @return A valid {@link DataFileReader}.
     */
    private static DataFileReader selectRightLoader(String fileName){
        try{
            for (String loaderName : loaders) {
                DataFileReader reader = (DataFileReader) Class.forName(loaderName).newInstance();
                if (reader.doesItReadThisFile(fileName))
                    return reader;
            }
        } catch (Exception ex){
            throw new UndeclaredThrowableException(ex);
        }

        throw new IllegalArgumentException("The provided file name is not compatible with any of the available file readers");
    }
}
