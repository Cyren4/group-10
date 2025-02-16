package com.sorbonne;

import org.eclipse.jetty.util.Index;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.neo4j.annotations.Public;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.*;
import java.util.stream.Stream;


public class CreateNeuron {
    // This gives us a log instance that outputs messages to the
    // standard log, normally found under `data/log/console.log`
    @Context
    public Log log;
    @Context
    public GraphDatabaseService db;


//    call nn.createNetwork([3,2,3], "classification", "relu", "sigmoid")
    @Procedure(name = "nn.createNetwork",mode = Mode.WRITE)
    @Description("")
    public Stream<CreateResult> createNetwork(@Name("network_structure") List<Long> network_structure,
                                              @Name("task_type") String task_type,
                                              @Name("hidden_activation") String hidden_activation,
                                              @Name("output_activation") String output_activation

    ) {
//        clear previous NN
        clearExistingNetwork();

//        Null network handling
        if (network_structure == null || network_structure.isEmpty()) {
            return Stream.of(new CreateResult("ko: network_structure is null or empty"));
        }

        try (Transaction tx = db.beginTx()) {
            // call function to create all neurons
            createAllNeurons(network_structure, task_type, hidden_activation, output_activation);
            // call function to create all neurons
            createAllRelations(network_structure, task_type, hidden_activation, output_activation);
        }  catch (Exception e) {
        log.error("Error creating neuron or relationship: ", e);
        return Stream.of(new CreateResult("ko"));
    }

        return Stream.of(new CreateResult("ok"));
    }



    @NotNull
    @Contract(pure = true)
    public Stream<CreateResult> createAllRelations(@Name("network_structure") List<Long> network_structure,
                                                 @Name("task_type") String task_type,
                                                 @Name("hidden_activation") String hidden_activation,
                                                 @Name("output_activation") String output_activation

    ) {
        try (Transaction tx = db.beginTx()) {
            Random random = new Random();
            //       for row_index in range(batch_size):  # Iterate over each row
            for (int layer_index = 0; layer_index < network_structure.size() - 1; layer_index++) {
                long num_neurons_current = network_structure.get(layer_index);
                long num_neurons_next = network_structure.get(layer_index + 1);

                for (long i = 0; i < num_neurons_current; i++) {
                    for (long j = 0; j < num_neurons_next; j++) {
                        Double weight = generateWeight(num_neurons_current, num_neurons_next);
                        String from_id = String.format("%d-%d", layer_index, i);
                        String to_id = String.format("%d-%d", layer_index + 1, j);
                        createRelationShipsNeuron(from_id, to_id, Double.toString(weight));
                    }
                }
            }
        }  catch (Exception e) {
            log.error("Error creating neurons: ", e);
            return Stream.of(new CreateResult("ko"));
        }
        return Stream.of(new CreateResult("ok"));
    }

    @NotNull
    @Contract(pure = true)
    public Stream<CreateResult> createAllNeurons(@Name("network_structure") List<Long> network_structure,
                                              @Name("task_type") String task_type,
                                              @Name("hidden_activation") String hidden_activation,
                                              @Name("output_activation") String output_activation
    ) {
        try (Transaction tx = db.beginTx()) {
            //       for row_index in range(batch_size):  # Iterate over each row
            for (int layer_index = 0; layer_index < network_structure.size(); layer_index++) {
                String layer;
                long num_neurons = network_structure.get(layer_index);
                // Assign name of layer
                layer = layer_index == 0 ? "input" : layer_index == network_structure.size() - 1 ? "output" : "hidden";

                for (int neuron_index = 0; neuron_index < network_structure.get(layer_index); neuron_index++) {
                    String activation = "None";
//               Assign activation function
                    if (layer.equals("hidden"))
                        activation = hidden_activation == null ? "relu" : hidden_activation; // Use user-specified or default activation
                    else if (layer.equals("output"))
                        activation = output_activation == null ? task_specific_activation(task_type, num_neurons) : output_activation; // Default to task-specific activation

                    String id = String.format("%d-%d", layer_index, neuron_index);
                    createNeuron(id, String.format("%d", layer_index), layer, activation);
                }
            }
        }  catch (Exception e) {
            log.error("Error creating neurons: ", e);
            return Stream.of(new CreateResult("ko"));
        }
        return Stream.of(new CreateResult("ok"));
    }
    @NotNull
    @Contract(pure = true)
    private static String task_specific_activation(String task_type, Long num_neurons){
        if (task_type.equals("classification"))
            if (num_neurons > 1)
                return "softmax";
            else
                return "sigmoid";
        return "linear";
    }


    // Pour utiliser call nn.createNeuron("123","0","input","sotfmax")
//    Create 1 neuron
    @Procedure(name = "nn.createNeuron",mode = Mode.WRITE)
    @Description("")
    public Stream<CreateResult> createNeuron(@Name("id") String id,
                                       @Name("layer") String layer,
                                       @Name("type") String type,
                                       @Name("activation_function") String activation_function
    ) {
        try (Transaction tx = db.beginTx()) {

          tx.execute("CREATE (n:Neuron {\n" +
                    "id: '" + id + "',\n" +
                    "layer:" + layer + ",\n" +
                    "type: '" + type + "',\n" +
                    "bias: 0.0,\n" +
                    "output: null,\n" +
                    "m_bias: 0.0,\n" +
                    "v_bias: 0.0,\n" +
                    "activation_function:'" + activation_function + "'\n" +
                    "})");
            tx.commit();
            return Stream.of(new CreateResult("ok"));

        } catch (Exception e) {

            return Stream.of(new CreateResult("ko"));
        }
    }
    @Procedure(name = "nn.createRelationShipsNeuron",mode = Mode.WRITE)
    @Description("")
    public Stream<CreateResult> createRelationShipsNeuron(
            @Name("from_id") String from_id,
            @Name("to_id") String to_id,
             @Name("weight") String weight
    ) {
        try (Transaction tx = db.beginTx()) {

            tx.execute(
            "MATCH (n1:Neuron" + "{id:'"+ from_id +"'})\n" +
            "MATCH (n2:Neuron" + "{id:'"+ to_id +"'})\n" +
            "CREATE (n1)-[:CONNECTED_TO {weight:" + weight + "}]->(n2)"
            );
            tx.commit();
            return Stream.of(new CreateResult("ok"));

        } catch (Exception e) {

            return Stream.of(new CreateResult("ko"));
        }


    }

/////////////////////////// Helper functions ///////////////////////////


    // Fonction pour supprimer tous les neuronnes existants
    private void clearExistingNetwork() {
        try (Transaction tx = db.beginTx()) {
            tx.execute("MATCH (n:Neuron) DETACH DELETE n");
            tx.commit();
        }
    }

//    fonction pour generer des poids random
    @NotNull
    @Contract(pure = true)
    private static double generateWeight(long numNeuronsCurrent, long numNeuronsNext) {
        Random random = new Random();
        double lowerBound = -Math.sqrt(6.0) / Math.sqrt(numNeuronsCurrent + numNeuronsNext);
        double upperBound = Math.sqrt(6.0) / Math.sqrt(numNeuronsCurrent + numNeuronsNext);
        return lowerBound + (upperBound - lowerBound) * random.nextDouble();
    }
    public static class CreateResult {

        public final String result;

        public CreateResult(String result) {
            this.result = result;
        }
    }
}