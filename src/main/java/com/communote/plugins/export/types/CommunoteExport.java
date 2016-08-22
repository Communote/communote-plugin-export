package com.communote.plugins.export.types;

import java.util.Collection;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Helper which is only used for XSD-Schema generation
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 *
 */
@XmlRootElement(name = "CommunoteExport")
public class CommunoteExport {
    @XmlElementWrapper
    @XmlElement(name = "User")
    private Collection<User> users;
    @XmlElementWrapper
    @XmlElement(name = "Group")
    private Collection<Group> groups;
    @XmlElementWrapper
    @XmlElement(name = "GroupMember")
    private Collection<GroupMember> groupMembers;
    @XmlElementWrapper
    @XmlElement(name = "Topic")
    private Collection<Topic> topics;
    @XmlElementWrapper
    @XmlElement(name = "Note")
    private Collection<Note> notes;
    @XmlElementWrapper
    @XmlElement(name = "Follow")
    private Collection<Follow> follows;
}
