package com.sorbonne;

import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.*;
import java.util.stream.Stream;

public class Evaluate {

    @Context
    public Log log;
    @Context
    public GraphDatabaseService db;

    @Procedure(name = "nn.evaluate",mode = Mode.WRITE)
    @Description("")
    public void evaluate() {
        Transaction tx = db.beginTx();
        try {
            tx.execute("""
                MATCH (n:Neuron {type: 'output'})
                RETURN n.id AS id, n.expected_output AS expected
            """);
            tx.commit();
        } catch (Exception e) {
            log.error("Error constrain_weights : ", e);
            throw e;
        }
    }
}