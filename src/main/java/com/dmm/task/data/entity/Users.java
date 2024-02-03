package com.dmm.task.data.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;
import lombok.ToString;

@Data
@Entity
@ToString(exclude = "password")
public class Users {
	@Id
	public String userName;
	public String password;
	public String name;
	public String roleName;
}