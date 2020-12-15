package com.pbz.demo.hello.controller;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.pbz.demo.hello.exception.HtmlRequestException;
import com.pbz.demo.hello.service.ClockImageService;
import com.pbz.demo.hello.service.SubtitleImageService;
import com.pbz.demo.hello.util.ExecuteCommand;
import com.pbz.demo.hello.util.JsonSriptParser;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@Api(tags = "视频操作接口")
@RequestMapping(value = "/image")
public class ImageController {

	@Autowired
	private ClockImageService clockImageService;
	@Autowired
	private SubtitleImageService subtitleImageService;

	@Autowired
	HttpServletRequest request;
	@Autowired
	HttpServletResponse response;

	private static int IMAGE_WIDTH = 450;
	private static int IMAGE_HEIGHT = 390;
	private static final String subtitle_video_name = "vSubtitle.mp4";
	private static final String final_video_name = "vFinal.mp4";

	private static final boolean isWindows = System.getProperty("os.name").startsWith("Windows");

	@ApiIgnore
	@RequestMapping(value = "/clock")
	@ResponseBody
	public void getClockImage(@RequestParam(name = "time") String time) throws Exception {
		Object username = request.getSession().getAttribute("name");
		if (!"admin".equals(username)) {
			throw new Exception("Please login first!");
		}
		verifyParameter(time);

		response.setHeader("Pragma", "No-cache");
		response.setHeader("Cache-Control", "no-cache");
		response.setDateHeader("Expires", 0);
		response.setContentType("image/jpeg");

		// 创建内存图像并获得其图形上下文
		BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics g = image.getGraphics();
		clockImageService.drawImage(g, IMAGE_WIDTH, IMAGE_HEIGHT, time);
		g.dispose();

		// 将图像输出到客户端
		ServletOutputStream sos = response.getOutputStream();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ImageIO.write(image, "JPEG", bos);
		byte[] buf = bos.toByteArray();
		response.setContentLength(buf.length);
		sos.write(buf);
		bos.close();
		sos.close();
	}

	@ApiIgnore
	@RequestMapping(value = "/subtitle")
	@ResponseBody
	public String saveSubtitleImages(@RequestParam(name = "filename") String filename) throws Exception {

		String filePath = System.getProperty("user.dir") + "/" + filename;// "1.srt";
		System.out.println(filePath);
		int number = subtitleImageService.saveSubtitleToImageFile(filePath, 768, 512);
		String strResult = "解析字幕文件错误!";
		if (number > 0) {
			strResult = "字幕文件解析成功，已在服务器端生成字幕图片，访问http://localhost:8080/NumberOfPicture.jpg查看图片，生成的图片总数:" + number;
			String command = System.getProperty("user.dir") + "/" + "jpg2video";
			String[] args = { "768", "512", "1" };
			if (isWindows) {
				command += ".bat";
			} else {
				command += ".sh";
			}
			boolean bRunScript = ExecuteCommand.executeCommand(command, args);
			if (bRunScript) {
				strResult += ". \"jpg2video\"脚本调用成功！已合成MP4，访问http://localhost:8080/" + subtitle_video_name + "4查看视频";
			}
		}
		return strResult;

	}

	@ApiOperation(value = "音频合成字幕生成视频(mp3+srt)", notes = "将音频合成字幕生成视频")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "subtitlefile", value = "subtitle file (*.srt)", paramType = "query", required = true, dataType = "string", defaultValue = "example.srt"),
			@ApiImplicitParam(name = "audiofile", value = "audio file (*.mp3)", paramType = "query", required = true, dataType = "string", defaultValue = "example.mp3") })
	@RequestMapping(value = "/combine", method = RequestMethod.GET)
	public ModelAndView combineSubtiteAndAudio2MP4(@RequestParam(name = "subtitlefile") String subtitleFile,
			@RequestParam(name = "audiofile") String audioFile) throws Exception {
		ModelAndView mv = new ModelAndView();
		mv.setViewName("video.html");
		String strResultMsg = "将字幕文件与音频文件合成视频文件出错！";
		String path = System.getProperty("user.dir") + "/" + subtitleFile;
		int number = subtitleImageService.saveSubtitleToImageFile(path, 800, 600);
		if (number > 0) {
			String suffix = isWindows ? ".bat" : ".sh";
			String cmd = System.getProperty("user.dir") + "/" + "jpg2video" + suffix;
			String[] args = { "800", "600", "1" };
			ExecuteCommand.executeCommand(cmd, args);
			String ffmpegPath = "ffmpeg";
			if (!isWindows) {
				ffmpegPath = "/usr/bin/ffmpeg";
				if (!new File(ffmpegPath).exists()) {
					ffmpegPath = "/usr/local/bin/ffmpeg";
				}
			}
			String[] cmds = { ffmpegPath, "-y", "-i", subtitle_video_name, "-i", audioFile, final_video_name };
			boolean b = ExecuteCommand.executeCommand(cmds, null, new File("."), null);
			if (b) {
				strResultMsg = "已为您合成视频文件，点击即可播放视频";
			}
		}
		mv.addObject("message", strResultMsg);

		return mv;
	}

	@ApiOperation(value = "通过剧本协议生成视频", notes = "通过剧本协议生成视频")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "script", value = "script file (*.json)", paramType = "query", required = true, dataType = "string", defaultValue = "example.json") })
	@RequestMapping(value = "/video", method = RequestMethod.GET)
	public ModelAndView generateVideoByscenario(@RequestParam(name = "script") String scriptFile)
			throws HtmlRequestException {
		ModelAndView mv = new ModelAndView();
		mv.setViewName("video.html");
		int index = 1;
		while (true) {
			String jpgFile = System.getProperty("user.dir") + "/" + Integer.toString(index) + ".jpg";
			File file = new File(jpgFile);
			if (file.exists()) {
				file.delete();
			} else
				break;
			index++;
		}
		String strResultMsg = "根据剧本生成视频粗错啦！";
		boolean b = false;
		try {
			b = JsonSriptParser.generateVideoByScriptFile(scriptFile);
		} catch (Exception e) {
			throw new HtmlRequestException("请检查剧本文件是否正确. " + e.getMessage());
		}
		if (b) {
			strResultMsg = "已为您合成视频文件，点击即可播放视频";
		}
		mv.addObject("message", strResultMsg);
		return mv;
	}

	private void verifyParameter(String time) throws Exception {
		if (time == null || time.trim().length() == 0) {
			throw new Exception("The time parameter is not specified.");
		}
		SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
		try {
			format.setLenient(false);// 此处指定日期/时间解析是否严格，true是不严格，false为严格
			format.parse(time);
		} catch (ParseException e) {
			throw new Exception("The format of time parameter is invalid! " + e.getMessage());
		}
	}

}