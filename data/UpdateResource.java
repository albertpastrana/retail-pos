import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class UpdateResource {
    public static void main(String[] args) throws Exception {
        String db = args[0];
        String name = args[1];
        String file = args[2];
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        Connection c = DriverManager.getConnection("jdbc:derby:" + db);
        c.setAutoCommit(false);
        try {
            PreparedStatement upd = c.prepareStatement(
                "UPDATE RESOURCES SET CONTENT = ? WHERE NAME = ?");
            upd.setBytes(1, readAll(file));
            upd.setString(2, name);
            int rows = upd.executeUpdate();
            c.commit();
            System.out.println("updated " + rows + " row(s) for " + name);
        } catch (Exception e) {
            c.rollback();
            throw e;
        } finally {
            c.close();
            try {
                DriverManager.getConnection("jdbc:derby:;shutdown=true");
            } catch (Exception ignored) {
            }
        }
    }

    private static byte[] readAll(String path) throws Exception {
        InputStream in = new FileInputStream(path);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        in.close();
        return out.toByteArray();
    }
}
