package com.pbz.demo.hello.controller;

import java.net.URLDecoder;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.pbz.demo.hello.dao.DocDao;
import com.pbz.demo.hello.entity.DocEntity;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@RestController
@Api(tags = "剧本数据库Rest API")
@RequestMapping(value = "/mogodb")
public class DocController {
	@Autowired
	public DocDao docDao;

	@ApiOperation(value = "保存JSON剧本到MongoDB", notes = "保存JSON剧本到数据库")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "id", value = "id Long", paramType = "query", required = true, dataType = "Long", defaultValue = "1001"),
			@ApiImplicitParam(name = "title", value = "title", paramType = "query", required = true, dataType = "string", defaultValue = "title"),
			@ApiImplicitParam(name = "description", value = "description", paramType = "query", required = true, dataType = "string", defaultValue = "description"),
			@ApiImplicitParam(name = "author", value = "author", paramType = "query", required = true, dataType = "string", defaultValue = "author"),
			@ApiImplicitParam(name = "jsonString", value = "json string", paramType = "body", required = true, dataType = "string") })
	@RequestMapping(value = "/save", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String saveJsonDoc2DataBase(@RequestParam("id") Long id, @RequestParam("title") String title,
			@RequestParam("description") String description, @RequestParam("author") String author,
			@RequestBody String jsonString) throws Exception {
		jsonString = URLDecoder.decode(jsonString, "UTF-8");
		if (!jsonString.startsWith("{") && !jsonString.endsWith("}")) {
			jsonString = jsonString.substring(1, jsonString.length() - 1);
		}
		System.out.println(jsonString);
		DocEntity docEntity = new DocEntity();
		docEntity.setId(id);
		docEntity.setTitle(title);
		docEntity.setDescription(description);
		docEntity.setBy(author);
		docEntity.setContent(jsonString);
		docDao.saveDoc(docEntity);

		return jsonString;
	}

	@RequestMapping(value = "/getAllDocs", method = RequestMethod.GET)
	@ResponseBody
	public List<DocEntity> getAllDocs() {
		return docDao.getAllDocs();
	}

}
