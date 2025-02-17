package com.sorbonne;

import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.*;
import java.util.stream.Stream;

public class ConstrainWeights {

    @Context
    public Log log;
    @Context
    public GraphDatabaseService db;

    @Procedure(name = "nn.constrain_weights",mode = Mode.WRITE)
    @Description("")
    public void constrain_weights() {
        Transaction tx = db.beginTx();
        try {
            tx.execute("""
                MATCH ()-[r:CONNECTED_TO]->()
                SET r.weight = CASE 
                    WHEN r.weight > 1.0 THEN 1.0 
                    WHEN r.weight < -1.0 THEN -1.0 
                    ELSE r.weight 
                END
            """);
            tx.commit();
        } catch (Exception e) {
            log.error("Error constrain_weights : ", e);
        }
    }
}