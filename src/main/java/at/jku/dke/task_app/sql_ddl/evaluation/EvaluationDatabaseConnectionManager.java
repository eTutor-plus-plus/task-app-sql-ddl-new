package at.jku.dke.task_app.sql_ddl.evaluation;

import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

@Service
public class EvaluationDatabaseConnectionManager {

    public Connection openForSubmission(Long taskId) throws SQLException {
        String dbName = "sql_ddl_eval_" + taskId + "_" + UUID.randomUUID();
        String h2Url = "jdbc:h2:mem:" + dbName + ";MODE=Oracle";
        Connection connection = DriverManager.getConnection(h2Url, "sa", "");
        connection.setAutoCommit(false);
        return connection;
    }
}
