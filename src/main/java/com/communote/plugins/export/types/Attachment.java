package com.communote.plugins.export.types;

import javax.xml.bind.annotation.XmlElement;

/**
 * Pojo holding details of an attachment file.
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 *
 */
public class Attachment {
    private String displayName;
    private String fileName;
    private long size;
    private String mimeType;

    @XmlElement(required = true)
    public String getDisplayName() {
        return displayName;
    }

    @XmlElement(required = true)
    public String getFileName() {
        return fileName;
    }

    @XmlElement(required = true)
    public String getMimeType() {
        return mimeType;
    }

    @XmlElement(required = true)
    public long getSize() {
        return size;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
