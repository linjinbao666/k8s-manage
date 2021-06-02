package ama.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Data
@Getter
@Setter
public class UserInfo implements Serializable {

	private static final long serialVersionUID = -7068295439141586262L;
	/** 用户标识 */
	private Long id;
	/** 用户姓名 */
	private String name;
	/** 登录用户名 */
	private String username;
	/** 用户类型（1、超级管理员 2、普通管理员 3、普通用户 */
	private Integer userType;
	/** 角色 */
	private int[] roleId;
	/** 所属单位标识 */
	private long unit;
	/** 部门标识 */
	private Integer department;

	/**
	 * 部门绑定的名称空间
	 */
	private String namespace;

}
