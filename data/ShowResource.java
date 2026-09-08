import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ShowResource {
    public static void main(String[] args) throws Exception {
        String db = args[0];
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        Connection c = DriverManager.getConnection("jdbc:derby:" + db);
        try {
            PreparedStatement st = c.prepareStatement(
                "SELECT CONTENT FROM RESOURCES WHERE NAME = ?");
            for (int i = 1; i < args.length; i++) {
                st.setString(1, args[i]);
                ResultSet rs = st.executeQuery();
                System.out.println("=== " + args[i] + " ===");
                while (rs.next()) {
                    String xml = new String(rs.getBytes(1), "UTF-8");
                    for (String l : xml.split("\n")) {
                        if (l.contains("column") || l.contains("taxesincluded")
                                || l.contains("taxcategoryid") || l.contains("pricevisible")) {
                            System.out.println(l.trim());
                        }
                    }
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
