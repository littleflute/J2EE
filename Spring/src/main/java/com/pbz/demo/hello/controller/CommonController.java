package com.pbz.demo.hello.controller;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.pbz.demo.hello.util.ExecuteCommand;
import com.pbz.demo.hello.util.FileUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@Api(tags = "通用功能接口")
public class CommonController {
	private static Semaphore semaphore = new Semaphore(1);
	private static final boolean isWindows = System.getProperty("os.name").startsWith("Windows");

	@ApiOperation(value = "执行服务器端命令", notes = "执行服务器端命令")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "cmd", value = "Dos/Shell Command. More format refer java Runtime.getRuntime().exec usage but need use comma instead of space. Example: sh,-c,ls%20*.jpg  mkdir,dir1", paramType = "query", required = true, dataType = "string", defaultValue = "cmd,/c,dir") })
	@RequestMapping(value = "/command", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> processCommandOnServer(String[] cmd) throws Exception {
		Map<String, Object> status = new HashMap<String, Object>();
		File logFile = new File("./Cmdlog.txt");
		if (logFile.exists()) {
			logFile.delete();
		}
		logFile.createNewFile();

		try {
			ExecuteCommand.executeCommand(cmd, null, new File("."), logFile.getAbsolutePath());
		} catch (Exception e) {
			String cmd0 = "sh";
			String cmd1 = "-c";
			if (isWindows) {
				cmd0 = "cmd";
				cmd1 = "/c";
			}
			String[] cmds = new String[cmd.length + 2];
			cmds[0] = cmd0;
			cmds[1] = cmd1;
			for (int i = 0; i < cmd.length; i++) {
				cmds[i+2] = cmd[i];
			}
			ExecuteCommand.executeCommand(cmds, null, new File("."), logFile.getAbsolutePath());
		}
		Thread.sleep(100);
		String strOut = FileUtil.readAllBytes(logFile.getAbsolutePath());
		status.put("Status", "OK!");
		status.put("Message", strOut);

		return status;
	}

	@ApiIgnore
	@RequestMapping("/api/v1/test")
	public Map<String, Object> authenticated(HttpServletRequest request) {
		Map<String, Object> ret = new HashMap<String, Object>();
		ret.put("message", "call method");

		int availablePermits = semaphore.availablePermits();
		if (availablePermits > 0) {
			System.out.println("抢到资源 " + availablePermits);
		} else {
			System.out.println("资源已被占用，稍后再试");
			ret.put("message", "I am busy!");
			return ret;
		}
		try {
			// 请求占用一个资源
			semaphore.acquire(1);
			System.out.println("资源正在被使用");
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			semaphore.release(1);// 释放一个资源
			System.out.println("-----------释放资源包----------");
		}

		return ret;
	}
}
