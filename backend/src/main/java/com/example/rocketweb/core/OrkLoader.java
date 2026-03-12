package com.example.rocketweb.core;

import net.sf.openrocket.document.OpenRocketDocument;
import net.sf.openrocket.file.openrocket.OpenRocketLoader;
import net.sf.openrocket.utils.ORLoaderWithoutSimulation;
import net.sf.openrocket.startup.Application;
import net.sf.openrocket.startup.GuiModule;
import net.sf.openrocket.plugin.PluginModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import net.sf.openrocket.file.DatabaseMotorFinder;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Loads .ork files into OpenRocketDocument objects.
 * Initializes the OpenRocket environment on first use.
 */
@Component
public class OrkLoader {

    private volatile boolean initialized = false;

    /**
     * Ensures the OpenRocket core environment is initialized.
     * This must be called before any OpenRocket-core API is used.
     */
    private synchronized void ensureInitialized() {
        if (!initialized) {
            Injector injector = Guice.createInjector(new GuiModule(), new PluginModule());
            Application.setInjector(injector);
            initialized = true;
        }
    }

    /**
     * Loads an .ork file and returns the corresponding OpenRocketDocument.
     *
     * @param file the .ork file to load
     * @return parsed OpenRocketDocument
     * @throws Exception if the file cannot be read or parsed
     */
    public OpenRocketDocument load(File file) throws Exception {
        ensureInitialized();
        try (InputStream stream = new FileInputStream(file)) {
            OpenRocketLoader loader = new OpenRocketLoader();
            return loader.loadFromStream(stream, new DatabaseMotorFinder());
        }
    }
}
