package com.im.user.entity.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("发出的好友请求vo")
public class SendedFriendRequestVo implements Serializable {

    /**
     * Column:    receiver_id
     * Nullable:  false
     */
    private Long receiverId;

    /**
     * 0-待验证，1-拒绝，2-同意，3-超时失效
     *
     * Column:    status
     * Nullable:  false
     */
    private Integer status;

    /**
     * 验证备注信息
     *
     * Column:    note
     * Nullable:  false
     */
    private String note;

    /**
     * Column:    receiver_username
     * Nullable:  false
     */
    private String receiverUsername;

    /**
     * Column:    receiver_avatar_url
     * Nullable:  false
     */
    private String receiverAvatarUrl;

}
