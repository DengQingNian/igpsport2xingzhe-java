package com.dengqn.igps2xingzhe.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author dengqn
 * @since 2024/7/20 11:23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XingZhe implements Serializable {
	private static final long serialVersionUID = 2522264649038579098L;

	private String username;
	private String password;
	private String pubKey;
}
