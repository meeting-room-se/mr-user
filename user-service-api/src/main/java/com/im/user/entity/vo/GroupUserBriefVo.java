package com.im.user.entity.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel
public class GroupUserBriefVo implements Serializable
{
    @ApiModelProperty("用户Id")
    private Long userid;

    @ApiModelProperty("用户头像地址")
    private String userAvatarUrl;

    @ApiModelProperty("用户名")
    private String username;


}
