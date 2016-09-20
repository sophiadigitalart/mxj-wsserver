package wsserver.server;

import java.io.File;

import com.cycling74.max.MaxObject;

/*
 * 
 * The FileResolver finds Files from the given root path.
 * The chars ".." are forbidden so the root's parents can't be accessed.
 * When the given path is a directory containing a file called index.html, that file is returned.
 * When no file is found, FileResolver returns null.
 * 
 * There is no unpacking, caching etc, so added/modified/removed files will always be up-to-date.
 * 
 */
public class FileResolver {

	private File root;
	
	public File getRoot() {
		return root;
	}
	
	public String getRootPath() {
		return root != null ? root.getAbsolutePath() : "";
	}
	public void setRoot(File root) {
		if(!root.exists() || !root.isDirectory()) throw new IllegalArgumentException("root must be directory");
		this.root = root;
		MaxObject.post("FileResolver: set file root to " + root.getAbsolutePath());
	}

	public boolean canServeFileType(String filename) {
		if(filename.endsWith(".html")) return true;
		if(filename.endsWith(".js")) return true;
		if(filename.endsWith(".css")) return true;
		if(filename.endsWith(".jpg")) return true;
		if(filename.endsWith(".jpeg")) return true;
		if(filename.endsWith(".png")) return true;
		if(filename.endsWith(".gif")) return true;
		return false;
	}
	
	public File get(String path) {
		if(root == null) return null;
		if(path.contains("..")) return null;
		if("/".equals(path)) return findIndex(root);
		path = path.replace("/", File.separator);
		File file = new File (root, path);
		if(!file.exists()) return null;
		if(!file.isDirectory()) {
			if(!canServeFileType(file.getName())) return null;
			return file;
		}
		return findIndex(file);
	}
	
	static File findIndex(File root) {
		File index = new File(root, "index.html");
		if(index.exists()) return index;
		return null;
	}
}
