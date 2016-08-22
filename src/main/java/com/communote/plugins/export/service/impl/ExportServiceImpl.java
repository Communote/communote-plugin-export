package com.communote.plugins.export.service.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;

import com.communote.common.io.IOHelper;
import com.communote.plugins.core.services.PluginPropertyService;
import com.communote.plugins.export.exporter.ExportFailedException;
import com.communote.plugins.export.exporter.Exporter;
import com.communote.plugins.export.exporter.FollowExporter;
import com.communote.plugins.export.exporter.GroupExporter;
import com.communote.plugins.export.exporter.GroupMemberExporter;
import com.communote.plugins.export.exporter.NoteExporter;
import com.communote.plugins.export.exporter.TopicExporter;
import com.communote.plugins.export.exporter.UserExporter;
import com.communote.plugins.export.exporter.Utils;
import com.communote.plugins.export.serializer.SerializerInitializationException;
import com.communote.plugins.export.serializer.XmlStreamingSerializer;
import com.communote.plugins.export.service.ExportNotPlannedException;
import com.communote.plugins.export.service.ExportService;
import com.communote.plugins.export.service.ExportStatus;
import com.communote.plugins.export.task.ExportEntitiesTaskHandler;
import com.communote.plugins.export.types.Follow;
import com.communote.plugins.export.types.Group;
import com.communote.plugins.export.types.GroupMember;
import com.communote.plugins.export.types.Note;
import com.communote.plugins.export.types.Topic;
import com.communote.plugins.export.types.User;
import com.communote.server.api.ServiceLocator;
import com.communote.server.api.core.application.CommunoteRuntime;
import com.communote.server.api.core.client.ClientTO;
import com.communote.server.api.core.config.StartupProperties;
import com.communote.server.api.core.security.AuthorizationException;
import com.communote.server.api.core.task.TaskAlreadyExistsException;
import com.communote.server.api.core.task.TaskManagement;
import com.communote.server.api.core.task.TaskStatusException;
import com.communote.server.api.core.task.TaskTO;
import com.communote.server.core.osgi.OSGiHelper;
import com.communote.server.core.security.AuthenticationHelper;
import com.communote.server.core.security.SecurityHelper;
import com.communote.server.model.task.TaskStatus;
import com.communote.server.persistence.user.client.ClientHelper;

/**
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 */
@Component
@Instantiate(name = "ExportService")
@Provides
public class ExportServiceImpl implements ExportService {

    private static final String EXPORT_ENTITIES_TASK_START_OFFSET = "com.communote.plugins.export.task.start.offset";
    private static final Logger LOGGER = LoggerFactory.getLogger(ExportServiceImpl.class);
    private static final String PROPERTY_EXPORT_STATUS = "exportStatus";
    private static final String PROPERTY_EXPORT_FILE = "exportFile";
    // start offset when running as saas: in 1h
    private static final long DEFAULT_EXPORT_TASK_START_OFFSET_SAAS = 3600l;
    // start offset when running as standalone: in 10m
    private static final long DEFAULT_EXPORT_TASK_START_OFFSET_STANDALONE = 600l;

    private static ExportService INSTANCE;

    public static ExportService getInstance() {
        return INSTANCE;
    }

    private final String symbolicName;

    private File workBaseDir;

    @Requires
    private PluginPropertyService propertyService;
    private File exportResultDir;

    public ExportServiceImpl(BundleContext bundleContext) {
        this.symbolicName = bundleContext.getBundle().getSymbolicName();
    }

    private void addExportTask() {
        TaskManagement taskManagement = ServiceLocator.instance().getService(TaskManagement.class);
        String taskName = ExportEntitiesTaskHandler.class.getName();
        taskManagement.addTaskHandler(taskName, ExportEntitiesTaskHandler.class);
        long defaultOffset = CommunoteRuntime.getInstance().getApplicationInformation()
                .isStandalone() ? DEFAULT_EXPORT_TASK_START_OFFSET_STANDALONE
                        : DEFAULT_EXPORT_TASK_START_OFFSET_SAAS;
        long startOffset = Long.getLong(EXPORT_ENTITIES_TASK_START_OFFSET, defaultOffset) * 1000;
        TaskTO existingTask = taskManagement.findTask(taskName);
        long startTime = System.currentTimeMillis() + startOffset;
        if (existingTask != null) {
            TaskStatus status = existingTask.getStatus();
            LOGGER.debug("Task {} already exists and has status {}", taskName, status);
            if (TaskStatus.FAILED.equals(status)) {
                // reset failed task after re-deployment of plugin because new plugin might have
                // fixed the cause of the failure
                taskManagement.resetTask(existingTask.getUniqueName());
                status = TaskStatus.PENDING;
            }
            if (TaskStatus.PENDING.equals(status)
                    && existingTask.getNextExecution().getTime() - startTime < 0) {
                try {
                    LOGGER.debug("Rescheduling task {} to respect start offset", taskName);
                    taskManagement.rescheduleTask(taskName, new Date(startTime));
                } catch (TaskStatusException e) {
                    // can happen in clustered environment but isn't critical
                    LOGGER.debug("Rescheduling task {} failed because it is not pending anymore",
                            taskName);
                }
            }
        } else {
            try {
                taskManagement.addTask(taskName, true, 0L, new Date(startTime),
                        ExportEntitiesTaskHandler.class);
            } catch (TaskAlreadyExistsException e) {
                // might occur in clustered environment but isn't critical
                LOGGER.debug("Adding task {} failed because it already exists", taskName);
            }
        }
    }

    private void cleanupExportDir(File clientOutputDir, String clientMessage) {
        try {
            FileUtils.deleteDirectory(clientOutputDir);
        } catch (IOException e) {
            LOGGER.warn("Removing attachments and XML files after export{} failed");
        }
    }

    private void copyXsd(File clientOutputDir, String clientMessage) {
        File staticFolder = new File(OSGiHelper.getBundleStorage(this.symbolicName), "static");
        try {
            FileUtils.copyFileToDirectory(new File(staticFolder, "communote-export.xsd"),
                    clientOutputDir);
        } catch (IOException e) {
            LOGGER.warn("Copying XSD file to output directory{} failed", clientMessage);
        }
    }

    @Override
    public void exportAllData() throws ExportNotPlannedException {
        ExportStatus status = getExportStatus();
        if (!ExportStatus.PLANNED.equals(status)) {
            throw new ExportNotPlannedException("Before starting the export it must be planned");
        }
        ClientTO client = ClientHelper.getCurrentClient();
        boolean isGlobalClient = ClientHelper.isClientGlobal(client);
        String clientMessage = isGlobalClient ? "" : " of client " + client.getClientId();

        propertyService.setClientProperty(PROPERTY_EXPORT_STATUS, ExportStatus.RUNNING.name());
        SecurityContext curSecurityContext = AuthenticationHelper
                .setInternalSystemToSecurityContext();
        try {
            long startTime = System.currentTimeMillis();
            LOGGER.info("Starting Communote data export{}", clientMessage);
            File clientOutputDir = getClientOutputDir(client);
            if (clientOutputDir.exists()) {
                FileUtils.cleanDirectory(clientOutputDir);
            } else {
                clientOutputDir.mkdir();
            }
            List<Long> exportedUsers = exportUsers(clientOutputDir, clientMessage);
            exportGroupsAndMembers(clientOutputDir, clientMessage);
            exportTopics(clientOutputDir, clientMessage);
            exportNotes(clientOutputDir, clientMessage);
            exportFollows(exportedUsers, clientOutputDir, clientMessage);
            copyXsd(clientOutputDir, clientMessage);
            Utils.zipDirectory(clientOutputDir, exportResultDir, getZipFileName(client));
            cleanupExportDir(clientOutputDir, clientMessage);
            propertyService
                    .setClientProperty(PROPERTY_EXPORT_STATUS, ExportStatus.COMPLETED.name());
            long duration = (System.currentTimeMillis() - startTime) / 1000;
            LOGGER.info("Finished Communote data export{} within about {} seconds", clientMessage,
                    duration);
        } catch (ExportFailedException | IOException | RuntimeException e) {
            LOGGER.error("Export{} failed", clientMessage, e);
            propertyService.setClientProperty(PROPERTY_EXPORT_STATUS, ExportStatus.FAILED.name());
        } finally {
            AuthenticationHelper.setSecurityContext(curSecurityContext);
        }
    }

    private void exportFollows(List<Long> exportedUsers, File targetDir, String clientMessage)
            throws ExportFailedException {
        exportXml(Follow.class, "follows", new FollowExporter(exportedUsers), targetDir,
                clientMessage);
    }

    private void exportGroupsAndMembers(File targetDir, String clientMessage)
            throws ExportFailedException {
        GroupExporter exporter = new GroupExporter();
        exportXml(Group.class, "groups", exporter, targetDir, clientMessage);
        exportXml(GroupMember.class, "groupMembers",
                new GroupMemberExporter(exporter.getExportedGroups()), targetDir, clientMessage);
    }

    private void exportNotes(File targetDir, String clientMessage) throws ExportFailedException {
        File attachmentTargetDir = new File(targetDir, "attachments");
        attachmentTargetDir.mkdir();
        exportXml(Note.class, "notes", new NoteExporter(attachmentTargetDir), targetDir,
                clientMessage);
    }

    private void exportTopics(File targetDir, String clientMessage) throws ExportFailedException {
        exportXml(Topic.class, "topics", new TopicExporter(), targetDir, clientMessage);
    }

    private List<Long> exportUsers(File targetDir, String clientMessage)
            throws ExportFailedException {
        UserExporter userExporter = new UserExporter();
        exportXml(User.class, "users", userExporter, targetDir, clientMessage);
        return userExporter.getExportedUsers();
    }

    /**
     * Export the entities of the given type with an XmlStreamingSerializer into an XML file.
     *
     * @param entityClass
     *            the class of the entities to export
     * @param typeName
     *            a name describing the entities. Will be used as file name and for logging
     * @param exporter
     *            the exporter which fetches the entities and passes them to the serializer
     * @param targetDir
     *            the directory to write the XML files to
     * @param clientMessage
     *            a message to append when logging. Should contain information of the current client
     * @throws ExportFailedException
     *             in case the export failed
     */
    private <T> void exportXml(Class<T> entityClass, String typeName, Exporter<T> exporter,
            File targetDir, String clientMessage) throws ExportFailedException {
        LOGGER.debug("Starting export of {}{}", typeName, clientMessage);
        File targetFile = new File(targetDir, typeName + ".xml");
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(targetFile);
        } catch (FileNotFoundException e) {
            throw new ExportFailedException("Exporting the " + typeName
                    + " failed because the output file could not be created", e);
        }
        try {
            exporter.export(new XmlStreamingSerializer<T>(entityClass, typeName, outputStream,
                    false, true));
            LOGGER.debug("Export of {}{} completed", typeName, clientMessage);
        } catch (SerializerInitializationException e) {
            throw new ExportFailedException("Exporting the " + typeName + " failed", e);
        } finally {
            IOHelper.close(outputStream);
        }
    }

    /**
     * Get the client-dependent directory where the exported data should be/is stored.
     *
     * @param client
     *            the current client
     * @return the directory for storing the exported data
     */
    private File getClientOutputDir(ClientTO client) {
        return new File(this.workBaseDir, client.getClientId());
    }

    @Override
    public File getExportResult() throws AuthorizationException {
        if (!SecurityHelper.isClientManager()) {
            throw new AuthorizationException("Current user is not client manager");
        }
        ExportStatus status = getExportStatus();
        if (!ExportStatus.COMPLETED.equals(status)) {
            return null;
        }
        ClientTO client = ClientHelper.getCurrentClient();
        return new File(this.exportResultDir, getZipFileName(client));
    }

    @Override
    public ExportStatus getExportStatus() {
        String status = propertyService.getClientProperty(PROPERTY_EXPORT_STATUS);
        if (status != null) {
            return ExportStatus.valueOf(status);
        }
        return null;
    }

    private String getZipFileName(ClientTO client) {
        StringBuilder fileName = new StringBuilder("Communote_export");
        if (!CommunoteRuntime.getInstance().getApplicationInformation().isStandalone()) {
            fileName.append("_");
            fileName.append(client.getClientId());
        }
        fileName.append(".zip");
        return fileName.toString();
    }

    @Override
    public boolean planExport() throws AuthorizationException {
        if (!SecurityHelper.isClientManager()) {
            throw new AuthorizationException("Current user is not client manager");
        }
        ExportStatus status = getExportStatus();
        if (status == null || status.equals(ExportStatus.COMPLETED)
                || status.equals(ExportStatus.FAILED)) {
            propertyService.setClientProperty(PROPERTY_EXPORT_STATUS, ExportStatus.PLANNED.name());
            if (status != null) {
                propertyService.setClientProperty(PROPERTY_EXPORT_FILE, null);
            }
            return true;
        }
        return false;
    }

    @Validate
    private void start() {
        StartupProperties startupProps = CommunoteRuntime.getInstance().getConfigurationManager()
                .getStartupProperties();
        File cacheRootDirectory = startupProps.getCacheRootDirectory();
        this.workBaseDir = new File(cacheRootDirectory, symbolicName);
        if (this.workBaseDir.exists()) {
            try {
                FileUtils.cleanDirectory(workBaseDir);
            } catch (IOException e) {
                throw new RuntimeException("Cleaning existing work directory failed", e);
            }
        } else {
            this.workBaseDir.mkdir();
        }
        this.exportResultDir = new File(startupProps.getDataDirectory(), symbolicName);
        if (!this.exportResultDir.exists()) {
            this.exportResultDir.mkdir();
        }

        addExportTask();
        INSTANCE = this;

    }

    @Invalidate
    private void stop() {
        // TODO what happens if export is running and someone removes plugin or stops Communote?
        INSTANCE = null;
        TaskManagement taskManagement = ServiceLocator.instance().getService(TaskManagement.class);
        taskManagement.removeTaskHandler(ExportEntitiesTaskHandler.class.getName());
        // note: not removing the export result dir because it would remove the exports, which might
        // have been expensive in creation
        try {
            FileUtils.deleteDirectory(workBaseDir);
        } catch (IOException e) {
            LOGGER.error("Cleaning work directory failed", e);
        }
    }

}
