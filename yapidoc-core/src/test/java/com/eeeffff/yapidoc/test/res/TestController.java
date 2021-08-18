package com.eeeffff.yapidoc.test.res;

import java.util.LinkedList;
import java.util.List;

import javax.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试控制器
 *
 * 
 * @version V1.0
 * @date 2019-05-27 13:22
 */
@RequestMapping("/test")
@RestController
public class TestController {

	/**
	 * 新增一个实例
	 *
	 * @param createParam 创建对象
	 * @return 创建后的信息
	 */
	@PostMapping(value = "/create")
	public CreateVO add(@RequestBody @Valid CreateParam createParam) {
		return new CreateVO();
	}

	/**
	 * 新增一个实例2
	 *
	 * @param createParam2 创建对象2
	 */
	@PostMapping(value = "/create2")
	public List<CreateVO> create2(@Valid @RequestBody List<CreateParam> createParam2) {
		return new LinkedList<>();
	}

	/**
	 * 获取一个实例
	 *
	 * @param userId 用户ID
	 * @param sex    性别
	 * @return 返回信息
	 */
	@GetMapping(value = "/get")
	public Result<CreateVO> get(@RequestParam(value = "userId", required = false) String userId,
			@RequestParam(value = "sex2") String sex) {
		return new Result<>();
	}

	/**
	 * 获取一个实例
	 *
	 * @param userId 用户ID
	 * @param sex    性别
	 * @return 返回信息
	 */
	@GetMapping(value = "/get")
	public Result<Result2<CreateParam>> get2(@RequestParam(value = "userId", required = false) String userId,
			@RequestParam(value = "sex2") String sex) {
		return new Result<>();
	}

}