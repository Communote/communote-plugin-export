package com.communote.plugins.export.types;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

/**
 * Pojo holding the details of a note.
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 *
 */
public class Note extends IdentifiableEntity {

    private long topicId;
    private long authorId;
    private boolean directMessage;
    private boolean activity;
    private long discussionId;
    private Long parentNoteId;
    private Long repostNoteId;
    private Date creationDate;
    private Date modificationDate;
    private Collection<String> tags;
    private String content;
    private Collection<IdentifiableEntity> mentions;
    private Collection<IdentifiableEntity> bookmarks;
    private List<Like> likes;
    private Collection<Attachment> attachments;

    @XmlElementWrapper
    @XmlElement(name = "Attachment")
    public Collection<Attachment> getAttachments() {
        return attachments;
    }

    @XmlAttribute(required = true)
    public long getAuthorId() {
        return authorId;
    }

    @XmlElementWrapper
    @XmlElement(name = "BookmarkingUser")
    public Collection<IdentifiableEntity> getBookmarks() {
        return bookmarks;
    }

    @XmlElement
    public String getContent() {
        return content;
    }

    @XmlElement(required = true)
    public Date getCreationDate() {
        return creationDate;
    }

    @XmlAttribute(required = true)
    public long getDiscussionId() {
        return discussionId;
    }

    @XmlElementWrapper
    @XmlElement(name = "Like")
    public List<Like> getLikes() {
        return likes;
    }

    @XmlElementWrapper
    @XmlElement(name = "MentionedUser")
    public Collection<IdentifiableEntity> getMentions() {
        return mentions;
    }

    @XmlElement(required = true)
    public Date getModificationDate() {
        return modificationDate;
    }

    @XmlAttribute
    public Long getParentNoteId() {
        return parentNoteId;
    }

    @XmlAttribute
    public Long getRepostNoteId() {
        return repostNoteId;
    }

    @XmlElementWrapper
    @XmlElement(name = "Tag")
    public Collection<String> getTags() {
        return tags;
    }

    @XmlAttribute(required = true)
    public long getTopicId() {
        return topicId;
    }

    @XmlAttribute
    public boolean isActivity() {
        return activity;
    }

    @XmlAttribute
    public boolean isDirectMessage() {
        return directMessage;
    }

    public void setActivity(boolean activity) {
        this.activity = activity;
    }

    public void setAttachments(Collection<Attachment> attachments) {
        this.attachments = attachments;
    }

    public void setAuthorId(long authorId) {
        this.authorId = authorId;
    }

    public void setBookmarks(Collection<IdentifiableEntity> bookmarks) {
        this.bookmarks = bookmarks;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public void setDirectMessage(boolean directMessage) {
        this.directMessage = directMessage;
    }

    public void setDiscussionId(long discussionId) {
        this.discussionId = discussionId;
    }

    public void setLikes(List<Like> likes) {
        this.likes = likes;
    }

    public void setMentions(Collection<IdentifiableEntity> mentions) {
        this.mentions = mentions;
    }

    public void setModificationDate(Date modificationDate) {
        this.modificationDate = modificationDate;
    }

    public void setParentNoteId(Long parentNoteId) {
        this.parentNoteId = parentNoteId;
    }

    public void setRepostNoteId(Long repostNoteId) {
        this.repostNoteId = repostNoteId;
    }

    public void setTags(Collection<String> tags) {
        this.tags = tags;
    }

    public void setTopicId(long topicId) {
        this.topicId = topicId;
    }
}
