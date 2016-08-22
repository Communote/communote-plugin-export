package com.communote.plugins.export.serializer;

/**
 * Serializer to serialize an arbitrary number of entities of a given type and write the result to a
 * stream.
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 *
 * @param <T>
 *            the type of the entity to serialize
 */
public interface StreamingSerializer<T> {

    /**
     * Serialize the entity and append it to the stream.
     *
     * @param entity
     *            the entity to serialize
     * @throws SerializerWriteException
     *             in case serialization or writing failed
     */
    void appendEntity(T entity) throws SerializerWriteException;

    /**
     * Should be called after all entities were appended. Can be used to write any finalizing data.
     *
     * @throws SerializerWriteException
     *             in case writing failed
     */
    void finishSerialization() throws SerializerWriteException;

    /**
     * Should be called before appending entities. Can be used to write any introducing data.
     *
     * @throws SerializerWriteException
     *             in case writing failed
     */
    void prepareSerialization() throws SerializerWriteException;
}
