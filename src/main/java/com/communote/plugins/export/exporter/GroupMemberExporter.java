package com.communote.plugins.export.exporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.communote.common.converter.Converter;
import com.communote.common.converter.IdentityConverter;
import com.communote.plugins.export.serializer.SerializerWriteException;
import com.communote.plugins.export.serializer.StreamingSerializer;
import com.communote.plugins.export.types.GroupMember;
import com.communote.server.api.ServiceLocator;
import com.communote.server.core.user.UserGroupManagement;
import com.communote.server.core.user.UserManagement;
import com.communote.server.core.vo.query.user.CommunoteEntityQuery;
import com.communote.server.core.vo.query.user.CommunoteEntityQueryParameters;
import com.communote.server.model.user.CommunoteEntity;
import com.communote.server.model.user.User;
import com.communote.server.model.user.group.Group;

/**
 * Exporter for extracting the direct members of groups.
 *
 * @author Communote GmbH - <a href="http://www.communote.com/">http://www.communote.com/</a>
 *
 */
public class GroupMemberExporter implements Exporter<GroupMember> {

    private class GroupToGroupMembersConverter implements
    Converter<Group, List<GroupMember>> {

        private UserManagement userManagement;

        @Override
        public List<GroupMember> convert(Group source) {
            List<GroupMember> result = new ArrayList<>();
            Set<CommunoteEntity> members = source.getGroupMembers();
            IdentityConverter<User> identityConverter = new IdentityConverter<>();
            for (CommunoteEntity member : members) {
                boolean add = false;
                boolean isGroup = false;
                if (groupIds.contains(member.getId())) {
                    isGroup = true;
                    add = true;
                } else {
                    User user = getUserManagement().getUserById(member.getId(),
                            identityConverter);
                    // ignore if it is a group that was not passed as part of the groupIds
                    // collection
                    add = Utils.isUserActiveOrDisabled(user);
                }
                if (add) {
                    GroupMember convertedMember = new GroupMember();
                    convertedMember.setEntityId(member.getId());
                    convertedMember.setGroup(isGroup);
                    convertedMember.setGroupId(source.getId());
                    result.add(convertedMember);
                }
            }
            return result;
        }

        private UserManagement getUserManagement() {
            if (userManagement == null) {
                userManagement = ServiceLocator.findService(UserManagement.class);
            }
            return userManagement;

        }

    }

    private final Collection<Long> groupIds;
    private final UserGroupManagement groupManagement;

    public GroupMemberExporter(Collection<Long> groupIds) {
        this.groupIds = groupIds;
        groupManagement = ServiceLocator.findService(UserGroupManagement.class);
    }

    @Override
    public void export(StreamingSerializer<GroupMember> serializer) throws ExportFailedException {
        try {
            serializer.prepareSerialization();
            for (Long groupId : groupIds) {
                exportGroupMembers(groupId, serializer, new GroupToGroupMembersConverter());
            }
            serializer.finishSerialization();
        } catch (SerializerWriteException e) {
            throw new ExportFailedException("Exporting the group members failed", e);
        }
    }

    private void exportGroupMembers(Long groupId, StreamingSerializer<GroupMember> serializer,
            GroupToGroupMembersConverter converter) throws SerializerWriteException {
        CommunoteEntityQuery query = new CommunoteEntityQuery();
        CommunoteEntityQueryParameters parameters = query.createInstance();
        parameters.setDirectGroupMembershipFilteringGroupId(groupId);
        List<GroupMember> members = groupManagement.findGroupById(groupId, converter);
        if (members != null) {
            for (GroupMember member : members) {
                serializer.appendEntity(member);
            }
        }
    }

}
