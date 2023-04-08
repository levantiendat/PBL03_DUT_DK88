package model;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

public class FileHandler {
	
	private static final Path PATH = Paths.get("File");
	private static FileHandler instance;
	
	private FileHandler() {
		try {
			Files.createDirectories(PATH);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static FileHandler getInstance() {
		if (instance == null) instance = new FileHandler();
		return instance;
	}
	
	private Boolean isImage(String fileExtension) {
		return Arrays.asList(new String[] {"png", "jpg", "jpeg", "bmp"}).contains(fileExtension.trim().toLowerCase());
	}
	
	public ResponseObject saveImage(MultipartFile file, String token) {
		if (!TokenProvider.isValidToken(token))
			return new ResponseObject(ResponseObject.RESPONSE_REQUEST_ERROR, "Login again to upload file!", null);
		Map<String, Object> data_token = TokenProvider.getDataFromToken(token);
		if (file.isEmpty())
			return new ResponseObject(ResponseObject.RESPONSE_REQUEST_ERROR, "File not found", null);
		if (file.getSize() / (1e6) > 5.0)
			return new ResponseObject(ResponseObject.RESPONSE_REQUEST_ERROR, "File too large (>5Mb)", null);
		String fileExtension = FilenameUtils.getExtension(file.getOriginalFilename());
		// TODO check file image or not and save with token
		if (!isImage(fileExtension))
			return new ResponseObject(ResponseObject.RESPONSE_REQUEST_ERROR, "File is not image", null);
		String fileNewName = UUID.randomUUID().toString().replace("-", "");
		String fileName = fileNewName + "." + fileExtension;
		Path finalFilePath = PATH.resolve(Paths.get(fileName)).normalize().toAbsolutePath();
		if (!finalFilePath.getParent().equals(PATH.toAbsolutePath()))
			return new ResponseObject(ResponseObject.RESPONSE_SYSTEM_ERROR, "Something wrong with file system in server!", null);
		try {
			Files.copy(file.getInputStream(), finalFilePath, StandardCopyOption.REPLACE_EXISTING);
			ResponseObject tmp  = DataHandler.insertImage(fileName, data_token.get("userName").toString());
			if (tmp.getRespCode() != ResponseObject.RESPONSE_OK)
				return tmp;
			return new ResponseObject(ResponseObject.RESPONSE_OK, "File upload successfully!", fileName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ResponseObject(ResponseObject.RESPONSE_SYSTEM_ERROR, "Something wrong with file system in server!", null);
	}
	
	public ResponseObject loadImage(String fileName, String token) {
		if (!TokenProvider.isValidToken(token))
			return new ResponseObject(ResponseObject.RESPONSE_REQUEST_ERROR, "Login again to load file!", null);
		if (!isImage(FilenameUtils.getExtension(fileName)))
			return new ResponseObject(ResponseObject.RESPONSE_REQUEST_ERROR, "File is not image", null);
		try {
			Path file = PATH.resolve(fileName);
			Resource resource = new UrlResource(file.toUri());
			if (resource.exists() || resource.isReadable()) {
				byte[] data = StreamUtils.copyToByteArray(resource.getInputStream());
				return new ResponseObject(ResponseObject.RESPONSE_OK, "OK!", data);
			}
			return new ResponseObject(ResponseObject.RESPONSE_SYSTEM_ERROR, "Something wrong with file system in server!", null); 
		} catch (IOException e) { }
		return new ResponseObject(ResponseObject.RESPONSE_SYSTEM_ERROR, "Something wrong with file system in server!", null);
	}
	
}
