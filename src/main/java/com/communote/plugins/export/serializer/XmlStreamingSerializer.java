package com.communote.plugins.export.serializer;

import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.communote.plugins.export.types.CommunoteExport;

/**
 * Serializer which streams the entities as XML. The entities to process should have proper JAXB
 * annotations.
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 *
 * @param <T>
 *            the type of the entity to process by this
 */
public class XmlStreamingSerializer<T> implements StreamingSerializer<T> {

    private static Logger LOGGER = LoggerFactory.getLogger(XmlStreamingSerializer.class);
    private final Marshaller marshaller;
    private final boolean append;
    private final boolean finalize;
    private XMLStreamWriter streamWriter;
    private final String wrapperElementName;
    private final Class<T> entityType;

    /**
     * Create a new XML serializer
     *
     * @param entityType
     *            the type of the entity to stream
     * @param wrapperElementName
     *            local name of a wrapper element which should encapsulate the
     * @param outputStream
     *            the stream to write to. The stream will never be closed.
     * @param append
     *            whether to append to the stream or start a new XML document with XML declaration
     *            and root element
     * @param finalize
     *            whether to finalize the XML document. If true the root element will be closed.
     *            Note: even if true the stream will not be closed.
     * @throws SerializerInitializationException
     *             in case the serializer cannot be initialized
     */
    public XmlStreamingSerializer(Class<T> entityType, String wrapperElementName,
            OutputStream outputStream, boolean append, boolean finalize)
            throws SerializerInitializationException {
        this.append = append;
        this.finalize = finalize;
        this.entityType = entityType;
        try {
            streamWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(outputStream,
                    "UTF-8");
            this.wrapperElementName = wrapperElementName;
            JAXBContext context = JAXBContext.newInstance(entityType);
            marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        } catch (JAXBException e) {
            throw new SerializerInitializationException("Creating the marshaller failed", e);
        } catch (XMLStreamException | FactoryConfigurationError e) {
            throw new SerializerInitializationException("Creating the output stream failed", e);
        }
    }

    @Override
    public void appendEntity(T entity) throws SerializerWriteException {
        JAXBElement<T> element = new JAXBElement<T>(new QName(entityType.getSimpleName()),
                entityType, entity);
        try {
            marshaller.marshal(element, streamWriter);
        } catch (JAXBException e) {
            throw new SerializerWriteException("Writing entity failed", e);
        }
    }

    /**
     * Close the writer and catch and log exceptions
     */
    private void closeStreamWriter() {
        try {
            streamWriter.close();
        } catch (XMLStreamException e) {
            LOGGER.error("Closing stream writer failed", e);
        }
    }

    @Override
    public void finishSerialization() throws SerializerWriteException {
        try {
            if (finalize) {
                streamWriter.writeEndDocument();
            } else {
                streamWriter.writeEndElement();
            }
            streamWriter.close();
        } catch (XMLStreamException e) {
            closeStreamWriter();
            throw new SerializerWriteException("Writing closing elements failed", e);
        }
    }

    @Override
    public void prepareSerialization() throws SerializerWriteException {
        try {
            if (!append) {
                streamWriter.writeStartDocument();
                streamWriter.writeStartElement(CommunoteExport.class.getSimpleName());
            }
            streamWriter.writeStartElement(wrapperElementName);
        } catch (XMLStreamException e) {
            closeStreamWriter();
            throw new SerializerWriteException("Writing opening elements failed", e);
        }
    }
}
