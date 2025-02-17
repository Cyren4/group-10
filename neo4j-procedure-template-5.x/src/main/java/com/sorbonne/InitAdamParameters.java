package com.sorbonne;

import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

        import java.util.*;
        import java.util.stream.Stream;

public class InitAdamParameters {

    @Context
    public Log log;
    @Context
    public GraphDatabaseService db;

    @Procedure(name = "nn.initialize_adam_parameters",mode = Mode.WRITE)
    @Description("")
    public void initialize_adam_parameters() {
        Transaction tx = db.beginTx();
        try {
            tx.execute("""
                    MATCH ()-[r:CONNECTED_TO]->()
                    SET r.m = 0.0, r.v = 0.0""");
            tx.execute("""
                    MATCH (n:Neuron)
                    SET n.m_bias = 0.0, n.v_bias = 0.0
                """);
            tx.commit();
        } catch (Exception e) {
            log.error("Error initialize_adam_parameters: ", e);
            throw e;
        }
    }
}