package com.example.rocketweb.core;

import net.sf.openrocket.document.OpenRocketDocument;
import net.sf.openrocket.file.openrocket.OpenRocketLoader;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class OrkLoader {

    /**
     * Loads a .ork file and returns an OpenRocketDocument.
     *
     * @param file the .ork file to load
     * @return the loaded OpenRocketDocument
     * @throws Exception if the file cannot be loaded
     */
    public OpenRocketDocument load(File file) throws Exception {
        OpenRocketLoader loader = new OpenRocketLoader();
        return loader.load(file);
    }
}
