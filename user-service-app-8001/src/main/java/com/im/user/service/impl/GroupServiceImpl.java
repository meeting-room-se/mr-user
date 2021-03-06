package com.im.user.service.impl;

import com.im.user.entity.po.GroupMemberPo;
import com.im.user.entity.po.GroupPo;
import com.im.user.entity.po.User;
import com.im.user.entity.request.GroupUpdateRequest;
import com.im.user.entity.vo.GroupBriefVo;
import com.im.user.entity.vo.GroupUserBriefVo;
import com.im.user.entity.vo.GroupVo;
import com.im.user.entity.vo.UserVo;
import com.im.user.exception.BusinessErrorEnum;
import com.im.user.mapper.GroupMapper;
import com.im.user.mapper.GroupMemberMapper;
import com.im.user.mapper.UserMapper;
import com.im.user.service.IGroupService;
import com.im.user.service.IUserService;
import com.mr.response.error.BusinessException;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;


@Component
@Service
public class GroupServiceImpl implements IGroupService
{

    @Resource
    private GroupMapper groupMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private GroupMemberMapper groupMemberMapper;

    @Override
    @Transactional
    public Long createGroup(Long creatorId, List<Long> initialUserIds) throws BusinessException
    {


        User creatorUser = userMapper.selectByPrimaryKey(creatorId);
        if(creatorUser == null)
        {
            throw new BusinessException("创建者id:" + creatorId+ "对应的用户不存在");
        }

        HashSet h = new HashSet(initialUserIds);
        initialUserIds.clear();
        initialUserIds.addAll(h);
        List<User>  initialUsers = new ArrayList<>();

        initialUsers.add(creatorUser);
        for (Long id: initialUserIds)
        {
            User user = userMapper.selectByPrimaryKey(id);
            if(user == null)
            {
                throw new BusinessException("群成员id:" +id + "对应的用户不存在");
            }
            initialUsers.add(user);
        }


        String randomId = UUID.randomUUID().toString().substring(0, 2);

        //插入群表
        GroupPo groupPo = GroupPo.builder()
                .createUserId(creatorId)
                .memberNum(initialUserIds.size() + 1)
                .name( randomId+ creatorUser.getUsername() +"的群聊")
                .avatarUrl("http://1.zmz121.cn:8010/res/file/pic/17201800000320200321080339502649.png")
                .build();

        int res = groupMapper.insertSelective(groupPo);
        if(res < 1)
        {
            throw new BusinessException("创建群聊失败");
        }

        Long groupId = groupPo.getId();

        //插入群成员表
        for(User user: initialUsers)
        {
            GroupMemberPo groupMemberPo = GroupMemberPo.builder()
                    .groupId(groupId)
                    .groupName(groupPo.getName())
                    .groupAvatarUrl(groupPo.getAvatarUrl())
                    .userId(user.getId())
                    .userName(user.getUsername())
                    .userAvatarUrl(user.getAvatarUrl())
                    .build();
            res = groupMemberMapper.insertSelective(groupMemberPo);
            if(res < 1)
            {
                throw new BusinessException("插入群成员失败");
            }
        }
        return groupId;
    }


    @Override
    public List<GroupBriefVo> queryJoinedGroup(Long userId) {
        List<GroupBriefVo> groupBriefVos = groupMemberMapper.selectByGroupMemberUserId(userId);
        return groupBriefVos;
    }

    @Override
    public List<GroupUserBriefVo> queryGroupUsers(Long groupId) {
        List<GroupUserBriefVo> groupUserBriefVos = groupMemberMapper.selectByGroupId(groupId);
        return groupUserBriefVos;
    }

    @Override
    public GroupPo queryGroupById(Long groupId)
    {
        return groupMapper.selectByPrimaryKey(groupId);
    }

    @Override
    public void updateGroupInfo(GroupUpdateRequest groupUpdateRequest) throws BusinessException {
        GroupPo groupPo = GroupPo.builder().build();
        BeanUtils.copyProperties(groupUpdateRequest,groupPo);
        int res = groupMapper.updateByPrimaryKeySelective(groupPo);
        if(res < 1)
        {
            throw new BusinessException("修改群信息失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertGroupUser(Long groupId, List<Long> insertUserIds) throws BusinessException {
        GroupPo groupPo = groupMapper.selectByPrimaryKey(groupId);
        if(groupPo == null){
            throw  new BusinessException("群不存在！");
        }

        List<User>  insertUsers = new ArrayList<>();


        for (Long id: insertUserIds)
        {
            User user = userMapper.selectByPrimaryKey(id);
            if(user == null)
            {
                throw new BusinessException("群成员id:" +id + "对应的用户不存在");
            }
            insertUsers.add(user);
        }
        //插入群表对应群人数增加
        int insertMemberNum = insertUsers.size();
        int currentMemberNum = groupPo.getMemberNum();
        updateGroupMemberNum(groupId,currentMemberNum,insertMemberNum);

        //插入群成员表
        for(User user: insertUsers)
        {
            GroupMemberPo groupMemberPo = GroupMemberPo.builder()
                    .groupId(groupId)
                    .groupAvatarUrl(groupPo.getAvatarUrl())
                    .groupName(groupPo.getName())
                    .userId(user.getId())
                    .userName(user.getUsername())
                    .userAvatarUrl(user.getAvatarUrl())
                    .build();
            GroupBriefVo groupBriefVo = groupMemberMapper.selectByGroupIdGroupMemberUserId(groupId, groupMemberPo.getUserId());
            if(groupBriefVo != null){
                throw new BusinessException("群成员已存在！");
            }
            int res = groupMemberMapper.insertSelective(groupMemberPo);
            if(res < 1)
            {
                throw new BusinessException("插入群成员失败");
            }

        }
    }

    public void updateGroupMemberNum(Long groupId,Integer currentMemberNums,Integer memberNumsTobeAdded) throws BusinessException {
        GroupPo groupPo = GroupPo.builder().build();
        int currentMemberNumNow =currentMemberNums;
        int res = 0;
        for(int i=0;i<3;i++){
            int newMembemNum = currentMemberNumNow + memberNumsTobeAdded;
            res = groupMapper.updateMemberNumsOptimistic(groupId,currentMemberNumNow,newMembemNum);
            if(res < 1){
                currentMemberNumNow = groupPo.getMemberNum();
            }else if(res>=1){
                return;
            }
        }
        if(res < 1){
            throw new BusinessException("修改群人数失败！");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawFromGroup(Long userId, Long groupId) throws BusinessException {
        GroupPo groupPo = groupMapper.selectByPrimaryKey(groupId);
        if(groupPo == null){
            throw  new BusinessException("群不存在！");
        }
        GroupBriefVo groupBriefVo = groupMemberMapper.selectByGroupIdGroupMemberUserId(groupId, userId);
        if(groupBriefVo == null){
            throw new BusinessException("所查群不存在该用户！");
        }
        int res = groupMemberMapper.deleteLogicGroupMember(groupId, userId);
        if(res < 1){
            throw new BusinessException("退群失败！");
        }
        int groupMemberNum = groupPo.getMemberNum();
        int memberNumNew = groupMemberNum -1;
        int res1 = groupMapper.updateMemberNumByPrimaryKey(memberNumNew,groupId);
        if(res1 < 1){
            throw new BusinessException("退群失败！");
        }
    }


}
