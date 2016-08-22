package com.communote.plugins.export.types;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

/**
 * Pojo holding the details of a topic.
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 *
 */
public class Topic extends IdentifiableEntity {

    private String alias;
    private String title;
    private String description;
    private Date creationDate;
    private Collection<String> tags;
    private List<TopicMember> members;
    private TopicMember.Role allUsersRole;

    @XmlElement(required = true)
    public String getAlias() {
        return alias;
    }

    @XmlElement(required = true)
    public TopicMember.Role getAllUsersRole() {
        return allUsersRole;
    }

    @XmlElement(required = true)
    public Date getCreationDate() {
        return creationDate;
    }

    @XmlElement
    public String getDescription() {
        return description;
    }

    @XmlElementWrapper
    @XmlElement(name = "TopicMember")
    public List<TopicMember> getMembers() {
        return members;
    }

    @XmlElementWrapper
    @XmlElement(name = "Tag")
    public Collection<String> getTags() {
        return tags;
    }

    @XmlElement
    public String getTitle() {
        return title;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setAllUsersRole(TopicMember.Role allUsersRole) {
        this.allUsersRole = allUsersRole;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setMembers(List<TopicMember> members) {
        this.members = members;
    }

    public void setTags(Collection<String> tags) {
        this.tags = tags;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
