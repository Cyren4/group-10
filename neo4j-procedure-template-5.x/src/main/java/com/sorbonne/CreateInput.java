package com.sorbonne;

import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.*;
import java.util.stream.Stream;

public class CreateInput {
    @Context
    public Log log;
    @Context
    public GraphDatabaseService db;


    @Procedure(name = "nn.createInputRowNode",mode = Mode.WRITE)
    @Description("")
    public Stream<CreateNeuron.CreateResult> createInputRowNode(@Name("id") String id
    ) {
        try (Transaction tx = db.beginTx()) {

            tx.execute("CREATE (n:Row {\n" +
                    "id:'" + id + "',\n" +
                    "type:'inputsRow'\n"+
                    "})");
            tx.commit();
            return Stream.of(new CreateNeuron.CreateResult("createInputRow: ok"));

        } catch (Exception e) {
            log.error("Error creating input row node: ", e);
            return Stream.of(new CreateNeuron.CreateResult("createInputRow : ko"));
        }
    }

    @Procedure(name = "nn.createInputsRelationShips",mode = Mode.WRITE)
    @Description("")
    public Stream<CreateNeuron.CreateResult> createInputsRelationShips(@Name("from_id") String from_id,
                                                                       @Name("to_id") String to_id,
                                                                       @Name("input_feature_id") String input_feature_id,
                                                                       @Name("value") String value
    ) {
        try (Transaction tx = db.beginTx()) {

            tx.execute(
                 "MATCH (n1:Row {id:'"+ from_id +"',type:'inputsRow'})\n" +
                    "MATCH (n2:Neuron {id:'"+ to_id +"',type:'input'})\n" +
                    "CREATE (n1)-[:CONTAINS {output:" + Double.parseDouble(value) + ",id:'"+ input_feature_id +"'}]->(n2)"
            );
            tx.commit();
            return Stream.of(new CreateNeuron.CreateResult("createInputsRelationShips: ok"));

        } catch (Exception e) {
            log.error("Error creating input row connection: ", e);
            return Stream.of(new CreateNeuron.CreateResult("createInputsRelationShips : ko"));
        }
    }
}
