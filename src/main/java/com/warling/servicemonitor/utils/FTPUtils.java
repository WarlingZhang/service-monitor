package com.warling.servicemonitor.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;

/**
 * 支持断点续传的FTP实用类
 * @version 0.1 实现基本断点上传下载
 * @version 0.2 实现上传下载进度汇报
 * @version 0.3 实现中文目录创建及中文文件创建，添加对于中文的支持
 */ 
public class FTPUtils {   
	//枚举类UploadStatus代码 
	public enum UploadStatus { 
		Create_Directory_Fail,   //远程服务器相应目录创建失败 
		Create_Directory_Success, //远程服务器闯将目录成功 
		Upload_New_File_Success, //上传新文件成功 
		Upload_New_File_Failed,   //上传新文件失败 
		File_Exits,      //文件已经存在 
		Remote_Bigger_Local,   //远程文件大于本地文件 
		Upload_From_Break_Success, //断点续传成功 
		Upload_From_Break_Failed, //断点续传失败 
		Delete_Remote_Faild;   //删除远程文件失败 
	} 
	//枚举类DownloadStatus代码 
	public enum DownloadStatus { 
		Remote_File_Noexist, //远程文件不存在 
		Local_Bigger_Remote, //本地文件大于远程文件 
		Download_From_Break_Success, //断点下载文件成功 
		Download_From_Break_Failed,   //断点下载文件失败 
		Download_New_Success,    //全新下载文件成功 
		Download_New_Failed;    //全新下载文件失败 
	}   
    private static Logger logger = Logger.getLogger(FTPUtils.class);  
    
    // 本地编码
    private String localCharset = "GBK";
    
    // FTP协议里面，规定文件名编码为iso-8859-1
    private static String SERVER_CHARSET = "ISO-8859-1";
    
    public FTPClient ftpClient;
    
    public ArrayList<String> fileNameList; 
    public ArrayList<String> filePathList;
    
    public FTPUtils(boolean isPrintCommmand){
    	ftpClient = new FTPClient();  
    	fileNameList = new ArrayList<String>();  
    	filePathList = new ArrayList<String>();
        if(isPrintCommmand){
        	//设置将过程中使用到的命令输出到控制台   
        	//ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));  
        }   
	}   
    /**   
     * 连接到FTP服务器  
     * @param hostname 主机名  
     * @param port 端口  
     * @param username 用户名  
     * @param password 密码  
     * @return 是否连接成功  
     * @throws IOException  
     */  
    public boolean connect(String hostname,int port,String username,String password) throws IOException{   
        ftpClient.connect(hostname, port);   
//        ftpClient.setControlEncoding(localCharset);
        if(FTPReply.isPositiveCompletion(ftpClient.getReplyCode())){
            if(ftpClient.login(username, password)){
            	if (FTPReply.isPositiveCompletion(ftpClient.sendCommand(
            			"OPTS UTF8", "ON"))) {
            		// 开启服务器对UTF-8的支持，如果服务器支持就用UTF-8编码，否则就使用本地编码（GBK）.
            		localCharset = "UTF-8";
            	}
    			ftpClient.setControlEncoding(localCharset);
    			ftpClient.enterLocalPassiveMode();// 设置被动模式
                return true;   
            }   
        }   
        disconnect();   
        return false;   
    }   
    /** *//**  
     * 从FTP服务器上下载文件,支持断点续传，上传百分比汇报  
     * @param remote 远程文件路径  
     * @param local 本地文件路径  
     * @return 上传的状态  
     * @throws IOException  
     */  
    public DownloadStatus download(String remote,String local) throws IOException{   
        //设置被动模式   
        ftpClient.enterLocalPassiveMode();   
        //设置以二进制方式传输   
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);   
        DownloadStatus result;   
        //检查远程文件是否存在   
        FTPFile[] files = ftpClient.listFiles(new String(remote.getBytes(localCharset),SERVER_CHARSET));   
        if(files.length != 1){   
            System.out.println("远程文件不存在");   
            return DownloadStatus.Remote_File_Noexist;   
        }   
        long lRemoteSize = files[0].getSize();   
        File f = new File(local);   
        //本地存在文件，进行断点下载   
        if(f.exists()){   
            long localSize = f.length();   
            //判定本地文件大小是否大于远程文件大小   
            if(localSize >= lRemoteSize){   
                System.out.println("本地文件大于远程文件，下载中止");   
                return DownloadStatus.Local_Bigger_Remote;   
            }   
            //进行断点续传，并记录状态   
            FileOutputStream out = new FileOutputStream(f,true);   
            ftpClient.setRestartOffset(localSize);   
            InputStream in = ftpClient.retrieveFileStream(new String(remote.getBytes(localCharset),SERVER_CHARSET));   
            byte[] bytes = new byte[1024];   
            long step = lRemoteSize /100;   
            long process=localSize /step;   
            int c;   
            while((c = in.read(bytes))!= -1){   
                out.write(bytes,0,c);   
                localSize+=c;   
                long nowProcess = localSize /step;   
                if(nowProcess > process){   
                    process = nowProcess;   
                    if(process % 10 == 0)   
                        System.out.println("下载进度："+process);   
                    //TODO 更新文件下载进度,值存放在process变量中   
                }   
            }   
            in.close();   
            out.close();   
            boolean isDo = ftpClient.completePendingCommand();   
            if(isDo){   
                result = DownloadStatus.Download_From_Break_Success;   
            }else {   
                result = DownloadStatus.Download_From_Break_Failed;   
            }   
        }else {
            OutputStream out = new FileOutputStream(f);   
            InputStream in= ftpClient.retrieveFileStream(new String(remote.getBytes(localCharset),SERVER_CHARSET));   
            byte[] bytes = new byte[1024];   
            long step = lRemoteSize /100;   
            long process=0;   
            long localSize = 0L;   
            int c;   
            while((c = in.read(bytes))!= -1){   
                out.write(bytes, 0, c);   
                localSize+=c;
                if (step >= 1) {
	                long nowProcess = localSize /step;   
	                if(nowProcess > process){   
	                    process = nowProcess;   
	                    if(process % 10 == 0)   
	                        System.out.println("下载进度："+process);   
	                    //TODO 更新文件下载进度,值存放在process变量中   
	                }
                }
            }   
            in.close();   
            out.close();   
            boolean upNewStatus = ftpClient.completePendingCommand();   
            if(upNewStatus){   
                result = DownloadStatus.Download_New_Success;   
            }else {   
                result = DownloadStatus.Download_New_Failed;   
            }   
        }   
        return result;   
    }   
    
    /**
     * 
     * @param ftpPath
     * @return
     * @throws IOException
     */
    public InputStream getInputStream(String ftpPath) throws IOException{   
        //设置被动模式
        ftpClient.enterLocalPassiveMode();
        //设置以二进制方式传输
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        //检查远程文件是否存在
        ftpPath = new String(ftpPath.getBytes(localCharset),SERVER_CHARSET);
        FTPFile[] files = ftpClient.listFiles(ftpPath);
        if(files.length != 1){
            System.out.println("远程文件不存在");
            return null;
        }
        InputStream in= ftpClient.retrieveFileStream(ftpPath);
        return in;
    }
    
    /**
     * 判断文件是否存在
     * @param ftpPath
     * @return
     * @throws IOException
     */
    public boolean exists(String ftpPath) throws IOException{
    	if (StringUtils.isBlank(ftpPath)) {
    		return false;
    	}
        //设置被动模式
        ftpClient.enterLocalPassiveMode();
        //设置以二进制方式传输
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        //检查远程文件是否存在
        ftpPath = new String(ftpPath.getBytes(localCharset),SERVER_CHARSET);
        FTPFile[] files = ftpClient.listFiles(ftpPath);
        if(files.length != 1){
            System.out.println("远程文件不存在");
            return false;
        }
        return true;
    }
    
    /**   
     * 上传文件到FTP服务器，支持断点续传  
     * @param local 本地文件名称，尽对路径  
     * @param remote 远程文件路径，使用/home/directory1/subdirectory/file.ext或是 http://www.guihua.org /subdirectory/file.ext 按照Linux上的路径指定方式，支持多级目录嵌套，支持递回创建不存在的目录结构  
     * @return 上传结果  
     * @throws IOException  
     */  
    public UploadStatus upload(String local,String remote) throws IOException{   
        //设置PassiveMode传输
        ftpClient.enterLocalPassiveMode();
        //设置以二进制流的方式传输
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        UploadStatus result;
        //对远程目录的处理
        String remoteFileName = remote;
        if(remote.contains("/")){
            remoteFileName = remote.substring(remote.lastIndexOf("/")+1);
            //创建服务器远程目录结构，创建失败直接返回   
            if(CreateDirecroty(remote, ftpClient)==UploadStatus.Create_Directory_Fail){
                return UploadStatus.Create_Directory_Fail;   
            }   
        }   
        //检查远程是否存在文件   
        FTPFile[] files = ftpClient.listFiles(new String(remoteFileName.getBytes(localCharset),SERVER_CHARSET));   
        if(files.length == 1){   
            long remoteSize = files[0].getSize();   
            File f = new File(local);   
            long localSize = f.length();   
            if(remoteSize==localSize){   
                return UploadStatus.File_Exits;   
            }else if(remoteSize > localSize){   
                return UploadStatus.Remote_Bigger_Local;   
            }   
            //尝试移动文件内读取指针,实现断点续传   
            result = uploadFile(remoteFileName, f, ftpClient, remoteSize);   
            //假如断点续传没有成功，则删除服务器上文件，重新上传   
            if(result == UploadStatus.Upload_From_Break_Failed){   
                if(!ftpClient.deleteFile(remoteFileName)){   
                    return UploadStatus.Delete_Remote_Faild;   
                }   
                result = uploadFile(remoteFileName, f, ftpClient, 0);   
            }   
        }else {   
            result = uploadFile(remoteFileName, new File(local), ftpClient, 0);   
        }   
        return result;   
    }   
    /**   
     * 断开与远程服务器的连接  
     * @throws IOException  
     */  
    public void disconnect() throws IOException{   
        if(ftpClient.isConnected()){   
            ftpClient.disconnect();   
        }   
    }   
    /**   
     * 递回创建远程服务器目录  
     * @param remote 远程服务器文件尽对路径  
     * @param ftpClient FTPClient 对象  
     * @return 目录创建是否成功  
     * @throws IOException  
     */  
    public UploadStatus CreateDirecroty(String remote,FTPClient ftpClient) throws IOException{   
        UploadStatus status = UploadStatus.Create_Directory_Success;   
        String directory = remote.substring(0,remote.lastIndexOf("/")+1);   
        if(!directory.equalsIgnoreCase("/")&&!ftpClient.changeWorkingDirectory(new String(directory.getBytes(localCharset),SERVER_CHARSET))){   
            //假如远程目录不存在，则递回创建远程服务器目录   
            int start=0;   
            int end = 0;   
            if(directory.startsWith("/")){   
                start = 1;   
            }else{
                start = 0;   
            }   
            end = directory.indexOf("/",start);   
            while(true){   
                String subDirectory = new String(remote.substring(start,end).getBytes(localCharset),SERVER_CHARSET);   
                if(!ftpClient.changeWorkingDirectory(subDirectory)){   
                    if(ftpClient.makeDirectory(subDirectory)){   
                        ftpClient.changeWorkingDirectory(subDirectory);   
                    }else {   
                        System.out.println("创建目录失败");   
                        return UploadStatus.Create_Directory_Fail;   
                    }   
                }   
                start = end + 1;   
                end = directory.indexOf("/",start);   
                //检查所有目录是否创建完毕   
                if(end <= start){   
                    break;   
                }   
            }   
        }   
        return status;
    }   
    /**   
     * 上传文件到服务器,新上传和断点续传  
     * @param remoteFile 远程文件名，在上传之前已经将服务器工作目录做了改变  
     * @param localFile 本地文件 File句柄，尽对路径  
     * @param processStep 需要显示的处理进度步进值  
     * @param ftpClient FTPClient 引用  
     * @return  
     * @throws IOException  
     */  
    public UploadStatus uploadFile(String remoteFile,File localFile,FTPClient ftpClient,long remoteSize) throws IOException{   
        UploadStatus status;   
        //显示进度的上传   
        long step = localFile.length() / 100;   
        long process = 0;   
        long localreadbytes = 0L;   
        RandomAccessFile raf = new RandomAccessFile(localFile,"r");   
        OutputStream out = ftpClient.appendFileStream(new String(remoteFile.getBytes(localCharset),SERVER_CHARSET));   
        //断点续传   
        if(remoteSize>0){   
            ftpClient.setRestartOffset(remoteSize);   
            process = remoteSize /step;   
            raf.seek(remoteSize);   
            localreadbytes = remoteSize;   
        }   
        byte[] bytes = new byte[1024];   
        int c;   
        while((c = raf.read(bytes))!= -1){   
            out.write(bytes,0,c);   
            localreadbytes+=c;   
            if(localreadbytes / step != process){   
                process = localreadbytes / step;   
                System.out.println("上传进度:" + process);   
                //TODO 汇报上传状态   
            }   
        }   
        out.flush();   
        raf.close();   
        out.close();   
        boolean result =ftpClient.completePendingCommand();   
        if(remoteSize > 0){   
            status = result?UploadStatus.Upload_From_Break_Success:UploadStatus.Upload_From_Break_Failed;   
        }else {   
            status = result?UploadStatus.Upload_New_File_Success:UploadStatus.Upload_New_File_Failed;   
        }   
        return status;   
    }  
    
    /** 
     * 递归遍历出目录下面所有文件 
     * @param pathName 需要遍历的目录，必须以"/"开始和结束 
     * @throws IOException 
     */  
    /*public void List(String pathName) throws IOException{  
        if(pathName.startsWith("/")&&pathName.endsWith("/")){  
            String directory = pathName;  
            //更换目录到当前目录  
            ftpClient.changeWorkingDirectory(directory);  
            FTPFile[] files = ftpClient.listFiles();  
            for(FTPFile file:files){  
                if(file.isFile()){ 
                	String str[] = {replaceName(file.getName()),directory+file.getName()};
                	arFiles.add(str);
                }else if(file.isDirectory()){  
                    List(directory+file.getName()+"/");  
                }  
            }  
        }  
    } */ 
      
    /** 
     * 递归遍历目录下面指定的文件名 
     * @param pathName 需要遍历的目录，必须以"/"开始和结束 
     * @param ext 文件的扩展名 
     * @throws IOException  
     */  
    public void List(String pathName,String ext) throws IOException{
    	List(pathName, ext, false);
    }
	/** 
     * 递归遍历目录下面指定的文件名 
     * @param pathName 需要遍历的目录，必须以"/"开始和结束 
     * @param ext 文件的扩展名 
     * @throws IOException  
     */  
    public void List(String pathName,String ext, boolean isCaseSensitive) throws IOException{
        String directory = new String(pathName.getBytes(localCharset),SERVER_CHARSET);
        if (!isCaseSensitive) {
        	ext = ext.toUpperCase();
        }
        String[] exts = ext.split(",");
        //更换目录到根目录  
        ftpClient.changeWorkingDirectory("//");
        //更换目录到当前目录  
        boolean b = ftpClient.changeWorkingDirectory(directory); 
        if (b) {
        	ftpClient.enterLocalPassiveMode();
        	FTPFile[] files = ftpClient.listFiles();  
            for(FTPFile file:files){  
                if(file.isFile()){ 
                	String prefix = file.getName().substring(file.getName().lastIndexOf(".")+1);
                	if (!isCaseSensitive) {
                		prefix = prefix.toUpperCase();
                	}
                    if(checkValue(exts,prefix)){
                    	String fileName = file.getName().substring(0,file.getName().lastIndexOf("."));
                    	String filePath = pathName+file.getName();
                    	fileNameList.add(replaceName(fileName));
                    	filePathList.add(filePath);
                    }  
                }else if(file.isDirectory()){  
                    List(pathName+file.getName()+"//",ext, isCaseSensitive);  
                }  
            }
		}       
    }  
    
    /** 
     * 递归遍历目录下面指定的文件名 
     * @param pathName 需要遍历的目录，必须以"/"开始和结束 
     * @param ext 文件的扩展名 
     * @throws IOException  
     */  
    public void List(String pathName) throws IOException{           
        String directory = new String(pathName.getBytes(localCharset),SERVER_CHARSET);
        //更换目录到根目录  
        ftpClient.changeWorkingDirectory("//");
        //更换目录到当前目录  
        boolean b = ftpClient.changeWorkingDirectory(directory); 
        if (b) {
        	ftpClient.enterLocalPassiveMode();
        	FTPFile[] files = ftpClient.listFiles();  
            for(FTPFile file:files){  
                if(file.isFile()){
                    String srcFileName = file.getName();
                    String fileName = null;
                    if (srcFileName.endsWith("tmp")) {
                    	continue;
					}
                    if (-1 == srcFileName.indexOf(".")) {
                        fileName = srcFileName;
                    } else {
                        fileName = srcFileName.substring(0, srcFileName.lastIndexOf("."));
                    }
                	String filePath = pathName+srcFileName;
                	fileNameList.add(replaceName(fileName));
                	filePathList.add(filePath);
                }else if(file.isDirectory()){
                    List(pathName+file.getName()+"//");
                }  
            }
		}       
    } 
    
    //判断数组是否存在某个元素
    public static boolean checkValue(String[] arr, String targetValue) {
    	return  Arrays.asList(arr).contains(targetValue);
    }
    
    public static String replaceName(String str){
    	if (StringUtils.isBlank(str)) {
    		return str;
    	}
 		String newFileName = str.replaceAll(" ", "");
 		if(newFileName.contains("【")){
			newFileName = newFileName.replaceAll("【", "[");
		}
		if(newFileName.contains("】")){
			newFileName = newFileName.replaceAll("】", "]");
		}
		if(newFileName.contains("（")){
			newFileName = newFileName.replaceAll("（", "[");
		}
		if(newFileName.contains("）")){
			newFileName = newFileName.replaceAll("）", "]");
		}
		if(newFileName.contains("〔")){
			newFileName = newFileName.replaceAll("〔", "[");
		}
		if(newFileName.contains("〕")){
			newFileName = newFileName.replaceAll("〕", "]");
		}
		if(newFileName.contains("﹝")){
			newFileName = newFileName.replaceAll("﹝", "[");
		}
		if(newFileName.contains("﹞")){
			newFileName = newFileName.replaceAll("﹞", "]");
		}
		if(newFileName.contains("－")){
			newFileName = newFileName.replaceAll("－", "-");
		}
        newFileName = newFileName.replaceAll("[［|(]", "[");
        newFileName = newFileName.replaceAll("[］|)]", "]");
 		return newFileName;
 	}
    
    public static void main(String[] args) {
    	long startTime=System.currentTimeMillis();   //获取开始时间

    	java.util.List<String> fileNameList = new ArrayList<String>();
    	java.util.List<String> filePathList = new ArrayList<String>();
    	//String path = "安徽省环保厅//建设项目//";//手工路径
		//String privatePath = "专网交换平台数据//环评项目//安徽//";//专网路径
    	String path = "安徽省环保厅//验收项目//";//手工路径
    	String privatePath = "专网交换平台数据//验收项目//安徽//";//专网路径
    	//获取文件名集合
		String suffix = "doc,docx,pdf,rar,zip,xlsx,xls";
		FTPUtils myFtp = new FTPUtils(true);
		try {
			if(myFtp.connect("10.102.33.168", 21, "kuaiwei", "zaq12WSX")){
				myFtp.List(path,suffix);
				myFtp.List(privatePath,suffix);
				fileNameList = myFtp.fileNameList;
				filePathList = myFtp.filePathList;
		        myFtp.disconnect();
			}
		} catch (IOException e) {
			logger.error("连接FTP出错", e);
            System.out.println("连接FTP出错："+e.getMessage());
        }
		
        for (int i = 0; i < fileNameList.size(); i++) {
        	String a = fileNameList.get(i);
        	String b = filePathList.get(i);
        	int c = fileNameList.indexOf(a);
            System.out.println("--序号--"+c+"--文件名--"+a+"--文件路径--"+b);
		}
        
        System.out.println("-----"+fileNameList.size());
        
        long endTime=System.currentTimeMillis(); //获取结束时间

    	System.out.println("程序运行时间： "+(endTime-startTime)+"ms");   	
    }
    
    /**
     * 获取子目录列表
     * @param parentPath
     * @return
     * @throws IOException
     */
	public FTPFile[] getChildDir(String parentPath) throws IOException {
		parentPath = new String(parentPath.getBytes(localCharset), SERVER_CHARSET);
		boolean success = ftpClient.changeWorkingDirectory(parentPath);
		FTPFile[] result = null;
        if (success) {
            result = ftpClient.listDirectories();
        }
		return result;
	}
	
	/**
	 * 获取指定目录下的所有文件列表
	 * @param parentPath
	 * @return
	 * @throws IOException 
	 */
	public FTPFile[] getChildFiles(String parentPath) throws IOException {
        FTPFile[] result = null;
        parentPath = new String(parentPath.getBytes(localCharset), SERVER_CHARSET);
        boolean success = ftpClient.changeWorkingDirectory(parentPath);
        if (success) {
            result = ftpClient.listFiles();
        }
		return result;
	}

}
