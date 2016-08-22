package com.communote.plugins.export.exporter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.communote.plugins.activity.base.service.ActivityService;
import com.communote.plugins.export.serializer.SerializerWriteException;
import com.communote.plugins.export.serializer.StreamingSerializer;
import com.communote.plugins.export.types.Attachment;
import com.communote.plugins.export.types.IdentifiableEntity;
import com.communote.plugins.export.types.Like;
import com.communote.plugins.export.types.Note;
import com.communote.server.api.ServiceLocator;
import com.communote.server.api.core.property.PropertyManagement;
import com.communote.server.core.blog.NoteManagement;
import com.communote.server.core.blog.notes.processors.RepostNoteStoringPreProcessor;
import com.communote.server.core.crc.FilesystemConnector;
import com.communote.server.core.crc.RepositoryConnectorDelegate;
import com.communote.server.core.crc.vo.ContentId;
import com.communote.server.core.vo.content.AttachmentFileTO;
import com.communote.server.core.vo.content.AttachmentTO;
import com.communote.server.model.note.NoteConstants;
import com.communote.server.model.note.NoteProperty;
import com.communote.server.model.note.NoteStatus;
import com.communote.server.model.user.User;
import com.communote.server.model.user.UserNoteProperty;
import com.communote.server.persistence.crc.ContentRepositoryException;
import com.communote.server.persistence.user.UserNotePropertyDao;

/**
 * Exporter for notes with attachments, likes and bookmarks.
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 *
 */
public class NoteExporter implements Exporter<Note> {

    private static final String EXPORT_ATTACHMENTS_APPEND_DISPLAY_NAME = "com.communote.plugins.export.attachments.append.displayname";
    private UserNotePropertyDao propertyDao;
    private RepositoryConnectorDelegate repoConnectorManager;
    private final File attachmentTargetDir;
    private final Pattern invalidFilenameCharsRegex;
    private boolean appendDisplayName;

    /**
     * Create an exporter
     *
     * @param attachmentTargetDir
     *            directory where to store the attachments. The directory must exist.
     */
    public NoteExporter(File attachmentTargetDir) {
        this.attachmentTargetDir = attachmentTargetDir;
        // characters that are not supported in windows (ntfs), should cover those of default linux
        // filesystems too
        invalidFilenameCharsRegex = Pattern.compile("[\\/:*?\"<>|]");
        String displayNameProperty = System.getProperty(EXPORT_ATTACHMENTS_APPEND_DISPLAY_NAME);
        if (displayNameProperty != null) {
            this.appendDisplayName = Boolean.parseBoolean(displayNameProperty);
        } else {
            this.appendDisplayName = true;
        }
    }

    private boolean containsProperty(Iterable<NoteProperty> properties, String keyGroup,
            String key, String value) {
        String propertyValue = getPropertyValue(properties, keyGroup, key);
        if (propertyValue != null) {
            return propertyValue.equals(value);
        }
        return false;
    }

    /**
     * Convert the note database entity to the POJO. This will also export tags, likes, bookmarks
     * and attachments.
     *
     * @param source
     *            the note to convert
     * @param tagsConverter
     *            the
     * @param session
     *            the session with which the provided note was loaded so it can be reused to load
     *            additional data
     * @return the note POJO
     * @throws ExportFailedException
     *             in case the export of attachments failed
     */
    private Note convert(com.communote.server.model.note.Note source,
            TagToStringConverter tagsConverter, Session session) throws ExportFailedException {
        Note target = new Note();
        target.setActivity(containsProperty(source.getProperties(),
                ActivityService.PROPERTY_KEY_GROUP, ActivityService.NOTE_PROPERTY_KEY_ACTIVITY,
                ActivityService.NOTE_PROPERTY_VALUE_ACTIVITY));
        target.setAuthorId(source.getUser().getId());
        target.setContent(source.getContent().getContent());
        target.setCreationDate(new Date(source.getCreationDate().getTime()));
        target.setDirectMessage(source.isDirect());
        target.setDiscussionId(source.getDiscussionId());
        target.setId(source.getId());
        target.setLikes(getLikes(source, session));
        target.setModificationDate(new Date(source.getLastModificationDate().getTime()));
        target.setMentions(convertUsersToIdentifiableEntities(source.getUsersToBeNotified()));
        if (source.getParent() != null) {
            target.setParentNoteId(source.getParent().getId());
        }
        String repostNoteId = getPropertyValue(source.getProperties(),
                PropertyManagement.KEY_GROUP, RepostNoteStoringPreProcessor.KEY_ORIGIN_NOTE_ID);
        if (repostNoteId != null) {
            try {
                target.setRepostNoteId(Long.parseLong(repostNoteId));
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        if (source.getTags() != null) {
            target.setTags(tagsConverter.convert(source.getTags()));
        }
        target.setBookmarks(convertUsersToIdentifiableEntities(source.getFavoriteUsers()));
        target.setTopicId(source.getBlog().getId());
        target.setAttachments(exportAttachments(source));
        return target;
    }

    private Collection<IdentifiableEntity> convertUsersToIdentifiableEntities(Set<User> users) {
        if (users == null) {
            return null;
        }
        HashSet<IdentifiableEntity> entities = new HashSet<>();
        for (User user : users) {
            if (Utils.isUserActiveOrDisabled(user)) {
                entities.add(new IdentifiableEntity(user.getId()));
            }
        }
        return entities;
    }

    private String escapeInvalidCharactersInFilename(String filename) {
        // TODO on FAT32 this replacement is probably not enough when the name contains non-ascii
        // (or non-default-code-page) characters
        filename = invalidFilenameCharsRegex.matcher(filename).replaceAll("_");
        filename = filename.trim();
        if (filename.charAt(filename.length() - 1) == '.') {
            filename += "_";
        }
        return filename;
    }

    @Override
    public void export(StreamingSerializer<Note> serializer) throws ExportFailedException {
        try {
            serializer.prepareSerialization();
            exportNotes(serializer);
            serializer.finishSerialization();
        } catch (SerializerWriteException e) {
            throw new ExportFailedException("Exporting the notes failed", e);
        }
    }

    /**
     * Export attachments of the given note by copying all assigned attachments that are stored in
     * the internal FileRepository to the attachmentTargetDir. For each copied file an Attachment
     * POJO is created which holds the metadata and the file name name of the copied file.
     * Attachments that are stored in other repositories are not exported.
     *
     * @param source
     *            the note whose attachments should be exported
     * @return the attachment POJOs
     * @throws ExportFailedException
     *             in case the attachments could not be copied
     */
    private Collection<Attachment> exportAttachments(com.communote.server.model.note.Note source)
            throws ExportFailedException {
        if (source.getAttachments() == null) {
            return null;
        }
        ArrayList<Attachment> exportedAttachments = new ArrayList<>();
        for (com.communote.server.model.attachment.Attachment attachment : source.getAttachments()) {
            // shortcut: use repo connector and not (inefficiently implemented)
            // ResourceStoringManagement
            RepositoryConnectorDelegate repoManager = getRepoConnectorManager();
            AttachmentTO attachmentTO;
            try {
                // only handle attachments stored in file system repo
                if (FilesystemConnector.DEFAULT_FILESYSTEM_CONNECTOR.equals(attachment
                        .getRepositoryIdentifier())) {
                    attachmentTO = repoManager.getContent(new ContentId(attachment
                            .getContentIdentifier(), attachment.getRepositoryIdentifier()));
                    if (attachmentTO instanceof AttachmentFileTO) {
                        String filename = getTargetFilename(attachment);
                        FileUtils.copyFile(((AttachmentFileTO) attachmentTO).getFile(), new File(
                                attachmentTargetDir, filename));
                        Attachment target = new Attachment();
                        target.setDisplayName(attachment.getName());
                        target.setFileName(filename);
                        target.setMimeType(attachment.getContentType());
                        target.setSize(attachment.getSize());
                        exportedAttachments.add(target);
                    }
                }
            } catch (ContentRepositoryException | IOException e) {
                throw new ExportFailedException(
                        "Exporting the notes failed because the attachments of note "
                                + source.getId() + " cannot be exported.", e);
            }

        }
        return exportedAttachments;
    }

    private void exportNotes(StreamingSerializer<Note> serializer) throws SerializerWriteException,
            ExportFailedException {
        SessionFactory sessionFactory = ServiceLocator.findService(SessionFactory.class);
        Session session = sessionFactory.openSession();
        session.setCacheMode(CacheMode.GET);
        TagToStringConverter tagsConverter = new TagToStringConverter();
        try {
            try {
                Criteria query = session.createCriteria(com.communote.server.model.note.Note.class);
                query.add(Restrictions.eq(NoteConstants.STATUS, NoteStatus.PUBLISHED));
                query.addOrder(Order.asc(NoteConstants.ID));
                query.setReadOnly(true);
                // TODO MySQL will load the result set completely into memory, this can be tuned
                // with query.setFetchSize(Integer.MIN_VALUE) which leads to loading the results row
                // by row. But setting this fetch-size causes an exception with PostgreSQL.
                ScrollableResults notes = query.scroll(ScrollMode.FORWARD_ONLY);
                long count = 0l;
                while (notes.next()) {
                    com.communote.server.model.note.Note note = (com.communote.server.model.note.Note) notes
                            .get(0);
                    serializer.appendEntity(convert(note, tagsConverter, session));
                    if (++count % 20 == 0) {
                        session.flush();
                        session.clear();
                    }
                }
                // TODO necessary? Guess it is, because we do not have a transaction which when
                // closed would take care of closing the results
                notes.close();
            } finally {
                session.close();
            }
        } catch (HibernateException e) {
            throw new ExportFailedException("Exporting the notes failed", e);
        }

    }

    private List<Like> getLikes(com.communote.server.model.note.Note source, Session session) {
        // directly using DAO and not PropertyManagement for performance reasons and avoiding to
        // fill cache with properties of old notes
        Collection<UserNoteProperty> properties = getUserNotePropertyDao().findProperties(
                source.getId(), PropertyManagement.KEY_GROUP,
                NoteManagement.USER_NOTE_PROPERTY_KEY_LIKE);
        if (properties != null) {
            ArrayList<Like> likes = new ArrayList<Like>();
            for (UserNoteProperty property : properties) {
                if (property.getPropertyValue().equals(Boolean.TRUE.toString())) {
                    // properties are loaded by DAO with another hibernate session, the assigned
                    // users are therefore detached and must be loaded to access the properties
                    User user = (User) session.get(User.class, property.getUser()
                            .getId());
                    if (Utils.isUserActiveOrDisabled(user)) {
                        likes.add(new Like(property.getUser().getId(), property
                                .getLastModificationDate()));
                    }
                }
            }
            if (likes.size() > 0) {
                return likes;
            }
        }
        return null;
    }

    private String getPropertyValue(Iterable<NoteProperty> properties, String keyGroup, String key) {
        // deliberately bypassing PropertyManagement for performance reasons
        if (properties != null) {
            for (NoteProperty property : properties) {
                if (property.getKeyGroup().equals(keyGroup)
                        && property.getPropertyKey().equals(key)) {
                    return property.getPropertyValue();
                }
            }
        }
        return null;
    }

    private RepositoryConnectorDelegate getRepoConnectorManager() {
        if (repoConnectorManager == null) {
            repoConnectorManager = ServiceLocator.findService(RepositoryConnectorDelegate.class);
        }
        return repoConnectorManager;
    }

    /**
     * Get the name of the file in the export of the provided attachment
     *
     * @param attachment
     *            the attachment to export
     * @return the name of the file of the exported attachment
     */
    private String getTargetFilename(com.communote.server.model.attachment.Attachment attachment) {
        if (this.appendDisplayName) {
            return attachment.getContentIdentifier() + "-"
                    + escapeInvalidCharactersInFilename(attachment.getName());
        }
        return attachment.getContentIdentifier();
    }

    /**
     * @return the lazily initialized DAO
     */
    private UserNotePropertyDao getUserNotePropertyDao() {
        if (this.propertyDao == null) {
            this.propertyDao = ServiceLocator.findService(UserNotePropertyDao.class);
        }
        return this.propertyDao;
    }

}
