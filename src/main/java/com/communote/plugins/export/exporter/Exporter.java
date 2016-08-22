package com.communote.plugins.export.exporter;

import com.communote.plugins.export.serializer.StreamingSerializer;

/**
 * Exporter for Communote entities.
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 *
 * @param <T>
 *            the Communote entity to export
 */
public interface Exporter<T> {

    /**
     * Export and serialize the entities
     * 
     * @param serializer
     *            serializer to write the entities to export
     * @throws ExportFailedException
     *             in case the export failed
     */
    void export(StreamingSerializer<T> serializer) throws ExportFailedException;
}
