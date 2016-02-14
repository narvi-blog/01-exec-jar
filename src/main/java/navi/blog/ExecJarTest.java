package navi.blog;

import org.flywaydb.core.Flyway;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static java.lang.System.out;

public class ExecJarTest {

        public static void main(String[] argv) {

            if(argv.length < 1) {
                out.print("Required at least one argument: filename with db configuration (check db.conf.example).");
                return;
            }

            try(InputStream input = new FileInputStream(argv[0])) {
                Properties prop = new Properties();
                prop.load(input);
                String url = prop.getProperty("db.url");
                String username = prop.getProperty("db.username");
                String password = prop.getProperty("db.password");

                Flyway flyway = new Flyway();
                flyway.setBaselineOnMigrate(true);
                flyway.setDataSource(url, username, password);

                flyway.migrate();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
}
