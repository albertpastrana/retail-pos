import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KeepCatalog {
    public static void main(String[] args) throws Exception {
        String db = args[0];
        String csv = args[1];
        Set<String> keepCodes = loadCodes(csv);
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        Connection c = DriverManager.getConnection("jdbc:derby:" + db);
        c.setAutoCommit(false);
        try {
            List<String> dropIds = new ArrayList<String>();
            Set<String> matchedCodes = new HashSet<String>();
            Statement s = c.createStatement();
            ResultSet rs = s.executeQuery("SELECT ID, REFERENCE FROM PRODUCTS");
            int total = 0;
            while (rs.next()) {
                total++;
                String id = rs.getString(1);
                String ref = rs.getString(2);
                String code = matchingCode(ref != null ? ref : id, keepCodes);
                if (code == null) {
                    dropIds.add(id);
                } else {
                    matchedCodes.add(code);
                }
            }
            rs.close();

            PreparedStatement nullLines = c.prepareStatement(
                "UPDATE TICKETLINES SET PRODUCT = NULL WHERE PRODUCT = ?");
            PreparedStatement delCom1 = c.prepareStatement(
                "DELETE FROM PRODUCTS_COM WHERE PRODUCT = ? OR PRODUCT2 = ?");
            PreparedStatement delCat = c.prepareStatement(
                "DELETE FROM PRODUCTS_CAT WHERE PRODUCT = ?");
            PreparedStatement delDiary = c.prepareStatement(
                "DELETE FROM STOCKDIARY WHERE PRODUCT = ?");
            PreparedStatement delCurrent = c.prepareStatement(
                "DELETE FROM STOCKCURRENT WHERE PRODUCT = ?");
            PreparedStatement delLevel = c.prepareStatement(
                "DELETE FROM STOCKLEVEL WHERE PRODUCT = ?");
            PreparedStatement delProd = c.prepareStatement(
                "DELETE FROM PRODUCTS WHERE ID = ?");

            int n = 0;
            for (String id : dropIds) {
                nullLines.setString(1, id);
                nullLines.addBatch();
                delCom1.setString(1, id);
                delCom1.setString(2, id);
                delCom1.addBatch();
                delCat.setString(1, id);
                delCat.addBatch();
                delDiary.setString(1, id);
                delDiary.addBatch();
                delCurrent.setString(1, id);
                delCurrent.addBatch();
                delLevel.setString(1, id);
                delLevel.addBatch();
                delProd.setString(1, id);
                delProd.addBatch();
                n++;
                if (n % 400 == 0) {
                    executeAll(nullLines, delCom1, delCat, delDiary, delCurrent, delLevel, delProd);
                }
            }
            executeAll(nullLines, delCom1, delCat, delDiary, delCurrent, delLevel, delProd);

            s.executeUpdate("UPDATE CATEGORIES SET PARENTID = NULL");
            Set<String> usedCats = new HashSet<String>();
            rs = s.executeQuery("SELECT DISTINCT CATEGORY FROM PRODUCTS");
            while (rs.next()) {
                usedCats.add(rs.getString(1));
            }
            rs.close();
            PreparedStatement delEmpty = c.prepareStatement(
                "DELETE FROM CATEGORIES WHERE ID = ?");
            int empty = 0;
            rs = s.executeQuery("SELECT ID FROM CATEGORIES");
            List<String> catIds = new ArrayList<String>();
            while (rs.next()) {
                catIds.add(rs.getString(1));
            }
            rs.close();
            for (String catId : catIds) {
                if ("000".equals(catId) || usedCats.contains(catId)) {
                    continue;
                }
                delEmpty.setString(1, catId);
                delEmpty.addBatch();
                empty++;
            }
            if (empty > 0) {
                delEmpty.executeBatch();
            }

            int remaining = 0;
            rs = s.executeQuery("SELECT COUNT(*) FROM PRODUCTS");
            if (rs.next()) {
                remaining = rs.getInt(1);
            }
            rs.close();
            int catsLeft = 0;
            rs = s.executeQuery("SELECT COUNT(*) FROM CATEGORIES");
            if (rs.next()) {
                catsLeft = rs.getInt(1);
            }
            rs.close();

            c.commit();
            System.out.println("keep_codes=" + keepCodes.size()
                + " catalog_before=" + total
                + " dropped_products=" + dropIds.size()
                + " remaining_products=" + remaining
                + " matched_models=" + matchedCodes.size()
                + " unused_models=" + (keepCodes.size() - matchedCodes.size())
                + " dropped_categories=" + empty
                + " remaining_categories=" + catsLeft);
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

    private static void executeAll(PreparedStatement... sts) throws Exception {
        for (PreparedStatement st : sts) {
            st.executeBatch();
        }
    }

    private static String matchingCode(String ref, Set<String> keepCodes) {
        for (String k : keepCodes) {
            if (ref.equals(k)) {
                return k;
            }
            if (ref.startsWith(k) && (ref.length() == k.length()
                    || !Character.isDigit(ref.charAt(k.length())))) {
                return k;
            }
        }
        return null;
    }

    private static Set<String> loadCodes(String csv) throws Exception {
        Set<String> codes = new HashSet<String>();
        BufferedReader r = new BufferedReader(
            new InputStreamReader(new FileInputStream(csv), StandardCharsets.UTF_8));
        String header = r.readLine();
        String line;
        while ((line = r.readLine()) != null) {
            if (line.length() == 0) {
                continue;
            }
            int comma = line.indexOf(',');
            if (comma <= 0) {
                continue;
            }
            codes.add(line.substring(0, comma));
        }
        r.close();
        if (header == null || codes.isEmpty()) {
            throw new IllegalStateException("no product codes in " + csv);
        }
        return codes;
    }
}
