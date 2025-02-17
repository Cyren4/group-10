package com.sorbonne;

import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.*;
import java.util.stream.Stream;

public class SetInputsOutputs {

    @Context
    public Log log;
    @Context
    public GraphDatabaseService db;

    @Procedure(name = "nn.set_inputs",mode = Mode.WRITE)
    @Description("")
    public void set_inputs(@Name("row_id") String row_id,
                           @Name("input_feature_id") String input_feature_id,
                           @Name("input_neuron_id") String input_neuron_id,
                           @Name("value") String value
    ) {
        Transaction tx = db.beginTx();
        try {
            String query = """
                    MATCH (row:Row {type: 'inputsRow', id: '$rowId'})-[r:CONTAINS {id: '$inputFeatureId'}]->(inputs:Neuron {type: 'input', id: '$inputNeuronId'})
                    SET r.output = $value
                """;

            // Execute the query
            tx.execute(query, Map.of(
                    "rowId", row_id,
                    "input_feature_id", input_feature_id,
                    "inputNeuronId", input_neuron_id,
                    "value", Double.parseDouble(value)
            ));
            tx.commit();
        } catch (Exception e) {
            log.error("Error set_inputs : ", e);
        }
    }

    @Procedure(name = "nn.set_expected_outputs",mode = Mode.WRITE)
    @Description("")
    public void set_expected_outputs(@Name("row_id") String row_id,
                           @Name("predicted_output_id") String predicted_output_id,
                           @Name("output_neuron_id") String output_neuron_id,
                           @Name("value") String value
    ) {
        Transaction tx = db.beginTx();
        try {
            String query = """
                    MATCH(:Neuron {type:'output', id: '$outputneuronid'})-[r:CONTAINS { id: '$predictedoutputid'}]->(row:Row {type:'outputsRow', id: '$rowid'})
                    SET r.expected_output = $value
                """;

            // Execute the query
            tx.execute(query, Map.of(
                    "rowid", row_id,
                    "predictedoutputid", predicted_output_id,
                    "outputneuronid", output_neuron_id,
                    "value", Double.parseDouble(value)
            ));
            tx.commit();
        } catch (Exception e) {
            log.error("Error set_expected_outputs : ", e);
        }
    }

}