package com.ztianzeng.apidoc.test;

import java.util.List;

import lombok.Data;

/**
 * @author zhaotianzeng
 * @version V1.0
 * @date 2019-06-05 16:38
 */
@Data
public class ListParam2 {
	private List<List<CreateParam2>> testList;
}