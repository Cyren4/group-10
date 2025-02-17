package com.sorbonne;

import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Evaluate {

    @Context
    public Log log;
    @Context
    public GraphDatabaseService db;
    public record EvaluationResult(String id, Object predicted) {
    }

    @Procedure(name="nn.evaluate_model", mode = Mode.READ)
    @Description("Evaluate the model on the test set")
    public Stream<EvaluationResult> evaluate_model() {
        try {
            Transaction tx = db.beginTx();

            Result result = tx.execute("""
                MATCH (n:Neuron {type: 'output'})
                RETURN n.id AS id, n.output AS predicted
            """);
            tx.commit();
            return result.stream()
                    .map(record -> new EvaluationResult(
                            record.get("id").toString(),
                            record.get("predicted")
                    ));
        } catch (Exception e) {
            log.error("Error evaluate_model : ", e);
            throw e;
        }
    }


/*
    public Map<String, Object> evaluate_model() {
        Transaction tx = db.beginTx();

        try {
            Result result = tx.execute("""
                        MATCH (n:Neuron {type: 'output'})
                        RETURN n.id AS id, n.output AS predicted
                    """);
            tx.commit();
            return result.stream()
                    .collect(Collectors.toMap(
                            record -> record.get("id").toString(),
                            record -> record.get("predicted")
                    ));
        } catch (Exception e) {
            log.error("Error evaluate_model : ", e);
            throw e;
        }
    }*/
}
