package com.example.rocketweb.core;

import net.sf.openrocket.document.OpenRocketDocument;
import net.sf.openrocket.file.openrocket.OpenRocketSaver;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Serializes an OpenRocketDocument back to the .ork (ZIP/XML) file format.
 */
@Component
public class OrkWriter {

    /**
     * Writes the given OpenRocketDocument to the specified file in .ork format.
     *
     * @param document the document to serialize
     * @param file     destination file (will be created or overwritten)
     * @throws IOException if writing fails
     */
    public void write(OpenRocketDocument document, File file) throws IOException {
        try (OutputStream out = new FileOutputStream(file)) {
            OpenRocketSaver saver = new OpenRocketSaver();
            saver.save(out, document, null);
        }
    }
}
