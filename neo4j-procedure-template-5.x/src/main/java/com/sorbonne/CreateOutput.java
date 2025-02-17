package com.sorbonne;

import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.*;
import java.util.stream.Stream;

public class CreateOutput {
    @Context
    public Log log;
    @Context
    public GraphDatabaseService db;


    @Procedure(name = "nn.createOutputRowNode",mode = Mode.WRITE)
    @Description("")
    public Stream<CreateNeuron.CreateResult> createInputRowNode(@Name("id") String id
    ) {
        try (Transaction tx = db.beginTx()) {

            tx.execute("CREATE (n:Row {\n" +
                    "id:'" + id + "',\n" +
                    "type:'outputsRow'\n"+
                    "})");
            tx.commit();
            return Stream.of(new CreateNeuron.CreateResult("createOutputRowNode: ok"));

        } catch (Exception e) {
            log.error("Error creating output row node: ", e);
            return Stream.of(new CreateNeuron.CreateResult("createOutputRowNode : ko"));
        }
    }

    @Procedure(name = "nn.createOutputsRelationShips",mode = Mode.WRITE)
    @Description("")
    public Stream<CreateNeuron.CreateResult> createInputsRelationShips(@Name("from_id") String from_id,
                                                                       @Name("to_id") String to_id,
                                                                       @Name("output_feature_id") String output_feature_id,
                                                                       @Name("value") long value
    ) {
        try (Transaction tx = db.beginTx()) {

            tx.execute(
                    "MATCH (n1:Neuron {id:'"+ from_id +"',type:'output'})\n" +
                            "MATCH (n2:Row {id:'"+ to_id +"',type:'outputsRow'})\n" +
                            "CREATE (n1)-[:CONTAINS {output:'" + value + "',id:'"+ output_feature_id +"'}]->(n2)"
            );
            tx.commit();
            return Stream.of(new CreateNeuron.CreateResult("createOutputsRelationShips: ok"));

        } catch (Exception e) {
            log.error("Error creating output row connection: ", e);
            return Stream.of(new CreateNeuron.CreateResult("createOutputsRelationShips : ko"));
        }
    }
}
