package com.sorbonne;

import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.*;
import java.util.stream.Stream;
public class backwardPassAdam {
    @Context
    public Log log;
    @Context
    public GraphDatabaseService db;

    @Procedure(name = "nn.backward_pass_adam",mode = Mode.WRITE)
    @Description("")
    public void backward_pass_adam(@Name("learning_rate") String learning_rate,
                                                    @Name("beta1") String beta1,
                                                    @Name("beta2") String beta2,
                                                    @Name("epsilon") String epsilon,
                                                    @Name("t") String t) {
        try  {
            Transaction tx = db.beginTx();
//            # Step 1: Update output layer
            tx.execute("""
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
                """, Map.of(
                    "learning_rate", Double.parseDouble(learning_rate),
                    "beta1", Double.parseDouble(beta1),
                    "beta2", Double.parseDouble(beta2),
                    "epsilon", Double.parseDouble(epsilon),
                    "t", Double.parseDouble(t)
            ));
            tx.commit();
        } catch (Exception e) {
            log.error("Error backward pass : ", e);
            throw e;
        }

//        # Step 2: Update hidden layers
        try {
            Transaction tx = db.beginTx();

            tx.execute("""
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
                    """, Map.of(
                    "learning_rate", Double.parseDouble(learning_rate),
                    "beta1", Double.parseDouble(beta1),
                    "beta2", Double.parseDouble(beta2),
                    "epsilon", Double.parseDouble(epsilon),
                    "t", Double.parseDouble(t)
            ));
            tx.commit();
        } catch (Exception e) {
            log.error("Error backward pass : ", e);
            throw e;
        }
    }

}
