import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DumpResource {
    public static void main(String[] args) throws Exception {
        String db = args[0];
        String outdir = args[1];
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        Connection c = DriverManager.getConnection("jdbc:derby:" + db);
        try {
            PreparedStatement st = c.prepareStatement(
                "SELECT CONTENT FROM RESOURCES WHERE NAME = ?");
            for (int i = 2; i < args.length; i++) {
                st.setString(1, args[i]);
                ResultSet rs = st.executeQuery();
                if (rs.next()) {
                    FileOutputStream out = new FileOutputStream(outdir + "/" + args[i] + ".xml");
                    out.write(rs.getBytes(1));
                    out.close();
                    System.out.println("dumped " + args[i]);
                } else {
                    System.out.println("missing " + args[i]);
                }
                rs.close();
            }
        } finally {
            c.close();
            try {
                DriverManager.getConnection("jdbc:derby:;shutdown=true");
            } catch (Exception ignored) {
            }
        }
    }
}
