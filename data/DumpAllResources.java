import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DumpAllResources {
	public static void main(String[] args) throws Exception {
		String driver = args[0];
		String url = args[1];
		String outdir = args[2];
		Class.forName(driver);
		Connection c = DriverManager.getConnection(url);
		new File(outdir).mkdirs();
		try {
			Statement st = c.createStatement();
			ResultSet rs = st.executeQuery("SELECT NAME, RESTYPE, CONTENT FROM RESOURCES ORDER BY NAME");
			while (rs.next()) {
				String name = rs.getString(1);
				int type = rs.getInt(2);
				byte[] content = rs.getBytes(3);
				if (content == null) {
					System.out.println(name + " type=" + type + " bytes=null");
					continue;
				}
				String safe = name.replace('/', '_');
				String ext = type == 1 ? ".bin" : ".txt";
				File out = new File(outdir, safe + ext);
				FileOutputStream fos = new FileOutputStream(out);
				fos.write(content);
				fos.close();
				String peek = type == 1
						? ("image " + content.length + " bytes")
						: new String(content, "UTF-8").replace('\n', ' ').trim();
				if (peek.length() > 80) {
					peek = peek.substring(0, 80);
				}
				System.out.println(name + "\ttype=" + type + "\tbytes=" + content.length + "\t" + peek);
			}
			rs.close();
		} finally {
			c.close();
			if (driver.contains("derby")) {
				try {
					DriverManager.getConnection("jdbc:derby:;shutdown=true");
				} catch (Exception ignored) {
				}
			} else {
				try {
					DriverManager.getConnection(url.replace("readonly=true", "shutdown=true"));
				} catch (Exception ignored) {
				}
			}
		}
	}
}
