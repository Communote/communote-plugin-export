package com.communote.plugins.export.exporter;

import com.communote.common.converter.CollectionConverter;
import com.communote.server.model.tag.Tag;

/**
 * Converter that turns tags into strings by extracting the default name
 * 
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 *
 */
public class TagToStringConverter extends CollectionConverter<Tag, String> {

    @Override
    public String convert(Tag source) {
        return source.getDefaultName();
    }

}
