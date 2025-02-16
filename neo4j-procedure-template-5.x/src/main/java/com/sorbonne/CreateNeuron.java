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


/////////////////////////////////// Procedures ///////////////////////////////////////////

//    call nn.createNetwork([3,2,3], "classification", "relu", "sigmoid")
    @Procedure(name = "nn.createNetwork",mode = Mode.WRITE)
    @Description("Create All Neurons and Connections")
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

        try {
            // call function to create all neurons
            createAllNeurons(network_structure, task_type, hidden_activation, output_activation);
            // call function to create all neurons
            createAllRelations(network_structure, task_type, hidden_activation, output_activation);
        }  catch (Exception e) {
        log.error("Error creating neuron or relationships: ", e);
        return Stream.of(new CreateResult("ko"));
    }

        return Stream.of(new CreateResult("ok"));
    }


//    Create 1 neuron - Pour utiliser call nn.createNeuron("123","0","input","sotfmax")
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

/////////////////////////// Function to Create Neurons and Connections /////////////////////


    @NotNull
    @Contract(pure = true)
    public Stream<CreateResult> createAllNeurons(@Name("network_structure") List<Long> network_structure,
                                                 @Name("task_type") String task_type,
                                                 @Name("hidden_activation") String hidden_activation,
                                                 @Name("output_activation") String output_activation
    ) {
        try {
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
            log.error("Error creating neurons:", e);
            return Stream.of(new CreateResult("ko"));
        }
        log.info("Finished creating the network Neurons.");
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


    @NotNull
    @Contract(pure = true)
    public Stream<CreateResult> createAllRelations(@Name("network_structure") List<Long> network_structure,
                                                   @Name("task_type") String task_type,
                                                   @Name("hidden_activation") String hidden_activation,
                                                   @Name("output_activation") String output_activation

    ) {
        try {
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
            log.error("Error creating Connections between neurons : ", e);
            return Stream.of(new CreateResult("ko"));
        }
        log.info("Finished creating the network Connections.");
        return Stream.of(new CreateResult("ok"));
    }

    ///////////////////////// Compute Functions ////////////////////////////

    public double compute_loss(@Name("task_type") String task_type) {
        Transaction tx = db.beginTx();
        Result result;

        try{
            if (task_type.equals("Classification")){
//                # Cross-Entropy Loss for classification
                result = tx.execute("""
                    MATCH (output:Neuron {type: 'output'})
                    MATCH (output)-[outputsValues_R:CONTAINS]->(row_for_outputs:Row {type: 'outputsRow'})
                    WITH outputsValues_R,
                         COALESCE(outputsValues_R.output, 0) AS predicted,
                         COALESCE(outputsValues_R.expected_output, 0) AS actual,
                         1e-10 AS epsilon
                    RETURN SUM(
                        -actual * LOG(predicted + epsilon) - (1 - actual) * LOG(1 - predicted + epsilon)
                    ) AS loss
                """);
            } else if (task_type.equals("regression")){
//                Mean Squared Error (MSE) for regression
                result = tx.execute("""
                    MATCH (output:Neuron {type: 'output'})
                    MATCH (output)-[outputsValues_R:CONTAINS]->(row_for_outputs:Row {type: 'outputsRow'})
                    WITH outputsValues_R,
                         COALESCE(outputsValues_R.output, 0) AS predicted,
                         COALESCE(outputsValues_R.expected_output, 0) AS actual
                    RETURN AVG((predicted - actual)^2) AS loss
                """
                );}
            else {
                throw new IllegalArgumentException("Invalid task type. Supported types are 'classification' and 'regression'.");
            }

        }catch (Exception e) {
            log.error("Error creating create_inputs_row_node: ", e);
            throw new IllegalArgumentException("Error while computing loss function");
        }
        Map<String, Object> record = result.next();
        tx.commit();
        return record.getOrDefault("loss", 0.0) instanceof Double
                ? (Double) record.get("loss")
                : 0.0;
    }

    ///////////////////////// Initialisation ////////////////////////////
    public Stream<CreateResult> create_inputs_row_node(@Name("network_structure") List<Long> network_structure,
                                                        @Name("batch_size") Long batch_size)
    {
        Transaction tx = db.beginTx();
        try{
            for (int row_index = 0; row_index < batch_size; row_index++) {
                tx.execute("CREATE (n:Row {\n" +
                        "id: '" + row_index + "',\n" +
                        "type: 'inputsRow'\n" +
                        "})");
                tx.commit();
            }
            long layer_index = 0;
            long num_neurons = network_structure.get(0);
            for (int row_index = 0; row_index < batch_size; row_index++) {
                for (int neuron_index = 0; neuron_index < num_neurons; neuron_index++){
                    String property_name = String.format("X_%d_%d", row_index, neuron_index);
                    String from_id = String.format("%d", row_index);
                    String to_id = String.format("%d-%d", layer_index, neuron_index);
                    String input_feature_id = String.format("%d_%d", row_index, neuron_index);
                    String value = "0";
                    tx.execute(
                            "MATCH (n1:Row {id:'"+ from_id +"',type:'inputsRow'})\n" +
                                    "MATCH (n2:Neuron {id:'"+ to_id +"',type:'input'})\n" +
                                    "CREATE (n1)-[:CONTAINS {output:'" + value + "',id:'"+ input_feature_id +"'}]->(n2)"
                    );
                    tx.commit();
                }
            }
        }  catch (Exception e) {
            log.error("Error creating create_inputs_row_node: ", e);
            return Stream.of(new CreateResult("ko"));
        }
        return Stream.of(new CreateResult("create_inputs_row_node success"));
    }

    public Stream<CreateResult> create_outputs_row_node(@Name("network_structure") List<Long> network_structure,
                                                         @Name("batch_size") Long batch_size)
    {
        Transaction tx = db.beginTx();
        try{
            for (int row_index = 0; row_index < batch_size; row_index++) {
                tx.execute("CREATE (n:Row {\n" +
                        "id: '" + row_index + "',\n" +
                        "type: 'inputsRow'\n" +
                        "})");
                tx.commit();
            }
            long layer_index = network_structure.size() - 1;
            long num_neurons = network_structure.get(network_structure.size() - 1);
            for (int row_index = 0; row_index < batch_size; row_index++) {
                for (int neuron_index = 0; neuron_index < num_neurons; neuron_index++){
                    String property_name = String.format("Y_%d_%d", row_index, neuron_index);
                    String from_id = String.format("%d-%d", layer_index, neuron_index);
                    String to_id = String.format("%d", row_index);
                    String output_feature_id = String.format("%d_%d", row_index, neuron_index);
                    String value = "0";
                    tx.execute(
                            "MATCH (n1:Neuron {id:'"+ from_id +"',type:'output'})\n" +
                                    "MATCH (n2:Row {id:'"+ to_id +"',type:'outputsRow'})\n" +
                                    "CREATE (n1)-[:CONTAINS {output:'" + value + "',id:'"+ output_feature_id +"'}]->(n2)"
                    );
                    tx.commit();
                }
            }
        }  catch (Exception e) {
            log.error("Error creating create_inputs_row_node: ", e);
            return Stream.of(new CreateResult("ko"));
        }
        return Stream.of(new CreateResult("create_outputs_row_node success"));
    }


/////////////////////////// Pass function /////////////////////////////


    public Stream<CreateResult> forward_pass() {
        Transaction tx = db.beginTx();
        try {
            tx.execute("""
                        MATCH (row_for_inputs:Row {type: 'inputsRow'})-[inputsValue_R:CONTAINS]->(input:Neuron {type: 'input'})
                                   MATCH (input)-[r1:CONNECTED_TO]->(hidden:Neuron {type: 'hidden'})
                                   MATCH (hidden)-[r2:CONNECTED_TO]->(output:Neuron {type: 'output'})
                                   MATCH (output)-[outputsValues_R:CONTAINS]->(row_for_outputs:Row {type: 'outputsRow'})
                                   WITH DISTINCT row_for_inputs,inputsValue_R, input,r1,hidden,r2,output ,outputsValues_R,row_for_outputs,
                                  \s
                                   SUM(COALESCE(outputsValues_R.output, 0) * r1.weight) AS weighted_sum
                                   SKIP 0 LIMIT 1000
                                   SET hidden.output = CASE\s
                                       WHEN hidden.activation_function = 'relu' THEN CASE WHEN (weighted_sum + hidden.bias) > 0 THEN (weighted_sum + hidden.bias) ELSE 0 END
                                       WHEN hidden.activation_function = 'sigmoid' THEN 1 / (1 + EXP(-(weighted_sum + hidden.bias)))
                                       WHEN hidden.activation_function = 'tanh' THEN (EXP(2 * (weighted_sum + hidden.bias)) - 1) / (EXP(2 * (weighted_sum + hidden.bias)) + 1)
                                       ELSE weighted_sum + hidden.bias
                                   END
                        		
                                WITH row_for_inputs,inputsValue_R, input,r1,hidden,r2,output ,outputsValues_R,row_for_outputs,
                                SUM(COALESCE(hidden.output, 0) * r2.weight) AS weighted_sum
                                   SET outputsValues_R.output = CASE\s
                                       WHEN output.activation_function = 'softmax' THEN weighted_sum  //Temporary value; softmax applied later
                                       WHEN output.activation_function = 'sigmoid' THEN 1 / (1 + EXP(-(weighted_sum + output.bias)))
                                       WHEN output.activation_function = 'tanh' THEN (EXP(2 * (weighted_sum + output.bias)) - 1) / (EXP(2 * (weighted_sum + output.bias)) + 1)
                                       ELSE weighted_sum + output.bias
                                   END
                                WITH COLLECT(output) AS output_neurons, COLLECT(outputsValues_R) AS outputsValues_Rs
                                      WITH output_neurons, outputsValues_Rs,
                                           [n IN outputsValues_Rs | exp(COALESCE(n.output, 0))] AS exp_outputs,
                                           [n IN output_neurons | n.activation_function] AS activation_functions
                                      WITH output_neurons, outputsValues_Rs, exp_outputs, activation_functions,\s
                                           REDUCE(sum = 0.0, x IN exp_outputs | sum + x) AS sum_exp_outputs
                                      UNWIND RANGE(0, SIZE(output_neurons) - 1) AS i
                                      UNWIND RANGE(0, SIZE(outputsValues_Rs) - 1) AS j
                                      WITH output_neurons[i] AS neuron,outputsValues_Rs[j] AS outputRow, exp_outputs[i] AS exp_output,\s
                                           activation_functions[i] AS activation_function, sum_exp_outputs
                                      WITH neuron,outputRow,\s
                                           CASE\s
                                               WHEN activation_function = 'softmax' THEN exp_output / sum_exp_outputs
                                               ELSE outputRow.output
                                           END AS adjusted_output
                                      SET outputRow.output = adjusted_output
                    """);
            tx.commit();
        } catch (Exception e) {
            log.error("Error creating Connections between neurons : ", e);
            return Stream.of(new CreateResult("ko"));
        }
        return Stream.of(new CreateResult("Forward pass completed successfully"));
    }


        public Stream<CreateResult> backward_pass_adam(@Name("learning_rate") Double learning_rate,
                                                   @Name("beta1") Double beta1,
                                                   @Name("beta1") Double beta2,
                                                   @Name("epsilon") Double epsilon,
                                                   @Name("t") Long t) {
        Transaction tx = db.beginTx();

        try  {
//            # Step 1: Update output layer
            String cypherQuery = """
                    MATCH (output:Neuron {type: 'output'})<-[r:CONNECTED_TO]-(prev:Neuron)
            MATCH (output)-[outputsValues_R:CONTAINS]->(row_for_outputs:Row {type: 'outputsRow'})
            WITH DISTINCT output,r,prev,outputsValues_R,row_for_outputs,
                    CASE
            WHEN output.activation_function = 'softmax' THEN outputsValues_R.output - outputsValues_R.expected_output
            WHEN output.activation_function = 'sigmoid' THEN (outputsValues_R.output - outputsValues_R.expected_output) * outputsValues_R.output * (1 - outputsValues_R.output)
            WHEN output.activation_function = 'tanh' THEN (outputsValues_R.output - outputsValues_R.expected_output) * (1 - outputsValues_R.output^2)
            ELSE outputsValues_R.output - outputsValues_R.expected_output  //For linear activation
            END AS gradient,
                    $t AS t
            MATCH (prev)-[r:CONNECTED_TO]->(output)
                    SET r.m = $beta1 * COALESCE(r.m, 0) + (1 - $beta1) * gradient * COALESCE(prev.output, 0)
            SET r.v = $beta2 * COALESCE(r.v, 0) + (1 - $beta2) * (gradient * COALESCE(prev.output, 0))^2
            SET r.weight = r.weight - $learning_rate * (r.m / (1 - ($beta1 ^ t))) /
                    (SQRT(r.v / (1 - ($beta2 ^ t))) + $epsilon)
            SET output.m_bias = $beta1 * COALESCE(output.m_bias, 0) + (1 - $beta1) * gradient
            SET output.v_bias = $beta2 * COALESCE(output.v_bias, 0) + (1 - $beta2) * (gradient^2)
            SET output.bias = output.bias - $learning_rate * (output.m_bias / (1 - ($beta1 ^ t))) /
                    (SQRT(output.v_bias / (1 - ($beta2 ^ t))) + $epsilon)
            SET output.gradient = gradient
                """.replace("$beta1", String.valueOf(beta1))
                    .replace("$beta2", String.valueOf(beta2))
                    .replace("$epsilon", String.valueOf(epsilon))
                    .replace("$t", String.valueOf(t));
            tx.execute(cypherQuery);
            tx.commit();
        } catch (Exception e) {
            log.error("Error forward pass : ", e);
            return Stream.of(new CreateResult("ko"));
        }

//        # Step 2: Update hidden layers
        try {
            String cypherQuery = """
                    MATCH (n:Neuron {type: 'hidden'})<-[:CONNECTED_TO]-(next:Neuron)
                           WITH n, next, $t AS t
                       MATCH (n)-[r:CONNECTED_TO]->(next)
                       WITH n, SUM(next.gradient * COALESCE(r.weight, 0)) AS raw_gradient, t
                       WITH n,
                            CASE\s
                                WHEN n.activation_function = 'relu' THEN CASE WHEN n.output > 0 THEN raw_gradient ELSE 0 END
                                WHEN n.activation_function = 'sigmoid' THEN raw_gradient * n.output * (1 - n.output)
                                WHEN n.activation_function = 'tanh' THEN raw_gradient * (1 - n.output^2)
                                ELSE raw_gradient  // For linear activation
                            END AS gradient, t
                       MATCH (prev:Neuron)-[r_prev:CONNECTED_TO]->(n)
                       SET r_prev.m = $beta1 * COALESCE(r_prev.m, 0) + (1 - $beta1) * gradient * COALESCE(prev.output, 0)
                       SET r_prev.v = $beta2 * COALESCE(r_prev.v, 0) + (1 - $beta2) * (gradient * COALESCE(prev.output, 0))^2
                       SET r_prev.weight = r_prev.weight - $learning_rate * (r_prev.m / (1 - ($beta1 ^ t))) /\s
                                           (SQRT(r_prev.v / (1 - ($beta2 ^ t))) + $epsilon)
                       SET n.m_bias = $beta1 * COALESCE(n.m_bias, 0) + (1 - $beta1) * gradient
                       SET n.v_bias = $beta2 * COALESCE(n.v_bias, 0) + (1 - $beta2) * (gradient^2)
                       SET n.bias = n.bias - $learning_rate * (n.m_bias / (1 - ($beta1 ^ t))) /\s
                                    (SQRT(n.v_bias / (1 - ($beta2 ^ t))) + $epsilon)
                       SET n.gradient = gradient
                    """.replace("$beta1", String.valueOf(beta1))
                    .replace("$beta2", String.valueOf(beta2))
                    .replace("$epsilon", String.valueOf(epsilon))
                    .replace("$t", String.valueOf(t));
            tx.execute(cypherQuery);
            tx.commit();
        } catch (Exception e) {
            log.error("Error forward pass : ", e);
            return Stream.of(new CreateResult("ko"));
        }

        return Stream.of(new CreateResult("Back pass completed successfully"));
    }

/////////////////////////// Initialisation Function ////////////////////
    @NotNull
    @Contract(pure = true)

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