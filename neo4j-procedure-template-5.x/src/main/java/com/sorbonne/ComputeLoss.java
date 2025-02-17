package com.sorbonne;

import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.*;
import java.util.stream.Stream;
public class ComputeLoss {
    @Context
    public Log log;
    @Context
    public GraphDatabaseService db;

    @Procedure(name = "nn.classification_loss",mode = Mode.WRITE)
    @Description("")
    public void backward_pass_adam() {

    }
}