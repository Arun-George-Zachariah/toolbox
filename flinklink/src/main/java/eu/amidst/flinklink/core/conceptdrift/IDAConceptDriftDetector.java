/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */
package eu.amidst.flinklink.core.conceptdrift;

import eu.amidst.core.conceptdrift.utils.GaussianHiddenTransitionMethod;
import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.Attributes;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.distribution.Normal;
import eu.amidst.core.learning.parametric.bayesian.utils.PlateuIIDReplication;
import eu.amidst.core.learning.parametric.bayesian.SVB;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import eu.amidst.flinklink.core.data.DataFlink;
import eu.amidst.flinklink.core.learning.parametric.ParallelVB;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains the functionality for using the concept drift apporoach based on probabilitic graphical models
 * detailed in the following paper,
 *
 * <i> Borchani et al. Modeling concept drift: A probabilistic graphical model based approach. IDA 2015. </i>
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#nbconceptdriftexample"> http://amidst.github.io/toolbox/CodeExamples.html#nbconceptdriftexample </a>  </p>
 *
 */
public class IDAConceptDriftDetector {

    /** Represents the drift detection mode. Only the global mode is currently provided.*/
    public enum DriftDetector {GLOBAL};


    /** Represents the size of the batch used by the {@link SVB} class*/
    int batchSize = 1000;

    /** Represents the variance added when making a transition*/
    double transitionVariance;

    /** Represents the index of the class variable of the classifier*/
    int classIndex = -1;

    /** Represents the drift detection mode. Only the global mode is currently provided.*/
    DriftDetector conceptDriftDetector = DriftDetector.GLOBAL;

    /** Represents the seed of the class*/
    int seed = 0;

    /** Represents the underlying learning engine*/
    ParallelVB svb;

    /** Represents the list of hidden vars modelling concept drift*/
    List<Variable> hiddenVars;

    /** Represents the number of global hidden variables*/
    int numberOfGlobalVars = 1;


    /** Represents the attributes*/
    Attributes attributes;


    /** **/
    DAG globalDAG;


    public DAG getGlobalDAG() {
        return globalDAG;
    }

    public void setAttributes(Attributes attributes) {
        this.attributes = attributes;
    }

    /**
     * Returns the class variable of the classifier
     * @return A <code>Variable</code> object
     */
    public Variable getClassVariable(){
        return this.svb.getLearntBayesianNetwork().getVariables().getVariableById(this.classIndex);
    }

    /**
     * Sets the number of global hidden variables modelling concept drift
     * @param numberOfGlobalVars A positive integer value.
     */
    public void setNumberOfGlobalVars(int numberOfGlobalVars) {
        this.numberOfGlobalVars = numberOfGlobalVars;
    }


    /**
     * Sets which is class variable of the model,
     * @param classIndex, a positive integer defining the index of the class variable.
     */
    public void setClassIndex(int classIndex) {
        this.classIndex = classIndex;
    }

    /**
     * Gets the index of the class variable of the model
     * @return A positive integer value.
     */
    public int getClassIndex(){return classIndex;}

    /**
     * Sets the batch size of the concept drift detection model
     * @param batchSize, a positive integer value
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * Sets the transition variance of the concept drift detection model
     * @param transitionVariance, a positive double value
     */
    public void setTransitionVariance(double transitionVariance) {
        this.transitionVariance = transitionVariance;
    }

    /**
     * Set the seed of the class
     * @param seed, an integer value
     */
    public void setSeed(int seed) {
        this.seed = seed;
    }

    /**
     * Retuns the SVB learningn engine
     * @return A <code>SVB</code> object.
     */
    public ParallelVB getSvb() {
        return svb;
    }

    /**
     * Builds the DAG structure of a Naive Bayes classifier with a global hidden Gaussian variable.
     */
    private void buildGlobalDAG(){
        Variables variables = new Variables(attributes);
        String className = attributes.getFullListOfAttributes().get(classIndex).getName();
        hiddenVars = new ArrayList<Variable>();

        for (int i = 0; i < this.numberOfGlobalVars ; i++) {
            hiddenVars.add(variables.newGaussianVariable("GlobalHidden_"+i));
        }

        Variable classVariable = variables.getVariableByName(className);

        this.globalDAG = new DAG(variables);

        for (Attribute att : attributes.getListOfNonSpecialAttributes()) {
            if (att.getName().equals(className))
                continue;

            Variable variable = variables.getVariableByName(att.getName());
            globalDAG.getParentSet(variable).addParent(classVariable);
            for (int i = 0; i < this.numberOfGlobalVars ; i++) {
                globalDAG.getParentSet(variable).addParent(hiddenVars.get(i));
            }
        }

        System.out.println(globalDAG.toString());


    }

    /**
     * Initialises the class for concept drift detection.
     */
    public void initLearning() {
        if (classIndex == -1)
            classIndex = attributes.getNumberOfAttributes()-1;


        switch (this.conceptDriftDetector){
            case GLOBAL:
                this.buildGlobalDAG();
                break;
        }


        svb = new ParallelVB();
        svb.setSeed(this.seed);
        svb.setPlateuStructure(new PlateuIIDReplication(hiddenVars));
        GaussianHiddenTransitionMethod gaussianHiddenTransitionMethod = new GaussianHiddenTransitionMethod(hiddenVars, 0, this.transitionVariance);
        gaussianHiddenTransitionMethod.setFading(1.0);
        svb.setTransitionMethod(gaussianHiddenTransitionMethod);
        svb.setBatchSize(this.batchSize);
        svb.setDAG(globalDAG);
        svb.setIdenitifableModelling(new IdentifiableIDAModel());

        svb.setOutput(false);
        svb.setMaximumGlobalIterations(100);
        svb.setMaximumLocalIterations(100);
        svb.setGlobalThreshold(0.001);
        svb.setLocalThreshold(0.001);

        svb.initLearning();
    }


    public double[] updateModelWithNewTimeSlice(DataFlink<DataInstance> data){

        svb.updateModel(data);

        double[] out = new double[hiddenVars.size()];
        for (int i = 0; i < out.length; i++) {
            Variable hiddenVar = this.hiddenVars.get(i);
            Normal normal = svb.getParameterPosterior(hiddenVar);
            out[i] = normal.getMean();
        }
        return out;
    }

    /**
     * Returns the list of global hidden variables
     * @return A list of <code>Variable</code> objects
     */
    public List<Variable> getHiddenVars() {
        return hiddenVars;
    }

    /**
     * Returns the Dynamic Bayesian network learnt with the concept drift adaptation method.
     * @return A <code>DynamicBayesianNetwork</code> object.
     */
    public BayesianNetwork getLearntDynamicBayesianNetwork(){
        return svb.getLearntBayesianNetwork();
    }


}
